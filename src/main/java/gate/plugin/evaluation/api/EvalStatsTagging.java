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
package gate.plugin.evaluation.api;

// TODO: add more measures which are commonly used or interesting
// or document if we use different names
// TP(true positives) + FP(False Positives) = Responses
// TP(true positives) + FN(False Negatives) = Targets
//
// For the simple tagging case with no features (hence no incorrect):
// TP is Correct
// FP is TrueSpurious
// FN is TrueMissing
// 
// If there are incorrects:
// TP is correct
// FP is TrueSpurious or Incorrect = Spurious
// FN is TrueMissing or Incorrect = Missing
// 

/**
 * A class to hold all the numbers for an evaluation and for calculating
 * various measures from the numbers.
 * 
 * @author Johann Petrak
 */
public abstract class EvalStatsTagging  {
  
  public EvalStatsTagging() {
    
  }
  
  /**
   * Create a copy of an existing EvalPRFStats object.
   * This can be used to get an exact copy of the EvalPRFStats object passed to the constructor.
   * 
   * @param other  TODO
   */
  // Why we did not implement clone(): http://www.artima.com/intv/bloch13.html
  public EvalStatsTagging(EvalStatsTagging other) {
    nTargets = other.nTargets;
    nResponses = other.nResponses;
    nCorrectStrict = other.nCorrectStrict;
    nCorrectPartial = other.nCorrectPartial;
    nIncorrectStrict = other.nIncorrectStrict;
    nIncorrectPartial = other.nIncorrectPartial;
    nSingleCorrectStrict = other.nSingleCorrectStrict;
    nSingleCorrectPartial = other.nSingleCorrectPartial;
    nTargetsWithStrictResponses = other.nTargetsWithStrictResponses;
    nTargetsWithLenientResponses = other.nTargetsWithLenientResponses;
  }
  
  // increment this EvalStatsTagging object with the counts from another one
  public void add(EvalStatsTagging other) {
    nTargets += other.nTargets;
    nResponses += other.nResponses;
    nCorrectStrict += other.nCorrectStrict;
    nCorrectPartial += other.nCorrectPartial;
    nIncorrectStrict += other.nIncorrectStrict;
    nIncorrectPartial += other.nIncorrectPartial;
    nSingleCorrectStrict += other.nSingleCorrectStrict;
    nSingleCorrectPartial += other.nSingleCorrectPartial;
    nTargetsWithStrictResponses += other.nTargetsWithStrictResponses;
    nTargetsWithLenientResponses += other.nTargetsWithLenientResponses;
  }
  
  public void addTargets(int n) { nTargets += n; }
  public void addResponses(int n) { nResponses += n; }
  public void addCorrectStrict(int n) { nCorrectStrict += n; }
  public void addCorrectPartial(int n) { nCorrectPartial += n; }
  public void addSingleCorrectStrict(int n) { nSingleCorrectStrict += n; }
  public void addSingleCorrectPartial(int n) { nSingleCorrectPartial += n; }
  public void addIncorrectStrict(int n) { nIncorrectStrict += n; }
  public void addIncorrectPartial(int n) {nIncorrectPartial += n; }
  public void addTargetsWithStrictResponses(int n) { nTargetsWithStrictResponses += n; }
  public void addTargetsWithLenientResponses(int n) { nTargetsWithLenientResponses += n; }
  
  // TARGETS the "gold" or "key" annotations.
  protected int nTargets;
  public int getTargets() { return nTargets; }
  
  // RESPONSES: the annotations that should match the targets a perfectly as possible
  protected int nResponses;
  public int getResponses() { return nResponses; }
  
  
  protected int nTargetsWithStrictResponses;
  public int getTargetsWithStrictResponses() { return nTargetsWithStrictResponses; }
  public int getTargetsWithNoStrictResponses() { return nTargets - nTargetsWithStrictResponses; }

  protected int nTargetsWithLenientResponses;
  public int getTargetsWithLenientResponses() { return nTargetsWithLenientResponses; }
  public int getTargetsWithNoLenientResponses() { return nTargets - nTargetsWithLenientResponses; }
  
