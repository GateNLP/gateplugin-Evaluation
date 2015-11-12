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
package gate.plugin.evaluation.api;

import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.ImmutableAnnotationSetImpl;
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 * A class for finding the differences between two annotation sets and calculating the counts needed
 * to obtain precision, recall, f-measure and other statistics.
 *
 * This is loosely based on the {@link gate.util.AnnotationDifferTagging} class but has been heavily
 * modified. One important change is that all the counts and the methods for calculating measures
 * from the counts are kept in a separate object of type {@link EvalStatsTagging}.
 * <p>
 * This class is mainly for finding the optimal matchings between the target and response sets and
 * for storing the response and target annotations that correspond to correct, incorrect, missing or
 * spurious matches.
 * <p>
 * Unlike the old class, this class also supports the use of a feature with a score in the response
 * annotations: the comparison can be done for only those responses where the score is larger or
 * equal than a given threshold. There is also a static method for creating a
 * {@link ByThEvalStatsTagging} for a set of thresholds.
 * <p>
 * This class also supports finding the best matches from response lists: a response list is
 * represented by an annotation which contains a special "edge" feature. The "edge" feature is a
 * list of annotation ids which are considered to be all response candidates. Each response
 * candidate must have a score feature and the response candidate list is used sorted by descending
 * score. When evaluating the response lists, this class will find, for each candidate list, the
 * response candidate with the highest score larger than a given score threshold that is equal or
 * partially equal to the target or use the response candidate with the highest score larger than
 * the given score threshold. There is also a static method for creating a
 * {@link ByThEvalStatsTagging} object for a set of thresholds.
 * <p>
 * This class also allows to choose between two ways of comparing annotations by the values of one
 * or more features: if {@link FeatureComparison.FEATURES_EQUALITY} is used, then all the features
 * specified must have identical values in in the target and response annotation. If
 * {@link FeatureComparison.FEATURES_SUBSUMPTION} is used, then only those features from the
 * specified feature list which do occur in the target annotation must match in the response
 * annotation.
 * <p>
 * Usage: other than the old class, this class requires all the parameters to be supplied in the
 * constructor. Once the object has been created, all the data that has been calculated is
 * available. Different constructors can be used to perform different kinds of calculations.
 * <p>
 * Certain operations will require the creation of several AnnotationDifferTagging objects
 * internally and for these operations, the user should call one of the static methods.
 *
 *
 * @author Valentin Tablan
 * @author Johann Petrak
 */
public class AnnotationDifferTagging {

  protected static final Logger logger = Logger.getLogger(AnnotationDifferTagging.class);

  protected EvalStatsTagging evalStats;

  /**
   * Access the counts and evaluation measures calculated by this AnnotationDifferTagging.
   *
   * This will return the actual EvalStatsTagging object stored in this AnnotationDifferTagging
   * object, not a copy!
   *
   * @return an EvalStatsTagging object with the counts for the annotation differences.
   */
  public EvalStatsTagging getEvalStatsTagging() {
    return evalStats;
  }

  private AnnotationDifferTagging() {
  }

  private Set<String> features;

  /**
   * Returns the set of features for this differ. If no features were used, returns an empty set.
   *
   * @return
   */
  public Set<String> getFeatureSet() {
    return features;
  }

  private FeatureComparison featureComparison;

  /**
   * Returns the feature comparison method name for this differ.
   *
   * @return
   */
  public FeatureComparison getFeatureComparison() {
    return featureComparison;
  }

  /**
   * Create a differ for the two sets and the given, potentially empty/null list of features.
   *
   * Create a differ and calculate the differences between the targets set - the set with the
   * annotations which are assumed to be correct - and the responses set - the set e.g. created by
   * an algorithm which should get evaluated against the targets set. The features list is a list of
   * features which need to have equal values for a target and a response annotation to be
   * considered identical. If the features list is null, then the responses have to match whatever
   * features are in the target. If the feature list is an empty list, then no features are ever
   * used for the comparison. In order for a response to match a target the types of the two
   * annotations have to match.
   *
   * @param targets A set of annotations which are regarded to be correct.
   * @param responses A set of annotations for which we asses how well they match the targets. A
   * response strictly matches a target if it a) is coextensive with the target, b) the types are
   * equal and c) they features match according to the features list and comparison method.
   * @param features TODO
   * @param fcm
   * @param annotationTypeSpecs an AnnotationTypeSpecs instance or null if key and response types
   * should be equal
   */
  public AnnotationDifferTagging(
          AnnotationSet targets,
          AnnotationSet responses,
          Set<String> features,
          FeatureComparison fcm,
          AnnotationTypeSpecs annotationTypeSpecs
  ) {
    // same as calling the constructor for using no threshold feature (null) and no threshold 
    // value (NaN).
    this(targets, responses, features, fcm, null, Double.NaN, annotationTypeSpecs);
  }

  /**
   * Create a differ that will calculate the stats for a specific score threshold. This does the
   * same as the constructor AnnotationDiffer(targets,responses,features) but will in addition also
   * expect every response to have a feature with the name given by the thresholdFeature parameter.
   * This feature is expected to contain a value that can be converted to a double and which will be
   * interpreted as a score or confidence. This score can then be used to perform the evaluation
   * such that only responses with a score higher than a certain threshold will be considered. The
   * differ will update the NavigableMap passed to the constructor to add an EvalStatsTagging object
   * for each score that is encountered in the responses set.
   * <p>
   * If the thresholdFeature is empty or null no statistics by threshold will be calculated.
   *
   * @param targets
   * @param responses
   * @param features
   * @param fcmp
   * @param scoreFeature
   * @param thresholdValue
   * @param annotationTypeSpecs
   */
  public AnnotationDifferTagging(
          AnnotationSet targets,
          AnnotationSet responses,
          Set<String> features,
          FeatureComparison fcmp,
          String scoreFeature,
          double thresholdValue,
          AnnotationTypeSpecs annotationTypeSpecs
  ) {
    this.features = features;
    this.featureComparison = fcmp;
    evalStats = calculateDiff(targets, responses, features, fcmp, scoreFeature,
            thresholdValue, null, null, annotationTypeSpecs);
  }

  /**
   * Calculate a new or add to an existing ByThEvalStatsTagging object. If this is called with the
 rankEvalStats parameter not null, then the by thresholds statistics for the differences between
 targets and responses will be added to that object (and it will be returned), otherwise a new
 object will be created for the statistics and returned. An exception will be thrown if a
 rankEvalStats object is passed to this method and its ThresholdsToUse setting is different from
 the one passed to this method.
 <p>
   * Depending on which ThresholdsToUse value is used, this will first find all the thresholds to
   * use from the responses set and then calculate the statistics for each of these thresholds. If
   * an existing ByThEvalStatsTagging was passed on to this method, the calculated
   * ByThEvalStatsTagging statistics are added to the existing statistics and the modified object is
   * returned.
   *
   * @param targets
   * @param responses
   * @param featureSet
   * @param fcmp
   * @param scoreFeature
   * @param thToUse
   * @param existingByThresholdEvalStats
   * @param annotationTypeSpecs
   * @return
   */
  public static ByThEvalStatsTagging calculateByThEvalStatsTagging(
          AnnotationSet targets,
          AnnotationSet responses,
          Set<String> featureSet,
          FeatureComparison fcmp,
          String scoreFeature,
          ThresholdsToUse thToUse,
          ByThEvalStatsTagging existingByThresholdEvalStats,
          AnnotationTypeSpecs annotationTypeSpecs
  ) {
    ByThEvalStatsTagging byThresholdEvalStats = null;
    if (existingByThresholdEvalStats == null) {
      byThresholdEvalStats = new ByThEvalStatsTagging(thToUse);
    } else {
      if (existingByThresholdEvalStats.getWhichThresholds() != thToUse) {
        throw new GateRuntimeException("The ThresholdsToUse parameter is different from the setting for the existingByThresholdEvalStats object");
      }
      byThresholdEvalStats = existingByThresholdEvalStats;
    }
    if (scoreFeature == null || scoreFeature.isEmpty()) {
      throw new GateRuntimeException("thresholdFeature must not be null or empty");
    }
    NavigableSet<Double> thresholds = new TreeSet<Double>();
    if (byThresholdEvalStats.getWhichThresholds() == ThresholdsToUse.USE_ALL
            || byThresholdEvalStats.getWhichThresholds() == ThresholdsToUse.USE_ALLROUNDED) {
      for (Annotation res : responses) {
        double score = getFeatureDouble(res.getFeatures(), scoreFeature, Double.NaN);
        if (Double.isNaN(score)) {
          throw new GateRuntimeException("Response does not have a score: " + res);
        }
        if (byThresholdEvalStats.getWhichThresholds() == ThresholdsToUse.USE_ALLROUNDED) {
          score = round(score, 100.0);
        }
        thresholds.add(score);
      }
    } else {
      thresholds = new TreeSet<Double>(byThresholdEvalStats.getWhichThresholds().getThresholds());
    }
      // Now calculate the EvalStatsTagging(threshold) for each threshold we found in decreasing order.
    // The counts we get will need to get added to all existing EvalStatsTagging which are already
    // in the rankEvalStats map for thresholds less than or equal to that threshold. 

    // start with the highest threshold
    Double th = null;
      // it is possible that at this point there is no threshold in the thresholds collection: this
    // can happen for USE_ALL and USE_ALLROUNDED and there were no responses. In that case, we 
    // still need to count the missing responses (for all entries) so we add the threshold +inf
    // to the thresholds collection.
    thresholds.add(Double.POSITIVE_INFINITY);

    // Run for all thresholds
    ByThEvalStatsTagging newMap = new ByThEvalStatsTagging();
    AnnotationDifferTagging tmpAD = new AnnotationDifferTagging();
    tmpAD.createAdditionalData = false;
    for (double t : thresholds) {
      EvalStatsTagging es = tmpAD.calculateDiff(targets, responses, featureSet, fcmp,
              scoreFeature, t, null, null, annotationTypeSpecs);
      newMap.put(t, es);
    }
    // add the new map to our Map
    byThresholdEvalStats.add(newMap);

    return byThresholdEvalStats;
  }

