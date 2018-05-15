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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Johann Petrak
 */
public enum ThresholdsOrRanksToUse {
  USE_TH11FROM0TO1(
          new ArrayList<Double>(Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)), 
          null,
          ThresholdsToUse.USE_11FROM0TO1), 
  USE_TH21FROM0TO1(new ArrayList<Double>(Arrays.asList(
          0.00, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 
          0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0)),
          null,
          ThresholdsToUse.USE_21FROM0TO1), 
  USE_TH51FROM0TO1(new ArrayList<Double>(Arrays.asList(
          0.000, 0.002, 0.004, 0.006, 0.008, 0.010, 0.012, 0.014, 0.016, 0.018,
          0.020, 0.022, 0.024, 0.026, 0.028, 0.030, 0.032, 0.034, 0.036, 0.038,
          0.040, 0.042, 0.044, 0.046, 0.048, 0.050, 0.052, 0.054, 0.056, 0.058,
          0.060, 0.062, 0.064, 0.066, 0.068, 0.070, 0.072, 0.074, 0.076, 0.078,
          0.080, 0.082, 0.084, 0.086, 0.088, 0.090, 0.092, 0.094, 0.096, 0.098, 1.000
          )),
          null,
          ThresholdsToUse.USE_51FROM0TO1),
  USE_TH_ALLROUNDED(null,null,ThresholdsToUse.USE_ALLROUNDED), 
  USE_TH_ALL(null,null,ThresholdsToUse.USE_ALL),
  USE_RANKS_11FROM0T10(null,new ArrayList<Integer>(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10)),null),
  USE_RANKS_11FROM0TO50(null,new ArrayList<Integer>(Arrays.asList(0,5,10,15,20,25,30,35,40,45)),null),
  USE_RANKS_11FROM0TO100(null,new ArrayList<Integer>(Arrays.asList(0,10,20,30,40,50,60,70,80,90,100)),null),
  USE_RANKS_ALL(null,null,null);
  private ThresholdsOrRanksToUse(List<Double> values, List<Integer> ranks, ThresholdsToUse equiv) {
    theThresholds = values;
    theRanks = ranks;
    equivalentThresholdsToUse = equiv;
  }
  private List<Double> theThresholds = null;
  private List<Integer> theRanks = null;
  private ThresholdsToUse equivalentThresholdsToUse = null;
  public List<Double> getThresholds() { return theThresholds; }
  public List<Integer> getRanks() { return theRanks; }
  /**
   * If this returns null, we use the ranks.
   * @return  TODO
   */
  public ThresholdsToUse getThresholdsToUse() { return equivalentThresholdsToUse; }
}
