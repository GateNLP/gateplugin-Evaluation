package gate.plugin.evaluation.api;

import gate.Annotation;
import gate.FeatureMap;
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

// TODO: make this classe use the EvalStats internally and just make the old method names
// to get delegated to the stats methods; Add a method to retrieve the stats object.
// Also, if there is a threshold feature, automatically create an ordered map of 
// stat objects by threshold and add a method to retrieve them.
// add constructors for making this class immutable and to avoid the set methods for features 
// and thresholds. 
// Deprecate the constructor for generating an overal differ from all differs, instead do
// something similar in the evalStats (we do not keep the annotations themselves there). 

// CHECK: should we directly support the reference set here or do the reference set processing
// outside of the differ??



/**
 * New modified AnnotationDiffer class.
 * This is the code that got modified.
 */
public class AnnotationDiffer {
  
  protected EvalStats evalStats = new EvalStats();
  public EvalStats getEvalStats() { return evalStats; }

  // This is only used if we have a threshold feature;
  protected NavigableMap<Double,EvalStats> evalStatsByThreshold;
  public NavigableMap<Double,EvalStats> getEvalStatsByThreshold() { return evalStatsByThreshold; }
  
  
  
  // For now we make the main parts of or own AnnotationDiffer object immutable: all the necessary
  // data has to be specified at creation time. Also, the sets for which to calculate the diffs
  // have to be specified at creation time.
  private AnnotationDiffer() {}
  public AnnotationDiffer(
          Collection<Annotation> targets, 
          Collection<Annotation> responses,
          List<String> features,
          String thresholdFeature,
          NavigableMap<Double,EvalStats> byThresholdEvalStats
                  ) {
    this.targets = targets;
    this.responses = responses;
    this.thresholdFeature = thresholdFeature;
    this.byThresholdEvalStats = byThresholdEvalStats;
    this.features = features;
    if(features != null && !features.isEmpty()) {
      significantFeaturesSet = new HashSet<String>(features);      
    }
    evalStats = calculateDiff(targets, responses, features, thresholdFeature, Double.NaN);
    // if we have a threshold feature, get all the thresholds and re-runn the calculateDiff 
    // method for each threshold. Add the per-threshold evalstats to the byThresholdEvalStats object.
    if(thresholdFeature != null && !thresholdFeature.isEmpty()) {
      System.out.println("DEBUG Calculating for the thresholds....");
      NavigableSet<Double> thresholds = new TreeSet<Double>();
      for(Annotation res : responses) {
        double score = getFeatureDouble(res.getFeatures(),thresholdFeature,Double.NaN);
        if(Double.isNaN(score)) {
          throw new GateRuntimeException("Response does not have a score: "+res);
        }
        thresholds.add(score);
      }
      // Now calculate the EvalStats(threshold) for each threshold we found in decreasing order.
      // The counts we get will need to get added to all existing EvalStats which are already
      // in the byThresholdEvalStats map. To simplify this, we accumulate the stats for each 
      // of our own thresholds and then use the accumulated stats for incrementing (this avoids
      // going through the map O(n^2) times)
      
      // Initialize the accumulating evalstats
      EvalStats accum = new EvalStats();
      // start with the highest threshold
      Double th = thresholds.last();
      while(th != null) {
        EvalStats es = calculateDiff(targets,responses,features,thresholdFeature,th);
        accum.add(es);
        Double nextTh = thresholds.lower(th);
        // now add the accum to all entries in the byThresholdEvalStats for which the threshold
        // is less than or equal than our current threshold and and larger than our next 
        // threshold
        boolean found = false;
        for(Double oth = byThresholdEvalStats.floorKey(th); oth != null && oth <= th && oth > nextTh; oth = byThresholdEvalStats.lowerKey(oth)) {
          if(oth == th) {
            found = true;
          }
          EvalStats oes = byThresholdEvalStats.get(oth);
          oes.add(accum);
        }
        // if the entry was not already in the byThresholdEvalStats, we need to add it, but this
        // entry also needs to count all the entries from the next higher one, of there is one
        if(!found) {
          EvalStats nextHigher = byThresholdEvalStats.higherEntry(th).getValue();
          if(nextHigher != null) {
            es.add(nextHigher);
          }
          byThresholdEvalStats.put(th, es);
        }
      }
    }
  }
  
