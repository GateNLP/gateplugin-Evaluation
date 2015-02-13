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
  USE_51FROM0TO1(new ArrayList<Double>(Arrays.asList(
          0.000, 0.002, 0.004, 0.006, 0.008, 0.010, 0.012, 0.014, 0.016, 0.018,
          0.020, 0.022, 0.024, 0.026, 0.028, 0.030, 0.032, 0.034, 0.036, 0.038,
          0.040, 0.042, 0.044, 0.046, 0.048, 0.050, 0.052, 0.054, 0.056, 0.058,
          0.060, 0.062, 0.064, 0.066, 0.068, 0.070, 0.072, 0.074, 0.076, 0.078,
          0.080, 0.082, 0.084, 0.086, 0.088, 0.090, 0.092, 0.094, 0.096, 0.098, 1.000
  ))),
  USE_ALLROUNDED(null), 
  USE_ALL(null);
  private ThresholdsToUse(List<Double> values) {
    theValues = values;
  }
  private List<Double> theValues = null;
  public List<Double> getThresholds() { return theValues; }
}
