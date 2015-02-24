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
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 * A class for finding the differences between two annotation sets and calculating the counts
 * needed to obtain precision, recall, f-measure and other statistics.
 *
 * This is loosely based on the {@link gate.util.AnnotationDifferTagging} class but has been heavily 
 * modified. One important change is that all the counts and the methods for calculating 
 * measures from the counts are kept in a separate object of type 
 * {@link EvalStatsTagging}.
 * <p>
 * This class is mainly for finding the
 * optimal matchings between the target and response sets and for storing the response and target
 * annotations that correspond to correct, incorrect, missing or spurious matches. 
 * <p>Unlike the old class, this class also supports the use of a feature with a score in the
 * response annotations: the comparison can be done for only those responses where the score is
 * larger or equal than a given threshold. There is also a static method 
 * for creating a {@link ByThEvalStatsTagging} for a set of thresholds.
 * <p>
 * This class also supports finding the best matches from response lists: a response list is 
 * represented by an annotation which contains a special "idList" feature. The "idList" feature
 * is a list of annotation ids which are considered to be all response candidates. Each response
 * candidate must have a score feature and the response candidate list is used sorted by descending 
 * score. When evaluating the response lists, this class will find, for each candidate list,
 * the response candidate with the highest score larger than a given score threshold 
 * that is equal or partially equal to the target or
 * use the response candidate with the highest score larger than the given score threshold. There is
 * also a static method
 * for creating a {@link ByThEvalStatsTagging} object for a set of thresholds.
 * <p>
 * This class also allows to choose between two ways of comparing annotations by the values of
 * one or more features: if {@link FeatureComparison.FEATURES_EQUALITY} is used, then all the 
 * features specified must have identical values in in the target and response annotation. If
 * {@link FeatureComparison.FEATURES_SUBSUMPTION} is used, then only those features from the 
 * specified feature list which do occur in the target annotation must match in the response
 * annotation.
 *<p>Usage: other than the old class, this class requires all the parameters to be supplied in
 * the constructor. Once the object has been created, all the data that has been calculated 
 * is available. Different constructors can be used to perform different kinds of calculations.
 * <p>
 * Certain operations will require the creation of several AnnotationDifferTagging objects internally
 * and for these operations, the user should call one of the static methods.
 * 
 * 
 * @author Valentin Tablan
 * @author Johann Petrak
 */
public class AnnotationDifferTagging {
  
  
  protected static final Logger logger = Logger.getLogger(AnnotationDifferTagging.class);
  
  protected EvalStatsTagging evalStats = new EvalStatsTagging();

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

  // This is only used if we have a threshold feature;
  protected ByThEvalStatsTagging evalStatsTaggingByThreshold;

  /**
   * Get counters by thresholds for all the scores we have seen.
   *
   * This will only return non-null if a score feature was specified when the
   * AnnotationDifferTagging object was created.
   *
   * @return The map with all seen scores mapped to their EvalStatsTagging.
   */
  public ByThEvalStatsTagging getEvalPRFStatsByThreshold() {
    return evalStatsTaggingByThreshold;
  }

  private AnnotationDifferTagging() {
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
   * equal and c) they features match according to the features list (see below).
   * @param features If null, the features match if all features that are in the target are also in
   * the response and have the same value as in the target. If an empty list, no features are
   * compared. Otherwise, the features in this list must have identical values in the target and in
   * the response. This also means that if a feature specified in this list is not present in the
   * target, it also must be not present in the response. (NOTE: this is different from how the old
   * AnnotationDiffer worked where if a feature was missing in the target it was never used for the
   * comparison at all).
   */
  public AnnotationDifferTagging(
          AnnotationSet targets,
          AnnotationSet responses,
          Set<String> features,
          FeatureComparison fcm
  ) {
    this(targets,responses,features,fcm,null,Double.NaN);
  }

  /**
   * Create a differ that will also calculate the stats by score thresholds. This does the same as
   * the constructor AnnotationDiffer(targets,responses,features) but will in addition also expect
   * every response to have a feature with the name given by the thresholdFeature parameter. This
   * feature is expected to contain a value that can be converted to a double and which will be
   * interpreted as a score or confidence. This score can then be used to perform the evaluation
   * such that only responses with a score higher than a certain threshold will be considered. The
   * differ will update the NavigableMap passed to the constructor to add an EvalStatsTagging object
   * for each score that is encountered in the responses set.
   * <p>
   * In order to create the statistics for several documents a single NavigableMap has to be used
   * for every AnnotationDiffer that is used for each document. Every time a new AnnotationDiffer
   * compares a new pair of sets, that NavigableMap will incrementally get updated with the new
   * counts. If either the thresholdFeature is empty or null or the byThresholdEvalStats object is
   * null, no statistics by thresholds will be calculated. If the thresholdFeature is empty or null
   * but a map is passed to the AnnotationDiffer, the map will remain unchanged.
   *
   * @param targets
   * @param responses
   * @param features
   * @param thresholdFeature
   * @param byThresholdEvalStats
   */
  public AnnotationDifferTagging(
          AnnotationSet targets,
          AnnotationSet responses,
          Set<String> features,
          FeatureComparison fcmp,
          String scoreFeature,
          double thresholdValue
  ) {
    //this.targets = targets;
    //this.responses = responses;
    //this.thresholdFeature = thresholdFeature;
    //this.byThresholdEvalStats = byThresholdEvalStats;
    //this.features = features;
    //if(features != null) {
    //  featuresSet = new HashSet<String>(features);
    //}
    evalStats = calculateDiff(targets, responses, features, fcmp, scoreFeature, thresholdValue, null);
  }
  