  /**
   * Create a list of candidate response lists for carrying out list-based evaluations.
   *
   * @param candidatesSet
   * @param listAnnotations
   * @param edgeFeature
   * @param scoreFeature
   * @return
   */
  // TODO: this method maybe should also gather statistics about how many candidate lists end up 
  // having 0 or 1 candidates only. 
  public static List<CandidateList> createCandidateLists(AnnotationSet candidatesSet,
          AnnotationSet listAnnotations, String edgeFeature, String scoreFeature, String elementType,
          boolean filterNils, String nilValue, String idFeature) {
    // for each response, create an actual sorted list of candidate annotations and also store the 
    // minimum and maximum score.
    List<CandidateList> responseCandidates = new ArrayList<CandidateList>(listAnnotations.size());
    for (Annotation ann : listAnnotations) {
      CandidateList cl = new CandidateList(candidatesSet, ann, edgeFeature, scoreFeature,
              elementType, filterNils, nilValue, idFeature);
      if (cl.size() > 0) {
        responseCandidates.add(cl);
      }
    }
    return responseCandidates;

  }

  /**
   * TODO!!!!
   *
   * @param targets
   * @param listAnnotations
   * @param responseCandidatesLists
   * @param featureSet
   * @param fcmp
   * @param listIdFeature
   * @param scoreFeature
   * @param thToUse
   * @param existingByThresholdEvalStats
   * @return
   */
  public static ByThEvalStatsTagging calculateListByThEvalStatsTagging(
          AnnotationSet targets,
          AnnotationSet listAnnotations,
          //AnnotationSet candidates,
          List<CandidateList> responseCandidatesLists,
          Set<String> featureSet,
          FeatureComparison fcmp,
          String listIdFeature,
          String scoreFeature,
          ThresholdsToUse thToUse,
          ByThEvalStatsTagging existingByThresholdEvalStats,
          AnnotationTypeSpecs typeSpecs
  ) {
    ByThEvalStatsTagging byThresholdEvalStats = null;
    if (existingByThresholdEvalStats == null) {
      byThresholdEvalStats = new ByThEvalStatsTagging(thToUse);
    } else {
      if (existingByThresholdEvalStats.getWhichThresholds() != thToUse) {
        throw new GateRuntimeException("The ThresholdsToUse parameter is different from the setting for the existingByThresholdEvalStats object");
      }
      byThresholdEvalStats = existingByThresholdEvalStats;
    }
    if (scoreFeature == null || scoreFeature.isEmpty()) {
      throw new GateRuntimeException("thresholdFeature must not be null or empty");
    }

    // for each response, create an actual sorted list of candidate annotations and also store the 
    // minimum and maximum score.
    //List<CandidateList> responseCandidates = new ArrayList<CandidateList>(listAnnotations.size());
    Set<Double> allScores = null;
    if (thToUse == ThresholdsToUse.USE_ALL || thToUse == ThresholdsToUse.USE_ALLROUNDED) {
      allScores = new HashSet<Double>();
    }

    if (allScores != null) {
      for (CandidateList listOfCandList : responseCandidatesLists) {
        for (Annotation ann : listOfCandList.getList()) {
          double score = object2Double(ann.getFeatures().get(scoreFeature));
          if (thToUse == ThresholdsToUse.USE_ALLROUNDED) {
            score = round(score, 100.0);
          }
          allScores.add(score);
        }
      }
    }

    // Either use the predefined set of thresholds or get the thresholds from the scores from
    // all the annotations pointed to from the list annotation. 
    NavigableSet<Double> thresholds = new TreeSet<Double>();
    if (thToUse == ThresholdsToUse.USE_ALL || thToUse == ThresholdsToUse.USE_ALLROUNDED) {
      thresholds.addAll(allScores);
    } else {
      // TODO: we can save some time by pre-creating the actual navigable sets of thresholds 
      thresholds.addAll(byThresholdEvalStats.getWhichThresholds().getThresholds());
    }

    thresholds.add(Double.NEGATIVE_INFINITY); // add the extreme value always
    
    // By increasing threshold, process the listAnnotations: 
    ByThEvalStatsTagging newMap = new ByThEvalStatsTagging();
    AnnotationDifferTagging tmpAD = new AnnotationDifferTagging();
    for (double th : thresholds) {
      logger.debug("DEBUG: running differ for th " + th + " nr targets is " + targets.size() + " nr responseCands is " + responseCandidatesLists.size());
      // TODO!!! CHECK: can we ignore the annotation type specs here??? Because we handle lists?
      if(th == Double.NEGATIVE_INFINITY) {
        tmpAD.createAdditionalData = true;
      } else {
        tmpAD.createAdditionalData = false;        
      }
      EvalStatsTagging es = tmpAD.calculateDiff(
              targets, listAnnotations, featureSet, fcmp, scoreFeature,
              th, null, responseCandidatesLists, typeSpecs);
      logger.debug("DEBUG: got stats: " + es);
      newMap.put(th, es);
    }
    byThresholdEvalStats.add(newMap);

    return byThresholdEvalStats;
  }

  public static ByRankEvalStatsTagging calculateListByRankEvalStatsTagging(
          AnnotationSet targets,
          AnnotationSet listAnnotations,
          //AnnotationSet candidates,
          List<CandidateList> responseCandidatesLists,
          Set<String> featureSet,
          FeatureComparison fcmp,
          String listIdFeature,
          String scoreFeature,
          ThresholdsOrRanksToUse thToUse,
          ByRankEvalStatsTagging existingByRankEvalStats,
          AnnotationTypeSpecs typeSpecs
  ) {
    ByRankEvalStatsTagging rankEvalStats = null;
    if (existingByRankEvalStats == null) {
      rankEvalStats = new ByRankEvalStatsTagging(thToUse);
    } else {
      if (existingByRankEvalStats.getWhichThresholds() != thToUse) {
        throw new GateRuntimeException("The ThresholdsOrRanksToUse parameter is different from the setting for the existingByRankEvalStats object");
      }
      rankEvalStats = existingByRankEvalStats;
    }

    // for each response, create an actual sorted list of candidate annotations and also store the 
    // minimum and maximum score.
    //List<CandidateList> responseCandidates = new ArrayList<CandidateList>(listAnnotations.size());
    int highestRank = -1;
    if (thToUse == ThresholdsOrRanksToUse.USE_RANKS_ALL ) {
      for (CandidateList listOfCandList : responseCandidatesLists) {
        int s = listOfCandList.size()-1;
        if(s>highestRank) { highestRank = s; }
      }
    }

    // Either use the predefined set of thresholds or get the thresholds from the scores from
    // all the annotations pointed to from the list annotation. 
    NavigableSet<Integer> thresholds = new TreeSet<Integer>();
    if (thToUse == ThresholdsOrRanksToUse.USE_RANKS_ALL) {
      for(int i=0; i<=highestRank; i++) {
        thresholds.add(i);
      }
    } else {
      // TODO: we can save some time by pre-creating the actual navigable sets of ranks
      thresholds.addAll(rankEvalStats.getWhichThresholds().getRanks());
    }

    thresholds.add(Integer.MAX_VALUE); // add the extreme value always
    
    // By increasing threshold, process the listAnnotations: 
    ByRankEvalStatsTagging newMap = new ByRankEvalStatsTagging();
    AnnotationDifferTagging tmpAD = new AnnotationDifferTagging();
    tmpAD.createAdditionalData = false;
    for (int rank : thresholds) {
      logger.debug("DEBUG: running differ for th " + rank + " nr targets is " + targets.size() + " nr responseCands is " + responseCandidatesLists.size());
      // TODO!!! CHECK: can we ignore the annotation type specs here??? Because we handle lists?
      if(rank == Integer.MAX_VALUE) {
        tmpAD.createAdditionalData = true;
      } else {
        tmpAD.createAdditionalData = false;        
      }
      EvalStatsTagging es = tmpAD.calculateDiff(
              targets, listAnnotations, featureSet, fcmp, 
              scoreFeature,
              null, rank, responseCandidatesLists, typeSpecs);
      logger.debug("DEBUG: got stats: " + es);
      newMap.put(rank, es);
    }
    rankEvalStats.add(newMap);

    return rankEvalStats;
  }

