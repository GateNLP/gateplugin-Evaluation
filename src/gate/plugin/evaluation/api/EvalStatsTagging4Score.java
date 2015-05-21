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

/**
 * A class to hold all the numbers for an evaluation and for calculating
 * various measures from the numbers.
 * In addition to the numbers, a EvalStatsTagging object also contains a confidence
 or score threshold. This is a double value such the the stats object
 represents the numbers if the evaluation was carried out with that 
 threshold, i.e. a response is only considered if the score of the response
 is >= the threshold. If the threshold is NaN then the stats object is not
 * associated with a threshold (usually that means it is a stats object that
 * represents all responses found).
 * 
 * @author Johann Petrak
 */
public class EvalStatsTagging4Score extends EvalStatsTagging {
  
  protected double threshold = Double.NaN;
  
  public EvalStatsTagging4Score() {
    super();
    this.threshold = Double.NaN;
  }
  public EvalStatsTagging4Score(double threshold) {
    super();
    this.threshold = threshold;
  }
  
  /**
   * Create a copy of an existing EvalPRFStats object.
   * This can be used to get an exact copy of the EvalPRFStats object passed to the constructor.
   * <p>
   * @param other 
   */
  // Why we did not implement clone(): http://www.artima.com/intv/bloch13.html
  public EvalStatsTagging4Score(EvalStatsTagging other) {
    super(other);
    if(other instanceof EvalStatsTagging4Score) {
      threshold = ((EvalStatsTagging4Score)other).threshold;
    } else {
      threshold = Double.NaN;
    }
  }
  
  public double getThreshold() { return threshold; }
  
  
  // Default conversion to String simply prints all the counts and measures.
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Threshold: "); sb.append(getThreshold()); sb.append("\n");
    sb.append(super.toString());
    return sb.toString();
  }

  /**
   * Create a String in TSV format containing all the headers for all fields in this object.
   * @return a String with all the headers of currently supported fields.
   */
  public static String getTSVHeaders() {
    StringBuilder sb = new StringBuilder();
    sb.append("threshold"); sb.append("\t");
    sb.append(EvalStatsTagging.getTSVHeaders());
    return sb.toString();
  }
  
  @Override
  public String getTSVLine() {
    StringBuilder sb = new StringBuilder();
    sb.append(threshold); sb.append("\t");
    sb.append(super.getTSVLine());
    return sb.toString();
  }
  
  
}
