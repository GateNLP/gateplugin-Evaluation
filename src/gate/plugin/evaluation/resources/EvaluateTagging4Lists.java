/*
 *  Copyright (c) 1995-2015, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 */
package gate.plugin.evaluation.resources;

import gate.plugin.evaluation.api.ContainmentType;
import gate.plugin.evaluation.api.NilTreatment;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.ImmutableAnnotationSetImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.CustomDuplication;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.evaluation.api.AnnotationDifferTagging;
import gate.plugin.evaluation.api.AnnotationDifferTagging.CandidateList;
import gate.plugin.evaluation.api.ByThEvalStatsTagging;
import gate.plugin.evaluation.api.ContingencyTableInteger;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.api.EvalStatsTaggingMacro;
import gate.plugin.evaluation.api.FeatureComparison;
import gate.plugin.evaluation.api.ThresholdsToUse;
import gate.util.Files;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 *
 * @author Johann Petrak
 */
@CreoleResource(
        name = "EvaluateTagging4Lists",
        helpURL ="https://github.com/johann-petrak/gateplugin-Evaluation/wiki/EvaluateTagging-PR",
        comment = "Calculate P/R/F evalutation measures for annotations with candidate lists")
public class EvaluateTagging4Lists extends EvaluateTaggingBase implements ControllerAwarePR, CustomDuplication {
  
  ///////////////////
  /// PR PARAMETERS 
  ///////////////////
  
  

  public String edgeName;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setEdgeName(String value) { edgeName = value; }
  public String getEdgeName() { return edgeName; }
  public String getExpandedEdgeName() { return Utils.replaceVariablesInString(getEdgeName()); }
  
  
  // TODO: maybe add field to specify your own thresholds?
  
  //////////////////// 
  // PR METHODS 
  ///////////////////

  @Override
  public Resource init() {    
    return this;
  }

  
  @Override
  public void reInit() {
    init();
  }
  
  @Override
  public void cleanup() {
    //
  }
  
  
  // fields shared between the execute method and the methods for initializing and finalization
  protected Map<String,EvalStatsTagging> allDocumentsStats;
  protected Map<String,EvalStatsTagging> allDocumentsReferenceStats = null;
  
  protected ContingencyTableInteger correctnessTableStrict;
  protected ContingencyTableInteger correctnessTableLenient;

  
  // This will be initialized at the start of the run and be incremented in the AnnotationDifferTagging
  // for each document.
  // This stores, for each type, the ByThEvalStatsTagging object for that type. The empty string
  // is used for the object that has the values over all types combined.
  protected Map<String,ByThEvalStatsTagging> evalStatsByThreshold;
  
  
  String expandedEdgeName;
  
  protected static final Logger logger = Logger.getLogger(EvaluateTagging4Lists.class);
  
  @Override
  public void execute() {
    
    if(needInitialization) {
      needInitialization = false;
      initializeForRunning();
    }
    
    // Per document initialization
    
    // Prepare the annotation sets
    
    
    // run the whole thing once for each type and also for the sets where all the specified types
    // are contained.
    
    // First do it for all types together, but only if more than one type was specified
    AnnotationSet keySet = null;
    AnnotationSet responseSet = null;
    AnnotationSet referenceSet = null;
    Set<String> typeSet = new HashSet<String>();
    Set<String> typeSet4ListAnns = new HashSet<String>();
    
    if(getAnnotationTypes().size() > 1) {
      // TODO: more flexible annotation types!
        for(String t : getAnnotationTypes()) {
          typeSet4ListAnns.add(t+"List");
        }
      typeSet.addAll(getAnnotationTypes());
      keySet = document.getAnnotations(expandedKeySetName).get(typeSet);
        responseSet = document.getAnnotations(expandedResponseSetName).get(typeSet4ListAnns);
      if(!expandedReferenceSetName.isEmpty()) {
          referenceSet = document.getAnnotations(expandedReferenceSetName).get(typeSet4ListAnns);
      }
      evaluateForType(keySet,responseSet,referenceSet,"");
    }
    // now do it for each type seperately
    for(String type : getAnnotationTypes()) {
      keySet = document.getAnnotations(expandedKeySetName).get(type);
      String origType = type;
      // TODO: THIS WAS DONE BEFORE, BUT SHOULD NOT BE NECESSARY ANY MORE!!
      // type = type + "List";
      responseSet = document.getAnnotations(expandedResponseSetName).get(type);
      if(!expandedReferenceSetName.isEmpty()) {
        referenceSet = document.getAnnotations(expandedReferenceSetName).get(type);
      }
      evaluateForType(keySet,responseSet,referenceSet,origType);      
    }
    
    
  }
  
