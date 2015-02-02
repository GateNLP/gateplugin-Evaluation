package gate.plugin.evaluation.api;

/**
 * A class to hold all the numbers for an evaluation and for calculating
 * various measures from the numbers.
 * In addition to the numbers, a EvalStats object also contains a confidence
 * or score threshold. This is a double value such the the stats object
 * represents the numbers if the evaluation was carried out with that 
 * threshold, i.e. a response is only considered if the score of the response
 * is >= the threshold. If the threshold is NaN then the stats object is not
 * associated with a threshold (usually that means it is a stats object that
 * represents all responses found).
 * 
 * @author Johann Petrak
 */
public class EvalStats {
  
  protected double threshold = Double.NaN;
  
  public EvalStats() {
    
  }
  public EvalStats(double threshold) {
    this.threshold = threshold;
  }
  
  public double getThreshold() { return threshold; }
  
  // increment this EvalStats object with the counts from another one
  public void add(EvalStats other) {
    nTargets += other.nTargets;
    nResponses += other.nResponses;
    nCorrectStrict += other.nCorrectStrict;
    nCorrectPartial += other.nCorrectPartial;
    nIncorrectStrict += other.nIncorrectStrict;
    nIncorrectPartial += other.nIncorrectPartial;
  }
  
  public void addTargets(int n) { nTargets += n; }
  public void addResponses(int n) { nResponses += n; }
  public void addCorrectStrict(int n) { nCorrectStrict += n; }
  public void addCorrectPartial(int n) { nCorrectPartial += n; }
  public void addIncorrectStrict(int n) { nIncorrectStrict += n; }
  public void addIncorrectPartial(int n) {nIncorrectPartial += n; }
  
  // TARGETS the "gold" or "key" annotations.
  protected int nTargets;
  public int getTargets() { return nTargets; }
  
  // RESPONSES: the annotations that should match the targets a perfectly as possible
  protected int nResponses;
  public int getResponses() { return nResponses; }
  
  // CORRECT
  // responses that are coextensive and equal
  protected int nCorrectStrict;  
  public int getCorrectStrict() { return nCorrectStrict; }
  // responses that are overlapping but not coextensive and that equal
  protected int nCorrectPartial;
  public int getCorrectPartial() { return nCorrectPartial; }
  public int getCorrectLenient() { return nCorrectStrict + nCorrectPartial; }
  
  // INCORRECT
  // responses that are coextensive but not equal (this is only possible if there is at least
  // one feature used for comparison.
  protected int nIncorrectStrict;
  public int getIncorrectStrict() { return nIncorrectStrict; }
  protected int nIncorrectPartial;
  public int getIncorrectPartial() { return nIncorrectPartial; }
  public int getIncorrectLenient() { return nIncorrectStrict + nIncorrectPartial; }
  
  // MISSING
  // A missing is a target for which no correct response is found.
  // (This is the OLD definition where an incorrect response is also a missing and spurious response)
  // A missing strict is a target annotation for which no correct strict annotation exists.
  // nMissingStrict = nTarget - nCorrectStrict 
  public int getMissingStrict() { return nTargets - nCorrectStrict; }
  // A missing lenient is a target for which no correct strict or correct partial exists. In other 
  // words, it is a target which does not even partly overlap a correct response. Every missing 
  // lenient is also a missing strict, but some missing stricts still have a partial overlapping
  // correct response. So missing strict - correctPartial = missingLenient (?)
  public int getMissingLenient() { return nTargets - nCorrectStrict - nCorrectPartial; }
  // NOTE: missingPartial does not make sense, since this essentially equals correctPartial
  
  // SPURIOUS  
  // A spurious annotation is a response for which  no correct target annotation exists.
  // (This is the OLD definition where an incorrect response is also a missing and spurious response)
  // A spurious strict is a response which is not correctStrict 
  public int getSpuriousStrict() { return nResponses - nCorrectStrict; }
  public int getSpuriousLenient() { return nResponses - nCorrectStrict - nCorrectPartial; }
  // NOTE: spurious partial does not make sense, since this essentially equals correct Partial#
  