  // CORRECT
  // responses that are coextensive and equal
  protected int nCorrectStrict;  
  public int getCorrectStrict() { return nCorrectStrict; }
  // responses that are overlapping but not coextensive and that equal
  protected int nCorrectPartial;
  public int getCorrectPartial() { return nCorrectPartial; }
  public int getCorrectLenient() { return nCorrectStrict + nCorrectPartial; }
  
  // SINGLE CORRECT: this is the number of targets which have correct response but do not overlap
  // with a spurious response. This is used to calculate an accuracy measure which cannot get
  // increased by simply adding all possibilities to the response set.
  protected int nSingleCorrectStrict;
  protected int nSingleCorrectPartial;
  public int getSingleCorrectStrict() { return nSingleCorrectStrict; }
  public int getSingleCorrectPartial() { return nSingleCorrectPartial; }
  public int getSingleCorrectLenient() { return nSingleCorrectStrict + nSingleCorrectPartial; }
  
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
  
  // This is mainly for lists and gives the number of missings which are not missing
  // because there was not response list at all. Rather the missings which did have responses,
  // but no correct response was among them.
  // In other words, this is the number of targets which do have a strict response but the
  // response is not a strict correct one
  public int getTrueMissingInResponsesStrict() { 
    return nTargetsWithStrictResponses - nCorrectStrict; 
  }
  
  // A true missing lenient is a target for which no strict or partial incorrect or correct 
  // response exists;
  public int getTrueMissingLenient() { return getTrueMissingStrict() - nCorrectPartial - nIncorrectPartial;  }
  // NOTE: a true missing partial does not make sense since it would essentially equal a correct partial