  /**
   * TODO!!!
   *
   * @param targets
   * @param listAnnotations
   * @param responseCandidatesLists
   * @param featureSet
   * @param fcmp
   * @param listIdFeature
   * @param scoreFeature
   * @param scoreThreshold
   * @param annotationTypeSpecs
   * @return
   */
  public static AnnotationDifferTagging calculateEvalStatsTagging4List(
          AnnotationSet targets,
          AnnotationSet listAnnotations,
          List<CandidateList> responseCandidatesLists,
          Set<String> featureSet,
          FeatureComparison fcmp,
          String listIdFeature,
          String scoreFeature,
          Double scoreThreshold,
          Integer rankThreshold,
          AnnotationTypeSpecs annotationTypeSpecs
  ) {
    AnnotationDifferTagging tmpAD = new AnnotationDifferTagging();
    //tmpAD.createAdditionalData = false;
    EvalStatsTagging es
            = tmpAD.calculateDiff(
                    targets, listAnnotations,
                    featureSet, fcmp,
                    scoreFeature,
                    scoreThreshold, rankThreshold,
                    responseCandidatesLists,
                    annotationTypeSpecs);
    tmpAD.evalStats = es;
    return tmpAD;
  }

  // the sets we record in case the threshold is NaN
  private AnnotationSet correctStrictAnns,
          correctPartialAnns,
          singleCorrectStrictAnns,
          singleCorrectPartialAnns,
          incorrectStrictAnns,
          incorrectPartialAnns,
          trueMissingLenientAnns,
          trueSpuriousLenientAnns,
          targetAnns;

  public AnnotationSet getCorrectStrictAnnotations() {
    return correctStrictAnns;
  }

  public AnnotationSet getCorrectPartialAnnotations() {
    return correctPartialAnns;
  }

  public AnnotationSet getIncorrectStrictAnnotations() {
    return incorrectStrictAnns;
  }

  public AnnotationSet getIncorrectPartialAnnotations() {
    return incorrectPartialAnns;
  }

  public AnnotationSet getTrueMissingLenientAnnotations() {
    return trueMissingLenientAnns;
  }

  public AnnotationSet getTrueSpuriousLenientAnnotations() {
    return trueSpuriousLenientAnns;
  }

  public AnnotationSet getSingleCorrectStrictAnnotations() {
    return singleCorrectStrictAnns;
  }

  public AnnotationSet getSingleCorrectPartialAnnotations() {
    return singleCorrectPartialAnns;
  }

  public AnnotationSet getTargetAnnotations() {
    return targetAnns;
  }

  /**
   * Add the annotations that indicate correct/incorrect etc to the output set. This will create one
   * annotation in the outSet for each annotation returned by getXXXAnnotations() but will change
   * the type to have a suffix that indicates if this was an incorrect or correct response, a missed
   * target etc. If the reference annotation is not null, this will also add additional annotations
   * with suffixes that indicate how the assignment changed between the reference set and the
   * response set. The indicator annotations for the reference set will have the suffix _R so e.g. a
   * strictly correct response for the annotation type Mention will get annotated as Mention_CS_R
   *
   * @param outSet
   * @param prefix
   */
  public void addIndicatorAnnotations(AnnotationSet outSet, String prefix) {
    if (prefix == null) {
      prefix = "";
    }
    addAnnsWithTypeSuffix(outSet, getCorrectStrictAnnotations(), prefix + "_CS", null);
    addAnnsWithTypeSuffix(outSet, getCorrectPartialAnnotations(), prefix + "_CP", null);
    addAnnsWithTypeSuffix(outSet, getIncorrectStrictAnnotations(), prefix + "_IS", null);
    addAnnsWithTypeSuffix(outSet, getIncorrectPartialAnnotations(), prefix + "_IP", null);
    addAnnsWithTypeSuffix(outSet, getTrueMissingLenientAnnotations(), prefix + "_ML", null);
    addAnnsWithTypeSuffix(outSet, getTrueSpuriousLenientAnnotations(), prefix + "_SL", null);
  }

  /**
   * Add changes between a reference and response set to contingency tables. This is a utility
   * function to add correct/wrong counts to the two contingency tables passed. The first table is
   * for the changes according to strict correctness, the second table is for the changes according
   * to lenient correctness. Each table is expected to have two rows indicating the reference set
   * being correct and wrong and two columns indicating the response set being correct and wrong. So
   * the count in cell (1,1) represents the number of times an annotation was correct in both the
   * reference and response sets, in cell (1,2) (row 1, column 2) represents the number of times an
   * annotation was correct in the reference set and wrong in the response set etc.
   * <p>
   * It is possible to only pass one of the two table objects.
   *
   * @param responses
   * @param reference
   * @param tableStrict
   * @param tableLenient
   */
  public static void addChangesToContingenyTables(
          AnnotationDifferTagging responses,
          AnnotationDifferTagging reference,
          ContingencyTableInteger tableStrict,
          ContingencyTableInteger tableLenient) {
    if (tableStrict == null && tableLenient == null) {
      throw new RuntimeException("Both contingency tables null, no point of calling this method!");
    }
    if (tableStrict != null && (tableStrict.nRows() != 2 || tableStrict.nColumns() != 2)) {
      throw new RuntimeException("tableStrict is not a 2x2 contingency table");
    }
    if (tableLenient != null && (tableLenient.nRows() != 2 || tableLenient.nColumns() != 2)) {
      throw new RuntimeException("tableLenient is not a 2x2 contingency table");
    }
    // the contingency table must be 2x2. We assume that the rows refer to the reference
    // set being correct or wrong, in that order and the columns to refer to the response set
    // being correct or wrong in that order.
    AnnotationSet refsCorrect = reference.getSingleCorrectStrictAnnotations();
    AnnotationSet respsCorrect = responses.getSingleCorrectStrictAnnotations();
    AnnotationSet refsCorrectPartial = reference.getSingleCorrectPartialAnnotations();
    AnnotationSet respsCorrectPartial = responses.getSingleCorrectPartialAnnotations();
    // for each correct reference, find the correct and incorrect ones from the response set
    for (Annotation ann : responses.getTargetAnnotations()) {
      boolean resStrictCorrect = false;
      boolean resLenientCorrect = false;
      boolean refStrictCorrect = false;
      boolean refLenientCorrect = false;

      // Either set is correct for a target if there is a singleCorrect answer
      AnnotationSet tmpSet = Utils.getCoextensiveAnnotations(respsCorrect, ann);
      if (tmpSet.size() > 0) {
        resStrictCorrect = true;
        resLenientCorrect = true;
      }

      tmpSet = Utils.getCoextensiveAnnotations(refsCorrect, ann);
      if (tmpSet.size() > 0) {
        refStrictCorrect = true;
        refLenientCorrect = true;
      }

      // if the lenient flags are still false, lets check if we got a partial response
      tmpSet = Utils.getOverlappingAnnotations(respsCorrectPartial, ann);
      if (tmpSet.size() > 0) {
        resLenientCorrect = true;
      }
      tmpSet = Utils.getOverlappingAnnotations(refsCorrectPartial, ann);
      if (tmpSet.size() > 0) {
        refLenientCorrect = true;
      }
      if (tableStrict != null) {
        if (refStrictCorrect && resStrictCorrect) {
          tableStrict.incrementBy(0, 0, 1);
        } else if (refStrictCorrect && !resStrictCorrect) {
          tableStrict.incrementBy(0, 1, 1);
        } else if (!refStrictCorrect && resStrictCorrect) {
          tableStrict.incrementBy(1, 0, 1);
        } else if (!refStrictCorrect && !resStrictCorrect) {
          tableStrict.incrementBy(1, 1, 1);
        }
      }
      if (tableLenient != null) {
        if (refLenientCorrect && resLenientCorrect) {
          tableLenient.incrementBy(0, 0, 1);
        } else if (refLenientCorrect && !resLenientCorrect) {
          tableLenient.incrementBy(0, 1, 1);
        } else if (!refLenientCorrect && resLenientCorrect) {
          tableLenient.incrementBy(1, 0, 1);
        } else if (!refLenientCorrect && !resLenientCorrect) {
          tableLenient.incrementBy(1, 1, 1);
        }
      }
    } // for
  }