  // TODO: need to allow for key and response types, and for lists, list element types too!
  protected void evaluateForType(
          AnnotationSet keySet, AnnotationSet responseSet, AnnotationSet referenceSet,
          String type) {
    AnnotationSet containingSet = null;
    String containingSetName = "";
    String containingType = "";
    if(!expandedContainingNameAndType.isEmpty()) {
      String[] setAndType = expandedContainingNameAndType.split(":",2);
      if(setAndType.length != 2 || setAndType[0].isEmpty() || setAndType[1].isEmpty()) {
        throw new GateRuntimeException("Runtime Parameter containingASAndName not of the form setname:typename");
      }      
      containingSetName = setAndType[0];
      containingType = setAndType[1];
      containingSet = document.getAnnotations(setAndType[0]).get(setAndType[1]);
      // now filter the keys and responses. If the containing set/type is the same as the key set/type,
      // do not filter the keys.
      ContainmentType ct = containmentType;
      if(ct == null) ct = ContainmentType.OVERLAPPING;
      responseSet = selectOverlappingBy(responseSet,containingSet,ct);
      // TODO: at the moment this will never be true since we have changed the single type to a list
      // of types. Think about when to not do this ...
      if(containingSetName.equals(expandedKeySetName) && containingType.equals(annotationTypes)) {
        // no need to do anything for the key set
      } else {
        keySet = selectOverlappingBy(keySet,containingSet,ct);
      }
      // if we have a reference set, we need to apply the same filtering to that one too
      if(referenceSet != null) {
        referenceSet = selectOverlappingBy(referenceSet,containingSet,ct);
      }
    } // have a containing set and type
    
    
    // TODO: to get the best candidate we need to have the candidates already sorted!
    // So we should better do the evaluation over the top element after the candidate lists 
    // have been created and we should refactor things so that creating the candidate lists
    // is a separate step!
    // Then we create the candidate lists here, then pass the already created candidate lists
    // to the static method for calculating the lists evaluation!
    AnnotationSet listAnns = null;
    AnnotationSet listAnnsReference = null;
    List<CandidateList> candList = null;
      listAnns = responseSet;
      listAnnsReference = referenceSet;
      candList = 
              AnnotationDifferTagging.createCandidateLists(
                      document.getAnnotations(expandedResponseSetName),
                      listAnns, expandedEdgeName, expandedScoreFeatureName);
      // get the highest scored annotation from each list
      responseSet = new AnnotationSetImpl(listAnns.getDocument());
      if(referenceSet != null) {
        referenceSet = new AnnotationSetImpl(listAnnsReference.getDocument());
      }
      for(CandidateList cl : candList) {
        responseSet.add(cl.get(0));
      }
    
    // Now depending on the NIL processing strategy, do something with those annotations which 
    // are identified as nil in the key and response sets.
    // NO_NILS: do nothing, all keys and responses are taken as is. If there are special NIL
    //   values in the key set, the responses must match them like any other value. Parameter nilValue
    //   is ignored here.
    // NIL_IS_ABSENT:
    //   In this case, all key and response annotation which are NIL are removed before the 
    //   evaluation is carried out. 
    // NIL_CLUSTERS: 
    //   In this case, a missing response does not equal a key nil, because we need to provide
    //   a label to be correct. Parameter nilValue is used so we know which keys and responses 
    //   are nils.
    //   We match all non-NILs in the usual way, ignoring all the NILS both in the key and 
    //   response sets. We accumulate all the NIL annotations over all documents and after all 
    //   documents have been processed, we try to find an optimal assignment between them, based
    //   on the NIL labels. 
    //   TODO!!!
    
    // Nils can only be represented if there is an id feature. If there is one and we treat
    // nils as absent, lets remove all the nils.
    
    if(getFeatureNames() != null && getFeatureNames().size() > 0 && getNilTreatment().equals(NilTreatment.NIL_IS_ABSENT)) {
      removeNilAnns(keySet);
      removeNilAnns(responseSet);
      if(referenceSet != null) {
        removeNilAnns(referenceSet);
      }
    }
    
    
    AnnotationDifferTagging docDiffer = new AnnotationDifferTagging(
            keySet,
            responseSet,
            featureSet,
            featureComparison,
            null // TODO CHECK should not need the annotation type specs here!
    );
    EvalStatsTagging es = docDiffer.getEvalStatsTagging();

      ByThEvalStatsTagging bth = evalStatsByThreshold.get(type);
      AnnotationDifferTagging.calculateListByThEvalStatsTagging(
              keySet,
              document.getAnnotations(expandedResponseSetName),
              candList, featureSet, featureComparison, 
              expandedEdgeName, expandedScoreFeatureName, bth.getWhichThresholds(), bth);      
    
    // Store the counts and measures as document feature values
    FeatureMap docFm = document.getFeatures();
    String featurePrefixResponseT = featurePrefixResponse;
    if(type.isEmpty()) {
      featurePrefixResponseT += "[ALL].";
    } else {
      featurePrefixResponseT += type;
    }
    docFm.put(featurePrefixResponseT+"FMeasureStrict", es.getFMeasureStrict(1.0));
    docFm.put(featurePrefixResponseT+"FMeasureLenient", es.getFMeasureLenient(1.0));
    docFm.put(featurePrefixResponseT+"PrecisionStrict", es.getPrecisionStrict());
    docFm.put(featurePrefixResponseT+"PrecisionLenient", es.getPrecisionLenient());
    docFm.put(featurePrefixResponseT+"RecallStrict", es.getRecallStrict());
    docFm.put(featurePrefixResponseT+"RecallLenient", es.getRecallLenient());
    docFm.put(featurePrefixResponseT+"SingleCorrectAccuracyStrict", es.getSingleCorrectAccuracyStrict());
    docFm.put(featurePrefixResponseT+"SingleCorrectAccuracyLenient", es.getSingleCorrectAccuracyLenient());
    docFm.put(featurePrefixResponseT+"CorrectStrict", es.getCorrectStrict());
    docFm.put(featurePrefixResponseT+"CorrectPartial", es.getCorrectPartial());
    docFm.put(featurePrefixResponseT+"IncorrectStrict", es.getIncorrectStrict());
    docFm.put(featurePrefixResponseT+"IncorrectPartial", es.getIncorrectPartial());
    docFm.put(featurePrefixResponseT+"TrueMissingStrict", es.getTrueMissingStrict());
    docFm.put(featurePrefixResponseT+"TrueMissingLenient", es.getTrueMissingLenient());
    docFm.put(featurePrefixResponseT+"TrueSpuriousStrict", es.getTrueSpuriousStrict());
    docFm.put(featurePrefixResponseT+"TrueSpuriousLenient", es.getTrueSpuriousLenient());
    docFm.put(featurePrefixResponseT+"Targets", es.getTargets());
    docFm.put(featurePrefixResponseT+"Responses", es.getResponses());
    
    
    logger.debug("DEBUG: type is "+type);
    logger.debug("DEBUG: all document stats types "+allDocumentsStats.keySet());
    allDocumentsStats.get(type).add(es);
    
    // Now if we have parameters to record the matchings, get the information from the docDiffer
    // and create the apropriate annotations.
    AnnotationSet outputAnnotationSet = null;
    if(!outputASResName.isEmpty()) {
      outputAnnotationSet = document.getAnnotations(outputASResName);
      docDiffer.addIndicatorAnnotations(outputAnnotationSet,"");
    }
    
    
    
    // If we have a reference set, also calculate the stats for the reference set
    EvalStatsTagging res = null;
    if(referenceSet != null) {
      AnnotationDifferTagging docRefDiffer = new AnnotationDifferTagging(
              keySet,
              referenceSet,
              featureSet,
              featureComparison,
              null // TODO CHECK should not need the annotation type specs here
      );
      res = docRefDiffer.getEvalStatsTagging();
      allDocumentsReferenceStats.get(type).add(res);
            
      // if we need to record the matchings, also add the annotations for how things changed
      // between the reference set and the response set.
      if(!outputASRefName.isEmpty()) {
        outputAnnotationSet = document.getAnnotations(outputASRefName);
        docRefDiffer.addIndicatorAnnotations(outputAnnotationSet,"");
        // Now add also the annotations that indicate the changes between the reference set and
        // the response set
        outputAnnotationSet = document.getAnnotations(outputASDiffName);
        AnnotationDifferTagging.addChangesIndicatorAnnotations(docDiffer, docRefDiffer, outputAnnotationSet);
      }
      
      // TODO: increment the overall counts of how things changed
      
      AnnotationDifferTagging.addChangesToContingenyTables(docDiffer, docRefDiffer, correctnessTableStrict, correctnessTableLenient);
      
      // add document features for the reference set
      String featurePrefixReferenceT = featurePrefixReference;
      if(type.isEmpty()) {
        featurePrefixReferenceT += "[ALL].";
      } else {
        featurePrefixReferenceT += type;
      }
      docFm.put(featurePrefixReferenceT + "FMeasureStrict", res.getFMeasureStrict(1.0));
      docFm.put(featurePrefixReferenceT + "FMeasureLenient", res.getFMeasureLenient(1.0));
      docFm.put(featurePrefixReferenceT + "PrecisionStrict", res.getPrecisionStrict());
      docFm.put(featurePrefixReferenceT + "PrecisionLenient", res.getPrecisionLenient());
      docFm.put(featurePrefixReferenceT + "RecallStrict", res.getRecallStrict());
      docFm.put(featurePrefixReferenceT + "RecallLenient", res.getRecallLenient());
      docFm.put(featurePrefixReferenceT + "SingleCorrectAccuracyStrict", res.getSingleCorrectAccuracyStrict());
      docFm.put(featurePrefixReferenceT + "SingleCorrectAccuracyLenient", res.getSingleCorrectAccuracyLenient());
      docFm.put(featurePrefixReferenceT + "CorrectStrict", res.getCorrectStrict());
      docFm.put(featurePrefixReferenceT + "CorrectPartial", res.getCorrectPartial());
      docFm.put(featurePrefixReferenceT + "IncorrectStrict", res.getIncorrectStrict());
      docFm.put(featurePrefixReferenceT + "IncorrectPartial", res.getIncorrectPartial());
      docFm.put(featurePrefixReferenceT + "TrueMissingStrict", res.getTrueMissingStrict());
      docFm.put(featurePrefixReferenceT + "TrueMissingLenient", res.getTrueMissingLenient());
      docFm.put(featurePrefixReferenceT + "TrueSpuriousStrict", res.getTrueSpuriousStrict());
      docFm.put(featurePrefixReferenceT + "TrueSpuriousLenient", res.getTrueSpuriousLenient());
      docFm.put(featurePrefixReferenceT + "Targets", res.getTargets());
      docFm.put(featurePrefixReferenceT + "Responses", res.getResponses());
    }
    /* TODO!!!! TEMPORARILY REMOVED SO WE CAN COMPILE THE REST
    if(mainTsvPrintStream != null) {
      // a line for the response stats for that document
      mainTsvPrintStream.println(outputTsvLine(document.getName(), type, expandedResponseSetName, es));
      if(res != null) {
        mainTsvPrintStream.println(outputTsvLine(document.getName(), type, expandedReferenceSetName, res));
      }
    }
    */
  }
  
  
  
