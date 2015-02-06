package gate.plugin.evaluation.api;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 * @author Johann Petrak
 */
public class ByThresholdEvalStats {
  protected NavigableMap<Double,EvalPRFStats> byThresholdEvalStats = new TreeMap<Double,EvalPRFStats>();
  protected WhichThresholds whichThresholds = WhichThresholds.USE_ALL;
  public WhichThresholds getWhichThresholds() { return whichThresholds; }
  public static final double[] THRESHOLDS11FROM0TO1 = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
  public static final double[] THRESHOLDS21FROM0TO1 = { 
    0.00, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 
    0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0 };
  

  /**
   * By default, all scores will be used.
   */
  public ByThresholdEvalStats() {
    
  }
  
  public ByThresholdEvalStats(WhichThresholds which) {
    whichThresholds = which;
    if(whichThresholds == null) {
      whichThresholds = WhichThresholds.USE_ALL;
    }
  }

  public NavigableMap.Entry<Double,EvalPRFStats> lowerEntry(Double th) {
    return byThresholdEvalStats.lowerEntry(th);
  }

  public EvalPRFStats get(Double oth) {
    return byThresholdEvalStats.get(oth);
  }

  public Double floorKey(Double th) {
    return byThresholdEvalStats.floorKey(th);
  }

  public Double lowerKey(Double oth) {
    return byThresholdEvalStats.lowerKey(oth);
  }

  public void put(Double th, EvalPRFStats es) {
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
  
  public NavigableMap<Double,EvalPRFStats> getByThresholdEvalStats() { return byThresholdEvalStats; }
  
  
  
  
}