  /**
   * Calculate a new or add to an existing ByThEvalStatsTagging object.
   * If this is called with the byThresholdEvalStats parameter not null, then the by thresholds
   * statistics for the differences between targets and responses will be added to that 
   * object (and it will be returned), otherwise a new object will be created for the statistics
   * and returned. An exception will be thrown if a byThresholdEvalStats object is passed to this
   * method and its ThresholdsToUse setting is different from the one passed to this method. 
   * @param targets
   * @param responses
   * @param features
   * @param fcmp
   * @param scoreFeature
   * @param byThresholdEvalStats
   * @return 
   */
  public static ByThEvalStatsTagging calculateByThEvalStatsTagging(
          AnnotationSet targets,
          AnnotationSet responses,
          Set<String> featureSet,
          FeatureComparison fcmp,
          String scoreFeature,
          ThresholdsToUse thToUse,
          ByThEvalStatsTagging existingByThresholdEvalStats
  ) {
    ByThEvalStatsTagging byThresholdEvalStats = null;
    if(existingByThresholdEvalStats == null) {
      byThresholdEvalStats = new ByThEvalStatsTagging(thToUse);
    } else {
      if(existingByThresholdEvalStats.getWhichThresholds() != thToUse) {
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
            score = round(score,100.0);
          }
          thresholds.add(score);
        }
      } else {
        thresholds = new TreeSet<Double>(byThresholdEvalStats.getWhichThresholds().getThresholds());
      }
      // Now calculate the EvalStatsTagging(threshold) for each threshold we found in decreasing order.
      // The counts we get will need to get added to all existing EvalStatsTagging which are already
      // in the byThresholdEvalStats map for thresholds less than or equal to that threshold. 

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
        EvalStatsTagging es = tmpAD.calculateDiff(targets, responses, featureSet, fcmp, scoreFeature, t, null);
        newMap.put(t, es);
      }
      // add the new map to our Map
      byThresholdEvalStats.add(newMap);

    return byThresholdEvalStats;
  }
  
  public static List<CandidateList> createCandidateLists(AnnotationSet candidatesSet, 
          AnnotationSet listAnnotations, String listIdFeature, String scoreFeature) {
    // for each response, create an actual sorted list of candidate annotations and also store the 
    // minimum and maximum score.
    List<CandidateList> responseCandidates = new ArrayList<CandidateList>(listAnnotations.size());
    for(Annotation ann : listAnnotations) {
      CandidateList cl = new CandidateList(candidatesSet, ann, listIdFeature, scoreFeature);
      if(cl.size > 0) {
        responseCandidates.add(cl);
      }
    }
    return responseCandidates;
    
  }
  
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
          ByThEvalStatsTagging existingByThresholdEvalStats
  ) {
    ByThEvalStatsTagging byThresholdEvalStats = null;
    if(existingByThresholdEvalStats == null) {
      byThresholdEvalStats = new ByThEvalStatsTagging(thToUse);
    } else {
      if(existingByThresholdEvalStats.getWhichThresholds() != thToUse) {
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
    if(thToUse == ThresholdsToUse.USE_ALL || thToUse == ThresholdsToUse.USE_ALLROUNDED) {
      allScores = new HashSet<Double>();
    }
    
        if(allScores != null) {
          for(CandidateList listOfCandList : responseCandidatesLists) {
          for(Annotation ann : listOfCandList.getList()) {
            double score = object2Double(ann.getFeatures().get(scoreFeature));
            if(thToUse == ThresholdsToUse.USE_ALLROUNDED) { 
              score = round(score,100.0);
            }
            allScores.add(score);
          }
          }
        }
    
    
    // Either use the predefined set of thresholds or get the thresholds from the scores from
    // all the annotations pointed to from the list annotation. 
    NavigableSet<Double> thresholds = new TreeSet<Double>();
    if(thToUse == ThresholdsToUse.USE_ALL || thToUse == ThresholdsToUse.USE_ALLROUNDED) {
      thresholds.addAll(allScores);
    } else {
      // TODO: we can save some time by pre-creating the actual navigable sets of thresholds 
      thresholds.addAll(byThresholdEvalStats.getWhichThresholds().getThresholds());
    }
    
    // By increasing threshold, process the listAnnotations: 
    ByThEvalStatsTagging newMap = new ByThEvalStatsTagging();
    AnnotationDifferTagging tmpAD = new AnnotationDifferTagging();
    tmpAD.createAdditionalData = false;
    for(double th : thresholds) {
      logger.debug("DEBUG: running differ for th "+th+" nr targets is "+targets.size()+" nr responseCands is "+responseCandidatesLists.size());
      EvalStatsTagging es = tmpAD.calculateDiff(targets, listAnnotations, featureSet, fcmp, scoreFeature, th, responseCandidatesLists);
      logger.debug("DEBUG: got stats: "+es);
      newMap.put(th, es);      
    }
    byThresholdEvalStats.add(newMap);
    
    return byThresholdEvalStats;
  }  

  //protected Collection<Annotation> targets;
  //protected Collection<Annotation> responses;
  //protected String thresholdFeature;
  //protected ByThEvalStatsTagging byThresholdEvalStats;
  //protected List<String> features;
  // protected Set<String> featuresSet;

  /**
   * Interface representing a pairing between a key annotation and a response one.
   */
  protected static interface Pairing {

    /**
     * Gets the key annotation of the pairing. Can be <tt>null</tt> (for spurious matches).
     *
     * @return an {@link Annotation} object.
     */
    public Annotation getKey();

    public int getResponseIndex();

    public int getValue();

    public int getKeyIndex();

    public int getScore();

    public void setType(int i);

    public void remove();

    /**
     * Gets the response annotation of the pairing. Can be <tt>null</tt> (for missing matches).
     *
     * @return an {@link Annotation} object.
     */
    public Annotation getResponse();

    /**
     * Gets the type of the pairing, one of {@link #CORRECT_TYPE},
     * {@link #PARTIALLY_CORRECT_TYPE}, {@link #SPURIOUS_TYPE} or {@link #MISSING_TYPE},
     *
     * @return an <tt>int</tt> value.
     */
    public int getType();
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
   */
  public void addIndicatorAnnotations(AnnotationSet outSet) {
    addAnnsWithTypeSuffix(outSet, getCorrectStrictAnnotations(), "_CS");
    addAnnsWithTypeSuffix(outSet, getCorrectPartialAnnotations(), "_CP");
    addAnnsWithTypeSuffix(outSet, getIncorrectStrictAnnotations(), "_IS");
    addAnnsWithTypeSuffix(outSet, getIncorrectPartialAnnotations(), "_IP");
    addAnnsWithTypeSuffix(outSet, getTrueMissingLenientAnnotations(), "_ML");
    addAnnsWithTypeSuffix(outSet, getTrueSpuriousLenientAnnotations(), "_SL");
  }

  public static void addChangesToContingenyTables(
          AnnotationDifferTagging responses,
          AnnotationDifferTagging reference, 
          ContingencyTable tableStrict,
          ContingencyTable tableLenient) {
    if(tableStrict == null & tableLenient == null) {
      throw new RuntimeException("Both contingency tables null, no point of calling this method!");
    }
    if(tableStrict != null && (tableStrict.nRows() != 2 || tableStrict.nColumns() != 2)) {
      throw new RuntimeException("tableStrict is not a 2x2 contingency table");
    }
    if(tableLenient != null && (tableLenient.nRows() != 2 || tableLenient.nColumns() != 2)) {
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
    for(Annotation ann : responses.getTargetAnnotations()) {
      boolean resStrictCorrect = false;
      boolean resLenientCorrect = false;
      boolean refStrictCorrect = false;
      boolean refLenientCorrect = false;
      
      // Either set is correct for a target if there is a singleCorrect answer
      AnnotationSet tmpSet = Utils.getCoextensiveAnnotations(respsCorrect, ann);
      if(tmpSet.size() > 0) {
        resStrictCorrect = true;
        resLenientCorrect = true;
      }
      
      tmpSet = Utils.getCoextensiveAnnotations(refsCorrect, ann);
      if(tmpSet.size() > 0) {
        refStrictCorrect = true;
        refLenientCorrect = true;
      }
      
      // if the lenient flags are still false, lets check if we got a partial response
      tmpSet = Utils.getOverlappingAnnotations(respsCorrectPartial, ann);
      if(tmpSet.size() > 0) {
        resLenientCorrect = true;
      }
      tmpSet = Utils.getOverlappingAnnotations(refsCorrectPartial, ann);
      if(tmpSet.size() > 0) {
        refLenientCorrect = true;
      }
      if(tableStrict != null) {
        if(refStrictCorrect && resStrictCorrect) {
          tableStrict.incrementBy(0, 0, 1);
        } else if(refStrictCorrect && !resStrictCorrect) {
          tableStrict.incrementBy(0, 1, 1);
        } else if(!refStrictCorrect && resStrictCorrect) {
          tableStrict.incrementBy(1, 0, 1);
        } else if(!refStrictCorrect && !resStrictCorrect) {
          tableStrict.incrementBy(1, 1, 1);
        }
      }
      if(tableLenient != null) {
        if(refLenientCorrect && resLenientCorrect) {
          tableLenient.incrementBy(0, 0, 1);
        } else if(refLenientCorrect && !resLenientCorrect) {
          tableLenient.incrementBy(0, 1, 1);
        } else if(!refLenientCorrect && resLenientCorrect) {
          tableLenient.incrementBy(1, 0, 1);
        } else if(!refLenientCorrect && !resLenientCorrect) {
          tableLenient.incrementBy(1, 1, 1);
        }
      }
    } // for
  }

  /**
   * Add the annotations that indicate changes between responses.
   * @param outSet
   * @param responses
   * @param reference
   */
  public static void addChangesIndicatorAnnotations(AnnotationDifferTagging responses, 
          AnnotationDifferTagging reference, AnnotationSet outSet) {
    
    
  // The following changes are, in theory possible:
  // CS -> CP, IS, IP, ML
    for(Annotation ann : reference.getCorrectStrictAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_CP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_IS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_IP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getTrueMissingLenientAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CS_ML");
    }
  // CP -> CS, IS, IP, ML
    for(Annotation ann : reference.getCorrectPartialAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_CS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_IS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_IP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getTrueMissingLenientAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_CP_ML");
    }
  // IS -> IP, CS, CP, ML
    for(Annotation ann : reference.getIncorrectStrictAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_IP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_CS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_CP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getTrueMissingLenientAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IS_ML");
    }
  // IP -> IS, CS, CP, ML
    for(Annotation ann : reference.getIncorrectPartialAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_IS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_CS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_CP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getTrueMissingLenientAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_IP_ML");
    }
  // ML -> CS, CP, IS, IP
    for(Annotation ann : reference.getTrueMissingLenientAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_CS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getCorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_CP");
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectStrictAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_IS");
      tmpSet = Utils.getOverlappingAnnotations(responses.getIncorrectPartialAnnotations(), ann);
      addAnnsWithTypeSuffix(outSet, tmpSet, "_ML_IP");
    }
  // SL -> A (absent)
    for(Annotation ann : reference.getTrueSpuriousLenientAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(responses.getTrueSpuriousLenientAnnotations(), ann);
      if(tmpSet.size() == 0) {
        addAnnsWithTypeSuffix(outSet, tmpSet, "_SL_A");
      }
    }
  // A (absent) -> SL
    for(Annotation ann : responses.getTrueSpuriousLenientAnnotations()) {
      AnnotationSet tmpSet;
      tmpSet = Utils.getOverlappingAnnotations(reference.getTrueSpuriousLenientAnnotations(), ann);
      if(tmpSet.size() == 0) {
        addAnnsWithTypeSuffix(outSet, tmpSet, "_A_SL");
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

  // TODO: the following is for calculating McNemar's test
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
  //public static ContingencyTable getPairedCorrectnessCountsStrict(
  //  ContingencyTable toIncrement, AnnotationDifferTagging responseDiffer, AnnotationDifferTagging referenceDiffer) {
  //  
  //}
  private static void addAnnsWithTypeSuffix(AnnotationSet outSet, Collection<Annotation> inAnns, String suffix) {
    for (Annotation ann : inAnns) {
      gate.Utils.addAnn(outSet, ann, ann.getType() + suffix, gate.Utils.toFeatureMap(ann.getFeatures()));
    }
  }

  // this contains the final choices calculated by this object.
  protected List<Pairing> choices = new ArrayList<Pairing>();

  // TODO: figure out how to support calculating Krippendorff's alpha and Fleiss's Kappa too!
  // Ideally all these things would incrementally calculate whatever contingency tables they need,
  // so the method would take an existing table and two differs and increment the counts.
  
  
  /**
   * This controls if the indiciator annotations are created. 
   * By default this is true, but if the class is used internally by a static method where 
   * the annotations would never get used anyway, the flag will be set to false.
   */
  private boolean createAdditionalData = true;
  
  /**
   * Computes a diff between two collections of annotations.
   *
   */
  private EvalStatsTagging calculateDiff(
          AnnotationSet keyAnns,
          AnnotationSet responseAnns,
          Set<String> features,
          FeatureComparison fcmp,
          String scoreFeature, // if not null, the name of a score feature
          double threshold, // if not NaN, we will calculate the stats only for responses with score >= threshold
          List<CandidateList> candidateLists
  ) {
    logger.debug("DEBUG: calculating the differences for threshold "+threshold);
    EvalStatsTagging es = new EvalStatsTagging(threshold);

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
    responseList = null;
    // If we do list processing, this records, for each response annotation, what the corresponding
    // index of the candidate list is. Since the responeList only contains annotations from those
    // candidate lists which have still at least one candidate, there is no 1:1 mapping between
    // the index of an annotation in the responseList and the index of the candidate lists, so 
    // this list is used instead.
    List<Integer> candidateIndices = new ArrayList<Integer>();
    
    // if the candidateLists parameter is not null, we need to prepare the responses 
    // from those lists.
    if(candidateLists != null) {
      logger.debug("DEBUG: candidate list size is "+candidateLists.size());
      // we create the response list by going through all the candidate lists and 
      // adding the highest score candidate to the response list if the candidate list 
      // still has entries for the threshold. That candidate may get replaced later ...
      responseList = new ArrayList<Annotation>(responseAnns.size());
      int cidx = 0;
      for(CandidateList cand : candidateLists) {
        cand.setThreshold(threshold);
        if(cand.size != 0) {
          responseList.add(cand.get(0));
          candidateIndices.add(cidx);
        } 
        cidx++;
      }
      logger.debug("DEBUG: response list size is now: "+responseList.size());
    } else {
      
      // if we do not need to process the candidate lists, check if we need to process for 
      // a threshold
    
      if (Double.isNaN(threshold)) {
        responseList = new ArrayList<Annotation>(responseAnns);
      } else {
        responseList = new ArrayList<Annotation>(responseAnns.size());
        for (Annotation res : responseAnns) {
          double score = getFeatureDouble(res.getFeatures(), scoreFeature, Double.NaN);
          if (Double.isNaN(score)) {
            throw new GateRuntimeException("Response without a score feature: " + res);
          }
          if (score >= threshold) {
            responseList.add(res);
          }
        }
      }
    } 
    //logger.debug("DEBUG: responseList size for threshold "+threshold+" is "+responseList.size());

    
    
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
    for (int i = 0; i < keyList.size(); i++) {
      for (int j = 0; j < responseList.size(); j++) {
        Annotation keyAnn = keyList.get(i);

        
        Annotation resAnn = null;
        PairingImpl choice = null;
        // If we process candidate lists, do not just compare with the response
        // annotation from the list but instead compare with all candidates still in the list
        // and use the first exact match, if none is found, the first partial match, if none
        // is found the candidate with the highest score that is coextensive, if none is found
        // the candidate with the highest score.
        // However to decide if we should attempt a match at all, we first compare the 
        // range if the list annotation with the key annotation. Only if they overlap, we 
        // go through the candidates.
        if(candidateLists != null) {
          CandidateList candList = candidateLists.get(candidateIndices.get(j));
          if(keyAnn.overlaps(candList.getListAnnotation())) {
            // find the best matching annotation and remember which kind of match we had
            int match = WRONG_VALUE;
            Annotation bestAnn = responseList.get(j);
            for(int c = 0; c < candList.size; c++) {
              Annotation tmpResp = candList.get(c);
              logger.debug("Checking annotation for th="+threshold+" at index: "+c+": "+tmpResp);
              if(isAnnotationsMatch(keyAnn,tmpResp,features,fcmp)) {
                // if we are coextensive, then we can stop: can't get any better!
                if(keyAnn.coextensive(tmpResp)) {
                  logger.debug("Found correct match!!");
                  match = CORRECT_VALUE;
                  bestAnn = tmpResp; 
                  break;
                } else {
                  logger.debug("Found a partial match, checking if we can add!");
                  // if we did not already find a match, store
                  if(match == WRONG_VALUE || match == MISMATCH_VALUE) {
                    logger.debug("Found a partial match and adding!");
                    match = PARTIALLY_CORRECT_VALUE;
                    bestAnn = tmpResp;
                  }
                }
              } else if(keyAnn.coextensive(tmpResp) && match == WRONG_VALUE) {
                logger.debug("Found a MISMATCH");
                match = MISMATCH_VALUE;
                bestAnn = tmpResp;
              } else {
                logger.debug("Found ODD: match="+match);
              }
            } // for
            responseList.set(j, bestAnn);
            choice = new PairingImpl(i,j,match);
          }
        } else {
        
          resAnn = responseList.get(j);
          choice = null;
          if (keyAnn.coextensive(resAnn)) {
            //we have full overlap -> CORRECT or WRONG
            if (isAnnotationsMatch(keyAnn, resAnn, features, fcmp)) {
              //we have a full match
              choice = new PairingImpl(i, j, CORRECT_VALUE);
            } else {
            //the two annotations are coextensive but don't match
              //we have a missmatch
              choice = new PairingImpl(i, j, MISMATCH_VALUE);
            }
          } else if (keyAnn.overlaps(resAnn)) {
            //we have partial overlap -> PARTIALLY_CORRECT or WRONG
            if (isAnnotationsMatch(keyAnn, resAnn, features, fcmp)) {
              choice = new PairingImpl(i, j, PARTIALLY_CORRECT_VALUE);
            } else {
              choice = new PairingImpl(i, j, WRONG_VALUE);
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

    //2) from all possible pairings, find the maximal set that also
    //maximises the total score
    Collections.sort(possibleChoices, new PairingScoreComparator());
    Collections.reverse(possibleChoices);
    finalChoices = new ArrayList<Pairing>();

    while (!possibleChoices.isEmpty()) {
      PairingImpl bestChoice = (PairingImpl) possibleChoices.remove(0);
      // TODO: 
      bestChoice.consume();
      finalChoices.add(bestChoice);
      switch (bestChoice.value) {
        case CORRECT_VALUE: {
          //logger.debug("DEBUG: add a correct strict one: "+bestChoice.getKey());
          if (createAdditionalData) {
            correctStrictAnns.add(bestChoice.getResponse());
          }
          es.addCorrectStrict(1);
          bestChoice.setType(CORRECT_TYPE);
          break;
        }
        case PARTIALLY_CORRECT_VALUE: {  // correct but only opverlap, not coextensive
          //logger.debug("DEBUG: add a correct partial one: "+bestChoice.getKey());
          if (createAdditionalData) {
            correctPartialAnns.add(bestChoice.getResponse());
          }
          es.addCorrectPartial(1);
          bestChoice.setType(PARTIALLY_CORRECT_TYPE);
          break;
        }
        case MISMATCH_VALUE: { // coextensive and not correct
          if (bestChoice.getKey() != null && bestChoice.getResponse() != null) {
            es.addIncorrectStrict(1);
            bestChoice.setType(MISMATCH_TYPE);
            if (createAdditionalData) {
              incorrectStrictAnns.add(bestChoice.getResponse());
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
              incorrectPartialAnns.add(bestChoice.getResponse());
            }
            bestChoice.setType(MISMATCH_TYPE);
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
          trueMissingLenientAnns.add((keyList.get(i)));
        }
        Pairing choice = new PairingImpl(i, -1, WRONG_VALUE);
        choice.setType(MISSING_TYPE);
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
          PairingImpl choice = new PairingImpl(-1, i, WRONG_VALUE);
          choice.setType(SPURIOUS_TYPE);
          finalChoices.add(choice);
        }
      }
    }

    // To count the single correct anntations, go through the correct annotations and find the
    // target they have been matched to, then see if that target overlaps with a spurious annotation.
    // If not we can count it as a single correct annotation. This is done for correct strict and
    // correct lenient.
    // We can only do this if we have the sets which will only happen if there is no threshold
    if (createAdditionalData) {
      for (Pairing p : finalChoices) {
        if (p.getType() == CORRECT_TYPE) {
          Annotation t = p.getKey();
          AnnotationSet ol = gate.Utils.getOverlappingAnnotations(spuriousAnnSet, t);
          if (ol.size() == 0) {
            es.addSingleCorrectStrict(1);
            singleCorrectStrictAnns.add(p.getResponse());
          }
          //logger.debug("DEBUG have a correct strict choice, overlapping: "+ol.size()+" key is "+t);
        } else if (p.getType() == PARTIALLY_CORRECT_TYPE) {
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
    if(createAdditionalData) {
      choices = finalChoices;
    } else {
      finalChoices = null;
    }
    return es;
  }

  /**
   * Check if a response annotation matches a key annotation. If the annotations have different
   * type, this returns false; Otherwise, if the features set is empty, this returns true;
   * Otherwise, if the features set is null, this returns true if all features in the key
   * annotation have the same values as the same features in the response annotation. Otherwise, if
   * the feature set is not empty, this compares the features based on the FeatureComparison specified:
   * If it is FEATURES_EQUALITY, then it only returns true of all the features in the features set
   * have identical values in the key and response annotation. If it is FEATURES_SUBSUMPTION,
   * it returns true if the response features subsume the key features limited to the features list.
   *
   * @param key
   * @param response
   * @param features
   * @return
   */
  public boolean isAnnotationsMatch(Annotation key, Annotation response, Set<String> features, FeatureComparison fcmp) {
    if (!key.getType().equals(response.getType())) {
      return false;
    }
    if (features == null) {
      // check if the key Ann is compatible with the response ann
      FeatureMap fmk = key.getFeatures();
      FeatureMap fmr = key.getFeatures();
      return fmr.subsumes(fmk);
    } else {
      if (features.isEmpty()) {
        return true;
      } else {
        if(fcmp.equals(FeatureComparison.FEATURE_EQUALITY)) {
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
          return fmr.subsumes(fmk,features);          
        }
      }
    }
  }
  
  /**
   * Performs some basic checks over the internal data structures from the last run.
   *
   * @throws Exception
   */
  void sanityCheck() throws Exception {
    //all keys and responses should have at most one choice left
    for (List<Pairing> choices : keyChoices) {
      if (choices != null) {
        if (choices.size() > 1) {
          throw new Exception("Multiple choices found!");
        } else if (!choices.isEmpty()) {
          //size must be 1
          Pairing aChoice = choices.get(0);
          //the SAME choice should be found for the associated response
          List<?> otherChoices = responseChoices.get(aChoice.getResponseIndex());
          if (otherChoices == null
                  || otherChoices.size() != 1
                  || otherChoices.get(0) != aChoice) {
            throw new Exception("Reciprocity error!");
          }
        }
      }
    }

    for (List<Pairing> choices : responseChoices) {
      if (choices != null) {
        if (choices.size() > 1) {
          throw new Exception("Multiple choices found!");
        } else if (!choices.isEmpty()) {
          //size must be 1
          Pairing aChoice = choices.get(0);
          //the SAME choice should be found for the associated response
          List<?> otherChoices = keyChoices.get(aChoice.getKeyIndex());
          if (otherChoices == null) {
            throw new Exception("Reciprocity error : null!");
          } else if (otherChoices.size() != 1) {
            throw new Exception("Reciprocity error: not 1!");
          } else if (otherChoices.get(0) != aChoice) {
            throw new Exception("Reciprocity error: different!");
          }
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
  public class PairingImpl implements Pairing {

    PairingImpl(int keyIndex, int responseIndex, int value) {
      this.keyIndex = keyIndex;
      this.responseIndex = responseIndex;
      this.value = value;
      scoreCalculated = false;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      switch (getType()) {
        case CORRECT_TYPE: sb.append("CORRECT: "); break;
        case PARTIALLY_CORRECT_TYPE: sb.append("PARTIAL: "); break;
        case MISSING_TYPE: sb.append("MISSING: "); break;
        case SPURIOUS_TYPE: sb.append("SPURIOUS: "); break;
        case MISMATCH_TYPE: sb.append("INCORRECT: "); break;
        default: sb.append("UNKNOWN: "); sb.append(getType());
      }
      sb.append(", T=");
      sb.append(getKey());
      sb.append(", R=");
      sb.append(getResponse());
      return sb.toString();
    }
    
    
    @Override
    public int getScore() {
      if (scoreCalculated) {
        return score;
      } else {
        calculateScore();
        return score;
      }
    }

    @Override
    public int getKeyIndex() {
      return this.keyIndex;
    }

    @Override
    public int getResponseIndex() {
      return this.responseIndex;
    }

    @Override
    public int getValue() {
      return this.value;
    }

    @Override
    public Annotation getKey() {
      return keyIndex == -1 ? null : keyList.get(keyIndex);
    }

    @Override
    public Annotation getResponse() {
      return responseIndex == -1 ? null
              : responseList.get(responseIndex);
    }

    @Override
    public int getType() {
      return type;
    }

    @Override
    public void setType(int type) {
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
    @Override
    public void remove() {
      List<Pairing> fromKey = keyChoices.get(keyIndex);
      fromKey.remove(this);
      List<Pairing> fromResponse = responseChoices.get(responseIndex);
      fromResponse.remove(this);
    }

    /**
     * Calculates the score for this choice as: type - sum of all the types of all OTHER mutually
     * exclusive choices
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
        res = first.getType() - second.getType();
      }
      //compare by completeness (a wrong match with both key and response
      //is "better" than one with only key or response
      if (res == 0) {
        res = (first.getKey() == null ? 0 : 1)
                + (first.getResponse() == null ? 0 : 1)
                + (second.getKey() == null ? 0 : -1)
                + (second.getResponse() == null ? 0 : -1);
      }
      return res;
    }
  }

  /**
   * Compares two choices based on start offset of key (or response if key not present) and type if
   * offsets are equal.
   */
  public static class PairingOffsetComparator implements Comparator<Pairing> {

    /**
     * Compares two choices based on start offset of key (or response if key not present) and type
     * if offsets are equal.
     */
    @Override
    public int compare(Pairing first, Pairing second) {
      Annotation key1 = first.getKey();
      Annotation key2 = second.getKey();
      Annotation res1 = first.getResponse();
      Annotation res2 = second.getResponse();
      Long start1 = key1 == null ? null : key1.getStartNode().getOffset();
      if (start1 == null) {
        start1 = res1.getStartNode().getOffset();
      }
      Long start2 = key2 == null ? null : key2.getStartNode().getOffset();
      if (start2 == null) {
        start2 = res2.getStartNode().getOffset();
      }
      int res = start1.compareTo(start2);
      if (res == 0) {
        //compare by type
        res = second.getType() - first.getType();
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

  // We use this class internally to represent a sorted candidate list which also has 
  // a state that knows:
  // = what is the smallest score still in the list
  // = what is the index of the smallest score
  // = what is the highest score in the list (this will always be at position 0 since the 
  //   list is sorted by descending score
  // = how many elements are still in the list
  // It has a method setThreshold(double) to set the lowest score still visible in the list. This will automatically
  // update the state and may cause the number of visible elements to get changed to 0
  // The purpose of all this is to make checking these lists more efficient by preventing
  // actually destructively removing elements from the list
  public static class CandidateList {
    // the constructor takes the original list annotation and initializes this object 
    // will all the candidate annotations, sorted by the value of the specified score feature.
    // The constructor also optionally takes a HashSet<Double> which will be extended to contain all the 
    // scores seen in this list (unless the parameter is null)
    public CandidateList(AnnotationSet annSet, Annotation listAnn, String listIdFeature, String scoreFeature) {
      this.scoreFeature = scoreFeature;
      this.listAnn = listAnn;
      // First get the list of ids
      Object val = listAnn.getFeatures().get(listIdFeature);
      if(!(val instanceof List<?>)) {
        throw new GateRuntimeException("The listIdFeature for a list annotation does not contain a list: "+listAnn);
      }
      List<Integer> ids = (List<Integer>)val;
      logger.debug("DEBUG: processing list annotation "+listAnn);
      logger.debug("DEBUG: id list is "+ids);
      
      if(!ids.isEmpty()) {
        cands = new ArrayList<Annotation>(ids.size());
        for(Integer id : ids) {
          logger.debug("DEBUG: trying to get annotation for id "+id);
          cands.add(annSet.get(id));
        }
        ByScoreComparator comp = new ByScoreComparator(scoreFeature);
        cands.sort(comp);
        logger.debug("DEBUG: cands is now "+cands);
        smallestScoreIndex = cands.size()-1;
        smallestScore = object2Double(cands.get(smallestScoreIndex).getFeatures().get(scoreFeature));
        highestScore = object2Double(cands.get(0).getFeatures().get(scoreFeature));
        size = cands.size();
      }
    }
    public double smallestScore = Double.NaN;
    public double highestScore = Double.NaN;
    public int size = 0;
    public int smallestScoreIndex = -1;
    
    
    private List<Annotation> cands;
    private Annotation listAnn; 
    private String scoreFeature;
    
    public void setThreshold(double th) {
      logger.debug("DEBUG: candidatelist setting threshold to "+th+" size before="+size);
      logger.debug("DEBUG: highest="+highestScore+" smallest="+smallestScore);
      if (th > highestScore) {
        size = 0;
        smallestScore = Double.NaN;
        highestScore = Double.NaN;
        smallestScoreIndex = -1;
      } else {
        // if the threshold is larger than our lowest score, adujst the lowest score, index and size
        // accordingly
        while (th > smallestScore && size > 0) {
          size--;
          smallestScoreIndex--;
          if(size>0) {
            smallestScore = object2Double(cands.get(smallestScoreIndex).getFeatures().get(scoreFeature));
          }
        }
        if (size == 0) {
          smallestScoreIndex = -1;
          smallestScore = Double.NaN;
          highestScore = Double.NaN;
        } 
      }
      logger.debug("DEBUG: candidatelist setting threshold to "+th+" size after="+size);
    }

    public Annotation get(int index) {
      return cands.get(index);
    }
    
    public List<Annotation> getList() {
      return cands;
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
  
  private static double object2Double(Object tmp) {
    if (tmp == null) {
      return Double.NaN;
    } else {
      if (tmp instanceof Double) {
        return (Double) tmp;
      } else if (tmp instanceof Number) {
        return ((Number) tmp).doubleValue();
      } else {
        throw new GateRuntimeException("Expected an object that is a Double or number but got "+tmp.getClass()+": "+tmp);
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
    if(value instanceof Double) {
      ret = (Double)value;
    } else if (value instanceof Number) {
      ret = ((Number) value).doubleValue();
    } else if (value instanceof String) {
      ret = Double.valueOf((String) value);
    } 
    return ret;
  }

  

}