  /**
   * Return the evaluation statistics for the given type or over all types if an empty String is
   * passed.
   * If a type name is passed which was not used for the evaluation, null is returned.
   * @param type
   * @return 
   */
  public EvalStatsTagging getEvalStatsTagging(String type) { 
    // if there was only one type specified, then the type-specific evalstats is also the 
    // overall evalstats and we will not have created a separate evalstats for "". In that case,
    // if the overall evalstats are requested, we return the one for the one and only type.
    if(type.equals("") && getAnnotationTypes().size() == 1) {
      return allDocumentsStats.get(getAnnotationTypes().get(0));
    }
    return allDocumentsStats.get(type); 
  }
  
  /**
   * Return the evaluation statistics for the reference set for the given type or over all types if an empty String is
   * passed.
   * If a type name is passed which was not used for the evaluation, null is returned. If no reference
   * set was specified, null is returned.
   * @param type
   * @return 
   */
  public EvalStatsTagging getEvalStatsTaggingReference(String type) {
    if(getReferenceASName() == null || getReferenceASName().isEmpty()) {
      return null;
    }
    // if there was only one type specified, then the type-specific evalstats is also the 
    // overall evalstats and we will not have created a separate evalstats for "". In that case,
    // if the overall evalstats are requested, we return the one for the one and only type.
    if(type.equals("") && getAnnotationTypes().size() == 1) {
      return allDocumentsReferenceStats.get(getAnnotationTypes().get(0));
    }
    return allDocumentsReferenceStats.get(type);     
  }
  
