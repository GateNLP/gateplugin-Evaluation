/*
 * Copyright (c) 2015-2018 University of Sheffield.
 * 
 * This file is part of gateplugin-Evaluation 
 * (see https://github.com/GateNLP/gateplugin-Evaluation).
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package gate.plugin.evaluation.resources;

import gate.plugin.evaluation.api.ContainmentType;
import gate.plugin.evaluation.api.NilTreatment;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.FeatureMap;
import gate.Resource;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
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
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.api.EvalStatsTagging4Rank;
import gate.plugin.evaluation.api.EvalStatsTagging4Score;
import gate.plugin.evaluation.api.ThresholdsOrRanksToUse;
import gate.plugin.evaluation.api.ThresholdsToUse;
import gate.util.GateRuntimeException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        helpURL ="https://github.com/GateNLP/gateplugin-Evaluation/wiki/EvaluateTagging-PR",
        comment = "Calculate P/R/F evalutation measures for annotations with candidate lists")
public class EvaluateTagging4Lists extends EvaluateTaggingBase 
  implements ControllerAwarePR
  //, CustomDuplication 
{
  public final static long serialVersionUID = 1L;
  
  ///////////////////
  /// PR PARAMETERS 
  ///////////////////
  
  

  protected String edgeFeatureName;
  @CreoleParameter(comment="The name of the feature that contains the list of candidate/element annotation ids",defaultValue="")
  @RunTime
  @Optional  
  public void setEdgeFeatureName(String value) { edgeFeatureName = value; }
  public String getEdgeFeatureName() { return edgeFeatureName; }
  public String getExpandedEdgeFeatureName() { return Utils.replaceVariablesInString(getEdgeFeatureName()); }
  
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
  
  /// API methods to access the stats data the PR calculates
  
  /**
   * Return the evaluation statistics.
   * @return todo
   */
  public EvalStatsTagging getEvalStatsTagging() { 
    return allDocumentsStats; 
  }
  
  /**
   * Get the evaluation statistics by threshold.
   * @return todo
   */
  public ByThEvalStatsTagging getByThEvalStatsTagging() {
    return evalStatsByThreshold;
  }
  
  /**
   * Get the evaluation statistics by rank
   * @return todo
   */
  public ByRankEvalStatsTagging getByRankEvalStatsTagging() {
    return evalStatsByRank;
  }
  
  /**
   * Get the evaluation statistics by rank for disambiguation accuracy
   * @return todo
   */
  public ByRankEvalStatsTagging getByRankEvalStats4ListAcc() {
    return byRank4ListAcc;
  }
  
  
  
  
  // This will be initialized at the start of the run and be incremented in the AnnotationDifferTagging
  // for each document.
  // This stores, for each type, the ByThEvalStatsTagging object for that type. The empty string
  // is used for the object that has the values over all types combined.
  protected ByThEvalStatsTagging evalStatsByThreshold;
  protected ByRankEvalStatsTagging evalStatsByRank;  
  // NOTE: depending on the parameters, one of the two above will be used and the other will be null!
  
  
  // this is for the by-list evaluation
  protected ByRankEvalStatsTagging byRank4ListAcc;
  
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
  protected String outputASListMaxName;
  protected String outputASListThName;


  protected PrintStream matchesTsvPrintStream;
  
  int nrListAnns = 0;
  int nrListAnnsWithKeys = 0;
  int nrListAnnsWithoutKeys = 0;
  int nrListAnnsNoMatch = 0;
  int nrListAnnsMatchLenient = 0;
  int nrListAnnsMatchStrict = 0;
  int nrListAnnsMatchPartial = 0;
  int nrListAnnsMatchStrictAt0 = 0;
  int nrListAnnsMatchPartialAt0 = 0;
  
  @Override
  public void execute() throws ExecutionException {
    //System.out.println("DEBUG: running tagging4lists execute");
    if(needInitialization) {
      needInitialization = false;
      initializeForRunning();
    }
    if(isInterrupted()) {
      throw new ExecutionException("PR was interrupted!"); 
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
      // TODO: we actually never use the refereceSet later, so commented out for now
      //if(referenceSet != null) {
      //  referenceSet = selectOverlappingBy(referenceSet,containingSet,ct);
      //}
    } // have a containing set and type
    
    boolean filterNils = false;
    if(getNilTreatment().equals(NilTreatment.NIL_IS_ABSENT)) {
      filterNils = true;
      removeNilAnns(keySet);
    }
    //System.out.println("DEBUG: after NIL filtering, keysize="+keySet.size());
    
    AnnotationSet listAnns = responseSet;
    //System.out.println("DEBUG evaluating for score feature "+expandedScoreFeatureName);
    List<CandidateList> candLists = 
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
    // However, depending on rank or threshold evaluation, the creation of candLists may have
    // happened differently!
    for(CandidateList cl : candLists) {
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
      AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists,
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
      if(!outputASListMaxName.isEmpty()) {
        AnnotationSet outSet = document.getAnnotations(outputASListThName);
        ad.addIndicatorAnnotations(outSet,"");
      }      
    } else if(evaluate4AllScores) {
      AnnotationDifferTagging.calculateListByThEvalStatsTagging(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists, featureSet, featureComparison, 
              expandedEdgeName, expandedScoreFeatureName, 
              bth.getWhichThresholds(), bth,
              annotationTypeSpecs);    
      
      /* 
      
      NOTE: this was the old attempt to add the extreme value to the stats object, but
      this did not work correctly. Instead we now always add the extreme vale to the actual 
      list of thresholds used above 
      
      // now in addition evaluate for the the score -Inf and create a differ object that
      // contains the indicator annotations for this. 
      AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              Double.NEGATIVE_INFINITY,
              null,
              annotationTypeSpecs);
      ByThEvalStatsTagging tmpEs = new ByThEvalStatsTagging(bth.getWhichThresholds());
      tmpEs.put(Double.NEGATIVE_INFINITY,ad.getEvalStatsTagging());
      bth.addNonCumulative(tmpEs);      
      */
      if(!outputASListMaxName.isEmpty()) {
        AnnotationSet outSet = document.getAnnotations(outputASListMaxName);
        AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              Double.NEGATIVE_INFINITY,
              null,
              annotationTypeSpecs);
        ad.addIndicatorAnnotations(outSet,"");
      } 
    } else if(evaluate4RankTh) {
      AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              null,
              rankThresholdToUse,      // Instead of this, we should use an internal field so we can use -Inf etc.
              annotationTypeSpecs);
      ByRankEvalStatsTagging tmpEs = new ByRankEvalStatsTagging(brk.getWhichThresholds());
      //System.out.println("DEBUG adding for rank "+rankThresholdToUse);
      tmpEs.put(rankThresholdToUse,ad.getEvalStatsTagging());
      brk.add(tmpEs);      
      if(!outputASListMaxName.isEmpty()) {
        AnnotationSet outSet = document.getAnnotations(outputASListThName);
        ad.addIndicatorAnnotations(outSet,"");
      }      
    } else if(evaluate4AllRanks) {
      AnnotationDifferTagging.calculateListByRankEvalStatsTagging(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists, featureSet, featureComparison, 
              expandedEdgeName, expandedScoreFeatureName, 
              brk.getWhichThresholds(), brk,
              annotationTypeSpecs);     
      
      /* 
      
      NOTE: this was the old attempt to add the extreme value to the stats object, but
      this did not work correctly. Instead we now always add the extreme vale to the actual 
      list of thresholds used above 
      
      
      // now in addition also evaluate for rank max_value and create a differ object that
      // contains the indicator annotations for this.
      AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              null,
              Integer.MAX_VALUE,      // Instead of this, we should use an internal field so we can use -Inf etc.
              annotationTypeSpecs);
      ByRankEvalStatsTagging tmpEs = new ByRankEvalStatsTagging(brk.getWhichThresholds());
      //System.out.println("DEBUG adding for rank "+rankThresholdToUse);
      //System.out.println("DEBUG: MAXVALUE evalstats="+ad.getEvalStatsTagging());
      // NOTE: we cannot use brk.add(tmpEs) here since the tmpEs object is already the correct
      // object for that rank with all values accumulated. 
      tmpEs.put(Integer.MAX_VALUE,ad.getEvalStatsTagging());
      //System.out.println("DEBUG before adding="+brk);
      brk.addNonCumulative(tmpEs);   
      //System.out.println("DEBUG after adding="+brk);
      */
      if(!outputASListMaxName.isEmpty()) {
        AnnotationSet outSet = document.getAnnotations(outputASListMaxName);
        AnnotationDifferTagging ad = AnnotationDifferTagging.calculateEvalStatsTagging4List(keySet,
              document.getAnnotations(expandedResponseSetName),
              candLists,
              featureSet,
              featureComparison,
              expandedEdgeName,
              expandedScoreFeatureName,
              null,
              Integer.MAX_VALUE,      // Instead of this, we should use an internal field so we can use -Inf etc.
              annotationTypeSpecs);
        ad.addIndicatorAnnotations(outSet,"");
      } 
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
      mainTsvPrintStream.println(outputTsvLine("list-best", document.getName(), typeSpec, 
              expandedResponseSetName, es));
    }
    
    // Now handle the list accuracy and per-list P/R statistics. In the previous code, we wanted
    // to make sure that the assignment of each match to a key is done equally to what the non-list
    // tagging evaluation does, so we can estimate the final P/R/F of a tagger that would pick 
    // from those lists. Now we look at each list separately and just see how well, within each
    // least the ranking of correct matches is done. Unlike with the previous code, this means 
    // that the candidates from two different lists can successfully get matched to the same target
    // and will both be counted as correct or partially correct
    // This will output a separate tsv file with the following information:
    // document, evaltype, overlaps with target, have cs match (0/1), have cp match (0/1), rank of first cs match,
    // rank of first cp match, score of first cs match, score of first cp match. 
    // If there is no match then rank will be MAXINT and score will be NaN.
    // The following stats are also calculated:
    // * accuracy strict and lenient at rank k for k = 0 .. maxRank:
    //    acc_strict at k is number of LLs where there is a strict match at rank i <= k 
    //      divided by total LLs that have a strict or partial match
    //    acc_lenient at k: same for strict or lenient match at i <= k, whichever match comes first
    // * P/R curve strict/lenient: since we only look at lists that have at least one partial match,
    //    the possible lenient recall is 100%, strict being lower. We look at precision and recall
    //    for each rank k 0 ... maxRank. This should work with the EvalStatsTagging4Rank/Score
    //    objects we already have.
    // * some distribution statistics about the ranks and scores of the first strict and lenient
    //   matches: this could initially be done using R based on the data we write. 
    // 
    // Strategy: go through all candidate lists, check if it overlaps with a target
    //   - no: output no-overlap, 0/0 line RETHINKING: maybe not output a line?
    //   - yes: check if there is a cp or cs match (or both), remember lowerst ranks and scores for those
    //      have no match: output overlap, 0/0 line: RETHINK, maybe not output a line?
    //      have match: output overlap, x/y line
    //     increment our stats objects.
    
    int nrTargets = keySet.size();
    //System.out.println("Number of targets found: "+nrTargets);
    //System.out.println("Number of candidate lists: "+candLists.size());
    
    // TODO: if we do not have responses (candidate lists), do it right!
    
    ByRankEvalStatsTagging tmpEs = new ByRankEvalStatsTagging(ThresholdsOrRanksToUse.USE_RANKS_ALL);
    // find what the highest rank is over all the lists that do have a match for this document
    int maxRankTh = 0; // always create a stats object for rank 0, if all else fails this will remain empty (no counts).
    for(CandidateList cl : candLists) {
      //System.out.println("Found a candidate list with candidates: "+cl.sizeAll());
      if(cl.sizeAll()-1 > maxRankTh) {
        maxRankTh = cl.sizeAll()-1;
      }
    }
    //System.out.println("Max Rank is: "+maxRankTh);
    // initialize the by threshold object with all thresholds we need
    for(int r = 0; r<= maxRankTh; r++) {
      tmpEs.put(r, new EvalStatsTagging4Rank(r));
    }
    for(CandidateList cl : candLists) {
      Annotation ll = cl.getListAnnotation();
      nrListAnns += 1;
      AnnotationSet keys = Utils.getOverlappingAnnotations(keySet, ll);
      if(keys.size() > 0) {
        nrListAnnsWithKeys += 1;
        // this is an overlap: we now need to check if any element in the list, if it still
        // overlaps any of the key anns, is actually a strict or partial match, and which
        // rank/score we have at the first (in order of decreasing preference) strict/partial match.
        cl.clearLimits();
        Annotation firstStrict = null;
        Annotation firstPartial = null;        
        int firstPartialIndex = -1;
        int firstStrictIndex = -1;
        for(int i = 0; i < cl.sizeAll(); i++) {
          Annotation el = cl.get(i);
          if(firstStrict == null) { // still not found a strict match, need to check
            // first check if the annotation is coextensive at all
            for(Annotation k : keys) {
              if(el.coextensive(k)) {
                boolean isMatch = 
                        AnnotationDifferTagging.isAnnotationsMatch(k, el, featureSet, 
                                featureComparison, true, annotationTypeSpecs);
                if(isMatch) {
                  firstStrict = el;
                  firstStrictIndex = i;
                }
              } // if coextensive
            } // for k
          }
          if(firstPartial == null) { // still not found a partial match, need to check
            // first check if the annotation is partial
            for(Annotation k : keys) {
              if(el.overlaps(k) && !el.coextensive(k)) {
                boolean isMatch = 
                        AnnotationDifferTagging.isAnnotationsMatch(k, el, featureSet, 
                                featureComparison, true, annotationTypeSpecs);
                if(isMatch) {
                  firstPartial = el;
                  firstPartialIndex = i;
                }
              } // if coextensive
            } // for k
            
          }          
        } // for
        // now we have any first strict or first partial matches
        // if we do have any match (strict or partial) then record the match by threshold in 
        // the stats objects for this document
        if(firstPartial != null || firstStrict != null) {
          double scAtStrict = Double.NaN;
          double scAtPartial = Double.NaN;
          if(!getExpandedScoreFeatureName().isEmpty()) {
            if(firstPartial != null) {
              scAtPartial = AnnotationDifferTagging.object2Double(firstPartial.getFeatures().get(getExpandedScoreFeatureName()));
            }
            if(firstStrict != null) {
              scAtStrict = AnnotationDifferTagging.object2Double(firstStrict.getFeatures().get(getExpandedScoreFeatureName()));
            }
          } 
          outputTsvLine4Matches(matchesTsvPrintStream,"list-matches", ll.getId(), document.getName(), 
                typeSpec, responseSet.getName(), keys.size(), firstStrictIndex, firstPartialIndex, 
                scAtStrict, scAtPartial);
          
          
          // since there is a partial or strict match, we update the stats objects.
          // This is done in the following way: for all candidates from 0 up until the first match,
          // an incorrectStrict or incorrectPartial is counted, once a correct strict is reached,
          // we count correct strict for the rest of thresholds, if we find correct partial, 
          // we count correct partial until we either reach correct strict or the end.
          // In other words: if there is both a correct strict and a correct partial, then:
          // - if the correct strict has an index lower than the partial, the strict is counted
          //   for all indices >= that index
          // - if the correct strict has an index higher than the partial, a partial is counted
          //   for all indices >= partial and < strict.
          // So the overal strategy is: count incorrect partial or strict until a partial match
          // is reached, if any, then continue counting that until a strict match is reached, if 
          // any, then continue counting that.
          
          // we go through all candidates for each list where we have at least a partial match 
          // therefore the number of "targets" shown in the output is different from the actual
          // targets, it is just the number of response lists that overlaps with at least one 
          // actual target.
          boolean haveStrict = false;
          boolean havePartial = false;
          for (int k = 0; k < cl.sizeAll(); k++) {
            EvalStatsTagging e = tmpEs.get(k);
            e.addTargets(1);
            e.addResponses(1);
            if (k == firstPartialIndex) {
              havePartial = true;
            }
            if (k == firstStrictIndex) {
              haveStrict = true;
            }
            if (haveStrict) { // already found a strict, go on counting that
              e.addCorrectStrict(1);
            } else if (havePartial) {
              e.addCorrectPartial(1);
            } else {
              if (cl.get(k).coextensive(ll)) {
                e.addIncorrectStrict(1);
              } else {
                e.addIncorrectPartial(1);
              }              
            } // else 
          } // for k    
          // now continue to add the counts to any remaining entries for ranks which were not 
          // present in this cl
          for(int l=cl.sizeAll(); l<=maxRankTh; l++) {
            EvalStatsTagging e = tmpEs.get(l);
            e.addTargets(1);
            e.addResponses(1);
            if (haveStrict) { // already found a strict, go on counting that
              e.addCorrectStrict(1);
            } else if (havePartial) {
              e.addCorrectPartial(1);
            } else {
              if (cl.get(l).coextensive(ll)) {
                e.addIncorrectStrict(1);
              } else {
                e.addIncorrectPartial(1);
              }              
            } // else 
            
          }
          nrListAnnsMatchLenient += 1;
          if(haveStrict) {
            nrListAnnsMatchStrict += 1;
            if(firstStrictIndex==0) {
              nrListAnnsMatchStrictAt0 += 1;
            }
          }
          if(havePartial) {
            nrListAnnsMatchPartial += 1;
            if(firstPartialIndex==0) {
              nrListAnnsMatchPartialAt0 += 1;
            }
          }
        } else { 
          // no strict or partial match found in the whole list
          outputTsvLine4Matches(matchesTsvPrintStream,"list-matches", ll.getId(), document.getName(), 
                typeSpec, responseSet.getName(), keys.size(), -1, -1, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
          nrListAnnsNoMatch += 1;
        }
      } else {
        // no overlapping key annotation, just count this as a non-overlap 
        nrListAnnsWithoutKeys += 1;
        outputTsvLine4Matches(matchesTsvPrintStream,"list-matches", ll.getId(), document.getName(), 
                typeSpec, responseSet.getName(), 0, -1, -1, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      }
      //System.out.println("tmpEs=\n"+tmpEs.toString4Debug()+"\n");

    } // for one single candidate list ... 
    
    // add the per-document stats objects to the global stats objects
    // add tmpEs to ...
    byRank4ListAcc.add(tmpEs);
    //System.out.println("-----------------> tmpEs");
    //System.out.println(tmpEs);
    //System.out.println("<----------------- tmpEs");
    // per document we only output the stats for rank 0
    if(mainTsvPrintStream!=null) {
      mainTsvPrintStream.println(outputTsvLine("list-disamb-best", document.getName(), typeSpec, 
              responseSet.getName(), tmpEs.get(0)));
    }
    
    
    
  }
  
  
  protected void outputTsvLine4Matches (
          PrintStream out,
          String evalType,
          int annId, // annotation id of list annotation
          String docName,
          AnnotationTypeSpec typeSpec,
          String setName,
          int nrKeys, // number of overlapping key annotations (if 0 then haveStrict/PartialMatch = 0)
          int rankOfStrictMatch,  // -1 if no match
          int rankOfPartialMatch,   // -1 if no match
          double scoreAtStrictMatch, // NaN if no score specified, -Inf if no match
          double scoreAtPartialMatch // NaN if no score specified, -Inf if no match
  ) {
    if(out == null) return;
    StringBuilder sb = new StringBuilder();
    sb.append(expandedEvaluationId); sb.append("\t");
    sb.append(evalType); sb.append("\t");
    sb.append(annId); sb.append("\t");
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
    if(typeSpec == null) {
      sb.append("[type:all:micro]");
    } else {
      sb.append(typeSpec);
    }
    sb.append("\t");
    sb.append(nrKeys); sb.append("\t");
    sb.append(rankOfStrictMatch); sb.append("\t");
    sb.append(rankOfPartialMatch); sb.append("\t");
    sb.append(scoreAtStrictMatch); sb.append("\t");
    sb.append(scoreAtPartialMatch); 
    out.println(sb.toString());
  }
  
  protected void outputTsvLine4MatchesHeader (PrintStream out) {
    if(out==null) return;
    StringBuilder sb = new StringBuilder();
    sb.append("evaluationId"); sb.append("\t");
    sb.append("evaluationType"); sb.append("\t");
    sb.append("annotationId"); sb.append("\t");
    sb.append("docName"); sb.append("\t");
    sb.append("setName"); sb.append("\t");
    sb.append("annotationType"); sb.append("\t");
    sb.append("nrKeys"); sb.append("\t");
    sb.append("rankOfStrictMatch"); sb.append("\t");
    sb.append("rankOfPartialMatch"); sb.append("\t");
    sb.append("scoreAtStrictMatch"); sb.append("\t");
    sb.append("scoreAtPartialMatch");
    out.println(sb.toString());
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
    Set<Annotation> nils = new HashSet<>();
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
  @Override
  protected void initializeForRunning() {
    //System.out.println("DEBUG: reinitializing");
    super.initializeForRunning();
    //System.out.println("DEBUG: running tagging4lists initialize");
    expandedEdgeName = getStringOrElse(getExpandedEdgeFeatureName(),"");
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
    
    
    if(!evaluate4RankTh && !evaluate4AllRanks && (expandedScoreFeatureName == null || expandedScoreFeatureName.isEmpty())) {
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
    if(evaluate4RankTh || evaluate4AllRanks) {  
      allDocumentsStats = new EvalStatsTagging4Rank(0);
      if(evaluate4AllRanks) {
        evalStatsByRank = new ByRankEvalStatsTagging(getWhichThresholds());
      } else {
        // if a specific rank was requested, we still need to initialize this object so we
        // use RANKS_ALL, meaninig in this context: all the ones we use, which is just that single one
        evalStatsByRank = new ByRankEvalStatsTagging(ThresholdsOrRanksToUse.USE_RANKS_ALL);
      }
      evalStatsByThreshold = null;
    } else if(evaluate4ScoreTh || evaluate4AllScores) {
      allDocumentsStats = new EvalStatsTagging4Score(Double.NaN);      
      if(evaluate4AllScores) {
        evalStatsByThreshold = new ByThEvalStatsTagging(getWhichThresholds().getThresholdsToUse());
      } else {
        // same trick as for ranks above
        evalStatsByThreshold = new ByThEvalStatsTagging(ThresholdsToUse.USE_ALL);
      }
      evalStatsByRank = null;
    }
    
    // If the featureNames list is null, this has the special meaning that the features in 
    // the key/target annotation should be used. In that case the featureNameSet will also 
    // be left null. Otherwise the list will get converted to a set.
    // Convert the feature list into a set
    if(featureNames != null) {
      if(featureNames.isEmpty()) {
        throw new GateRuntimeException("Need at least one feature for list evaluation");
      }
      for(String t : getFeatureNames()) {
        if(t == null || t.isEmpty()) {
          throw new GateRuntimeException("List of feature names to use contains a null or empty type name!");
        }      
      }
      Set<String> featureNameSet = new HashSet<>();
      featureNameSet.addAll(featureNames);
      // check if we have duplicate entries in the featureNames
      if(featureNameSet.size() != featureNames.size()) {
        throw new GateRuntimeException("Duplicate feature in the feature name list");
      }
    } else {
      throw new GateRuntimeException("Need at least one feature for list evaluation");
    }
    
    List<String> types = new ArrayList<>();
    types.add(getExpandedKeyType()+"="+getExpandedListType());
    annotationTypeSpecs = new AnnotationTypeSpecs(types);
    
    types = new ArrayList<>();
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
    
    if(!expandedOutputASPrefix.isEmpty()) {
      outputASListMaxName = expandedOutputASPrefix+"_ResListMax";
      outputASListThName = expandedOutputASPrefix+"_ResListTh";
    } else {
      outputASListMaxName = "";
      outputASListThName = "";
    }
    
    byRank4ListAcc = new ByRankEvalStatsTagging(ThresholdsOrRanksToUse.USE_RANKS_ALL);
    
    matchesTsvPrintStream = getOutputStream("matches");
    outputTsvLine4MatchesHeader(matchesTsvPrintStream);
    
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
    /** not used yet
    if(scoreDistPrintStream != null) {
      scoreDistPrintStream.close();
    }
    */
  }
  
  
  
  public void outputDefaultResults() {
    
    // TODO: think of a way of how to add the interpolated precision strict interpolated precision
    // lenient to the by thresholds lines!!!
    
    // NOTE: cannot remember why we use the ListType for some and the ElementType for others here,
    // the problem is that this will look as if we had different types in the result file and 
    // will make the R package complain. So for now, until we remember what the motivation was,
    // we only use one of the types here, and for now we choose to use the element type.
    //AnnotationTypeSpec typeSpecNormal = new AnnotationTypeSpec(getExpandedKeyType(),getExpandedElementType());
    //AnnotationTypeSpec typeSpecList   = new AnnotationTypeSpec(getExpandedKeyType(),getExpandedListType());
    //AnnotationTypeSpec typeSpecList   = new AnnotationTypeSpec(getExpandedKeyType(),getExpandedElementType());
    AnnotationTypeSpec typeSpecNormal = annotationTypeSpecs.getSpecs().get(0);
    AnnotationTypeSpec typeSpecList   = annotationTypeSpecs.getSpecs().get(0);
    outputEvalStatsForType(System.out, allDocumentsStats, typeSpecNormal.toString(), expandedResponseSetName);
    if(mainTsvPrintStream != null) { 
      mainTsvPrintStream.println(
              outputTsvLine("list-best", null, typeSpecNormal, getResponseASName(), allDocumentsStats)); 
    }
    if(evalStatsByThreshold != null) {
      for(double th : evalStatsByThreshold.getByThresholdEvalStats().navigableKeySet()) {
        outputEvalStatsForType(System.out, evalStatsByThreshold.get(th), typeSpecList.toString(), expandedResponseSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("list-score", null, typeSpecList, expandedResponseSetName, evalStatsByThreshold.get(th))); }
      }
    } else {
      //System.out.println("Keyset for list-rank: "+evalStatsByRank.keySet());
      for(int rank : evalStatsByRank.getByRankEvalStats().navigableKeySet()) {
        outputEvalStatsForType(System.out, evalStatsByRank.get(rank), typeSpecList.toString(), expandedResponseSetName);
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("list-rank", null, typeSpecList, expandedResponseSetName, evalStatsByRank.get(rank))); 
        }
      }      
    }
      for(int rank : byRank4ListAcc.getByRankEvalStats().navigableKeySet()) {
        // TODO: need to first add eval type to that output before we can output this too
        // outputEvalStatsForType(System.out, evalStatsByRank.get(rank), typeSpecList.toString(), expandedResponseSetName);
        //System.err.println("Before writing list-disamb, stream is "+mainTsvPrintStream+" by rank object has thresholds: "+byRank4ListAcc.keySet());
        if(mainTsvPrintStream != null) { 
          mainTsvPrintStream.println(
                  outputTsvLine("list-disamb",null,typeSpecNormal, getResponseASName(), byRank4ListAcc.get(rank)));
        }
      }      

    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists: "+r4(nrListAnns));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists without target: "+r4(nrListAnnsWithoutKeys));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists with target: "+r4(nrListAnnsWithKeys));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists with target but no match: "+r4(nrListAnnsNoMatch));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists with strict match: "+r4(nrListAnnsMatchStrict));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists with strict match at 0: "+r4(nrListAnnsMatchStrictAt0));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists with partial match: "+r4(nrListAnnsMatchPartial));
    System.out.println(expandedEvaluationId+" set="+expandedResponseSetName+", type="+typeSpecNormal.toString()+
            " Number of lists with partial match at 0: "+r4(nrListAnnsMatchPartialAt0));
    
    
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

  /*
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
  */
  
  
}