  // This is the number of targets which do have a strict or lenient response, but the response
  // is not a correct strict or correct partial one.
  public int getTrueMissingInResponsesLenient() { 
    return nTargetsWithLenientResponses - nCorrectStrict - nCorrectPartial;
  }
  
  
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
      // if there are no targets, then we return 1.0 otherwise we return 0.0
      if(nTargets == 0) {
        return 1.0;
      } else {
        return 0.0;
      }
    } else {
      return nCorrectStrict/(double)nResponses;
    }
  }
  
  public double getPrecisionLenient() {
    if(nResponses == 0) {
      if(nTargets == 0) {
        return 1.0;
      } else {
        return 0.0;
      }
    } else {
      return getCorrectLenient()/(double)nResponses;
    }    
  }
  
  // RECALL 
  // Recall is the portion of targets that have a correct response
  public double getRecallStrict() {
    if(nTargets == 0) {
      if(nResponses == 0) {
        return 1.0;
      } else {
        return 0.0;        
      }
    } else {
      return nCorrectStrict/(double)nTargets;
    }
  }
  
  public double getRecallInResponsesStrict() {
    if(nTargetsWithStrictResponses == 0) {
      if(nResponses == 0) {
        return 1.0;
      } else {
        return 0.0;        
      }
    } else {
      assert(nTargetsWithStrictResponses>=nCorrectStrict);
      return nCorrectStrict/(double)nTargetsWithLenientResponses;
    }    
  }
  
  public double getRecallLenient() {
    if(nTargets == 0) {
      if(nResponses == 0) {
        return 1.0;
      } else {
        return 0.0;
      }
    } else {
      return getCorrectLenient()/(double)nTargets;
    }    
  }

  public double getRecallInResponsesLenient() {
    if(nTargetsWithLenientResponses == 0) {
      if(nResponses == 0) {
        return 1.0;
      } else {
        return 0.0;        
      }
    } else {
      assert(nTargetsWithLenientResponses>=getCorrectLenient());
      return getCorrectLenient()/(double)nTargetsWithLenientResponses;
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
  
  public double getSingleCorrectAccuracyStrict() {
    if(getTargets() == 0) {
      return 0.0;
    } else {
      return getSingleCorrectStrict() / (double)getTargets();
    }
  }
  
  public double getSingleCorrectAccuracyLenient() {
    if(getTargets() == 0) {
      return 0.0;
    } else {
      return getSingleCorrectLenient() / (double) getTargets();
    }
  }
  
  public double getFMeasureAverage(double beta) {
     return (getFMeasureLenient(beta) + getFMeasureStrict(beta)) / (2.0);
  }
  
  // Default conversion to String simply prints all the counts and measures.
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Precision Strict: "); sb.append(getPrecisionStrict()); sb.append("\n");
    sb.append("Recall Strict: "); sb.append(getRecallStrict()); sb.append("\n");
    sb.append("F1.0 Strict: "); sb.append(getFMeasureStrict(1.0)); sb.append("\n");
    sb.append("Accuracy Strict: "); sb.append(getSingleCorrectAccuracyStrict()); sb.append("\n");
    sb.append("Precision Lenient: "); sb.append(getPrecisionLenient()); sb.append("\n");
    sb.append("Recall Lenient: "); sb.append(getRecallLenient()); sb.append("\n");
    sb.append("F1.0 Lenient: "); sb.append(getFMeasureLenient(1.0)); sb.append("\n");
    sb.append("Accuracy Lenient: "); sb.append(getSingleCorrectAccuracyLenient()); sb.append("\n");
    sb.append("Targets: "); sb.append(getTargets()); sb.append("\n");
    sb.append("Responses: "); sb.append(getResponses()); sb.append("\n");
    sb.append("Correct Strict: "); sb.append(getCorrectStrict()); sb.append("\n");
    sb.append("Single Correct Strict: "); sb.append(getSingleCorrectStrict()); sb.append("\n");
    sb.append("Incorrect Strict: "); sb.append(getIncorrectStrict()); sb.append("\n");
    sb.append("Missing Strict: "); sb.append(getMissingStrict()); sb.append("\n");
    sb.append("True Missing Strict: "); sb.append(getTrueMissingStrict()); sb.append("\n");
    sb.append("True Missing In Responses Strict: "); sb.append(getTrueMissingInResponsesStrict()); sb.append("\n");
    sb.append("Spurious Strict: "); sb.append(getSpuriousStrict()); sb.append("\n");
    sb.append("True Spurious Strict: "); sb.append(getTrueSpuriousStrict()); sb.append("\n");
    sb.append("Correct Partial: "); sb.append(getCorrectPartial()); sb.append("\n");
    sb.append("Single Correct Partial: "); sb.append(getSingleCorrectPartial()); sb.append("\n");
    sb.append("Incorrect Partial: "); sb.append(getIncorrectPartial()); sb.append("\n");
    sb.append("Missing Lenient: "); sb.append(getMissingLenient()); sb.append("\n");
    sb.append("True Missing Lenient: "); sb.append(getTrueMissingLenient()); sb.append("\n");
    sb.append("True Missing In Responses Lenient: "); sb.append(getTrueMissingInResponsesLenient()); sb.append("\n");
    sb.append("Spurious Lenient: "); sb.append(getSpuriousLenient()); sb.append("\n");
    sb.append("True Spurious Lenient: "); sb.append(getTrueSpuriousLenient()); sb.append("\n");
    sb.append("Recall In Responses Strict: "); sb.append(getRecallInResponsesStrict()); sb.append("\n");
    sb.append("Recall In Responses Lenient: "); sb.append(getRecallInResponsesLenient()); sb.append("\n");
    return sb.toString();
  }

  /**
   * Create a String in TSV format containing all the headers for all fields in this object.
   * @return a String with all the headers of currently supported fields.
   */
  public static String getTSVHeaders() {
    StringBuilder sb = new StringBuilder();
    sb.append("thresholdType"); sb.append("\t");
    sb.append("threshold"); sb.append("\t");
    sb.append("precisionStrict"); sb.append("\t");
    sb.append("recallStrict"); sb.append("\t");
    sb.append("F1Strict"); sb.append("\t");
    sb.append("accuracyStrict"); sb.append("\t");
    sb.append("precisionLenient"); sb.append("\t");
    sb.append("recallLenient"); sb.append("\t");
    sb.append("F1Lenient"); sb.append("\t");
    sb.append("accuracyLenient"); sb.append("\t");
    sb.append("targets"); sb.append("\t");
    sb.append("responses"); sb.append("\t");
    sb.append("correctStrict"); sb.append("\t");
    sb.append("singleCorrectStrict"); sb.append("\t");
    sb.append("incorrectStrict"); sb.append("\t");
    sb.append("missingStrict"); sb.append("\t");
    sb.append("trueMissingStrict"); sb.append("\t");
    sb.append("spuriousStrict"); sb.append("\t");
    sb.append("trueSpuriousStrict"); sb.append("\t");
    sb.append("correctPartial"); sb.append("\t");
    sb.append("singleCorrectPartial"); sb.append("\t");
    sb.append("incorrectPartial"); sb.append("\t");
    sb.append("missingLenient"); sb.append("\t");
    sb.append("trueMissingLenient"); sb.append("\t");
    sb.append("spuriousLenient"); sb.append("\t");
    sb.append("trueSpuriousLenient"); 
    return sb.toString();
  }
  
  public String getTSVLine() {
    StringBuilder sb = new StringBuilder();
    sb.append(getPrecisionStrict()); sb.append("\t");
    sb.append(getRecallStrict()); sb.append("\t");
    sb.append(getFMeasureStrict(1.0)); sb.append("\t");
    sb.append(getSingleCorrectAccuracyStrict()); sb.append("\t");
    sb.append(getPrecisionLenient()); sb.append("\t");
    sb.append(getRecallLenient()); sb.append("\t");
    sb.append(getFMeasureLenient(1.0)); sb.append("\t");
    sb.append(getSingleCorrectAccuracyLenient()); sb.append("\t");
    sb.append(getTargets()); sb.append("\t");
    sb.append(getResponses()); sb.append("\t");
    sb.append(getCorrectStrict()); sb.append("\t");
    sb.append(getSingleCorrectStrict()); sb.append("\t");
    sb.append(getIncorrectStrict()); sb.append("\t");
    sb.append(getMissingStrict()); sb.append("\t");
    sb.append(getTrueMissingStrict()); sb.append("\t");
    sb.append(getSpuriousStrict()); sb.append("\t");
    sb.append(getTrueSpuriousStrict()); sb.append("\t");
    sb.append(getCorrectPartial()); sb.append("\t");
    sb.append(getSingleCorrectPartial()); sb.append("\t");
    sb.append(getIncorrectPartial()); sb.append("\t");
    sb.append(getMissingLenient()); sb.append("\t");
    sb.append(getTrueMissingLenient()); sb.append("\t");
    sb.append(getSpuriousLenient()); sb.append("\t");
    sb.append(getTrueSpuriousLenient()); 
    return sb.toString();
  }
  
  public String shortCounts() {
    StringBuilder sb = new StringBuilder();
    sb.append("T/R/CS/IS/TMS/TSS="); 
    sb.append(getTargets()); sb.append("/");
    sb.append(getResponses()); sb.append("/");
    sb.append(getCorrectStrict()); sb.append("/");
    sb.append(getIncorrectStrict()); sb.append("/");
    sb.append(getTrueMissingStrict()); sb.append("/");
    sb.append(getTrueSpuriousStrict()); sb.append(" ");
    sb.append("PS/RS/FS=");
    sb.append(getPrecisionStrict()); sb.append("/");
    sb.append(getRecallStrict()); sb.append("/");
    sb.append(getFMeasureStrict(1.0)); 
    
    return sb.toString();
  }
  
  /**
   * True if the instance is for ranks.
   * 
   * @return  TODO
   */
  public boolean isRank() {
    return (this instanceof EvalStatsTagging4Rank);
  }
  
  public boolean isScore() {
    return (this instanceof EvalStatsTagging4Score);
  }
  // TODO: can we add agreement measures based on SingleCorrectAccuracy?
  
  
}
