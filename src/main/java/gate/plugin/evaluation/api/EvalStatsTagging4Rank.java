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

import gate.util.GateRuntimeException;

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
public class EvalStatsTagging4Rank extends EvalStatsTagging {
  
  protected int rank;
  
  /**
   * Using this class without actually specifying a rank threshold does not make sense.
   */
  private EvalStatsTagging4Rank() {
  }
  public EvalStatsTagging4Rank(int threshold) {
    super();
    this.rank = threshold;
  }
  
  /**
   * Create a copy of an existing EvalPRFStats object.
   * This can be used to get an exact copy of the EvalPRFStats object passed to the constructor.
   * 
   * @param other  TODO
   */
  // Why we did not implement clone(): http://www.artima.com/intv/bloch13.html
  public EvalStatsTagging4Rank(EvalStatsTagging other) {
    super(other);
    if(other instanceof EvalStatsTagging4Rank) {
      rank = ((EvalStatsTagging4Rank)other).rank;
    } else {
      throw new GateRuntimeException("Cannot create a EvalStatsTagging4Rank from "+other.getClass());
    }
  }
  
  public int getRank() { return rank; }
  
  public void setRank(Integer rank) { this.rank = rank; }
  
  
  // Default conversion to String simply prints all the counts and measures.
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Rank: "); sb.append(getRank()); sb.append("\n");
    sb.append(super.toString());
    return sb.toString();
  }

  
  @Override
  public String getTSVLine() {
    StringBuilder sb = new StringBuilder();
    sb.append("rank"); sb.append("\t");
    sb.append(rank); sb.append("\t");
    sb.append(super.getTSVLine());
    return sb.toString();
  }
  
  public String toString4Debug() {
    StringBuilder sb = new StringBuilder();
    sb.append(rank); sb.append(": ");
    sb.append(getTargets()); sb.append("/");
    sb.append(getResponses()); sb.append("/");
    sb.append(getCorrectStrict()); sb.append("/");
    sb.append(getIncorrectStrict());
    return sb.toString();    
  }
  
  
}
