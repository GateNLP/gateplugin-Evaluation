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

/**
 * A class to hold all the numbers for an evaluation and for calculating
 * various measures from the numbers.
 * In addition to the numbers, a EvalStatsTagging object also contains a confidence
 or score threshold. This is a double value such the the stats object
 represents the numbers if the evaluation was carried out with that 
 threshold, i.e. a response is only considered if the score of the response
 is greater or equal than the threshold. If the threshold is NaN then the stats object is not
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
   * 
   * @param other  TODO
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

  
  @Override
  public String getTSVLine() {
    StringBuilder sb = new StringBuilder();
    sb.append("score"); sb.append("\t");
    sb.append(threshold); sb.append("\t");
    sb.append(super.getTSVLine());
    return sb.toString();
  }
  
  
}
