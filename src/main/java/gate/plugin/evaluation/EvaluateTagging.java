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

import gate.plugin.evaluation.api.AnnotationTypeSpec;
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
import gate.creole.ControllerAwarePR;
import gate.creole.CustomDuplication;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.HiddenCreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.evaluation.api.AnnotationDifferTagging;
import gate.plugin.evaluation.api.AnnotationTypeSpecs;
import gate.plugin.evaluation.api.ByThEvalStatsTagging;
import gate.plugin.evaluation.api.ContingencyTableInteger;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.api.EvalStatsTagging4Score;
import gate.plugin.evaluation.api.EvalStatsTaggingMacro;
import gate.plugin.evaluation.api.ThresholdsToUse;
import gate.util.GateRuntimeException;
import java.io.PrintStream;
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
        name = "EvaluateTagging",
        helpURL ="https://github.com/GateNLP/gateplugin-Evaluation/wiki/EvaluateTagging-PR",
        comment = "Calculate P/R/F evalutation measures")
public class EvaluateTagging extends EvaluateTaggingBase
// TODO: need to properly implement this later!
//  implements ControllerAwarePR, CustomDuplication  
{

  public final static long serialVersionUID = 1L;

  ///////////////////
  /// PR PARAMETERS: all the ones common to Tagging and Tagging4Lists are in the TaggingBase class
  ///////////////////

  protected List<String> annotationTypes;
  @CreoleParameter (comment="The annotation types to use for evaluations, at least one type must be given",defaultValue="Mention")
  @RunTime
  public void setAnnotationTypes(List<String> name) { annotationTypes = name; }
  public List<String> getAnnotationTypes() { return annotationTypes; }
  

  
  // maybe not supported for list based PR even if we eventually support it for the normal one?
  public String featureNameNilCluster;
  @CreoleParameter(comment = "(NOT IMPLEMENTED YET) The feature containing the nil cluster name or id", defaultValue = "")
  @RunTime
  @Optional  
  @HiddenCreoleParameter
  public void setFeatureNameNilCluster(String value) { featureNameNilCluster = value; }
  public String getFeatureNameNilCluster() { return featureNameNilCluster; }
  public String getExpandedFeatureNameNilCluster() { return Utils.replaceVariablesInString(getFeatureNameNilCluster()); }
  String expandedFeatureNameNilCluster;
  
  
  protected ThresholdsToUse whichThresholds;
  @CreoleParameter(comment="",defaultValue="USE_ALL")
  @RunTime
  @Optional  
  public void setWhichThresholds(ThresholdsToUse value) { whichThresholds = value; }
  public ThresholdsToUse getWhichThresholds() { return whichThresholds; }

  
  // TODO: maybe separate parameter for user-specified score thresholds which would allow 
  // to evaluate for one specific singe score too?
  
  
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
  // This stores, for each typeSpec, the ByThEvalStatsTagging object for that typeSpec. The empty string
  // is used for the object that has the values over all types combined.
  // If no score feature is specified (no by threshold evaluation), this field stays null.
  protected Map<String,ByThEvalStatsTagging> evalStatsByThreshold;
  

  // Indicates that evaluation by score threshold is done. If this is true, the evalStatsByThreshold
  // field is non-null after initialization.
  protected boolean doScoreEvaluation = false;
  
  protected static final String initialFeaturePrefixResponse = "evaluateTagging.response.";
  protected static final String initialFeaturePrefixReference = "evaluateTagging.reference.";
  
  
  protected static final Logger logger = Logger.getLogger(EvaluateTagging.class);
  
  @Override
  public void execute() throws ExecutionException {
    
    if(needInitialization) {
      needInitialization = false;
      initializeForRunning();
    }
    if(isInterrupted()) {
      throw new ExecutionException("PR was interrupted!"); 
    }
    
    AnnotationSet keySet = null;
    AnnotationSet responseSet = null;
    AnnotationSet referenceSet = null;
    
    HashSet<String> keyTypes = new HashSet<String>();
    HashSet<String> responseTypes = new HashSet<String>();
    keyTypes.addAll(annotationTypeSpecs.getKeyTypes());
    responseTypes.addAll(annotationTypeSpecs.getResponseTypes());
    if(getAnnotationTypes().size() > 1) {
      keySet = document.getAnnotations(expandedKeySetName).get(keyTypes);
      responseSet = document.getAnnotations(expandedResponseSetName).get(responseTypes);
      if(!expandedReferenceSetName.isEmpty()) {        
        referenceSet = document.getAnnotations(expandedReferenceSetName).get(responseTypes);
      }
      evaluateForType(keySet,responseSet,referenceSet,null);
    }
    // now do it for each typeSpec seperately
    for(AnnotationTypeSpec typeSpec : annotationTypeSpecs.getSpecs()) {
      keySet = document.getAnnotations(expandedKeySetName).get(typeSpec.getKeyType());
      responseSet = document.getAnnotations(expandedResponseSetName).get(typeSpec.getResponseType());
      if(!expandedReferenceSetName.isEmpty()) {
        referenceSet = document.getAnnotations(expandedReferenceSetName).get(typeSpec.getResponseType());
      }
      evaluateForType(keySet,responseSet,referenceSet,typeSpec);      
    }
    
    
  }
  
  // TODO: need to allow for key and response types, and for lists, list element types too!
  /**
   * Do the evaluation for one typeSpec, described by a AnnotationTypeSpec instance.
   * If typeSpec is null, create the evaluation over all types.
   * @param keySet
   * @param responseSet
   * @param referenceSet
   * @param typeSpec 
   */
  protected void evaluateForType(
          AnnotationSet keySet, AnnotationSet responseSet, AnnotationSet referenceSet,
          AnnotationTypeSpec typeSpec) {
    
    // For accessing the type->EvalStats map we use the string type still ...
    String type = "";
    if(typeSpec != null) { type = typeSpec.getKeyType(); }
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
      // TODO: at the moment this will never be true since we have changed the single typeSpec to a list
      // of types. Think about when to not do this ...
      if(containingSetName.equals(expandedKeySetName) && containingType.equals(type)) {
        // no need to do anything for the key set
      } else {
        keySet = selectOverlappingBy(keySet,containingSet,ct);
      }
      // if we have a reference set, we need to apply the same filtering to that one too
      if(referenceSet != null) {
        referenceSet = selectOverlappingBy(referenceSet,containingSet,ct);
      }
    } // have a containing set and typeSpec
    
    
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
            annotationTypeSpecs
    );
    EvalStatsTagging es = docDiffer.getEvalStatsTagging();

    if(doScoreEvaluation) {
      ByThEvalStatsTagging bth = evalStatsByThreshold.get(type);
      AnnotationDifferTagging.calculateByThEvalStatsTagging(
                keySet, responseSet, featureSet, featureComparison, expandedScoreFeatureName, 
              bth.getWhichThresholds(), bth, annotationTypeSpecs);
    }
    
    // Store the counts and measures as document feature values
    FeatureMap docFm = document.getFeatures();
    if (getAddDocumentFeatures()) {
      String featurePrefixResponseT = featurePrefixResponse;
      if (typeSpec == null) {
        featurePrefixResponseT += "[ALL].";
      } else {
        featurePrefixResponseT += (typeSpec + ".");
      }
      docFm.put(featurePrefixResponseT + "FMeasureStrict", es.getFMeasureStrict(1.0));
      docFm.put(featurePrefixResponseT + "FMeasureLenient", es.getFMeasureLenient(1.0));
      docFm.put(featurePrefixResponseT + "PrecisionStrict", es.getPrecisionStrict());
      docFm.put(featurePrefixResponseT + "PrecisionLenient", es.getPrecisionLenient());
      docFm.put(featurePrefixResponseT + "RecallStrict", es.getRecallStrict());
      docFm.put(featurePrefixResponseT + "RecallLenient", es.getRecallLenient());
      docFm.put(featurePrefixResponseT + "SingleCorrectAccuracyStrict", es.getSingleCorrectAccuracyStrict());
      docFm.put(featurePrefixResponseT + "SingleCorrectAccuracyLenient", es.getSingleCorrectAccuracyLenient());
      docFm.put(featurePrefixResponseT + "CorrectStrict", es.getCorrectStrict());
      docFm.put(featurePrefixResponseT + "CorrectPartial", es.getCorrectPartial());
      docFm.put(featurePrefixResponseT + "IncorrectStrict", es.getIncorrectStrict());
      docFm.put(featurePrefixResponseT + "IncorrectPartial", es.getIncorrectPartial());
      docFm.put(featurePrefixResponseT + "TrueMissingStrict", es.getTrueMissingStrict());
      docFm.put(featurePrefixResponseT + "TrueMissingLenient", es.getTrueMissingLenient());
      docFm.put(featurePrefixResponseT + "TrueSpuriousStrict", es.getTrueSpuriousStrict());
      docFm.put(featurePrefixResponseT + "TrueSpuriousLenient", es.getTrueSpuriousLenient());
      docFm.put(featurePrefixResponseT + "Targets", es.getTargets());
      docFm.put(featurePrefixResponseT + "Responses", es.getResponses());
    }
    
    logger.debug("DEBUG: type is "+typeSpec);
    logger.debug("DEBUG: all document stats types "+allDocumentsStats.keySet());
    //System.out.println("DEBUG: all document stats types "+allDocumentsStats.keySet());
    //System.out.println("Getting: "+type);
    //System.out.println("DEBUG: allDocumentStats: "+allDocumentsStats);
    //System.out.println("DEBUG trying to add stats: "+es);
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
              annotationTypeSpecs
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
      if (getAddDocumentFeatures()) {
        String featurePrefixReferenceT = featurePrefixReference;
        if (typeSpec == null) {
          featurePrefixReferenceT += "[ALL].";
        } else {
          featurePrefixReferenceT += (typeSpec + ".");
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
    }
    if(mainTsvPrintStream != null) {
      // a line for the response stats for that document
      mainTsvPrintStream.println(outputTsvLine("normal", document.getName(), typeSpec, expandedResponseSetName, es));
      if(res != null) {
        mainTsvPrintStream.println(outputTsvLine("normal", document.getName(), typeSpec, expandedReferenceSetName,  res));
      }
    }
  }
  
  
  
  /**
   * Return the evaluation statistics for the given typeSpec or over all types if an empty String is
 passed.
   * If a typeSpec name is passed which was not used for the evaluation, null is returned.
   * @param type
   * @return 
   */
  public EvalStatsTagging getEvalStatsTagging(String type) { 
    // if there was only one typeSpec specified, then the typeSpec-specific evalstats is also the 
    // overall evalstats and we will not have created a separate evalstats for "". In that case,
    // if the overall evalstats are requested, we return the one for the one and only typeSpec.
    if(type.equals("") && annotationTypeSpecs.size() == 1) {
      return allDocumentsStats.get(annotationTypeSpecs.getKeyTypes().get(0));
    }
    return allDocumentsStats.get(type); 
  }
  
  /**
   * Return the evaluation statistics for the reference set for the given typeSpec or over all types if an empty String is
 passed.
   * If a typeSpec name is passed which was not used for the evaluation, null is returned. If no reference
 set was specified, null is returned.
   * @param type
   * @return 
   */
  public EvalStatsTagging getEvalStatsTaggingReference(String type) {
    if(getReferenceASName() == null || getReferenceASName().isEmpty()) {
      return null;
    }
    // if there was only one typeSpec specified, then the typeSpec-specific evalstats is also the 
    // overall evalstats and we will not have created a separate evalstats for "". In that case,
    // if the overall evalstats are requested, we return the one for the one and only typeSpec.
    if(type.equals("") && annotationTypeSpecs.size() == 1) {
      return allDocumentsReferenceStats.get(annotationTypeSpecs.getKeyTypes().get(0));
    }
    return allDocumentsReferenceStats.get(type);     
  }
  
  /**
   * Get the evaluation statistics by threshold for the given typeSpec or over all types if an empty
 String is passed.
   * @param type
   * @return 
   */
  public ByThEvalStatsTagging getByThEvalStatsTagging(String type) {
    // if there was only one typeSpec specified, then the typeSpec-specific evalstats is also the 
    // overall evalstats and we will not have created a separate evalstats for "". In that case,
    // if the overall evalstats are requested, we return the one for the one and only typeSpec.
    if(type.equals("") && annotationTypeSpecs.size() == 1) {
      return evalStatsByThreshold.get(annotationTypeSpecs.getKeyTypes().get(0));
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
    if(getNilValue() != null) { nilStr = getNilValue(); }
    // NOTE: this method only gets invoked if feature names is non-null and contains at least
    // one element (does not make sense to invoke it otherwise!)
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
  
  
  protected void initializeForRunning() {
    super.initializeForRunning();
    //System.out.println("DEBUG: running tagging initialize");

    List<String> typesPlusEmpty = new ArrayList<String>();
    if(getAnnotationTypes().size() > 1) {
      typesPlusEmpty.add("");
    }

    //create the data structure that hold an evalstats object over all documents for each typeSpec 
    allDocumentsStats = new HashMap<String, EvalStatsTagging>();
    
    // if we also have a reference set, create the data structure that holds an evalstats object
    // over all documents for each typeSpec. This is left null if no reference set is specified!    
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
    
    if(getAnnotationTypes() == null || getAnnotationTypes().isEmpty()) {
      throw new GateRuntimeException("List of annotation types to use is not specified or empty!");
    }
    annotationTypeSpecs = new AnnotationTypeSpecs(getAnnotationTypes());
    //System.out.println("DEBUG got type specs: "+annotationTypeSpecs);
    
    
    
    if(!expandedScoreFeatureName.isEmpty()) {
      evalStatsByThreshold = new HashMap<String, ByThEvalStatsTagging>();
      doScoreEvaluation = true;
    }
    
    typesPlusEmpty.addAll(annotationTypeSpecs.getKeyTypes());
    for(String t : typesPlusEmpty) {
      logger.debug("DEBUG: initializing alldocument stats for type "+t);
      allDocumentsStats.put(t,new EvalStatsTagging4Score(Double.NaN));
      if(evalStatsByThreshold != null) {
        evalStatsByThreshold.put(t,new ByThEvalStatsTagging(getWhichThresholds()));
      }    
      if(allDocumentsReferenceStats != null) {
        allDocumentsReferenceStats.put(t,new EvalStatsTagging4Score(Double.NaN));
      }
      
    }
    
    featurePrefixResponse = initialFeaturePrefixResponse + getExpandedEvaluationId() + "." + getResponseASName() + "." ;
    featurePrefixReference = initialFeaturePrefixReference + getExpandedEvaluationId() + "." + getReferenceASName() + ".";
    
    mainTsvPrintStream = getOutputStream(null);
    if(mainTsvPrintStream != null) {
      mainTsvPrintStream.print("evaluationId"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("evaluationType"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("docName"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("setName"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("annotationType"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.println(EvalStatsTagging.getTSVHeaders());
    }
    

  }
  
  
  public void finishRunning() {
    outputDefaultResults();
    if(mainTsvPrintStream != null) {
      mainTsvPrintStream.close();    
    }
  }
  
  
  public void outputDefaultResults() {
    
    // TODO: think of a way of how to add the interpolated precision strict interpolated precision
    // lenient to the by thresholds lines!!!
    
    // output for each of the types ...
    for(AnnotationTypeSpec typeSpec : annotationTypeSpecs.getSpecs()) {
      //System.out.println("DEBUG: alldocumentsStats="+allDocumentsStats+" typeSpec="+typeSpec+" expandedResponseSetName="+expandedResponseSetName);
      outputEvalStatsForType(System.out, allDocumentsStats.get(typeSpec.getKeyType()), typeSpec.toString(), expandedResponseSetName);
      if(mainTsvPrintStream != null) { 
        mainTsvPrintStream.println(
                outputTsvLine("normal",null, typeSpec, getResponseASName(), allDocumentsStats.get(typeSpec.getKeyType()))); }
      if(!expandedReferenceSetName.isEmpty()) {
        outputEvalStatsForType(System.out, allDocumentsReferenceStats.get(typeSpec.getKeyType()), typeSpec.toString(), expandedReferenceSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("normal",null, typeSpec, expandedReferenceSetName,  allDocumentsReferenceStats.get(typeSpec.getKeyType()))); }
      }
      if(evalStatsByThreshold != null) {
        ByThEvalStatsTagging bthes = evalStatsByThreshold.get(typeSpec.getKeyType());
        for(double th : bthes.getByThresholdEvalStats().navigableKeySet()) {
          outputEvalStatsForType(System.out, bthes.get(th), typeSpec.toString(), expandedResponseSetName);
          if(mainTsvPrintStream != null) { 
            mainTsvPrintStream.println(
                    outputTsvLine("score", null, typeSpec, expandedResponseSetName, bthes.get(th))); }
        }
      }
    }
    // If there was more than one typeSpec, also output the summary stats over all types
    if(annotationTypeSpecs.size() > 1) {
      outputEvalStatsForType(System.out, allDocumentsStats.get(""), "all(micro)", expandedResponseSetName);
      if(mainTsvPrintStream != null) { 
        mainTsvPrintStream.println(
                outputTsvLine("normal", null, null, expandedResponseSetName, allDocumentsStats.get(""))); }
      if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
        outputEvalStatsForType(System.out, allDocumentsReferenceStats.get(""), "all(micro)", expandedReferenceSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("normal", null, null, expandedReferenceSetName, allDocumentsReferenceStats.get(""))); }
      }      
      if(evalStatsByThreshold != null) {
        ByThEvalStatsTagging bthes = evalStatsByThreshold.get("");
        for(double th : bthes.getByThresholdEvalStats().navigableKeySet()) {
          outputEvalStatsForType(System.out, bthes.get(th), "all(micro)", expandedResponseSetName);
          if(mainTsvPrintStream != null) { 
            mainTsvPrintStream.println(
                    outputTsvLine("score", null, null, expandedResponseSetName, bthes.get(th))); }
        }        
      }
      EvalStatsTaggingMacro esm = new EvalStatsTaggingMacro();
      for(String type : annotationTypeSpecs.getKeyTypes()) {
        esm.add(allDocumentsStats.get(type));
      }
      outputEvalStatsForType(System.out, esm, "all(macro)", expandedResponseSetName);
      if(mainTsvPrintStream != null) { 
        mainTsvPrintStream.println(
                outputTsvLine("normal", null, null, expandedResponseSetName, esm)); }
      if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
        esm = new EvalStatsTaggingMacro();
        for(String type : annotationTypeSpecs.getKeyTypes()) {
          esm.add(allDocumentsReferenceStats.get(type));
        }
        outputEvalStatsForType(System.out, esm, "all(macro)", expandedReferenceSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("normal", null, null, expandedReferenceSetName, esm)); }
      }
    }
      
    if(!expandedReferenceSetName.isEmpty()) {
      outputContingencyTable(System.out, correctnessTableStrict);      
      outputContingencyTable(System.out, correctnessTableLenient);
    }

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

  // TODO: just deactivate this for now, we need to properly handle this later
  // Right now, we need the PR not to complain!
  /*
  @Override
  public Resource duplicate(Factory.DuplicationContext dc) throws ResourceInstantiationException {
    // Instead of throwing an exception, just log a warning. This allows to still use 
    // pipelines where the Evalation PR is included to get duplicated, and all is fine as 
    // long the PR is not RUN in that situation!
    System.err.println("WARNING: duplication does not work yet for the Evaluation PRs, Evaluation should not be run!");
    // throw new UnsupportedOperationException("At the moment, this PR may not be duplicated and must be run single-threaded"); 
    
    // TODO: duplicate such that all duplicates get a flag set which indicates that they are 
    // duplicates, and only the original has the flag not set.
    // Also, use a shared object to give all PRs access to everyone elses's statistics objects.
    // Also, use a shared obejcts to give all PRs access to a singleton object for writing to
    // the output files.
    // Finally, implement the controllerExecutionFinished() method such that only the original
    // will do the actual summarization: it will access all stats objects from all other PRs and
    // summarize them and 
    
  }
  */
  
}
