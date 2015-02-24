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

// TODO: add a list for getting the distribution of ranks/score of first lenient/exact match
// if we process lists. 
// This should get output as a separate tsv both per document and over all documents
// Maybe only if enabled with a parameter.
// If we process lists, also output per-document information about what the best is we can do
// in that document with the candidate lists we have.

// TODO: add a datastructure for incremental update of the contingency tables needed for 
// McNemar etc, if we have a reference set.
// These should get output at the end with values over all documents

// TODO: think about how this should deal with parallelization and custom duplication.
// At the moment this will not work properly or even fail when run from duplicated pipelines. 
// In the best case, each duplicate will just report the statistics on its own subset of documents,
// in the worst case, some resources will override each other (e.g. when writing from each 
// duplicate to the tsv files). 
// The proper way to deal with this requires that we have a standard way of how to share 
// data between those custom duplicated instances of the PR which belong to the same job (in a 
// larger VM, there could duplicates that belong to different jobs between which we do not want
// to share data).
// For now, we just prevent the multithreaded use of this PR by throwing an exception if an 
// attempt is made to custom-duplicate it. 
// Later on, we could change this such that all duplicated instances just call the methods of
// the original in a synchronized way.
// Finally, once we can calculate overall stats on all stats objects (including the byThreshold
// objects) we can try to support multi-threading in the following way:
// = all per-document io needs to go through a single instance synchronized method, or each 
//   instance creates their own output file
// = each duplicate creates their own stats objects over all documents it sees
// = there is a way to access all the stats objects of all the other duplicates somehow, or 
//   ideally there is a way to do this for just the "original" 
// = the controllerExecutionFinished method either knows if it is running for the original or 
//   can somehow make sure that the actual finishing code is only run for one of the duplicates.
//   That finishing code that would get the stats obejcts from all duplicates and merge/sum them,
//   then output the final statistics. 


// TODO(!!!): Add a java class for holding counts or instances of pairings between the reference set
// and the response set so we can calculate p-values for the SingleResponse accuracy analysis. 
// This could be used to get p-values for the McNemar test and the paired t-test.

// TODO: make the output directory URL more easily parametrizable by making the runtime parameter
// an OR parameter with a String version which can then be expandable like the other String
// parameters.

/**
 *
 * @author Johann Petrak
 */
@CreoleResource(name = "EvaluatePRF",
        comment = "Calculate P/R/F evalutation measures for documents")