  protected Set<?> significantFeaturesSet;  
  protected Collection<Annotation> targets;
  protected Collection<Annotation> responses;
  protected String thresholdFeature;
  protected NavigableMap<Double,EvalStats> byThresholdEvalStats;
  protected List<String> features;
  
  /**
   * Constructor to be used when you have a collection of AnnotationDiffer
   * and want to consider it as only one AnnotationDiffer.
   * Then you can only use the methods getPrecision/Recall/FMeasure...().
   * @param differs collection to be regrouped in one AnnotationDiffer
   */
  /*
  public AnnotationDiffer(Collection<AnnotationDiffer> differs) {
    correctMatches = 0;
    partiallyCorrectMatches = 0;
    missing = 0;
    spurious = 0;
    int keyCount = 0;
    int responseCount = 0;
    for (AnnotationDiffer differ : differs) {
      // set the correct, partial, spurious and missing values to be
      // the sum of those in the collection
      correctMatches += differ.getCorrectMatches();
      partiallyCorrectMatches += differ.getPartiallyCorrectMatches();
      missing += differ.getMissing();
      spurious += differ.getSpurious();
      keyCount += differ.getKeysCount();
      responseCount += differ.getResponsesCount();
    }
    keyList = new ArrayList<Annotation>(Collections.nCopies(keyCount, (Annotation) null));
    responseList = new ArrayList<Annotation>(Collections.nCopies(responseCount, (Annotation) null));    
  }
  */

  /**
   * Interface representing a pairing between a key annotation and a response 
   * one.
   */
  public static interface Pairing{
    /**
     * Gets the key annotation of the pairing. Can be <tt>null</tt> (for 
     * spurious matches).
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
     * Gets the response annotation of the pairing. Can be <tt>null</tt> (for 
     * missing matches).
     * @return an {@link Annotation} object.
     */
    public Annotation getResponse();
    
    /**
     * Gets the type of the pairing, one of {@link #CORRECT_TYPE},
     * {@link #PARTIALLY_CORRECT_TYPE}, {@link #SPURIOUS_TYPE} or
     * {@link #MISSING_TYPE},
     * @return an <tt>int</tt> value.
     */
    public int getType();
  }

  // the sets we record in case the threshold is NaN
  public Set<Annotation> 
          correctStrictAnns, 
          correctPartialAnns,
          incorrectStrictAnns,
          incorrectPartialAnns,
          trueMissingLenientAnns,
          trueSpuriousLenientAnns;
  
  public Set<Annotation> getCorrectStrictAnnotations() { return correctStrictAnns; }
  public Set<Annotation> getCorrectPartialAnnotations() { return correctPartialAnns; }
  public Set<Annotation> getIncorrectStrictAnnotations() { return incorrectStrictAnns; }
  public Set<Annotation> getIncorrectPartialAnnotations() { return incorrectPartialAnns; }
  public Set<Annotation> getTrueMissingLenientAnnotations() { return trueMissingLenientAnns; }
  public Set<Annotation> getTrueSpuriousLenientAnnotations() { return trueSpuriousLenientAnns; }
  
  
  
