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
  public double getThreshold() { return threshold; }
  public void setThreshold(double value) { threshold = value; }
  
  
  // TARGETS the "gold" or "key" annotations.
  protected int nTargets;
  public int getNTargets() { return nTargets; }
  
  // RESPONSES: the annotations that should match the targets a perfectly as possible
  protected int nResponses;
  public int getNResponses() { return nResponses; }
  
  // CORRECT
  // responses that are coextensive and equal
  protected int nCorrectStrict;  
  public int getNCorrectStrict() { return nCorrectStrict; }
  // responses that are overlapping but not coextensive and that equal
  protected int nCorrectPartial;
  public int getNCorrectPartial() { return nCorrectPartial; }
  public int getNCorrectLenient() { return nCorrectStrict + nCorrectPartial; }
  
  // INCORRECT
  // responses that are coextensive but not equal (this is only possible if there is at least
  // one feature used for comparison.
  protected int nIncorrectStrict;
  public int getNIncorrectStrict() { return nIncorrectStrict; }
  protected int nIncorrectPartial;
  public int getNIncorrectPartial() { return nIncorrectPartial; }
  protected int nIncorrectLenient;
  public int getNIncorrectLenient() { return nIncorrectStrict + nIncorrectPartial; }
  
  // MISSING
  // A missing is a target for which no correct response is found.
  // (This is the OLD definition where an incorrect response is also a missing and spurious response)
  // A missing strict is a target annotation for which no correct strict annotation exists.
  protected int nMissingStrict; 
  public int getNMissingStrict() { return nMissingStrict; }
  // A missing lenient is a target for which no correct strict or correct partial exists. In other 
  // words, it is a target which does not even partly overlap a correct response. Every missing 
  // lenient is also a missing strict, but some missing stricts still have a partial overlapping
  // correct response. So missing strict - correctPartial = missingLenient (?)
  protected int nMissingLenient;
  public int getNMissingLenient() { return nMissingLenient; }
  // NOTE: missingPartial does not make sense, since this essentially equals correctPartial
  
  // SPURIOUS  
  // A spurious annotation is a response for which  no correct target annotation exists.
  // (This is the OLD definition where an incorrect response is also a missing and spurious response)
  // A spurious strict is a response which is not correctStrict 
  protected int nSpuriousStrict;
  public int getNSpuriousStrict() { return nSpuriousStrict; }
  protected int nSpuriousLenient;
  public int getNSpuriousLenient() { return nSpuriousLenient; }
  // NOTE: spurious partial does not make sense, since this essentially equals correct Partial#
  
  // TRUE MISSING: we define this to be a target for which not even an incorrect response exists
  // A true missing strict is a target for which no correct strict or incorrect strict response
  // exists;
  protected int nTrueMissingStrict;
  public int getNTrueMissignStrict() { return nTrueMissingStrict; }
  // A true missing lenient is a target for which no strict or partial incorrect or correct 
  // response exists;
  protected int nTrueMissingLenient;
  public int getNTrueMissingLenient() { return nTrueMissingLenient; }
  // NOTE: a true missing partial does not make sense since it would essentially equal a correct partial
  
  // TRUE SPURIOUS: we define this to be a response for which not even an incorrect target exists.
  protected int nTrueSpuriousStrict;
  public int getNTrueSpuriousStrict() { return nTrueSpuriousStrict; }
  protected int nTrueSpuriousLenient;
  public int getNTrueSpuriousLenient() { return nTrueSpuriousLenient; }
  
  // this will make sure that constraints between the counts are all met
  public void sanityCheck() {
    // TODO!!!
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
      return getNCorrectLenient()/(double)nResponses;
    }    
  }
  
  // RECALL 
  // Recall is the portion of targets that have a correct response
  public double getRecallStrict() {
    if(nResponses == 0) {
      return 0.0;
    } else {
      return nCorrectStrict/(double)nResponses;
    }
  }
  
  public double getRecallLenient() {
    if(nResponses == 0) {
      return 0.0;
    } else {
      return getNCorrectLenient()/(double)nResponses;
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
