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

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 * @author Johann Petrak
 */
public class ByThEvalStatsTagging {
  protected NavigableMap<Double,EvalStatsTagging> byThresholdEvalStats = new TreeMap<Double,EvalStatsTagging>();
  protected WhichThresholds whichThresholds = WhichThresholds.USE_ALL;
  public WhichThresholds getWhichThresholds() { return whichThresholds; }
  public static final double[] THRESHOLDS11FROM0TO1 = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
  public static final double[] THRESHOLDS21FROM0TO1 = { 
    0.00, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 
    0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0 };
  

  /**
   * By default, all scores will be used.
   */
  public ByThEvalStatsTagging() {
    
  }
  
  public ByThEvalStatsTagging(WhichThresholds which) {
    whichThresholds = which;
    if(whichThresholds == null) {
      whichThresholds = WhichThresholds.USE_ALL;
    }
  }

  public NavigableMap.Entry<Double,EvalStatsTagging> lowerEntry(Double th) {
    return byThresholdEvalStats.lowerEntry(th);
  }

  public EvalStatsTagging get(Double oth) {
    return byThresholdEvalStats.get(oth);
  }

  public Double floorKey(Double th) {
    return byThresholdEvalStats.floorKey(th);
  }

  public Double lowerKey(Double oth) {
    return byThresholdEvalStats.lowerKey(oth);
  }

  public void put(Double th, EvalStatsTagging es) {
    byThresholdEvalStats.put(th,es);
  }

  public int size() {
    return byThresholdEvalStats.size();
  }

  public Double firstKey() {
    return byThresholdEvalStats.firstKey();
  }

  public Double higherKey(Double th) {
    return byThresholdEvalStats.higherKey(th);
  }
  
  public enum WhichThresholds {
    USE_11FROM0TO1,
    USE_21FROM0TO1,
    USE_ALLROUNDED,
    USE_ALL;
  }
  
  // We just allow access to the contained NavigableMap object instead of actually wrappign the 
  // interface. 
  
  public NavigableMap<Double,EvalStatsTagging> getByThresholdEvalStats() { return byThresholdEvalStats; }
  
  
  
  
}
