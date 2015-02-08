/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.evaluation.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Johann Petrak
 */
public enum ThresholdsToUse {
  USE_11FROM0TO1(new ArrayList<Double>(Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0))), 
  USE_21FROM0TO1(new ArrayList<Double>(Arrays.asList(
          0.00, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 
          0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0))), 
  USE_ALLROUNDED(null), 
  USE_ALL(null);
  private ThresholdsToUse(List<Double> values) {
    theValues = values;
  }
  private List<Double> theValues = null;
  public List<Double> getThresholds() { return theValues; }
}