  // TRUE MISSING: we define this to be a target for which not even an incorrect response exists
  // (and obviously no correct response either).
  // A true missing strict is a target for which no correct strict or incorrect strict response
  // exists;
  public int getTrueMissingStrict() { return nTargets - nCorrectStrict - nIncorrectStrict; }
  // A true missing lenient is a target for which no strict or partial incorrect or correct 
  // response exists;
  public int getTrueMissingLenient() { return getTrueMissingStrict() - nCorrectPartial - nIncorrectPartial;  }
  // NOTE: a true missing partial does not make sense since it would essentially equal a correct partial
  
  // TRUE SPURIOUS: we define this to be a response for which not even an incorrect target exists.
  public int getTrueSpuriousStrict() { return nResponses - nCorrectStrict - nIncorrectStrict; }
  public int getTrueSpuriousLenient() { return getTrueSpuriousStrict() - nCorrectPartial - nIncorrectPartial; }
  
  // this will make sure that constraints between the counts are all met
  public void sanityCheck() {
    assert getMissingStrict() == nIncorrectStrict + getTrueMissingStrict();
    assert getMissingLenient() == nIncorrectStrict + nIncorrectPartial + getTrueMissingLenient();
    assert nTargets == nCorrectStrict + nIncorrectStrict + getTrueMissingStrict();
    assert nTargets == nCorrectPartial + nIncorrectStrict + nIncorrectPartial + getTrueMissingLenient();
    
    assert getSpuriousStrict() == nIncorrectStrict + getTrueSpuriousStrict();
    assert getSpuriousLenient() == nIncorrectStrict + nIncorrectPartial + getTrueSpuriousLenient();
    assert nResponses == nIncorrectStrict + nCorrectStrict + getTrueSpuriousStrict();
    assert nResponses == nIncorrectPartial + nIncorrectStrict + nIncorrectPartial + getSpuriousLenient();

    assert nTargets == getMissingStrict() + nCorrectStrict;
    assert nResponses == getSpuriousStrict() + nCorrectStrict;
    
  }
  
  // PRECISION
  // Precision is the portion of responses that are correct: correct/responses
  // Precision strict is the portion of responses that are correct strict
  public double getPrecisionStrict() { 
    if(nResponses == 0) {
      return 0.0;
    } else {
      return nCorrectStrict/(double)nResponses;
    }
  }
  
  public double getPrecisionLenient() {
    if(nResponses == 0) {
      return 0.0;
    } else {
      return getCorrectLenient()/(double)nResponses;
    }    
  }
  
  // RECALL 
  // Recall is the portion of targets that have a correct response
  public double getRecallStrict() {
    if(nResponses == 0) {
      return 0.0;
    } else {
      return nCorrectStrict/(double)nTargets;
    }
  }
  
  public double getRecallLenient() {
    if(nResponses == 0) {
      return 0.0;
    } else {
      return getCorrectLenient()/(double)nTargets;
    }    
  }
  
  // F-Measure: harmonic mean of precision and recall, weighted. Equal weight is given if beta = 1.0
  // Van Rijsbergens effectiveness is 1-F for alpha = 1/(1+beta^2)
  public double getFMeasureStrict(double beta){
    double precision = getPrecisionStrict();
    double recall = getRecallStrict();
    double betaSq = beta * beta;
    double answer = (((betaSq + 1) * precision * recall ) / (betaSq * precision + recall));
    if(Double.isNaN(answer)) answer = 0.0;
    return answer;
  }
  
  public double getFMeasureLenient(double beta){
    double precision = getPrecisionLenient();
    double recall = getRecallLenient();
    double betaSq = beta * beta;
    double answer = (((betaSq + 1) * precision * recall) / (betaSq * precision + recall));
    if(Double.isNaN(answer)) answer = 0.0;
    return answer;
  }
  
  
}