  /**
   * Get the evaluation statistics by threshold for the given type or over all types if an empty
   * String is passed.
   * @param type
   * @return 
   */
  public ByThEvalStatsTagging getByThEvalStatsTagging(String type) {
    // if there was only one type specified, then the type-specific evalstats is also the 
    // overall evalstats and we will not have created a separate evalstats for "". In that case,
    // if the overall evalstats are requested, we return the one for the one and only type.
    if(type.equals("") && getAnnotationTypes().size() == 1) {
      return evalStatsByThreshold.get(getAnnotationTypes().get(0));
    }
    return evalStatsByThreshold.get(type);
  }
  
  
  ////////////////////////////////////////////
  /// HELPER METHODS
  ////////////////////////////////////////////
  
  /**
   * Return a new set with all the NIL annotations removed.
   * @param set 
   */
  private AnnotationSet removeNilAnns(AnnotationSet set) {
    String nilStr = "";
    if(getNilValue() != null) { nilValue = getNilValue(); }
    String idFeature = getFeatureNames().get(0);
    Set<Annotation> nils = new HashSet<Annotation>();
    for (Annotation ann : set) {
      Object val = ann.getFeatures().get(idFeature);
      String valStr = val == null ? "" : val.toString();
      if (valStr.equals(nilStr)) {
        nils.add(ann);
      }
    }
    AnnotationSet newset = new AnnotationSetImpl(set);
    newset.removeAll(nils);
    return newset;
  }
  