  /**
   * Add the annotations that indicate changes between responses.
   *
   * @param outSet
   * @param responses
   * @param reference
   */
  // NOTE: at the moment we only use the feature set/feature comparison strategy from the 
  // response set and do not check if it is the same as for the reference set. Calculating
  // differences really only makes sense if the same set and strategy are used!!
  public static void addChangesIndicatorAnnotations(AnnotationDifferTagging responses,
          AnnotationDifferTagging reference, AnnotationSet outSet) {

    Set<String> fs = responses.getFeatureSet();
    FeatureComparison fc = FeatureComparison.FEATURE_EQUALITY;

    AnnotationSet allRefs = new AnnotationSetImpl(reference.getCorrectPartialAnnotations().getDocument());
    allRefs.addAll(reference.getCorrectPartialAnnotations());
    allRefs.addAll(reference.getCorrectStrictAnnotations());
    allRefs.addAll(reference.getIncorrectPartialAnnotations());
    allRefs.addAll(reference.getIncorrectStrictAnnotations());
    allRefs.addAll(reference.getTrueMissingLenientAnnotations());
    allRefs.addAll(reference.getTrueSpuriousLenientAnnotations());
  // The following changes are, in theory possible:
    // CS -> CP, IS, IP, ML
    for (Annotation ann : reference.getCorrectStrictAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_CP", "-");
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_IS", "-");
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_IP", "-");
      tmpSet = getOverlappingAnnsNotIn(responses.getTrueMissingLenientAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_ML", "-");
    }
    // CP -> CS, IS, IP, ML
    for (Annotation ann : reference.getCorrectPartialAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_CS", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_IS", "-");
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_IP", "-");
      tmpSet = getOverlappingAnnsNotIn(responses.getTrueMissingLenientAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_ML", "-");
    }
    // IS -> IP, CS, CP, ML
    for (Annotation ann : reference.getIncorrectStrictAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_IP", "+-");
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_CS", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_CP", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getTrueMissingLenientAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_ML", "+-");
    }
    // IP -> IS, CS, CP, ML
    for (Annotation ann : reference.getIncorrectPartialAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_IS", "+-");
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_CS", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_CP", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getTrueMissingLenientAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_ML", "+-");
    }
    // ML -> CS, CP, IS, IP
    for (Annotation ann : reference.getTrueMissingLenientAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_CS", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getCorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_CP", "+");
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectStrictAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_IS", "+-");
      tmpSet = getOverlappingAnnsNotIn(responses.getIncorrectPartialAnnotations(), ann, allRefs, fs, fc);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_IP", "+-");
    }
    // SL -> A (absent)
    for (Annotation ann : reference.getTrueSpuriousLenientAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getTrueSpuriousLenientAnnotations(), ann);
      if (tmpSet.size() == 0) {
        FeatureMap fm = gate.Utils.toFeatureMap(ann.getFeatures());
        fm.put("_eval.change", "+-");
        gate.Utils.addAnn(outSet, ann, ann.getType() + "_SL_A", fm);
      }
    }
    // A (absent) -> SL
    for (Annotation ann : responses.getTrueSpuriousLenientAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(reference.getTrueSpuriousLenientAnnotations(), ann);
      //System.err.println("\n\nDEBUG: checking ann in response "+ann+"\ngot overlaps: "+tmpSet+"\nsize is "+tmpSet.size());
      if (tmpSet.size() == 0) {
        FeatureMap fm = gate.Utils.toFeatureMap(ann.getFeatures());
        fm.put("_eval.change", "+-");
        gate.Utils.addAnn(outSet, ann, ann.getType() + "_A_SL", fm);
      }
    }
  // This would amount to 22 different pairings of which the following 9 are good:
    // CP -> CS
    // IS -> CS, CP
    // IP -> IS, CS, CP
    // ML -> CS, CP
    // SL -> A
// The following 9 are bad
    // CS -> CP, IS, IP, ML
    // CP -> IS, IP, ML
    // IS -> IP
    // A - SL
    // and the following 4 are indifferent
    // IS -> ML
    // IP -> ML
    // ML -> IS, IP
    // If we ignore the span, we get:
    // Good: IL->CL, ML -> CL, SL -> A
    // Bad:  CL->IL, CL->ML, A -> SL
    // Indifferent: IL -> ML, ML -> IL
    // Span only changes:
    // good: CP -> CS, IP -> IS
    // bad:  CS -> CP, IS -> IP
  }

  // TODO!!!: the following is for calculating McNemar's test
  /**
   * This returns a 2x2 contingency table with the counts for both correct, both incorrect and
   * different correctness. This method only makes sense if the correctness and incorrectness can be
   * established reasonably i.e. if there is a feature for checking the correctness of a response.
   * TODO: two methods for lenient and strict!!
   *
   * @param responseDiffer
   * @param referenceDiffer
   * @return
   */
  //public static ContingencyTableInteger getPairedCorrectnessCountsStrict(
  //  ContingencyTableInteger toIncrement, AnnotationDifferTagging responseDiffer, AnnotationDifferTagging referenceDiffer) {
  //  
  //}
  // this contains the final choices calculated by this object.
  protected List<Pairing> choices = new ArrayList<Pairing>();

  // TODO: figure out how to support calculating Krippendorff's alpha and Fleiss's Kappa too!
  // Ideally all these things would incrementally calculate whatever contingency tables they need,
  // so the method would take an existing table and two differs and increment the counts.
  /**
   * This controls if the indiciator annotations are created. By default this is true, but if the
   * class is used internally by a static method where the annotations would never get used anyway,
   * the flag will be set to false.
   */
  private boolean createAdditionalData = true;

  /**
   * Computes a diff between two collections of annotations.
   *
   */
  // TODO: this should also record on the fly, if we are processing lists, at which rank a 
  // correct strict or correct partial match was first encountered and otherwise set that rank 
  // to some special N/A value. This would be useful to calculate a statistic about the average
  // rank of the correct answer among those list which have a correct answer. 
  private EvalStatsTagging calculateDiff(
          AnnotationSet keyAnns,
          AnnotationSet responseAnns,
          Set<String> features,
          FeatureComparison fcmp,
          String scoreFeature, // if not null, the name of a score feature
          Double scoreThreshold, // if not NaN/null, we will calculate the stats only for responses with score >= scoreThreshold
          Integer rankThreshold, // if not null, use stats only for responses with rankThreshold <= that rankThreshold
          List<CandidateList> candidateLists, // if not null, we do list evaluation
          AnnotationTypeSpecs typeSpecs
  ) {

    // NOTE: for list evaluation, the list annotation type has to now match the key annotation
    // type, not the candidate annotation type. In other words, the candidate annotation type
    // does not matter at all for the comparison - as soon as an annotation is a candidate,
    // it is used. 
    // NOTE: currentl we do not allow multi-type list based evaluation!!
    //System.out.println("DEBUG running calculateDiff for scoreTh"+scoreThreshold+" rankTh="+rankThreshold);
    EvalStatsTagging es;
    if (rankThreshold != null) {
      es = new EvalStatsTagging4Rank(rankThreshold);
      if (scoreThreshold != null && scoreThreshold != Double.NaN) {
        throw new GateRuntimeException("Score threshold must be null or NaN if rank threshold is given");
      }
    } else {
      if (scoreThreshold == null) {
        scoreThreshold = Double.NaN;
      }
      es = new EvalStatsTagging4Score(scoreThreshold);
    }

    if (createAdditionalData) {
      correctStrictAnns = new AnnotationSetImpl(keyAnns.getDocument());
      correctPartialAnns = new AnnotationSetImpl(keyAnns.getDocument());
      incorrectStrictAnns = new AnnotationSetImpl(keyAnns.getDocument());
      incorrectPartialAnns = new AnnotationSetImpl(keyAnns.getDocument());
      trueMissingLenientAnns = new AnnotationSetImpl(keyAnns.getDocument());
      trueSpuriousLenientAnns = new AnnotationSetImpl(keyAnns.getDocument());
      targetAnns = new AnnotationSetImpl(keyAnns.getDocument());
      targetAnns.addAll(keyAnns);
      singleCorrectPartialAnns = new AnnotationSetImpl(keyAnns.getDocument());
      singleCorrectStrictAnns = new AnnotationSetImpl(keyAnns.getDocument());
    }
    keyList = new ArrayList<Annotation>(keyAnns);
    // sort to avoid non-determinism
    Collections.sort(keyList,new OffsetAndMoreComparator(features));
    responseList = null;
    // If we do list processing, this records, for each response annotation, what the corresponding
    // index of the candidate list is. Since the responeList only contains annotations from those
    // candidate lists which have still at least one candidate, there is no 1:1 mapping between
    // the index of an annotation in the responseList and the index of the candidate lists, so 
    // this list is used instead.
    List<Integer> candidateIndices = new ArrayList<Integer>();

    // if the candidateLists parameter is not null, we need to prepare the responses 
    // from those lists.
    if (candidateLists != null) {
      logger.debug("DEBUG: candidate list size is " + candidateLists.size());
      // we create the response list by going through all the candidate lists and 
      // adding the highest score candidate to the response list if the candidate list 
      // still has entries for the scoreThreshold. That candidate may get replaced later ...
      responseList = new ArrayList<Annotation>(responseAnns.size());
      int cidx = 0;
      for (CandidateList cand : candidateLists) {
        if(rankThreshold != null) {
          cand.setRank(rankThreshold);
        } else {
          cand.setThreshold(scoreThreshold);
        }
        //System.out.println("DEBUG after setting to "+rankThreshold+"/"+scoreThreshold+" size="+cand.size());
        if (cand.size() != 0) {
          responseList.add(cand.get(0));
          candidateIndices.add(cidx);
        }
        cidx++;
      }
      logger.debug("DEBUG: response list size is now: " + responseList.size());
    } else {

      // if we do not need to process the candidate lists, check if we need to process for 
      // a scoreThreshold
      if (Double.isNaN(scoreThreshold)) {
        responseList = new ArrayList<Annotation>(responseAnns);
      } else {
        responseList = new ArrayList<Annotation>(responseAnns.size());
        for (Annotation res : responseAnns) {
          double score = getFeatureDouble(res.getFeatures(), scoreFeature, Double.NaN);
          if (Double.isNaN(score)) {
            throw new GateRuntimeException("Response without a score feature: " + res);
          }
          if (score >= scoreThreshold) {
            responseList.add(res);
          }
        }        
      }
      
    }
    Collections.sort(responseList,new OffsetAndMoreComparator(features));
    //logger.debug("DEBUG: responseList size for scoreThreshold "+scoreThreshold+" is "+responseList.size());

    keyChoices = new ArrayList<List<Pairing>>(keyList.size());
    // initialize by nr_keys nulls
    keyChoices.addAll(Collections.nCopies(keyList.size(), (List<Pairing>) null));
    responseChoices = new ArrayList<List<Pairing>>(responseList.size());
    // initialize by nr_responses null
    responseChoices.addAll(Collections.nCopies(responseList.size(), (List<Pairing>) null));

    possibleChoices = new ArrayList<Pairing>();

    es.addTargets(keyAnns.size());
    es.addResponses(responseList.size());

    //1) try all possible pairings
    // If we have candidate lists, we also want to roughly be able to find out eventually
    // how many of the targets have true missing responses (strict and lenient) because
    // there was no response at all versus because there were responses in the list, but none was correct. 
    // To do this we use two arrays of the same size as the keylist and set a flag for each key
    // if there is any annotation that matches strict and if there is any annotation that matches
    // strict or partially. 
    // We then count the number of keys that have a strict match or a lenient match at all. 
    // This kind of counting the matches is different from how we do the missing responses below:
    // in the "proper" way below, a match gets "used", so it can never count for two keys, but
    // here we simply count how often a response could "potentially" be a chance for a correct
    // response, or in other words, we count how many targets are certain to never get a correct
    // response because there is simply no overlapping or coextensive response. 
    boolean[] haveStrictResponse = new boolean[keyList.size()];
    boolean[] haveLenientResponse = new boolean[keyList.size()];
    for(int i=0; i<keyList.size(); i++) { haveStrictResponse[i] = false; haveLenientResponse[i] = false; }
    for (int i = 0; i < keyList.size(); i++) {
      for (int j = 0; j < responseList.size(); j++) {
        Annotation keyAnn = keyList.get(i);

        Annotation resAnn = null;
        Pairing choice = null;
        // If we process candidate lists, do not just compare with the response
        // annotation from the list but instead compare with all candidates still in the list
        // and use the first exact match, if none is found, the first partial match, if none
        // is found the candidate with the highest score that is coextensive, if none is found
        // the candidate with the highest score.
        // However to decide if we should attempt a match at all, we first compare the 
        // range if the list annotation with the key annotation. Only if they overlap, we 
        // go through the candidates.
        // NOTE: this will only consider list annotation which match the type of the key 
        // annotation according to the type specs
        if (candidateLists != null) {
          CandidateList candList = candidateLists.get(candidateIndices.get(j));
          // check already at this point that the candidate list has a type
          // that matches the key, based on the type specifications we got!
          String candType = candList.getListAnnotation().getType();
          String keyType = keyAnn.getType();
          //System.out.println("DEBUG: checking cand type "+candType+" against key type "+keyType+" typeSpecs "+typeSpecs);
          if (!typeSpecs.getKeyType(candType).equals(keyType)) {
            continue;
          }

          if (keyAnn.overlaps(candList.getListAnnotation())) {
            // find the best matching annotation and remember which kind of match we had
            int match = WRONG_VALUE;
            Annotation bestAnn = responseList.get(j);
            // We initialize responselist(i) with candList.get(0) so the above is identical to
            // Annotation bestAnn = candList.get(0);
            boolean foundStrict = false;
            boolean foundPartial = false;
            for (int c = 0; c < candList.size(); c++) {
              Annotation tmpResp = candList.get(c);
              //logger.debug("Checking annotation at index: " + c + ": " + tmpResp);
              if (isAnnotationsMatch(keyAnn, tmpResp, features, fcmp, true, typeSpecs)) {
                // if we are coextensive, then we can stop: can't get any better!
                if (keyAnn.coextensive(tmpResp)) {
                  //logger.debug("Found correct match!!");
                  match = CORRECT_VALUE;
                  bestAnn = tmpResp;
                  foundStrict = true;
                  haveStrictResponse[i]=true;
                  haveLenientResponse[i]=true;
                  break;
                } else {
                  //logger.debug("Found a partial match, checking if we can add!");
                  // if we did not already find a match, store
                  if (match == WRONG_VALUE || match == MISMATCH_VALUE) {
                    //logger.debug("Found a partial match and adding!");
                    match = PARTIALLY_CORRECT_VALUE;
                    bestAnn = tmpResp;
                    foundPartial = true;
                    haveLenientResponse[i]=true;
                  }
                }
              } else if (keyAnn.coextensive(tmpResp) && match == WRONG_VALUE) {
                //logger.debug("Found a MISMATCH");
                match = MISMATCH_VALUE;
                bestAnn = tmpResp;
              } else {
                // if we get here then 
                // = there is certainly no match
                // = the annotation may be overlapping, or if it is coextensive, than
                //   we already found a coextensive one which is no match previously.
                // we have to continue until we either find a beter match or are done.
                //logger.debug("Found ODD: match=" + match);
              }
            } // for
            logger.debug("Took best match from index "+j+" was "+match);
            responseList.set(j, bestAnn);
            choice = new Pairing(i, j, match);
          }
        } else {

          resAnn = responseList.get(j);
          choice = null;
          if (keyAnn.coextensive(resAnn)) {
            //we have full overlap -> CORRECT or WRONG
            if (isAnnotationsMatch(keyAnn, resAnn, features, fcmp, false, typeSpecs)) {
              //we have a full match
              choice = new Pairing(i, j, CORRECT_VALUE);
              haveStrictResponse[i]=true;
              haveLenientResponse[i]=true;
            } else {
              //the two annotations are coextensive but don't match
              //we have a missmatch
              choice = new Pairing(i, j, MISMATCH_VALUE);
            }
          } else if (keyAnn.overlaps(resAnn)) {
            //we have partial overlap -> PARTIALLY_CORRECT or WRONG
            if (isAnnotationsMatch(keyAnn, resAnn, features, fcmp, false, typeSpecs)) {
              choice = new Pairing(i, j, PARTIALLY_CORRECT_VALUE);
              haveLenientResponse[i]=true;
            } else {
              choice = new Pairing(i, j, WRONG_VALUE);
            }
          }
        }

        //add the new choice if any
        if (choice != null) {
          addPairing(choice, i, keyChoices);
          addPairing(choice, j, responseChoices);
          possibleChoices.add(choice);
        }
      }//for j
    }//for i

    int nTargetsWithStrictResponses = 0;
    int nTargetsWithLenientResponses = 0;
    for(int i=0; i<keyList.size(); i++) {
      if(haveStrictResponse[i]) nTargetsWithStrictResponses++;
      if(haveLenientResponse[i]) nTargetsWithLenientResponses++;
    }
    
    // we are interested in the number of targets which do not have any strict response at all,
    // and the targets which do not have any lenient response at all (not strict or partial).
    // Then if we know the true missing strict, the number of lists with no responses will be
    // the missing strict minus the number of targets without any strict response etc.
    
    es.addTargetsWithStrictResponses(nTargetsWithStrictResponses);
    es.addTargetsWithLenientResponses(nTargetsWithLenientResponses);
    
    //2) from all possible pairings, find the maximal set that also
    //maximises the total score
    Collections.sort(possibleChoices, new PairingScoreComparator());
    Collections.reverse(possibleChoices);
    finalChoices = new ArrayList<Pairing>();

    while (!possibleChoices.isEmpty()) {
      Pairing bestChoice = (Pairing) possibleChoices.remove(0);
      // TODO: 
      bestChoice.consume();
      finalChoices.add(bestChoice);
      switch (bestChoice.value) {
        case CORRECT_VALUE: {
          //logger.debug("DEBUG: add a correct strict one: "+bestChoice.getKey());
          if (createAdditionalData) {
            Annotation tmp = bestChoice.getResponse();
            tmp.getFeatures().put("gate.plugin.evaluation.targetId", bestChoice.getKey().getId());
            correctStrictAnns.add(tmp);
          }
          es.addCorrectStrict(1);
          bestChoice.setPairingType(CORRECT_TYPE);
          break;
        }
        case PARTIALLY_CORRECT_VALUE: {  // correct but only opverlap, not coextensive
          //logger.debug("DEBUG: add a correct partial one: "+bestChoice.getKey());
          if (createAdditionalData) {
            Annotation tmp = bestChoice.getResponse();
            tmp.getFeatures().put("gate.plugin.evaluation.targetId", bestChoice.getKey().getId());
            correctPartialAnns.add(tmp);
          }
          es.addCorrectPartial(1);
          bestChoice.setPairingType(PARTIALLY_CORRECT_TYPE);
          break;
        }
        case MISMATCH_VALUE: { // coextensive and not correct
          if (bestChoice.getKey() != null && bestChoice.getResponse() != null) {
            es.addIncorrectStrict(1);
            bestChoice.setPairingType(MISMATCH_TYPE);
            if (createAdditionalData) {
              Annotation tmp = bestChoice.getResponse();
              tmp.getFeatures().put("gate.plugin.evaluation.targetId", bestChoice.getKey().getId());
              incorrectStrictAnns.add(tmp);
            }
          } else if (bestChoice.getKey() != null) {
            logger.debug("DEBUG: GOT a MISMATCH_VALUE (coext and not correct) with no key " + bestChoice);
          } else if (bestChoice.getResponse() != null) {
            logger.debug("DEBUG: GOT a MISMATCH_VALUE (coext and not correct) with no response " + bestChoice);
          }
          break;
        }
        case WRONG_VALUE: { // overlapping and not correct
          if (bestChoice.getKey() != null && bestChoice.getResponse() != null) {
            es.addIncorrectPartial(1);
            if (createAdditionalData) {
              Annotation tmp = bestChoice.getResponse();
              tmp.getFeatures().put("gate.plugin.evaluation.targetId", bestChoice.getKey().getId());
              incorrectPartialAnns.add(tmp);
            }
            bestChoice.setPairingType(MISMATCH_TYPE);
          } else if (bestChoice.getKey() == null) {
            // this is a responseAnns which overlaps with a keyAnns but does not have a keyAnns??
            logger.debug("DEBUG: GOT a WRONG_VALUE (overlapping and not correct) with no key " + bestChoice);
          } else if (bestChoice.getResponse() == null) {
            logger.debug("DEBUG: GOT a WRONG_VALUE (overlapping and not correct) with no response " + bestChoice);
          }
          break;
        }
        default: {
          throw new GateRuntimeException("Invalid pairing type: "
                  + bestChoice.value);
        }
      }
    }
    //add choices for the incorrect matches (MISSED, SPURIOUS)
    //get the unmatched keys
    for (int i = 0; i < keyChoices.size(); i++) {
      List<Pairing> aList = keyChoices.get(i);
      if (aList == null || aList.isEmpty()) {
        if (createAdditionalData) {
          Annotation tmp = keyList.get(i);
          tmp.getFeatures().put("gate.plugin.evaluation.targetId", tmp.getId());
          trueMissingLenientAnns.add(tmp);
        }
        Pairing choice = new Pairing(i, -1, WRONG_VALUE);
        choice.setPairingType(MISSING_TYPE);
        finalChoices.add(choice);
      }
    }

    //get the unmatched responses
    // In order to find overlaps between targets(keys) and spurious annotations, we need
    // to store the spurious annotations in an actual annotation set
    AnnotationSetImpl spuriousAnnSet = new AnnotationSetImpl(responseAnns.getDocument());
    //spuriousAnnSet.clear();
    for (int i = 0; i < responseChoices.size(); i++) {
      List<Pairing> aList = responseChoices.get(i);
      if (aList == null || aList.isEmpty()) {
        if (createAdditionalData) {
          trueSpuriousLenientAnns.add(responseList.get(i));
          spuriousAnnSet.add(responseList.get(i));
          Pairing choice = new Pairing(-1, i, WRONG_VALUE);
          choice.setPairingType(SPURIOUS_TYPE);
          finalChoices.add(choice);
        }
      }
    }

    // To count the single correct anntations, go through the correct annotations and find the
    // target they have been matched to, then see if that target overlaps with a spurious annotation.
    // If not we can count it as a single correct annotation. This is done for correct strict and
    // correct lenient.
    // We can only do this if we have the sets which will only happen if there is no scoreThreshold
    if (createAdditionalData) {
      for (Pairing p : finalChoices) {
        if (p.getPairingType() == CORRECT_TYPE) {
          Annotation t = p.getKey();
          AnnotationSet ol = gate.Utils.getOverlappingAnnotations(spuriousAnnSet, t);
          if (ol.size() == 0) {
            es.addSingleCorrectStrict(1);
            singleCorrectStrictAnns.add(p.getResponse());
          }
          //logger.debug("DEBUG have a correct strict choice, overlapping: "+ol.size()+" key is "+t);
        } else if (p.getPairingType() == PARTIALLY_CORRECT_TYPE) {
          Annotation t = p.getKey();
          AnnotationSet ol = gate.Utils.getOverlappingAnnotations(spuriousAnnSet, t);
          if (ol.size() == 0) {
            es.addSingleCorrectPartial(1);
            singleCorrectPartialAnns.add(p.getResponse());
          }
          //logger.debug("DEBUG have a correct partial choice, overlapping: "+ol.size()+" key is "+t);
        }
      }
    }
    if (createAdditionalData) {
      choices = finalChoices;
    } else {
      finalChoices = null;
    }
    // before we exit, make all the annotation sets we created immutable, if they are not already immutabe
    // new ImmutableAnnotationSetImpl(doc, annotationsToAdd)
    correctStrictAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), correctStrictAnns);
    correctPartialAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), correctPartialAnns);
    incorrectStrictAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), incorrectStrictAnns);
    incorrectPartialAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), incorrectPartialAnns);
    trueMissingLenientAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), trueMissingLenientAnns);
    trueSpuriousLenientAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), trueSpuriousLenientAnns);
    targetAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), targetAnns);
    singleCorrectPartialAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), singleCorrectPartialAnns);
    singleCorrectStrictAnns = new ImmutableAnnotationSetImpl(keyAnns.getDocument(), singleCorrectStrictAnns);
    return es;
  }

  /**
   * Check if a response annotation matches a key annotation. If the annotations have different
   * type, this returns false; Otherwise, if the features set is empty, this returns true;
   * Otherwise, if the features set is null, this returns true if all features in the key annotation
   * have the same values as the same features in the response annotation. Otherwise, if the feature
   * set is not empty, this compares the features based on the FeatureComparison specified: If it is
   * FEATURES_EQUALITY, then it only returns true of all the features in the features set have
   * identical values in the key and response annotation. If it is FEATURES_SUBSUMPTION, it returns
   * true if the response features subsume the key features limited to the features list.
   *
   * @param key
   * @param response
   * @param features
   * @return
   */
  public static boolean isAnnotationsMatch(Annotation key, Annotation response, Set<String> features,
          FeatureComparison fcmp, boolean is4List, AnnotationTypeSpecs typeSpecs) {
    // If we compare candidates for a list-based evaluation, we do not care about the type of 
    // the candidate ann
    if (!is4List) {
      if (typeSpecs == null) {
        if (!key.getType().equals(response.getType())) {
          return false;
        }
      } else {
        if (!key.getType().equals(typeSpecs.getKeyType(response.getType()))) {
          return false;
        }
      }
    }
    if (features == null) {
      // NOTE: originally we gave features==null a different meaning from features=[]:
      // The idea was to check if whatever features there are, check if they are compatible.
      // However, this behaviour is confusing and it is nearly impossible to choose between
      // null and [] in a PR parameter, so we decided to make both null and [] in the same way:
      // not care about any features at all
      // FeatureMap fmk = key.getFeatures();
      // FeatureMap fmr = key.getFeatures();
      // return fmr.subsumes(fmk);
      return true;
    } else {
      if (features.isEmpty()) {
        return true;
      } else {
        if (fcmp.equals(FeatureComparison.FEATURE_EQUALITY)) {
          // need to check if the features in the feature list all have the same value in both
          // annotations
          FeatureMap fmk = key.getFeatures();
          FeatureMap fmr = response.getFeatures();
          for (String fn : features) {
            Object o1 = fmk.get(fn);
            Object o2 = fmr.get(fn);
            //logger.debug("DEBUG: comparing values "+o1+" and "+o2);
            if (o1 == null && o2 != null) {
              return false;
            }
            if (o2 == null && o1 != null) {
              return false;
            }
            return o1.equals(o2);
          }
          return true;
        } else {
          FeatureMap fmk = key.getFeatures();
          FeatureMap fmr = key.getFeatures();
          return fmr.subsumes(fmk, features);
        }
      }
    }
  }

  /**
   * Adds a new pairing to the internal data structures.
   *
   * @param pairing the pairing to be added
   * @param index the index in the list of pairings
   * @param listOfPairings the list of {@link Pairing}s where the pairing should be added
   */
  protected void addPairing(Pairing pairing, int index, List<List<Pairing>> listOfPairings) {
    List<Pairing> existingChoices = listOfPairings.get(index);
    if (existingChoices == null) {
      existingChoices = new ArrayList<Pairing>();
      listOfPairings.set(index, existingChoices);
    }
    existingChoices.add(pairing);
  }

  /**
   * Represents a pairing of a key annotation with a response annotation and the associated score
   * for that pairing.
   */
  public class Pairing {

    Pairing(int keyIndex, int responseIndex, int value) {
      this.keyIndex = keyIndex;
      this.responseIndex = responseIndex;
      this.value = value;
      scoreCalculated = false;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      switch (getPairingType()) {
        case CORRECT_TYPE:
          sb.append("CORRECT: ");
          break;
        case PARTIALLY_CORRECT_TYPE:
          sb.append("PARTIAL: ");
          break;
        case MISSING_TYPE:
          sb.append("MISSING: ");
          break;
        case SPURIOUS_TYPE:
          sb.append("SPURIOUS: ");
          break;
        case MISMATCH_TYPE:
          sb.append("INCORRECT: ");
          break;
        default:
          sb.append("UNKNOWN: ");
          sb.append(getPairingType());
      }
      sb.append(", T=");
      sb.append(getKey());
      sb.append(", R=");
      sb.append(getResponse());
      return sb.toString();
    }

    public int getScore() {
      if (scoreCalculated) {
        return score;
      } else {
        calculateScore();
        return score;
      }
    }

    public int getKeyIndex() {
      return this.keyIndex;
    }

    public int getResponseIndex() {
      return this.responseIndex;
    }

    public int getValue() {
      return this.value;
    }

    public Annotation getKey() {
      return keyIndex == -1 ? null : keyList.get(keyIndex);
    }

    public Annotation getResponse() {
      return responseIndex == -1 ? null
              : responseList.get(responseIndex);
    }

    /**
     * Gets the pairing type, one of {@link #CORRECT_TYPE},
     * {@link #PARTIALLY_CORRECT_TYPE}, {@link #SPURIOUS_TYPE} or {@link #MISSING_TYPE}.
     *
     * @return an int value representign the pairing type.
     */
    public int getPairingType() {
      return type;
    }

    public void setPairingType(int type) {
      this.type = type;
    }

    /**
     * Removes all mutually exclusive OTHER choices possible from the data structures.
     * <tt>this</tt> gets removed from {@link #possibleChoices} as well.
     */
    public void consume() {
      possibleChoices.remove(this);
      List<Pairing> sameKeyChoices = keyChoices.get(keyIndex);
      sameKeyChoices.remove(this);
      possibleChoices.removeAll(sameKeyChoices);

      List<Pairing> sameResponseChoices = responseChoices.get(responseIndex);
      sameResponseChoices.remove(this);
      possibleChoices.removeAll(sameResponseChoices);

      for (Pairing item : new ArrayList<Pairing>(sameKeyChoices)) {
        item.remove();
      }
      for (Pairing item : new ArrayList<Pairing>(sameResponseChoices)) {
        item.remove();
      }
      sameKeyChoices.add(this);
      sameResponseChoices.add(this);
    }

    /**
     * Removes this choice from the two lists it belongs to
     */
    public void remove() {
      List<Pairing> fromKey = keyChoices.get(keyIndex);
      fromKey.remove(this);
      List<Pairing> fromResponse = responseChoices.get(responseIndex);
      fromResponse.remove(this);
    }

    /**
     * Calculates the score for this choice as: pairing-type - sum of all the pairing-types of all
     * OTHER mutually exclusive choices
     */
    void calculateScore() {
      //this needs to be a set so we don't count conflicts twice
      Set<Pairing> conflictSet = new HashSet<Pairing>();
      //add all the choices from the same response annotation
      conflictSet.addAll(responseChoices.get(responseIndex));
      //add all the choices from the same key annotation
      conflictSet.addAll(keyChoices.get(keyIndex));
      //remove this choice from the conflict set
      conflictSet.remove(this);
      score = value;
      for (Pairing item : conflictSet) {
        score -= item.getValue();
      }
      scoreCalculated = true;
    }

    /**
     * The index in the key collection of the key annotation for this pairing
     */
    int keyIndex;
    /**
     * The index in the response collection of the response annotation for this pairing
     */
    int responseIndex;

    /**
     * The type of this pairing.
     */
    int type;

    /**
     * The value for this pairing. This value depends only on this pairing, not on the conflict set.
     */
    int value;

    /**
     * The score of this pairing (calculated based on value and conflict set).
     */
    int score;
    boolean scoreCalculated;
  }

  /**
   * Compares two pairings: the better score is preferred; for the same score the better type is
   * preferred (exact matches are preffered to partial ones).
   */
  protected static class PairingScoreComparator implements Comparator<Pairing> {

    /**
     * Compares two choices: the better score is preferred; for the same score the better type is
     * preferred (exact matches are preffered to partial ones).
     *
     * @return a positive value if the first pairing is better than the second, zero if they score
     * the same or negative otherwise.
     */
    @Override
    public int compare(Pairing first, Pairing second) {
      //compare by score
      int res = first.getScore() - second.getScore();
      //compare by type
      if (res == 0) {
        res = first.getPairingType() - second.getPairingType();
      }
      //compare by completeness (a wrong match with both key and response
      //is "better" than one with only key or response
      if (res == 0) {
        res = (first.getKey() == null ? 0 : 1)
                + (first.getResponse() == null ? 0 : 1)
                + (second.getKey() == null ? 0 : -1)
                + (second.getResponse() == null ? 0 : -1);
      }
      if (res == 0) {
        // compare the offsets or the responses
        if(first.getResponse() != null && second.getResponse() != null) {
          res = first.getResponse().getStartNode().getOffset().compareTo(second.getResponse().getStartNode().getOffset());
          if(res == 0) {
            res = first.getResponse().getEndNode().getOffset().compareTo(second.getResponse().getEndNode().getOffset());
          }
          // if still equal, compare the annotation ids
          if(res == 0) {
            res = first.getResponse().getId().compareTo(second.getResponse().getId());
          }
        }        
      }
      if (res == 0) {
        // compare the offsets or the keys
        if(first.getKey() != null && second.getKey() != null) {
          res = first.getKey().getStartNode().getOffset().compareTo(second.getKey().getStartNode().getOffset());
          if(res == 0) {
            res = first.getKey().getEndNode().getOffset().compareTo(second.getKey().getEndNode().getOffset());
          }
          // if still equal, compare the annotation ids
          if(res == 0) {
            res = first.getKey().getId().compareTo(second.getKey().getId());
          }
        }
      }
      return res;
    }
  }

  /**
   * Type for correct pairings (when the key and response match completely)
   */
  public static final int CORRECT_TYPE = 0;

  /**
   * Type for partially correct pairings (when the key and response match in type and significant
   * features but the spans are just overlapping and not identical.
   */
  public static final int PARTIALLY_CORRECT_TYPE = 1;

  /**
   * Type for missing pairings (where the key was not matched to a response).
   */
  public static final int MISSING_TYPE = 2;

  /**
   * Type for spurious pairings (where the response is not matching any key).
   */
  public static final int SPURIOUS_TYPE = 3;

  /**
   * Type for mismatched pairings (where the key and response are co-extensive but they don't
   * match).
   */
  public static final int MISMATCH_TYPE = 4;

  /**
   * Score for a correct pairing.
   */
  private static final int CORRECT_VALUE = 3;

  /**
   * Score for a partially correct pairing.
   */
  private static final int PARTIALLY_CORRECT_VALUE = 2;

  /**
   * Score for a mismatched pairing (higher then for WRONG as at least the offsets were right).
   */
  private static final int MISMATCH_VALUE = 1;

  /**
   * Score for a wrong (missing or spurious) pairing.
   */
  private static final int WRONG_VALUE = 0;

  /**
   * A list with all the key annotations
   */
  protected List<Annotation> keyList;

  /**
   * A list with all the response annotations
   */
  protected List<Annotation> responseList;

  /**
   * A list of lists representing all possible choices for each key
   */
  protected List<List<Pairing>> keyChoices;

  /**
   * A list of lists representing all possible choices for each response
   */
  protected List<List<Pairing>> responseChoices;

  /**
   * All the posible choices are added to this list for easy iteration.
   */
  protected List<Pairing> possibleChoices;

  /**
   * A list with the choices selected for the best result.
   */
  protected List<Pairing> finalChoices;

  public List<Pairing> getFinalChoices() {
    return finalChoices;
  }

  /**
   * A class to represent a candidate list as we need it for our evaluation. The original candidate
   * lists may be unsorted or sorted by a key which is different from the score we want to use. This
   * class represents each list the way we need it. It also allows to set a score or rank threshold
   * after which only those candidates within the threshold will be visible and the value returned
   * by the size() method is adjusted accordingly. Also, since the score or rank threshold needs to
   * get adjusted frequently, we use a method that avoids going through all candidates every time
   * when the score threshold gets adjusted.
   */
  public static class CandidateList {
    // the constructor takes the original list annotation and initializes this object 
    // with all the candidate annotations, sorted by the value of the specified score feature, if
    // the score feature is not null. 
    // NOTE: if the elementType is null, we do not check that the element type matches the 
    // type of the candidate annotation, otherwise only candidates with elementType as type
    // get included in the list!
    public CandidateList(AnnotationSet annSet, Annotation listAnn, String listIdFeature,
            String scoreFeature, String elementType, boolean filterNils, String nilValue,
            String idFeature) {

      if (scoreFeature == null || scoreFeature.isEmpty()) {
        this.scoreFeature = null; // this indicates that we do not have a score feature 
      } else {
        this.scoreFeature = scoreFeature;
      }
      this.listAnn = listAnn;
      this.type = listAnn.getType();
      // First get the list of ids
      Object val = listAnn.getFeatures().get(listIdFeature);
      if (!(val instanceof List<?>)) {
        throw new GateRuntimeException("The listIdFeature for a list annotation does not contain a list: " + listAnn);
      }
      List<Integer> ids = (List<Integer>) val;
      logger.debug("DEBUG: processing list annotation " + listAnn);
      logger.debug("DEBUG: id list is " + ids);

      if (!ids.isEmpty()) {
        cands = new ArrayList<Annotation>(ids.size());
        for (Integer id : ids) {
          logger.debug("DEBUG: trying to get annotation for id " + id);
          Annotation cand = annSet.get(id);
          if (cand == null) {
            throw new GateRuntimeException("Something is wrong: no candidate for id=" + id + " for list ann=" + listAnn);
          }
          if (elementType == null || cand.getType().equals(elementType)) {
            if (filterNils) {
              String nilFValue = (String) cand.getFeatures().get(idFeature);
              if (!nilFValue.equals(nilValue)) {
                cands.add(cand);
              }
            } else {
              cands.add(cand);
            }
          }
        }
        if (this.scoreFeature != null) {
          ByScoreComparator comp = new ByScoreComparator(scoreFeature);
          Collections.sort(cands, comp);
          logger.debug("DEBUG: cands sorted, is now " + cands);
        }
        theSize = cands.size();
      } else {
        theSize = 0;
        cands = new ArrayList<Annotation>(0);
      }
    }
    private int theSize = 0;

    /**
     * Current visible size of the list, depending onthe threshold that has been set.
     * After initialization it is the number of all candidates that were annotations (ids which
     * do not point to annotations are discarded) which match the element type if it was given, 
     * which is identical to sizeAll()
     * @return 
     */
    public int size() {
      return theSize;
    }
    
    /**
     * Return the number of all candidates, irrespective of the threshold.
     * @return 
     */
    public int sizeAll() {
      return cands.size();
    }
    

    private double currentThreshold = Double.NEGATIVE_INFINITY;

    private String scoreFeature;
    private List<Annotation> cands;
    private Annotation listAnn;
    private String type;

    /**
     * Set the score threshold. After this the value returned by size() will be adjusted to only
     * include those elements with a score >= the given score.
     *
     * @param th
     */
    public void setThreshold(double th) {
      if (scoreFeature == null) {
        throw new GateRuntimeException("setThreshold can only be used if there is a score feature!");
      }
      if (th == currentThreshold) {
        return;
      }
      // if th > currentThreshold, then we need to reduce the size of the visible candidates
      // by excluding those which have a score < the new threshold
      // if th < currentThreshold, then we need to increase the size of the visible candidates
      // by including those which have a score >= the new threshold
      int newSize = size();
      if (th < currentThreshold) {
        for (int i = this.size(); i < cands.size(); i++) {
          double sc = object2Double(cands.get(i).getFeatures().get(scoreFeature));
          if (sc >= th) {
            newSize = i;
          } else {
            break;
          }
        }
      } else if (th > currentThreshold) {
        for (int i = this.size() - 1; i >= 0; i--) {
          double sc = object2Double(cands.get(i).getFeatures().get(scoreFeature));
          if (sc < th) {
            newSize = i;
          } else {
            break;
          }
        }
      }
      theSize = newSize;
      currentThreshold = th;
    }

    public void setRank(int rank) {
      if (rank >= cands.size()) {
        theSize = cands.size();
      } else if (rank < 0) {
        theSize = 0;
      } else {
        theSize = rank + 1;
      }
    }

    /**
     * Unset any rank or threshold limit that may be in place.
     */
    public void clearLimits() {
      theSize = cands.size();
      currentThreshold = Double.NEGATIVE_INFINITY;
    }
    
    
    public Annotation get(int index) {
      if (index >= theSize) {
        throw new GateRuntimeException("Attempt to access element larger than the currently set size");
      }
      return cands.get(index);
    }

    public List<Annotation> getList() {
      List<Annotation> ret = new ArrayList<Annotation>();
      for (int i = 0; i < theSize; i++) {
        ret.add(cands.get(i));
      }
      return ret;
    }

    public Annotation getListAnnotation() {
      return listAnn;
    }

    protected static class ByScoreComparator implements Comparator<Annotation> {

      private String scoreFeature;

      public ByScoreComparator(String fn) {
        scoreFeature = fn;
      }

      @Override
      public int compare(Annotation o1, Annotation o2) {
        double s1 = object2Double(o1.getFeatures().get(scoreFeature));
        double s2 = object2Double(o2.getFeatures().get(scoreFeature));
        return Double.compare(s2, s1);
      }

    }
  }

  public static double object2Double(Object tmp) {
    if (tmp == null) {
      return Double.NaN;
    } else {
      if (tmp instanceof Double) {
        return (Double) tmp;
      } else if (tmp instanceof Float) {
        return ((Float) tmp).doubleValue();
      } else if (tmp instanceof Number) {
        return ((Number) tmp).doubleValue();
      } else {
        throw new GateRuntimeException("Expected an object that is a Double or number but got " + tmp.getClass() + ": " + tmp);
      }
    }
  }

  private static double round(double what, double factor) {
    return (double) Math.round(what * factor) / factor;
  }

  /**
   * Return the value of a FeatureMap entry as a double. If the entry is not found, the defaultValue
   * is returned. If the entry cannot be converted to a double, an exception is thrown (depending on
   * what kind of conversion was attempted, e.g. when converting from a string, it could be a
   * NumberFormatException).
   *
   * @param fm
   * @param key
   * @param defaultValue
   * @return
   */
  public static double getFeatureDouble(FeatureMap fm, String key, double defaultValue) {
    Object value = fm.get(key);
    if (value == null) {
      return defaultValue;
    }
    double ret = defaultValue;
    if (value instanceof Double) {
      ret = (Double) value;
    } else if (value instanceof Number) {
      ret = ((Number) value).doubleValue();
    } else if (value instanceof String) {
      ret = Double.valueOf((String) value);
    }
    return ret;
  }

  /**
   * Return the set of annotations which overlap with an annotation not matchin an annotation in
   * another set. This returns the subset of annotations from the annotation set "from" which
   * overlap with the given annotation "ann" but are not a) coextensive with an annotation in the
   * set "notIn" AND match that annotation based on the given features and feature comparison
   * method.
   * <p>
   * This method is only used for the utility method to create indicator annotations for the changes
   * between the reference and response set.
   *
   * @param from
   * @param ann
   * @param notIn
   * @param fs
   * @param fc
   * @return
   */
  private static AnnotationSet getOverlappingAnnsNotIn(AnnotationSet from, Annotation ann,
          AnnotationSet notIn, Set<String> fs, FeatureComparison fc) {
    AnnotationSet tmpSet = new AnnotationSetImpl(from.getDocument());
    tmpSet.addAll(gate.Utils.getOverlappingAnnotations(from, ann));
    // now make sure that none of the annotations in tmpSet occurs in notIn
    Iterator<Annotation> it = tmpSet.iterator();
    while (it.hasNext()) {
      Annotation a = it.next();
      AnnotationSet coexts = gate.Utils.getCoextensiveAnnotations(notIn, a, a.getType());
      for (Annotation c : coexts) {
        // if c matches a, remove a, i.e. do it.remove()
        //System.out.println("DEBUG: set is "+fs+" method is "+fc);
        //System.out.println("DEBUG: Comparing for exclusion: "+a+" WITH "+c);
        if (isAnnotationsMatch(a, c, fs, fc, false, null)) {
          //System.out.println("DEBUG: FOUND A MATCH!!!!");
          it.remove();
          break;
        }
      }
    }
    return tmpSet;
  }

  private static void addAnnsWithTypeSuffix(AnnotationSet outSet, Collection<Annotation> inAnns, String suffix, String changeInd) {
    for (Annotation ann : inAnns) {
      FeatureMap fm = gate.Utils.toFeatureMap(ann.getFeatures());
      if (changeInd != null && !changeInd.isEmpty()) {
        fm.put("_eval.change", changeInd);
      }
      gate.Utils.addAnn(outSet, ann, ann.getType() + suffix, fm);
    }
  }
  
  /**
   * Comparator to sort annotations by offset (first starting, then ending), then by 
   * the features in the features set, then by annotation id. 
   */
    public static class OffsetAndMoreComparator implements Comparator<Annotation> {

    private List<String> features = null;

    OffsetAndMoreComparator(Set<String> features) {
      if(features != null) {
        this.features = new ArrayList<String>(features);     
        Collections.sort(this.features);  // we need to sort so we always compare features in the same order
      }
    }

    @Override
    public int compare(Annotation a1, Annotation a2) {
      int result;

      // compare start offsets
      result = a1.getStartNode().getOffset().compareTo(
              a2.getStartNode().getOffset());

      // if start offsets are equal compare end offsets
      if (result == 0) {
        result = a1.getEndNode().getOffset().compareTo(
                a2.getEndNode().getOffset());
      } // if

      if(result == 0) {
        // we still cannot distinguish the annotations, try comparing by features, but only
        // if we actually have features
        if(features != null && !features.isEmpty()) {
          // compare by each feature until we get something that is not equal, then return
          // null is always < than some value
          for(String feature : features) {
            Object v1 = a1.getFeatures().get(feature);
            Object v2 = a2.getFeatures().get(feature);
            if(v1 == null && v2 == null) {
              // result = 0;
            } else if(v1 == null && v2 != null) {
              return -1;
            } else if(v2 != null && v2 == null) {
              return 1;
            } else {
              if(v1 instanceof Comparable && v2 instanceof Comparable) {
                result = ((Comparable)v1).compareTo((Comparable)v2);
                if(result != 0) return result;
              } else {
                // result = 0;
              }
            }
          } // for
          // if we exit the for loop, than we did not return with result!=0, so result is still 0
          // We need to compare the annotation Ids
          return(a1.getId().compareTo(a2.getId()));
        }
      }
      
      return result;
    }
  } 

}