public class EvaluateTagging extends AbstractLanguageAnalyser
  implements ControllerAwarePR, CustomDuplication  {

  ///////////////////
  /// PR PARAMETERS 
  ///////////////////
  
  
  private String keyASName;
  @CreoleParameter (comment="The name of the annotation set that contains the target/key annotations (gold standard)", defaultValue="Key")
  @RunTime
  public void setKeyASName(String name) { keyASName = name; }
  public String getKeyASName() { return keyASName; }
  public String getExpandedKeyASName() { return Utils.replaceVariablesInString(getKeyASName()); }
  
  private String responseASName;
  @CreoleParameter (comment="The name of the annotation set that contains the response annotations",defaultValue ="Response")
  @RunTime
  public void setResponseASName(String name) { responseASName = name; }
  public String getResponseASName() { return responseASName; }
  public String getExpandedResponseASName() { return Utils.replaceVariablesInString(getResponseASName()); }
  
  private String referenceASName;
  @CreoleParameter (comment="The name of the annotation set that contains the reference/old response annotations. Empty means no reference set.")
  @Optional
  @RunTime
  public void setReferenceASName(String name) { referenceASName = name; }
  public String getReferenceASName() { return referenceASName; }
  public String getExpandedReferenceASName() { return Utils.replaceVariablesInString(getReferenceASName()); }
  
  private String containingASNameAndType;
  @CreoleParameter (comment="The name of the restricting annotation set and the name of the type in the form asname:typename")
  @Optional
  @RunTime
  public void setContainingASNameAndType(String name) { containingASNameAndType = name; }
  public String getContainingASNameAndType() { return containingASNameAndType; }
  public String getExpandedContainingASNameAndType() { return Utils.replaceVariablesInString(getContainingASNameAndType()); }
  
  private ContainmentType containmentType;
  @CreoleParameter (comment="How the responses are restricted to the annotations of the containingASNameAndType",defaultValue="OVERLAPPING")
  @Optional
  @RunTime
  public void setContainmentType(ContainmentType ct) { ct = containmentType; }
  public ContainmentType getContainmentType() { return containmentType; }
  
  private List<String> annotationTypes;
  @CreoleParameter (comment="The annotation types to use for evaluations, at least one type must be given",defaultValue="Mention")
  @RunTime
  public void setAnnotationTypes(List<String> name) { annotationTypes = name; }
  public List<String> getAnnotationTypes() { return annotationTypes; }
  
  private List<String> featureNames;
  private Set<String> featureSet;
  @CreoleParameter (comment="A list of feature names to use for comparison, can be empty. First is used as the id feature, if necessary.")
  @RunTime
  @Optional
  public void setFeatureNames(List<String> names) { 
    featureNames = names; 
    if(featureNames != null) {
      featureSet = new HashSet<String>(featureNames);
    }
  }
  public List<String> getFeatureNames() { return featureNames; }
  
  public FeatureComparison featureComparison;
  @CreoleParameter(comment="",defaultValue="FEATURE_EQUALITY")
  @RunTime
  @Optional  
  public void setFeatureComparison(FeatureComparison value) { featureComparison = value; }
  public FeatureComparison getFeatureComparison() { return featureComparison; }
     
  
  
  /*
  private List<String> byValueFeatureNames;
  @CreoleParameter (comment="A list of feature names to use for breaking up the evaluation (NOT IMPLEMENTED YET)")
  @RunTime
  @Optional
  public void setByValueFeatureNames(List<String> names) { byValueFeatureNames = names; }
  public List<String> getByValueFeatureNames() { return byValueFeatureNames; }
  */
  
  public String listIdFeatureName;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setListIdFeatureName(String value) { listIdFeatureName = value; }
  public String getListIdFeatureName() { return listIdFeatureName; }
  public String getExpandedListIdFeatureName() { return Utils.replaceVariablesInString(getListIdFeatureName()); }
  
  
  private String scoreFeatureName;
  @CreoleParameter (comment="The name of the feature which contains a numeric score or confidence. If specified will generated P/R curve.")
  @Optional
  @RunTime
  public void setScoreFeatureName(String name) { scoreFeatureName = name; }
  public String getScoreFeatureName() { return scoreFeatureName; }
  public String getExpandedScoreFeatureName() { return Utils.replaceVariablesInString(getScoreFeatureName()); }
  
  private String outputASName;
  @CreoleParameter (comment="The name of the annotation set for creating descriptive annotations. If empty, no annotations are created.")
  @Optional
  @RunTime
  public void setOutputASName(String name) { outputASName = name; }
  public String getOutputASName() { return outputASName; }
  public String getExpandedOutputASName() { return Utils.replaceVariablesInString(getOutputASName()); }
  
  public String featureNameNilCluster;
  @CreoleParameter(comment = "", defaultValue = "")
  @RunTime
  @Optional  
  public void setFeatureNameNilCluster(String value) { featureNameNilCluster = value; }
  public String getFeatureNameNilCluster() { return featureNameNilCluster; }
  public String getExpandedFeatureNameNilCluster() { return Utils.replaceVariablesInString(getFeatureNameNilCluster()); }
  
  public NilTreatment nilTreatment;
  @CreoleParameter(comment="",defaultValue="NO_NILS")
  @RunTime
  @Optional  
  public void setNilTreatment(NilTreatment value) { nilTreatment = value; }
  public NilTreatment getNilTreatment() { return nilTreatment; }
     
  public String nilValue;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setNilValue(String value) { nilValue = value; }
  public String getNilValue() { return nilValue; }
  public String getExpandedNilValue() { return Utils.replaceVariablesInString(getNilValue()); }
     
  public URL outputDirectoryUrl;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setOutputDirectoryUrl(URL value) { outputDirectoryUrl = value; }
  public URL getOutputDirectoryUrl() { return outputDirectoryUrl; }
     
  public String evaluationId;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setEvaluationId(String value) { evaluationId = value; }
  public String getEvaluationId() { return evaluationId == null ? "" : evaluationId; }
  public String getExpandedEvaluationId() { return Utils.replaceVariablesInString(getEvaluationId()); }
     
  public ThresholdsToUse whichThresholds;
  @CreoleParameter(comment="",defaultValue="USE_ALL")
  @RunTime
  @Optional  
  public void setWhichThresholds(ThresholdsToUse value) { whichThresholds = value; }
  public ThresholdsToUse getWhichThresholds() { return whichThresholds; }
  
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
  
  // This will be initialized at the start of the run and be incremented in the AnnotationDifferTagging
  // for each document.
  // This stores, for each type, the ByThEvalStatsTagging object for that type. The empty string
  // is used for the object that has the values over all types combined.
  protected Map<String,ByThEvalStatsTagging> evalStatsByThreshold;
  
  
  protected static final String initialFeaturePrefixResponse = "evaluateTagging.response.";
  protected static final String initialFeaturePrefixReference = "evaluateTagging.reference.";
  
  protected String featurePrefixResponse;
  protected String featurePrefixReference;
  
  
  protected boolean doListEvaluation = false;
  protected boolean doScoreEvaluation = false;
  
  String expandedKeySetName;
  String expandedResponseSetName;
  String expandedReferenceSetName;
  String expandedContainingNameAndType;
  String expandedListIdFeatureName;
  String expandedScoreFeatureName;
  String expandedOutputASName;
  String expandedFeatureNameNilCluster;
  String expandedNilValue;
  String expandedEvaluationId;
  
  
  protected static final Logger logger = Logger.getLogger(EvaluateTagging.class);
  
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
      if(doListEvaluation) {
        for(String t : getAnnotationTypes()) {
          typeSet4ListAnns.add(t+"List");
        }
      }
      typeSet.addAll(getAnnotationTypes());
      keySet = document.getAnnotations(expandedKeySetName).get(typeSet);
      if(doListEvaluation) {
        responseSet = document.getAnnotations(expandedResponseSetName).get(typeSet4ListAnns);
      } else {
        responseSet = document.getAnnotations(expandedResponseSetName).get(typeSet);
      }
      if(!expandedReferenceSetName.isEmpty()) {
        if(doListEvaluation) {
          referenceSet = document.getAnnotations(expandedReferenceSetName).get(typeSet4ListAnns);
        } else {
          referenceSet = document.getAnnotations(expandedReferenceSetName).get(typeSet);
        }
      }
      evaluateForType(keySet,responseSet,referenceSet,"");
    }
    // now do it for each type seperately
    for(String type : getAnnotationTypes()) {
      keySet = document.getAnnotations(expandedKeySetName).get(type);
      String origType = type;
      if(doListEvaluation) {
        type = type + "List";
      }
      responseSet = document.getAnnotations(expandedResponseSetName).get(type);
      if(!expandedReferenceSetName.isEmpty()) {
        referenceSet = document.getAnnotations(expandedReferenceSetName).get(type);
      }
      evaluateForType(keySet,responseSet,referenceSet,origType);      
    }
    
    
  }
  
  protected void evaluateForType(
          AnnotationSet keySet, AnnotationSet responseSet, AnnotationSet referenceSet,
          String type) {
    AnnotationSet containingSet = null;
    String containingSetName = "";
    String containingType = "";
    if(!expandedContainingNameAndType.isEmpty()) {
      String[] setAndType = expandedContainingNameAndType.split(":",2);
      if(setAndType.length != 2 || setAndType[0].isEmpty() || setAndType[1].isEmpty()) {
        throw new GateRuntimeException("Runtime Parameter continingASAndName not of the form setname:typename");
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
      // of types. Thing about when to not do this ...
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
    if(doListEvaluation) {
      listAnns = responseSet;
      listAnnsReference = referenceSet;
      candList = 
              AnnotationDifferTagging.createCandidateLists(
                      document.getAnnotations(expandedResponseSetName),
                      listAnns, expandedListIdFeatureName, expandedScoreFeatureName);
      // get the highest scored annotation from each list
      responseSet = new AnnotationSetImpl(listAnns.getDocument());
      if(referenceSet != null) {
        referenceSet = new AnnotationSetImpl(listAnnsReference.getDocument());
      }
      for(CandidateList cl : candList) {
        responseSet.add(cl.get(0));
      }
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
            featureComparison
    );
    EvalStatsTagging es = docDiffer.getEvalStatsTagging();

    if(doScoreEvaluation) {
      ByThEvalStatsTagging bth = evalStatsByThreshold.get(type);
      AnnotationDifferTagging.calculateByThEvalStatsTagging(
              keySet, responseSet, featureSet, featureComparison, expandedScoreFeatureName, bth.getWhichThresholds(), bth);
    } 
    if(doListEvaluation) {
      ByThEvalStatsTagging bth = evalStatsByThreshold.get(type);
      AnnotationDifferTagging.calculateListByThEvalStatsTagging(
              keySet,
              document.getAnnotations(expandedResponseSetName),
              candList, featureSet, featureComparison, 
              expandedListIdFeatureName, expandedScoreFeatureName, bth.getWhichThresholds(), bth);      
    }
    
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
    if(!expandedOutputASName.isEmpty()) {
      outputAnnotationSet = document.getAnnotations(expandedOutputASName);
      docDiffer.addIndicatorAnnotations(outputAnnotationSet);
    }
    
    
    
    // If we have a reference set, also calculate the stats for the reference set
    EvalStatsTagging res = null;
    if(referenceSet != null) {
      AnnotationDifferTagging docRefDiffer = new AnnotationDifferTagging(
              keySet,
              referenceSet,
              featureSet,
              featureComparison
      );
      res = docRefDiffer.getEvalStatsTagging();
      allDocumentsReferenceStats.get(type).add(res);
            
      // if we need to record the matchings, also add the annotations for how things changed
      // between the reference set and the response set.
      if(outputAnnotationSet != null) {
        docRefDiffer.addIndicatorAnnotations(outputAnnotationSet);
        // Now add also the annotations that indicate the changes between the reference set and
        // the response set
        AnnotationDifferTagging.addChangesIndicatorAnnotations(docDiffer, docRefDiffer, outputAnnotationSet);
      }
      
      // TODO: increment the overall counts of how things changed
      
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
    
    if(mainTsvPrintStream != null) {
      // a line for the response stats for that document
      mainTsvPrintStream.println(outputTsvLine(document.getName(), type, expandedResponseSetName, es));
      if(res != null) {
        mainTsvPrintStream.println(outputTsvLine(document.getName(), type, expandedReferenceSetName, res));
      }
    }
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
  
  private PrintStream mainTsvPrintStream;
  private PrintStream scoreDistPrintStream;
  
  /** 
   * Create and open an print stream to the file where the Tsv rows should get written to.
   * If no output directory was specified, this returns null.
   * Otherwise it returns a stream that writes to a file in the output directory that has
   * the name "EvaluateTagging-ID.tsv" where "ID" is the value of the evaluationId parameter.
   * If the evaluationId parameter is not set, the file name is "EvaluateTagging.tsv".
   * @return 
   */
  private PrintStream getOutputStream(String suffix) {
    if(getOutputDirectoryUrl() == null) {
      return null;
    }
    File dir = Files.fileFromURL(getOutputDirectoryUrl());
    if(!dir.exists()) {
      throw new GateRuntimeException("Output directory does not exists: "+getOutputDirectoryUrl());
    }
    if(!dir.isDirectory()) {
      throw new GateRuntimeException("Not a directory: "+getOutputDirectoryUrl());
    }
    String fname = getStringOrElse(getEvaluationId(), "").equals("") 
            ? "EvaluateTagging.tsv" : "EvaluateTagging-"+getEvaluationId();
    if(suffix != null && !suffix.isEmpty()) {
      fname += "-"+suffix;
    }
    fname += ".tsv";
    File outFile = new File(dir,fname);
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(outFile);
    } catch (FileNotFoundException ex) {
      throw new GateRuntimeException("Could not open output file "+outFile,ex);
    }    
    return new PrintStream(os);
  }
  
  private boolean needInitialization = true;
  // This needs to run as part of the first execute, since at the moment, the parametrization
  // does not work correctly with the controller callbacks. 
  private void initializeForRunning() {

    expandedKeySetName = getStringOrElse(getExpandedKeyASName(), "");
    expandedResponseSetName = getStringOrElse(getExpandedResponseASName(),"");
    expandedReferenceSetName = getStringOrElse(getExpandedReferenceASName(),"");
    expandedContainingNameAndType = getStringOrElse(getExpandedContainingASNameAndType(),"");
    expandedEvaluationId = getStringOrElse(getExpandedEvaluationId(),"");
    expandedFeatureNameNilCluster = getStringOrElse(getExpandedFeatureNameNilCluster(),"");
    expandedListIdFeatureName = getStringOrElse(getExpandedListIdFeatureName(),"");
    expandedNilValue = getStringOrElse(getExpandedNilValue(),"");
    expandedOutputASName = getStringOrElse(getExpandedOutputASName(),"");
    expandedScoreFeatureName = getStringOrElse(getExpandedScoreFeatureName(),"");
    
    if(getAnnotationTypes() == null || getAnnotationTypes().isEmpty()) {
      throw new GateRuntimeException("List of annotation types to use is not specified or empty!");
    }
    for(String t : getAnnotationTypes()) {
      if(t == null || t.isEmpty()) {
        throw new GateRuntimeException("List of annotation types to use contains a null or empty type name!");
      }      
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
    }
    
    // If a score feature name is specified, we need to do either by score or list-based
    // evaluation. In both cases we need a data structure to hold one by-threshold-object per 
    // type.
    if(!expandedScoreFeatureName.isEmpty()) {
      evalStatsByThreshold = new HashMap<String, ByThEvalStatsTagging>();
      // also figure out if we want to do list or score evaluation or none of the two
      if(!expandedListIdFeatureName.isEmpty()) {
        doListEvaluation = true;
      } else {
        doScoreEvaluation = true;
      }
    }
    
    
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
  
  
  private String getStringOrElse(String value, String elseValue) {
    if(value == null) return elseValue; else return value;
  }
  
  /**
   * Filter the annotations in the set toFilter and select only those which 
   * overlap with any annotation in set by.
   * 
   * @param toFilter
   * @param by
   * @return 
   */
  private AnnotationSet selectOverlappingBy(AnnotationSet toFilterSet, AnnotationSet bySet, ContainmentType how) {
    if(toFilterSet.isEmpty()) return toFilterSet;
    if(bySet.isEmpty()) return AnnotationSetImpl.emptyAnnotationSet;
    Set<Annotation> selected = new HashSet<Annotation>();
    for(Annotation byAnn : bySet) {
      AnnotationSet tmp = null;
      if(how == ContainmentType.OVERLAPPING) {
        tmp = gate.Utils.getOverlappingAnnotations(toFilterSet, byAnn);
      } else if(how == ContainmentType.CONTAINING) {
        tmp = gate.Utils.getContainedAnnotations(toFilterSet, byAnn);          
      } else if(how == ContainmentType.COEXTENSIVE) {
        tmp = gate.Utils.getCoextensiveAnnotations(toFilterSet, byAnn);        
      } else {
        throw new GateRuntimeException("Odd ContainmentType parameter value: "+how);
      }
      selected.addAll(tmp);
    }
    return new ImmutableAnnotationSetImpl(document, selected);    
  }
  
  /**
   * Create an output line for a TSV file representation of the evaluation results.
   * This prefixes the TSV line created by the EvalStatsTagging object with the evaluation id,
   * the given annotation type, and the given document name. 
   * If an null/empty annotation type is passed, the type "[type:all:micro]" is used instead.
   * If a null document name is passed, the name "[doc:all:micro]" is used instead.
   * @param docName
   * @param annotationType
   * @param es
   * @return 
   */
  protected String outputTsvLine(
          String docName,
          String annotationType,
          String setName,
          EvalStatsTagging es
  ) {
    StringBuilder sb = new StringBuilder();
    sb.append(expandedEvaluationId); sb.append("\t");
    if(docName == null) {
      sb.append("[doc:all:micro]");
    } else {
      sb.append(docName);
    }
    sb.append("\t");
    if(setName == null || setName.isEmpty()) {
      sb.append(expandedResponseSetName);
    } else {
      sb.append(setName);
    }
    sb.append("\t");
    if(annotationType == null || annotationType.isEmpty()) {
      sb.append("[type:all:micro]");
    } else {
      sb.append(annotationType);
    }
    sb.append("\t");
    sb.append(es.getTSVLine());
    return sb.toString();    
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
  
  private static double r4(double x) {
    return ((double) Math.round(x * 10000.0) / 10000.0);
  }
  
  // Output the complete EvalStats object, but in a format that makes it easier to grep
  // out the lines one is interested in based on threshold and type
  protected void outputEvalStatsForType(PrintStream out, EvalStatsTagging es, String type, String set) {
    String ts = "none";
    double th = es.getThreshold();
    if(!Double.isNaN(th)) {
      if(Double.isInfinite(th)) {
        ts="inf";
      } else {
        ts = "" + r4(th);
      }
    }
    ts = ", th="+ts+", ";
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Precision Strict: "+r4(es.getPrecisionStrict()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Recall Strict: "+r4(es.getRecallStrict()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"F1.0 Strict: "+r4(es.getFMeasureStrict(1.0)));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Accuracy Strict: "+r4(es.getSingleCorrectAccuracyStrict()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Precision Lenient: "+r4(es.getPrecisionLenient()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Recall Lenient: "+r4(es.getRecallLenient()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"F1.0 Lenient: "+r4(es.getFMeasureLenient(1.0)));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Accuracy Lenient: "+r4(es.getSingleCorrectAccuracyLenient()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Targets: "+es.getTargets());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Responses: "+es.getResponses());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Correct Strict: "+es.getCorrectStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Correct Partial: "+es.getCorrectPartial());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Incorrect Strict: "+es.getIncorrectStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Incorrect Partial: "+es.getIncorrectPartial());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Missing Strict: "+es.getMissingStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Missing Strict: "+es.getTrueMissingStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Missing Lenient: "+es.getMissingLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Missing Lenient: "+es.getTrueMissingLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Spurious Strict: "+es.getSpuriousStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Spurious Strict: "+es.getTrueSpuriousStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Spurious Lenient: "+es.getSpuriousLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Spurious Lenient: "+es.getTrueSpuriousLenient());
  }
  
  
  public void outputDefaultResults() {
    
    // TODO: think of a way of how to add the interpolated precision strict interpolated precision
    // lenient to the by thresholds lines!!!
    
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
  
  

  
  
}
