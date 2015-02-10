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

import gate.util.MethodNotImplementedException;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 * @author Johann Petrak
 */
public class ByThEvalStatsTagging {
  protected NavigableMap<Double,EvalStatsTagging> byThresholdEvalStats = new TreeMap<Double,EvalStatsTagging>();
  protected ThresholdsToUse whichThresholds = ThresholdsToUse.USE_ALL;
  public ThresholdsToUse getWhichThresholds() { return whichThresholds; }
  

  /**
   * By default, all scores will be used.
   */
  public ByThEvalStatsTagging() {
    
  }
  
  public ByThEvalStatsTagging(ThresholdsToUse which) {
    whichThresholds = which;
    if(whichThresholds == null) {
      whichThresholds = ThresholdsToUse.USE_ALL;
    }
  }
  
  // we also remember the thresholds for which we get the highest F strict and the highest F lenient
  public double highestFMeasureLenientThreshold() {
    // TODO
    throw new MethodNotImplementedException();
  }
  public double highestFMeasureStrictThreshold() {
    // TODO
    throw new MethodNotImplementedException();
  }
  
  // we should also provide a way to calculate the area under the P/R curve for strict and lenient
  // precision/recall curves
  public double areaUnderPRLenient() {
    // TODO
    throw new MethodNotImplementedException();
  }
  public double areaUnderPRStrict() {
    // TODO
    throw new MethodNotImplementedException();
  }

  // TODO, IMPORTANT: a method to add another object with stats by thresholds to this one. 
  public void add(ByThEvalStatsTagging other) {
    // TODO!!!!!
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
  
  
  // We just allow access to the contained NavigableMap object instead of actually wrappign the 
  // interface. 
  
  public NavigableMap<Double,EvalStatsTagging> getByThresholdEvalStats() { return byThresholdEvalStats; }
  
  
  
  
}