  /**
   * Computes a diff between two collections of annotations.
   * @param key the collection of key annotations.
   * @param response the collection of response annotations.
   * @return a list of {@link Pairing} objects representing the pairing set
   * that results in the best score.
   */
  private EvalStats calculateDiff(
          Collection<Annotation> key, 
          Collection<Annotation> response,
          List<String> features,
          String thresholdFeature,  // if not null, the name of a score feature
          double threshold  // if not NaN, we will calculate the stats only for responses with score > threshold
          )
  {
    System.out.println("DEBUG: calculating the differences for threshold "+threshold);
    EvalStats es = new EvalStats(threshold);
    // If the threshold is not NaN, then we will calculate a temporary EvalStats object with that
    // threshold and then insert or update such an EvalStats object in the global evalStatsByThreshold object. 
    
    if(Double.isNaN(threshold)) {
      correctStrictAnns = new HashSet<Annotation>();
      correctPartialAnns = new HashSet<Annotation>();
      incorrectStrictAnns = new HashSet<Annotation>();
      incorrectPartialAnns = new HashSet<Annotation>();
      trueMissingLenientAnns = new HashSet<Annotation>();
      trueSpuriousLenientAnns = new HashSet<Annotation>();
    }
    keyList = new ArrayList<Annotation>(key);
    responseList = new ArrayList<Annotation>(response);
    
    keyChoices = new ArrayList<List<Pairing>>(keyList.size());
    // initialize by nr_keys nulls
    keyChoices.addAll(Collections.nCopies(keyList.size(), (List<Pairing>) null));
    responseChoices = new ArrayList<List<Pairing>>(responseList.size());
    // initialize by nr_responses null
    responseChoices.addAll(Collections.nCopies(responseList.size(), (List<Pairing>) null));

    possibleChoices = new ArrayList<Pairing>();

    es.addTargets(key.size());
    es.addResponses(response.size());
    
    //1) try all possible pairings
    for(int i = 0; i < keyList.size(); i++){
      for(int j =0; j < responseList.size(); j++){
        Annotation keyAnn = keyList.get(i);
        Annotation resAnn = responseList.get(j);
        if(!Double.isNaN(threshold)) {
          // try to get the score for the response
          double score = getFeatureDouble(resAnn.getFeatures(),thresholdFeature,Double.NaN);
          if(Double.isNaN(score)) {
            throw new GateRuntimeException("Response without a score feature: "+resAnn);
          }
          // We are only interested in responses which have a score >= the threshold
          if(score < threshold) {
            continue; // check the next response
          }
        }
        PairingImpl choice = null;
        if(keyAnn.coextensive(resAnn)){
          //we have full overlap -> CORRECT or WRONG
          if(keyAnn.isCompatible(resAnn, significantFeaturesSet)){
            //we have a full match
            choice = new PairingImpl(i, j, CORRECT_VALUE);
          }else{
            //the two annotations are coextensive but don't match
            //we have a missmatch
            choice = new PairingImpl(i, j, MISMATCH_VALUE);
          }
        }else if(keyAnn.overlaps(resAnn)){
          //we have partial overlap -> PARTIALLY_CORRECT or WRONG
          if(keyAnn.isPartiallyCompatible(resAnn, significantFeaturesSet)){
            choice = new PairingImpl(i, j, PARTIALLY_CORRECT_VALUE);
          }else{
            choice = new PairingImpl(i, j, WRONG_VALUE);
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

    while(!possibleChoices.isEmpty()){
      PairingImpl bestChoice = (PairingImpl)possibleChoices.remove(0);
      bestChoice.consume();
      finalChoices.add(bestChoice);
      switch(bestChoice.value){
        case CORRECT_VALUE:{
          if(Double.isNaN(threshold)) { correctStrictAnns.add(bestChoice.getResponse()); }
          es.addCorrectStrict(1);
          bestChoice.setType(CORRECT_TYPE);
          break;
        }
        case PARTIALLY_CORRECT_VALUE:{  // correct but only opverlap, not coextensive
          if(Double.isNaN(threshold)) { correctPartialAnns.add(bestChoice.getResponse()); }
          es.addCorrectPartial(1);
          bestChoice.setType(PARTIALLY_CORRECT_TYPE);
          break;
        }
        case MISMATCH_VALUE:{ // coextensive and not correct
          if(bestChoice.getKey() != null && bestChoice.getResponse() != null) {
            es.addIncorrectStrict(1);
            bestChoice.setType(MISMATCH_TYPE);
            if(Double.isNaN(threshold)) { incorrectStrictAnns.add(bestChoice.getResponse()); }
          } else if(bestChoice.getKey() != null) {
            System.out.println("DEBUG: GOT a MISMATCH_VALUE (coext and not correct) with no key "+bestChoice);
          } else if(bestChoice.getResponse() != null) {
            System.out.println("DEBUG: GOT a MISMATCH_VALUE (coext and not correct) with no response "+bestChoice);
          }
          break;
        }
        case WRONG_VALUE:{ // overlapping and not correct
          if(bestChoice.getKey() != null && bestChoice.getResponse() != null) {
            es.addIncorrectPartial(1);
            if(Double.isNaN(threshold)) { incorrectPartialAnns.add(bestChoice.getResponse()); }
          } else if(bestChoice.getKey() == null){            
            // this is a response which overlaps with a key but does not have a key??
            System.out.println("DEBUG: GOT a WRONG_VALUE (overlapping and not correct) with no key "+bestChoice);
          } else if(bestChoice.getResponse() == null) {
            System.out.println("DEBUG: GOT a WRONG_VALUE (overlapping and not correct) with no response "+bestChoice);
          }
          break;
        }
        default:{
          throw new GateRuntimeException("Invalid pairing type: " +
                  bestChoice.value);
        }
      }
    }
    //add choices for the incorrect matches (MISSED, SPURIOUS)
    //get the unmatched keys
    for(int i = 0; i < keyChoices.size(); i++){
      List<Pairing> aList = keyChoices.get(i);
      if(aList == null || aList.isEmpty()){
        trueMissingLenientAnns.add((keyList.get(i)));
        Pairing choice = new PairingImpl(i, -1, WRONG_VALUE);
        choice.setType(MISSING_TYPE);
        finalChoices.add(choice);
        //es.addTrueMissingLenient(1);
      }
    }

    //get the unmatched responses
    for(int i = 0; i < responseChoices.size(); i++){
      List<Pairing> aList = responseChoices.get(i);
      if(aList == null || aList.isEmpty()){
        trueSpuriousLenientAnns.add((responseList.get(i)));
        PairingImpl choice = new PairingImpl(-1, i, WRONG_VALUE);
        choice.setType(SPURIOUS_TYPE);
        finalChoices.add(choice);
        //es.addTrueSpurious(1);
      }
    }

    return es;
  }

  /**
   * Gets the strict precision (the ratio of correct responses out of all the 
   * provided responses).
   * @return a <tt>double</tt> value.
   */
  public double getPrecisionStrict(){
    return evalStats.getPrecisionStrict();
  }

  /**
   * Gets the strict recall (the ratio of key matched to a response out of all 
   * the keys).
   * @return a <tt>double</tt> value.
   */  
  public double getRecallStrict(){
    return evalStats.getRecallStrict();
  }

  /**
   * Gets the lenient precision (where the partial matches are considered as
   * correct).
   * @return a <tt>double</tt> value.
   */
  public double getPrecisionLenient(){
    return evalStats.getPrecisionLenient();
  }

  /**
   * Gets the average of the strict and lenient precision values.
   * @return a <tt>double</tt> value.
   */
  public double getPrecisionAverage() {
    return (getPrecisionLenient() + getPrecisionStrict()) / (2.0);
  }

  /**
   * Gets the lenient recall (where the partial matches are considered as
   * correct).
   * @return a <tt>double</tt> value.
   */
  public double getRecallLenient(){
    return evalStats.getRecallLenient();
  }

  /**
   * Gets the average of the strict and lenient recall values.
   * @return a <tt>double</tt> value.
   */
  public double getRecallAverage() {
    return (getRecallLenient() + getRecallStrict()) / (2.0);
  }

  /**
   * Gets the strict F-Measure (the harmonic weighted mean of the strict
   * precision and the strict recall) using the provided parameter as relative 
   * weight. 
   * @param beta The relative weight of precision and recall. A value of 1 
   * gives equal weights to precision and recall. A value of 0 takes the recall 
   * value completely out of the equation.
   * @return a <tt>double</tt>value.
   */
  public double getFMeasureStrict(double beta){
    return evalStats.getFMeasureStrict(beta);
  }

  /**
   * Gets the lenient F-Measure (F-Measure where the lenient precision and 
   * recall values are used) using the provided parameter as relative weight. 
   * @param beta The relative weight of precision and recall. A value of 1 
   * gives equal weights to precision and recall. A value of 0 takes the recall 
   * value completely out of the equation.
   * @return a <tt>double</tt>value.
   */
  public double getFMeasureLenient(double beta){
    return evalStats.getFMeasureLenient(beta);
  }

  /**
   * Gets the average of strict and lenient F-Measure values.
   * @param beta The relative weight of precision and recall. A value of 1 
   * gives equal weights to precision and recall. A value of 0 takes the recall 
   * value completely out of the equation.
   * @return a <tt>double</tt>value.
   */  
  public double getFMeasureAverage(double beta) {
    double answer = (getFMeasureLenient(beta) + getFMeasureStrict(beta)) / (2.0);
    return answer;
  }

  /**
   * Gets the number of correct matches.
   * @return an <tt>int<tt> value.
   */
  public int getCorrectMatches(){
    return evalStats.getCorrectStrict();
  }

  /**
   * Gets the number of partially correct matches.
   * @return an <tt>int<tt> value.
   */
  public int getPartiallyCorrectMatches(){
    return evalStats.getCorrectPartial();
  }

  /**
   * Gets the number of pairings of type {@link #MISSING_TYPE}.
   * @return an <tt>int<tt> value.
   */
  public int getMissing(){
    return evalStats.getMissingLenient();
  }

  /**
   * Gets the number of pairings of type {@link #SPURIOUS_TYPE}.
   * @return an <tt>int<tt> value.
   */
  public int getSpurious(){
    return evalStats.getSpuriousLenient();
  }

  /**
   * Gets the number of pairings which are not correct. 
   * (number of responses which are not correct)
   * @return an <tt>int<tt> value.
   */
  public int getFalsePositivesStrict(){
    return responseList.size() - getCorrectMatches();
  }

  /**
   * Gets the number of responses that aren't either correct or partially 
   * correct.
   * @return an <tt>int<tt> value.
   */
  public int getFalsePositivesLenient(){
    return responseList.size() - getCorrectMatches() - getPartiallyCorrectMatches();
  }

  /**
   * Gets the number of keys provided.
   * @return an <tt>int<tt> value.
   */
  public int getKeysCount() {
    return keyList.size();
  }

  /**
   * Gets the number of responses provided.
   * @return an <tt>int<tt> value.
   */
  public int getResponsesCount() {
    return responseList.size();
  }

  /**
   * Prints to System.out the pairings that are not correct.
   */
  public void printMissmatches(){
    //get the partial correct matches
    for (Pairing aChoice : finalChoices) {
      switch(aChoice.getValue()){
        case PARTIALLY_CORRECT_VALUE:{
          System.out.println("Missmatch (partially correct):");
          System.out.println("Key: " + keyList.get(aChoice.getKeyIndex()).toString());
          System.out.println("Response: " + responseList.get(aChoice.getResponseIndex()).toString());
          break;
        }
      }
    }

    //get the unmatched keys
    for(int i = 0; i < keyChoices.size(); i++){
      List<Pairing> aList = keyChoices.get(i);
      if(aList == null || aList.isEmpty()){
        System.out.println("Missed Key: " + keyList.get(i).toString());
      }
    }

    //get the unmatched responses
    for(int i = 0; i < responseChoices.size(); i++){
      List<Pairing> aList = responseChoices.get(i);
      if(aList == null || aList.isEmpty()){
        System.out.println("Spurious Response: " + responseList.get(i).toString());
      }
    }
  }



  /**
   * Performs some basic checks over the internal data structures from the last
   * run.
   * @throws Exception
   */
  void sanityCheck()throws Exception{
    //all keys and responses should have at most one choice left
    for (List<Pairing> choices : keyChoices)  {
      if(choices != null){
        if(choices.size() > 1){
          throw new Exception("Multiple choices found!");
        }
        else if(!choices.isEmpty()){
          //size must be 1
          Pairing aChoice = choices.get(0);
          //the SAME choice should be found for the associated response
          List<?> otherChoices = responseChoices.get(aChoice.getResponseIndex());
          if(otherChoices == null ||
             otherChoices.size() != 1 ||
             otherChoices.get(0) != aChoice){
            throw new Exception("Reciprocity error!");
          }
        }
      }
    }

    for (List<Pairing> choices : responseChoices) {
      if(choices != null){
        if(choices.size() > 1){
          throw new Exception("Multiple choices found!");
        }
        else if(!choices.isEmpty()){
          //size must be 1
          Pairing aChoice = choices.get(0);
          //the SAME choice should be found for the associated response
          List<?> otherChoices = keyChoices.get(aChoice.getKeyIndex());
          if(otherChoices == null){
            throw new Exception("Reciprocity error : null!");
          }else if(otherChoices.size() != 1){
            throw new Exception("Reciprocity error: not 1!");
          }else if(otherChoices.get(0) != aChoice){
            throw new Exception("Reciprocity error: different!");
          }
        }
      }
    }
  }
  
  /**
   * Adds a new pairing to the internal data structures.
   * @param pairing the pairing to be added
   * @param index the index in the list of pairings
   * @param listOfPairings the list of {@link Pairing}s where the
   * pairing should be added
   */
  protected void addPairing(Pairing pairing, int index, List<List<Pairing>> listOfPairings){
    List<Pairing> existingChoices = listOfPairings.get(index);
    if(existingChoices == null){
      existingChoices = new ArrayList<Pairing>();
      listOfPairings.set(index, existingChoices);
    }
    existingChoices.add(pairing);
  }

  /**
   * Gets the set of features considered significant for the matching algorithm.
   * @return a Set.
   */
  public Set<?> getSignificantFeaturesSet() {
    return significantFeaturesSet;
  }

  /**
   * Set the set of features considered significant for the matching algorithm.
   * A <tt>null</tt> value means that all features are significant, an empty 
   * set value means that no features are significant while a set of String 
   * values specifies that only features with names included in the set are 
   * significant. 
   * @param significantFeaturesSet a Set of String values or <tt>null<tt>.
   */
  public void setSignificantFeaturesSet(Set<String> significantFeaturesSet) {
    this.significantFeaturesSet = significantFeaturesSet;
  }

  /**
   * Represents a pairing of a key annotation with a response annotation and
   * the associated score for that pairing.
   */
   public class PairingImpl implements Pairing{
    PairingImpl(int keyIndex, int responseIndex, int value) {
      this.keyIndex = keyIndex;
      this.responseIndex = responseIndex;
      this.value = value;
      scoreCalculated = false;
	    }

    @Override
    public int getScore(){
      if(scoreCalculated) return score;
      else{
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
    public Annotation getKey(){
      return keyIndex == -1 ? null : keyList.get(keyIndex);
    }

    @Override
    public Annotation getResponse(){
      return responseIndex == -1 ? null :
        responseList.get(responseIndex);
    }

    @Override
    public int getType(){
      return type;
    }
    

    @Override
    public void setType(int type) {
      this.type = type;
    }

    /**
     * Removes all mutually exclusive OTHER choices possible from
     * the data structures.
     * <tt>this</tt> gets removed from {@link #possibleChoices} as well.
     */
    public void consume(){
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
    public void remove(){
      List<Pairing> fromKey = keyChoices.get(keyIndex);
      fromKey.remove(this);
      List<Pairing> fromResponse = responseChoices.get(responseIndex);
      fromResponse.remove(this);
    }

    /**
     * Calculates the score for this choice as:
     * type - sum of all the types of all OTHER mutually exclusive choices
     */
    void calculateScore(){
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
     * The index in the response collection of the response annotation for this
     * pairing
     */
    int responseIndex;
    
    /**
     * The type of this pairing.
     */
    int type;
    
    /**
     * The value for this pairing. This value depends only on this pairing, not
     * on the conflict set.
     */
    int value;
    
    /**
     * The score of this pairing (calculated based on value and conflict set).
     */
    int score;
    boolean scoreCalculated;
  }

   /**
    * Return the value of a FeatureMap entry as a double.
    * If the entry is not found, the defaultValue is returned. If the entry cannot be converted
    * to a double, an exception is thrown (depending on what kind of conversion was attempted, e.g.
    * when converting from a string, it could be a NumberFormatException).
    * @param fm
    * @param key
    * @param defaultValue
    * @return 
    */
   public double getFeatureDouble(FeatureMap fm, String key, double defaultValue) {
     Object value = fm.get(key);
     if(value == null) return defaultValue;
     double ret = defaultValue;
     if(value instanceof String) {
       ret = Double.valueOf((String)value);
     } else if(value instanceof Number) {
       ret = ((Number)value).doubleValue();
     } 
     return ret;
   }
   
   
   /**
    * Compares two pairings:
    * the better score is preferred;
    * for the same score the better type is preferred (exact matches are
    * preffered to partial ones).
    */   
	protected static class PairingScoreComparator implements Comparator<Pairing> {
    /**
     * Compares two choices:
     * the better score is preferred;
     * for the same score the better type is preferred (exact matches are
     * preffered to partial ones).
     * @return a positive value if the first pairing is better than the second,
     * zero if they score the same or negative otherwise.
     */

	  @Override
    public int compare(Pairing first, Pairing second){
      //compare by score
      int res = first.getScore() - second.getScore();
      //compare by type
      if(res == 0) res = first.getType() - second.getType();
      //compare by completeness (a wrong match with both key and response
      //is "better" than one with only key or response
      if(res == 0){
        res = (first.getKey() == null ? 0 : 1) +
              (first.getResponse() == null ? 0 : 1) +
              (second.getKey() == null ? 0 : -1) +
              (second.getResponse() == null ? 0 : -1);
      }
      return res;
	  }
	}

    /**
     * Compares two choices based on start offset of key (or response
     * if key not present) and type if offsets are equal.
     */
	public static class PairingOffsetComparator implements Comparator<Pairing> {
    /**
     * Compares two choices based on start offset of key (or response
     * if key not present) and type if offsets are equal.
     */
	  @Override
    public int compare(Pairing first, Pairing second){
	    Annotation key1 = first.getKey();
	    Annotation key2 = second.getKey();
	    Annotation res1 = first.getResponse();
	    Annotation res2 = second.getResponse();
	    Long start1 = key1 == null ? null : key1.getStartNode().getOffset();
	    if(start1 == null) start1 = res1.getStartNode().getOffset();
	    Long start2 = key2 == null ? null : key2.getStartNode().getOffset();
	    if(start2 == null) start2 = res2.getStartNode().getOffset();
	    int res = start1.compareTo(start2);
	    if(res == 0){
	      //compare by type
	      res = second.getType() - first.getType();
	    }

//
//
//
//	    //choices with keys are smaller than ones without
//	    if(key1 == null && key2 != null) return 1;
//	    if(key1 != null && key2 == null) return -1;
//	    if(key1 == null){
//	      //both keys are null
//	      res = res1.getStartNode().getOffset().
//	      		compareTo(res2.getStartNode().getOffset());
//	      if(res == 0) res = res1.getEndNode().getOffset().
//      				compareTo(res2.getEndNode().getOffset());
//	      if(res == 0) res = second.getType() - first.getType();
//	    }else{
//	      //both keys are present
//	      res = key1.getStartNode().getOffset().compareTo(
//	          key2.getStartNode().getOffset());
//
//	      if(res == 0){
//		      //choices with responses are smaller than ones without
//		      if(res1 == null && res2 != null) return 1;
//		      if(res1 != null && res2 == null) return -1;
//		      if(res1 != null){
//			      res = res1.getStartNode().getOffset().
//    						compareTo(res2.getStartNode().getOffset());
//		      }
//		      if(res == 0)res = key1.getEndNode().getOffset().compareTo(
//		              key2.getEndNode().getOffset());
//		      if(res == 0 && res1 != null){
//				      res = res1.getEndNode().getOffset().
//	    						compareTo(res2.getEndNode().getOffset());
//		      }
//		      if(res == 0) res = second.getType() - first.getType();
//	      }
//	    }
      return res;
	  }

	}

        


  /** Type for correct pairings (when the key and response match completely)*/
  public static final int CORRECT_TYPE = 0;
  
  /** 
   * Type for partially correct pairings (when the key and response match 
   * in type and significant features but the spans are just overlapping and 
   * not identical.
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
   * Type for mismatched pairings (where the key and response are co-extensive
   * but they don't match).
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
   * Score for a mismatched pairing (higher then for WRONG as at least the 
   * offsets were right).
   */
  private static final int MISMATCH_VALUE = 1;
  
  /**
   * Score for a wrong (missing or spurious) pairing.
   */  
  private static final int WRONG_VALUE = 0;


  /**
   * The number of correct matches.
   */
  //protected int correctMatches;

  /**
   * The number of partially correct matches.
   */
  //protected int partiallyCorrectMatches;
  
  /**
   * The number of missing matches.
   */
  //protected int missing;
  
  /**
   * The number of spurious matches.
   */  
  //protected int spurious;

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

  //protected int incorrectStrict = 0;
  //protected int incorrectPartial = 0;
  //protected int truemissing = 0;
  //protected int truespurious = 0;
  
  public int getIncorrectStrict() {
    return evalStats.getIncorrectStrict();
  }
  public int getIncorrectLenient() {
    return evalStats.getIncorrectLenient();
  }
  public int getIncorrectPartial() {
    return evalStats.getIncorrectLenient();
  }
  
  
}
