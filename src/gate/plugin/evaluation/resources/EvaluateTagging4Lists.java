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
import gate.plugin.evaluation.api.AnnotationTypeSpec;
import gate.plugin.evaluation.api.AnnotationTypeSpecs;
import gate.plugin.evaluation.api.ByRankEvalStatsTagging;
import gate.plugin.evaluation.api.ByThEvalStatsTagging;
import gate.plugin.evaluation.api.ContingencyTableInteger;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.api.EvalStatsTagging4Rank;
import gate.plugin.evaluation.api.EvalStatsTagging4Score;
import gate.plugin.evaluation.api.ThresholdsOrRanksToUse;
import static gate.plugin.evaluation.resources.EvaluateTagging.initialFeaturePrefixResponse;
import gate.util.GateRuntimeException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

// Main purpose of this is to find out what the *maximum* achievable performance is depending
// on a threshold of the score or the rank (rank to be implemented later)

// NOTES: to keep the complexity managable, the list evaluation does not create an evaluation
// over all types together if more than one type is specified, it only shows the evaluation
// for each separate type. 
// Also, no reference set is supported.
// For the indicator annotations, check if the following trick may work: only create additional
// info if we either do the whole thing for one threshold only, or if we do it for a range 
// and we have reached the threshold which always includes the whole list.

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
  
  

  protected String edgeName;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setEdgeName(String value) { edgeName = value; }
  public String getEdgeName() { return edgeName; }
  public String getExpandedEdgeName() { return Utils.replaceVariablesInString(getEdgeName()); }
  
  protected String listType;
  @CreoleParameter(comment="The annotation type of the list annotations",defaultValue="LookupList")
  @RunTime
  public void setListType(String value) { listType = value; }
  public String getListType() { return listType; }
  public String getExpandedListType() { return Utils.replaceVariablesInString(getListType()); }
  
  protected String keyType;
  @CreoleParameter(comment="The annotation type of the key/target annotations",defaultValue="Mention")
  @RunTime
  public void setKeyType(String value) { keyType = value; }
  public String getKeyType() { return keyType; }
  public String getExpandedKeyType() { return Utils.replaceVariablesInString(getKeyType()); }
  
  protected String elementType;
  @CreoleParameter(comment="The annotation type of the list element annotations",defaultValue="Lookup")
  @RunTime
  public void setElementType(String value) { elementType = value; }
  public String getElementType() { return elementType; }
  public String getExpandedElementType() { return Utils.replaceVariablesInString(getElementType()); }
  
  protected String scoreThreshold;
  protected double scoreThresholdToUse = Double.NaN;
  @CreoleParameter(comment="A specific score threshold to use, -Infinity can be used",defaultValue="",disjunction="THORRANK",priority=1)
  @RunTime
  @Optional
  public void setScoreThreshold(String value) { scoreThreshold = value; }
  public String getScoreThreshold() { return scoreThreshold; }
  
  protected String rankThreshold;
  protected Integer rankThresholdToUse = Integer.MAX_VALUE;
  @CreoleParameter(comment="A spacific rank threshold to use, 'max' can be used",defaultValue="",disjunction="THORRANK",priority=2)
  @RunTime
  @Optional
  public void setRankThreshold(String value) { rankThreshold = value; }
  public String getRankThreshold() { return rankThreshold; }
  
  protected ThresholdsOrRanksToUse whichThresholds;
  @CreoleParameter(comment="",defaultValue="USE_TH_ALL")
  @RunTime
  @Optional  
  public void setWhichThresholds(ThresholdsOrRanksToUse value) { whichThresholds = value; }
  public ThresholdsOrRanksToUse getWhichThresholds() { return whichThresholds; }

  
  
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
  
  
  
  // This will be initialized at the start of the run and be incremented in the AnnotationDifferTagging
  // for each document.
  // This stores, for each type, the ByThEvalStatsTagging object for that type. The empty string
  // is used for the object that has the values over all types combined.
  protected ByThEvalStatsTagging evalStatsByThreshold;
  protected ByRankEvalStatsTagging evalStatsByRank;  
  // NOTE: depending on the parameters, one of the two above will be used and the other will be null!
  
  // This will either by 4Score for the set of all best scores or 4Rank for the set of 
  // rank 1 (index 0) entries.
  protected EvalStatsTagging allDocumentsStats;
  
  AnnotationTypeSpecs annotationTypeSpecs4Best;
  
  String expandedEdgeName;
  
  protected static final String initialFeaturePrefixResponse = "evaluateTagging4Lists.response.";
  protected static final String initialFeaturePrefixReference = "evaluateTagging4Lists.reference.";
  
  protected static final Logger logger = Logger.getLogger(EvaluateTagging4Lists.class);
  
  // after init, exactly one of these should be true
  protected boolean evaluate4ScoreTh = false;
  protected boolean evaluate4RankTh = false;
  protected boolean evaluate4AllScores = false;
  protected boolean evaluate4AllRanks = false;
  
  @Override
  public void execute() {
    //System.out.println("DEBUG: running tagging4lists execute");
    if(needInitialization) {
      needInitialization = false;
      initializeForRunning();
    }
    
    //System.out.println("DOC: "+document);
        
    // This should iterate exactly once because we created the annotationTypeSpecs from 
    // single key and list annotation types this PR uses.
    for(AnnotationTypeSpec typeSpec : annotationTypeSpecs.getSpecs()) {
      AnnotationSet keySet = document.getAnnotations(expandedKeySetName).get(typeSpec.getKeyType());
      //System.out.println("DEBUG: getting anns for set "+expandedResponseSetName+" and type "+typeSpec.getResponseType());
      AnnotationSet responseSet = document.getAnnotations(expandedResponseSetName).get(typeSpec.getResponseType());
      // for now, no support for a reference set!!
      evaluateForType(keySet,responseSet,null,typeSpec);      
    }
    
    
  }
  
  protected void evaluateForType(
          AnnotationSet keySet, AnnotationSet responseSet, AnnotationSet referenceSet, AnnotationTypeSpec typeSpec) {
    String type = typeSpec.getKeyType();
    //System.out.println("DEBUG: evaluating for type "+typeSpec+" keysize="+keySet.size()+" resSize="+responseSet.size());
    if(!expandedContainingNameAndType.isEmpty()) {
      String[] setAndType = expandedContainingNameAndType.split(":",2);
      if(setAndType.length != 2 || setAndType[0].isEmpty() || setAndType[1].isEmpty()) {
        throw new GateRuntimeException("Runtime Parameter containingASAndName not of the form setname:typename");
      }      
      String containingSetName = setAndType[0];
      String containingType = setAndType[1];
      AnnotationSet containingSet = document.getAnnotations(setAndType[0]).get(setAndType[1]);
      // now filter the keys and responses. If the containing set/type is the same as the key set/type,
      // do not filter the keys.
      ContainmentType ct = containmentType;
      if(ct == null) ct = ContainmentType.OVERLAPPING;
      responseSet = selectOverlappingBy(responseSet,containingSet,ct);
      if(containingSetName.equals(expandedKeySetName) && containingType.equals(type)) {
        // no need to do anything for the key set
      } else {
        keySet = selectOverlappingBy(keySet,containingSet,ct);
      }
      // if we have a reference set, we need to apply the same filtering to that one too
      if(referenceSet != null) {
        referenceSet = selectOverlappingBy(referenceSet,containingSet,ct);
      }
    } // have a containing set and type
    
    boolean filterNils = false;
    if(getNilTreatment().equals(NilTreatment.NIL_IS_ABSENT)) {
      filterNils = true;
      removeNilAnns(keySet);
    }
    //System.out.println("DEBUG: after NIL filtering, keysize="+keySet.size());
    
    AnnotationSet listAnns = responseSet;
    System.out.println("DEBUG evaluating for score feature "+expandedScoreFeatureName);
    List<CandidateList> candList = 
              AnnotationDifferTagging.createCandidateLists(
                      document.getAnnotations(expandedResponseSetName),
                      listAnns, 
                      expandedEdgeName, 
                      expandedScoreFeatureName, // this should be null if we evaluate for ranks                      
                      getExpandedElementType(),
                      filterNils,getNilValue(),getFeatureNames().get(0));
    // get the highest scored annotation from each list
    responseSet = new AnnotationSetImpl(listAnns.getDocument());
    // if we evaluate by rank, use rank 1 (position 0) for the evaluation, so this is the 
    // same for rank and threshold-based evaluations!
    // However, depending on rank or threshold evaluation, the creation of candList may have
    // happened differently!
    for(CandidateList cl : candList) {
      responseSet.add(cl.get(0));
      //System.out.println("DEBUG: adding annotation: "+cl.get(0));
    }
    //System.out.println("DEBUG: after creation of actual responses, respsize="+responseSet.size());
    
    AnnotationDifferTagging docDiffer = new AnnotationDifferTagging(
            keySet,
            responseSet,
            featureSet,
            featureComparison,
            annotationTypeSpecs4Best  // for this eval, we need to compare key with element type, not list type!
    );
    EvalStatsTagging es = docDiffer.getEvalStatsTagging();
    //System.out.println("DEBUG: after differ for normal: featureSet="+featureSet+" typeSpecs="+annotationTypeSpecs+" featComp="+featureComparison);
    //System.out.println("DEBUG: after differ for normal: keys="+keySet.size()+" resp="+responseSet.size()+"\nEvalStats="+es);

    ByThEvalStatsTagging bth = evalStatsByThreshold;
    ByRankEvalStatsTagging brk = evalStatsByRank;
    // if we only evaluate for a particular score or rank, do that, otherwise do the whole 
    // ByTh thing
    
    if(evaluate4ScoreTh) {
      AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(
              keySet,
              document.getAnnotations(expandedResponseSetName),
              candList,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              scoreThresholdToUse,      // Instead of this, we should use an internal field so we can use -Inf etc.
              null,
              annotationTypeSpecs);
      ByThEvalStatsTagging tmpEs = new ByThEvalStatsTagging(bth.getWhichThresholds());
      tmpEs.put(scoreThresholdToUse,ad.getEvalStatsTagging());
      bth.add(tmpEs);
    } else if(evaluate4AllScores) {
      AnnotationDifferTagging.calculateListByThEvalStatsTagging(
              keySet,
              document.getAnnotations(expandedResponseSetName),
              candList, featureSet, featureComparison, 
              expandedEdgeName, expandedScoreFeatureName, 
              bth.getWhichThresholds(), bth,
              annotationTypeSpecs);      
    } else if(evaluate4RankTh) {
      AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(
              keySet,
              document.getAnnotations(expandedResponseSetName),
              candList,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              null,
              rankThresholdToUse,      // Instead of this, we should use an internal field so we can use -Inf etc.
              annotationTypeSpecs);
      ByRankEvalStatsTagging tmpEs = new ByRankEvalStatsTagging(brk.getWhichThresholds());
      tmpEs.put(rankThresholdToUse,ad.getEvalStatsTagging());
      brk.add(tmpEs);      
    } else if(evaluate4AllRanks) {
      AnnotationDifferTagging.calculateListByRankEvalStatsTagging(
              keySet,
              document.getAnnotations(expandedResponseSetName),
              candList, featureSet, featureComparison, 
              expandedEdgeName, expandedScoreFeatureName, 
              brk.getWhichThresholds(), brk,
              annotationTypeSpecs);            
    }

    // Store the counts and measures as document feature values
    FeatureMap docFm = document.getFeatures();
    if (getAddDocumentFeatures()) {
      String featurePrefixResponseT = featurePrefixResponse;
      featurePrefixResponseT += type;
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
    
    //logger.debug("DEBUG: type is "+type);
    //logger.debug("DEBUG: all document stats types "+allDocumentsStats);
    allDocumentsStats.add(es);
    
    // Now if we have parameters to record the matchings, get the information from the docDiffer
    // and create the apropriate annotations.
    AnnotationSet outputAnnotationSet = null;
    if(!outputASResName.isEmpty()) {
      outputAnnotationSet = document.getAnnotations(outputASResName);
      docDiffer.addIndicatorAnnotations(outputAnnotationSet,"");
    }
    
    if(mainTsvPrintStream != null) {
      // a line for the response stats for that document
      mainTsvPrintStream.println(outputTsvLine("list-best", document.getName(), typeSpec, expandedResponseSetName, es));
    }
  }
  
  
  
  /**
   * Return the evaluation statistics.
   * @param type
   * @return 
   */
  public EvalStatsTagging getEvalStatsTagging() { 
    return allDocumentsStats; 
  }
  
  /**
   * Get the evaluation statistics by threshold.
   * @param type
   * @return 
   */
  public ByThEvalStatsTagging getByThEvalStatsTagging() {
    return evalStatsByThreshold;
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
  

  // This needs to run as part of the first execute, since at the moment, the parametrization
  // does not work correctly with the controller callbacks. 
  protected void initializeForRunning() {
    
    super.initializeForRunning();
    //System.out.println("DEBUG: running tagging4lists initialize");
    expandedEdgeName = getStringOrElse(getExpandedEdgeName(),"");
    expandedScoreFeatureName = getStringOrElse(getExpandedScoreFeatureName(),"");
    
    
    if(getExpandedListType() == null || getExpandedListType().isEmpty()) {
      throw new GateRuntimeException("List annotation type is not specified or empty!");
    }
    if(getExpandedElementType() == null || getExpandedElementType().isEmpty()) {
      throw new GateRuntimeException("Element annotation type is not specified or empty!");
    }
    
    if(getScoreThreshold() != null && !getScoreThreshold().isEmpty()) {
      scoreThresholdToUse = Double.parseDouble(getScoreThreshold());
      evaluate4ScoreTh = true;
      evaluate4AllScores = false;
      evaluate4AllRanks = false;
      evaluate4RankTh = false;
    } else if(getRankThreshold() != null && !getRankThreshold().isEmpty()) {
      if(getRankThreshold().toLowerCase().equals("max")) {
        rankThresholdToUse = Integer.MAX_VALUE;
      } else {
        rankThresholdToUse = Integer.parseInt(getRankThreshold());
      }
      evaluate4RankTh = true;
      evaluate4AllScores = false;
      evaluate4AllRanks = false;
      evaluate4ScoreTh = false;
    } else if(getWhichThresholds().getThresholdsToUse() == null) {
      evaluate4AllScores = false;
      evaluate4ScoreTh = false;
      evaluate4RankTh = false;
      evaluate4AllRanks = true;
    } else {
      evaluate4AllScores = true;
      evaluate4ScoreTh = false;
      evaluate4RankTh = false;
      evaluate4AllRanks = false;
    }
    
    
    if(!evaluate4RankTh && (expandedScoreFeatureName == null || expandedScoreFeatureName.isEmpty())) {
      throw new GateRuntimeException("Score feature name is not specified or empty but not evaluating by rank!");
    }
    
    if(getExpandedKeyType() == null || getExpandedKeyType().isEmpty()) {
      throw new GateRuntimeException("Key annotation type is not specified or empty!");
    }
    
    if(getFeatureNames() != null) {
      for(String t : getFeatureNames()) {
        if(t == null || t.isEmpty()) {
          throw new GateRuntimeException("List of feature names to use contains a null or empty type name!");
        }      
      }
    }
      
    // If the equivalent thresholdstouse is null, we use the rank!
    if(evaluate4RankTh || getWhichThresholds().getThresholdsToUse() == null) {  
      allDocumentsStats = new EvalStatsTagging4Rank(1);
      evalStatsByRank = new ByRankEvalStatsTagging(getWhichThresholds());
    } else {
      allDocumentsStats = new EvalStatsTagging4Score(Double.NaN);      
      evalStatsByThreshold = new ByThEvalStatsTagging(getWhichThresholds().getThresholdsToUse());
    }
    
    // If the featureNames list is null, this has the special meaning that the features in 
    // the key/target annotation should be used. In that case the featureNameSet will also 
    // be left null. Otherwise the list will get converted to a set.
    // Convert the feature list into a set
    if(featureNames != null) {
      if(0 == featureNames.size()) {
        throw new GateRuntimeException("Need at least one feature for list evaluation");
      }
      for(String t : getFeatureNames()) {
        if(t == null || t.isEmpty()) {
          throw new GateRuntimeException("List of feature names to use contains a null or empty type name!");
        }      
      }
      Set<String> featureNameSet = new HashSet<String>();
      featureNameSet.addAll(featureNames);
      // check if we have duplicate entries in the featureNames
      if(featureNameSet.size() != featureNames.size()) {
        throw new GateRuntimeException("Duplicate feature in the feature name list");
      }
    } else {
      throw new GateRuntimeException("Need at least one feature for list evaluation");
    }
    
    List<String> types = new ArrayList<String>();
    types.add(getExpandedKeyType()+"="+getExpandedListType());
    annotationTypeSpecs = new AnnotationTypeSpecs(types);
    
    types = new ArrayList<String>();
    types.add(getExpandedKeyType()+"="+getExpandedElementType());
    annotationTypeSpecs4Best = new AnnotationTypeSpecs(types);
    
    // Establish the default containment type if it was not specified. 
    if(getContainmentType() == null) {
      containmentType = ContainmentType.OVERLAPPING;
    }
    if(getNilTreatment() == null) {
      nilTreatment = NilTreatment.NO_NILS;
    }
    
    if(!(getExpandedReferenceASName() == null || getExpandedReferenceASName().isEmpty())) {
      System.err.println("WARNING: Reference set cannot be used with List evaluation (yet?), ignored!");
      expandedReferenceSetName = null;
      referenceASName = null;
    } 
    
    featurePrefixResponse = initialFeaturePrefixResponse + getExpandedEvaluationId() + "." + getResponseASName() + "." ;
    featurePrefixReference = initialFeaturePrefixReference + getExpandedEvaluationId() + "." + getReferenceASName() + ".";
    

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
  
  
  
  public void outputDefaultResults() {
    
    // TODO: think of a way of how to add the interpolated precision strict interpolated precision
    // lenient to the by thresholds lines!!!
    AnnotationTypeSpec typeSpecNormal = new AnnotationTypeSpec(getExpandedKeyType(),getExpandedElementType());
    AnnotationTypeSpec typeSpecList = new AnnotationTypeSpec(getExpandedKeyType(),getExpandedListType());
    outputEvalStatsForType(System.out, allDocumentsStats, typeSpecNormal.toString(), expandedResponseSetName);
    if(mainTsvPrintStream != null) { 
      mainTsvPrintStream.println(
              outputTsvLine("list-best", null, typeSpecNormal, getResponseASName(), allDocumentsStats)); }
    if(evalStatsByThreshold != null) {
      for(double th : evalStatsByThreshold.getByThresholdEvalStats().navigableKeySet()) {
        outputEvalStatsForType(System.out, evalStatsByThreshold.get(th), typeSpecList.toString(), expandedResponseSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("list-score", null, typeSpecList, expandedResponseSetName, evalStatsByThreshold.get(th))); }
      }
    } else {
      for(int rank : evalStatsByRank.getByRankEvalStats().navigableKeySet()) {
        outputEvalStatsForType(System.out, evalStatsByRank.get(rank), typeSpecList.toString(), expandedResponseSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("list-rank", null, typeSpecList, expandedResponseSetName, evalStatsByRank.get(rank))); }
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