  // This needs to run as part of the first execute, since at the moment, the parametrization
  // does not work correctly with the controller callbacks. 
  protected void initializeForRunning() {
    super.initializeForRunning();
    
    expandedEdgeName = getStringOrElse(getExpandedEdgeName(),"");
    
    if(getAnnotationTypes() == null || getAnnotationTypes().isEmpty()) {
      throw new GateRuntimeException("List of annotation types to use is not specified or empty!");
    }
    
    if(getFeatureNames() != null) {
      for(String t : getFeatureNames()) {
        if(t == null || t.isEmpty()) {
          throw new GateRuntimeException("List of feature names to use contains a null or empty type name!");
        }      
      }
    }
      
    List<String> typesPlusEmpty = new ArrayList<String>();
    if(getAnnotationTypes().size() > 1) {
      typesPlusEmpty.add("");
    }

    //create the data structure that hold an evalstats object over all documents for each type 
    allDocumentsStats = new HashMap<String, EvalStatsTagging>();
    
    // if we also have a reference set, create the data structure that holds an evalstats object
    // over all documents for each type. This is left null if no reference set is specified!    
    if(!expandedReferenceSetName.isEmpty()) {
      allDocumentsReferenceStats = new HashMap<String, EvalStatsTagging>();
      correctnessTableStrict = new ContingencyTableInteger(2, 2);
      correctnessTableLenient = new ContingencyTableInteger(2, 2);
      correctnessTableLenient.setName(expandedEvaluationId+"-"+expandedReferenceSetName+"/"+expandedResponseSetName+"(lenient)");
      correctnessTableLenient.setRowLabel(0, expandedReferenceSetName+":correct");
      correctnessTableLenient.setRowLabel(1, expandedReferenceSetName+":wrong");
      correctnessTableLenient.setColumnLabel(0, expandedResponseSetName+":correct");
      correctnessTableLenient.setColumnLabel(1, expandedResponseSetName+":wrong");
      correctnessTableStrict.setName(expandedEvaluationId+"-"+expandedReferenceSetName+"/"+expandedResponseSetName+"(strict)");
      correctnessTableStrict.setRowLabel(0, expandedReferenceSetName+":correct");
      correctnessTableStrict.setRowLabel(1, expandedReferenceSetName+":wrong");
      correctnessTableStrict.setColumnLabel(0, expandedResponseSetName+":correct");
      correctnessTableStrict.setColumnLabel(1, expandedResponseSetName+":wrong");
    }
    
    evalStatsByThreshold = new HashMap<String, ByThEvalStatsTagging>();
    
    
    typesPlusEmpty.addAll(getAnnotationTypes());
    for(String t : typesPlusEmpty) {
      logger.debug("DEBUG: initializing alldocument stats for type "+t);
      allDocumentsStats.put(t,new EvalStatsTagging());
      if(evalStatsByThreshold != null) {
        evalStatsByThreshold.put(t,new ByThEvalStatsTagging(getWhichThresholds()));
      }    
      if(allDocumentsReferenceStats != null) {
        allDocumentsReferenceStats.put(t,new EvalStatsTagging());
      }
      
    }
    
    // If the featureNames list is null, this has the special meaning that the features in 
    // the key/target annotation should be used. In that case the featureNameSet will also 
    // be left null. Otherwise the list will get converted to a set.
    // Convert the feature list into a set
    if(featureNames != null) {
      Set<String> featureNameSet = new HashSet<String>();
      featureNameSet.addAll(featureNames);
      // check if we have duplicate entries in the featureNames
      if(featureNameSet.size() != featureNames.size()) {
        throw new GateRuntimeException("Duplicate feature in the feature name list");
      }
    }
    
    
    // Establish the default containment type if it was not specified. 
    if(getContainmentType() == null) {
      containmentType = ContainmentType.OVERLAPPING;
    }
    if(getNilTreatment() == null) {
      nilTreatment = NilTreatment.NO_NILS;
    }
    
    featurePrefixResponse = initialFeaturePrefixResponse + getResponseASName() + ".";
    featurePrefixReference = initialFeaturePrefixReference + getReferenceASName() + ".";

    mainTsvPrintStream = getOutputStream(null);
    // Output the initial header line
    if(mainTsvPrintStream != null) {
      mainTsvPrintStream.print("evaluationId"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("docName"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("setName"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("annotationType"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.println(EvalStatsTagging.getTSVHeaders());
    }
    /** not used yet
    if(doListEvaluation || doScoreEvaluation) {
      scoreDistPrintStream = getOutputStream("-scores");
    }
    */
  }
  
  
  
  public void finishRunning() {
    outputDefaultResults();
    if(mainTsvPrintStream != null) {
      mainTsvPrintStream.close();    
    }
    /** not used yet
    if(scoreDistPrintStream != null) {
      scoreDistPrintStream.close();
    }
    */
  }
  
  
  // TODO: make this work per type once we collect the tables per type!
  public void outputContingencyTable(PrintStream out, ContingencyTableInteger table) {
    out.println(expandedEvaluationId+" "+table.getName()+"correct/correct: "+table.get(0, 0));
    out.println(expandedEvaluationId+" "+table.getName()+"correct/wrong: "+table.get(0, 1));
    out.println(expandedEvaluationId+" "+table.getName()+"wrong/correct: "+table.get(1, 0));
    out.println(expandedEvaluationId+" "+table.getName()+"wrong/wrong: "+table.get(1, 1));    
  }
  
  
  public void outputDefaultResults() {
    
    // TODO: think of a way of how to add the interpolated precision strict interpolated precision
    // lenient to the by thresholds lines!!!
    /* TODO TEMPORARILY REMOVED 
    // output for each of the types ...
    for(String type : getAnnotationTypes()) {
      //System.out.println("DEBUG: alldocumentsStats="+allDocumentsStats+" type="+type+" expandedResponseSetName="+expandedResponseSetName);
      outputEvalStatsForType(System.out, allDocumentsStats.get(type), type, expandedResponseSetName);
      if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, type, getResponseASName(), allDocumentsStats.get(type))); }
      if(!expandedReferenceSetName.isEmpty()) {
        outputEvalStatsForType(System.out, allDocumentsReferenceStats.get(type), type, expandedReferenceSetName);
        if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, type, expandedReferenceSetName, allDocumentsReferenceStats.get(type))); }
      }
      if(evalStatsByThreshold != null) {
        ByThEvalStatsTagging bthes = evalStatsByThreshold.get(type);
        for(double th : bthes.getByThresholdEvalStats().navigableKeySet()) {
          outputEvalStatsForType(System.out, bthes.get(th), type, expandedResponseSetName);
          if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, type, expandedResponseSetName, bthes.get(th))); }
        }
      }
    }
    // If there was more than one type, also output the summary stats over all types
    if(getAnnotationTypes().size() > 1) {
      outputEvalStatsForType(System.out, allDocumentsStats.get(""), "all(micro)", expandedResponseSetName);
      if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, "", expandedResponseSetName, allDocumentsStats.get(""))); }
      if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
        outputEvalStatsForType(System.out, allDocumentsReferenceStats.get(""), "all(micro)", expandedReferenceSetName);
        if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, "", expandedReferenceSetName, allDocumentsReferenceStats.get(""))); }
      }      
      if(evalStatsByThreshold != null) {
        ByThEvalStatsTagging bthes = evalStatsByThreshold.get("");
        for(double th : bthes.getByThresholdEvalStats().navigableKeySet()) {
          outputEvalStatsForType(System.out, bthes.get(th), "all(micro)", expandedResponseSetName);
          if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, "", expandedResponseSetName, bthes.get(th))); }
        }        
      }
      EvalStatsTaggingMacro esm = new EvalStatsTaggingMacro();
      for(String type : getAnnotationTypes()) {
        esm.add(allDocumentsStats.get(type));
      }
      outputEvalStatsForType(System.out, esm, "all(macro)", expandedResponseSetName);
      if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, "", expandedResponseSetName, esm)); }
      if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
        esm = new EvalStatsTaggingMacro();
        for(String type : getAnnotationTypes()) {
          esm.add(allDocumentsReferenceStats.get(type));
        }
        outputEvalStatsForType(System.out, esm, "all(macro)", expandedReferenceSetName);
        if(mainTsvPrintStream != null) { mainTsvPrintStream.println(outputTsvLine(null, "", expandedReferenceSetName, esm)); }
      }
    }
      
    if(!expandedReferenceSetName.isEmpty()) {
      outputContingencyTable(System.out, correctnessTableStrict);      
      outputContingencyTable(System.out, correctnessTableLenient);
    }
    */

  }
  
  
  ////////////////////////////////////////////
  /// CONTROLLER AWARE PR methods
  ////////////////////////////////////////////
  
  @Override
  public void controllerExecutionStarted(Controller cntrlr) throws ExecutionException {
    needInitialization = true;
  }

  @Override
  public void controllerExecutionFinished(Controller cntrlr) throws ExecutionException {
    // only do anything at all if we had actually been executed once. The callback gets also
    // invoked if the PR was disabled, so we have to check ...
    // needInitialization is set in the started callback and reset in execute, so if it is still
    // on, we never were in execute.
    if(!needInitialization) {
      finishRunning();
      needInitialization = true;
    }
  }

  @Override
  public void controllerExecutionAborted(Controller cntrlr, Throwable thrwbl) throws ExecutionException {
    if(!needInitialization) {
      System.err.println("Processing was aborted: "+thrwbl.getMessage());
      thrwbl.printStackTrace(System.err);
      System.err.println("Here are the summary stats for what was processed: ");
      finishRunning();
      needInitialization = true;
    }
  }

  @Override
  public Resource duplicate(Factory.DuplicationContext dc) throws ResourceInstantiationException {
    throw new UnsupportedOperationException("At the moment, this PR may not be duplicated and must be run single-threaded"); 
    // TODO: duplicate such that all duplicates get a flag set which indicates that they are 
    // duplicates, and only the original has the flag not set.
    // Also, use a shared object to give all PRs access to everyone elses's statistics objects.
    // Also, use a shared obejcts to give all PRs access to a singleton object for writing to
    // the output files.
    // Finally, implement the controllerExecutionFinished() method such that only the original
    // will do the actual summarization: it will access all stats objects from all other PRs and
    // summarize them and 
  }
  
  private static class AnnotationTypeSpec {
    public String keyType = "";
    public String keyElementType = "";
    public String responseType = "";
    public String responseElementType = "";
    public String toString() {
      String k = keyType;
      String r = responseType;
      if(!keyElementType.isEmpty()) {
        k += "["+keyElementType+"]";
      }
      if(!responseElementType.isEmpty()) {
        r += "["+responseElementType+"]";
      }
      if(k.equals(r)) {
        return k;
      } else {
        return k+"|"+r;
      }
    }
  }

  
  
}
