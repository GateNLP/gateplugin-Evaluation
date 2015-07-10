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

import gate.util.GateRuntimeException;
import gate.util.MethodNotImplementedException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A data structure that contains evaluation statistics by a score threshold. 
 * @author Johann Petrak
 */
/*
 * This data structure contains the evaluation statistics for each of a number of thresholds
 * t_i. the evaluation statistics ES(t_i) for the threshold t_i are calculated for a response
 * set which only includes responses for which the score feature s >= t_i. From this follows:
 * 1) t_i < t_j => all responses included in ES(t_j) are also included in ES(t_i), R(t_i) contains R(t_j)
 * 2) if we calculate some new ES(t_k) which is not already in the data structure, then 
 * the responses R(t_k) would have gotten included in all ES(t_i) with t_i < t_k. So the counts
 * of ES(t_k) need to get added to all ES(t_i) with t_i < t_k
 * If there is an ES(t_j) with t_j > t_k, then the R(t_j) would all be included in R(t_k), so we
 * need to add the counts from ES(t_j) to ES(t_k) before adding ES(t_k)
 * 3) if a document has no responses and we create this data structure by creating the thresholds
 * from all scores found in the documents, then we cannot find any threshold in such a document.
 * However, we still have to count the misising annotations. Since those missing annotations 
 * will show up, no matter what the threshold would be, we need to add the counts to ALL elements
 * that already are in the data structure. This can be accomplished by using a "virtual" ES(+inf)
 * 
*/
public class ByThEvalStatsTagging implements NavigableMap<Double,EvalStatsTagging> {
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
  
  /**
   * Add another ByThEvalStatsTagging object to this one.
   * @param other 
   */
  public void add(ByThEvalStatsTagging other) {
    add(other,true);
  }
  
  public void addNonCumulative(ByThEvalStatsTagging other) {
    add(other,false);
  }
  
  public void add(ByThEvalStatsTagging other, boolean cumulative) {
    // If the same threshold is in both, the stats in other get added to the stats in this for this
    // threshold. If there is a stats object for a threshold in the other set but not in this set,
    // then the stats object gets added to this set, with the next higher object of this set
    // added to it.
    // If there is a stats object in this set with no object in the other set, it gets incremented
    // by the next higher set in the other set.
    
    // Create an ordered set with all the thresholds from both this and other
    //System.out.println("DEBUG: adding other map to this map");
    NavigableSet<Double> allThs = new TreeSet<Double>();
    allThs.addAll(byThresholdEvalStats.keySet());
    allThs.addAll(other.getByThresholdEvalStats().keySet());
    for(double th : allThs) {
      //System.out.println("DEBUG: th="+th);
      EvalStatsTagging thisES = byThresholdEvalStats.get(th);
      EvalStatsTagging otherES = other.getByThresholdEvalStats().get(th);
      if(thisES != null && otherES != null) {
        //System.out.println("DEBUG: same th in both, th="+th);
        thisES.add(otherES);
      } else if(otherES != null && thisES == null) {
        //System.out.println("DEBUG: other exists, not in this, th="+th);       
        if(cumulative) {
          EvalStatsTagging newES = new EvalStatsTagging4Score(otherES);
          // check if there is a next higher evalstats object in this map
          NavigableMap.Entry<Double,EvalStatsTagging> thisHigherEntry = byThresholdEvalStats.higherEntry(th);
          if(thisHigherEntry != null) {
            //System.out.println("DEBUG: next higher this exists, adding nexthigher th="+thisHigherEntry.getKey());
            newES.add(thisHigherEntry.getValue());
          }
          byThresholdEvalStats.put(th, newES);
        } else {
          byThresholdEvalStats.put(th, new EvalStatsTagging4Score(otherES));
        }
      } else if(otherES == null && thisES != null) {
        //System.out.println("DEBUG: this exists, not in other, th="+th);
        EvalStatsTagging newES = new EvalStatsTagging4Score(thisES);
        NavigableMap.Entry<Double,EvalStatsTagging> otherHigherEntry = other.getByThresholdEvalStats().higherEntry(th);
        if(otherHigherEntry != null) {
          //System.out.println("DEBUG: next higher other exists, adding nexthigher th="+otherHigherEntry.getKey());
          newES.add(otherHigherEntry.getValue());
        }
        byThresholdEvalStats.put(th, newES);
      } else {
        throw new GateRuntimeException("Odd error, this should never happen!");
      }
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

  
  @Override
  public NavigableMap.Entry<Double,EvalStatsTagging> lowerEntry(Double th) {
    return byThresholdEvalStats.lowerEntry(th);
  }

  public EvalStatsTagging get(Double oth) {
    return byThresholdEvalStats.get(oth);
  }

  @Override
  public Double floorKey(Double th) {
    return byThresholdEvalStats.floorKey(th);
  }

  @Override
  public Double lowerKey(Double oth) {
    return byThresholdEvalStats.lowerKey(oth);
  }


  @Override
  public int size() {
    return byThresholdEvalStats.size();
  }

  @Override
  public Double firstKey() {
    return byThresholdEvalStats.firstKey();
  }

  @Override
  public Double higherKey(Double th) {
    return byThresholdEvalStats.higherKey(th);
  }
  
  
  public NavigableMap<Double,EvalStatsTagging> getByThresholdEvalStats() { return byThresholdEvalStats; }

  @Override
  public NavigableMap.Entry<Double,EvalStatsTagging> higherEntry(Double th) {
    return byThresholdEvalStats.higherEntry(th);
  }

  @Override
  public Entry<Double, EvalStatsTagging> floorEntry(Double key) {
    return byThresholdEvalStats.floorEntry(key);
  }

  @Override
  public Entry<Double, EvalStatsTagging> ceilingEntry(Double key) {
    return byThresholdEvalStats.ceilingEntry(key);
  }

  @Override
  public Double ceilingKey(Double key) {
    return byThresholdEvalStats.ceilingKey(key);
  }

  @Override
  public Entry<Double, EvalStatsTagging> firstEntry() {
    return byThresholdEvalStats.firstEntry();
  }

  @Override
  public Entry<Double, EvalStatsTagging> lastEntry() {
    return byThresholdEvalStats.lastEntry();
  }

  @Override
  public Entry<Double, EvalStatsTagging> pollFirstEntry() {
    return byThresholdEvalStats.pollFirstEntry();
  }

  @Override
  public Entry<Double, EvalStatsTagging> pollLastEntry() {
    return byThresholdEvalStats.pollFirstEntry();
  }

  @Override
  public NavigableMap<Double, EvalStatsTagging> descendingMap() {
    return byThresholdEvalStats.descendingMap();
  }

  @Override
  public NavigableSet<Double> navigableKeySet() {
    return byThresholdEvalStats.navigableKeySet();
  }

  @Override
  public NavigableSet<Double> descendingKeySet() {
    return byThresholdEvalStats.descendingKeySet();
  }

  @Override
  public NavigableMap<Double, EvalStatsTagging> subMap(Double fromKey, boolean fromInclusive, Double toKey, boolean toInclusive) {
    return byThresholdEvalStats.subMap(fromKey, fromInclusive, toKey, toInclusive);
  }

  @Override
  public NavigableMap<Double, EvalStatsTagging> headMap(Double toKey, boolean inclusive) {
    return byThresholdEvalStats.headMap(toKey, inclusive);
  }

  @Override
  public NavigableMap<Double, EvalStatsTagging> tailMap(Double fromKey, boolean inclusive) {
    return byThresholdEvalStats.tailMap(fromKey, inclusive);
  }

  @Override
  public SortedMap<Double, EvalStatsTagging> subMap(Double fromKey, Double toKey) {
    return byThresholdEvalStats.subMap(fromKey, toKey);
  }

  @Override
  public SortedMap<Double, EvalStatsTagging> headMap(Double toKey) {
    return byThresholdEvalStats.headMap(toKey);
  }

  @Override
  public SortedMap<Double, EvalStatsTagging> tailMap(Double fromKey) {
    return byThresholdEvalStats.tailMap(fromKey);
  }

  @Override
  public Comparator<? super Double> comparator() {
    return byThresholdEvalStats.comparator();
  }

  @Override
  public Double lastKey() {
    return byThresholdEvalStats.lastKey();
  }

  @Override
  public Set<Double> keySet() {
    return byThresholdEvalStats.keySet();
  }

  @Override
  public Collection<EvalStatsTagging> values() {
    return byThresholdEvalStats.values();
  }

  @Override
  public Set<Entry<Double, EvalStatsTagging>> entrySet() {
    return byThresholdEvalStats.entrySet();
  }

  @Override
  public boolean isEmpty() {
    return byThresholdEvalStats.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return byThresholdEvalStats.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return byThresholdEvalStats.containsValue(value);
  }

  @Override
  public EvalStatsTagging get(Object key) {
    return byThresholdEvalStats.get(key);
  }


  @Override
  public EvalStatsTagging remove(Object key) {
    return byThresholdEvalStats.remove(key);
  }

  @Override
  public void putAll(Map<? extends Double, ? extends EvalStatsTagging> m) {
    byThresholdEvalStats.putAll(m);
  }

  @Override
  public void clear() {
    byThresholdEvalStats.clear();
  }

  @Override
  public EvalStatsTagging put(Double key, EvalStatsTagging value) {
    return byThresholdEvalStats.put(key, value);
  }

  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Thresholds to use: ");
    sb.append(getWhichThresholds());
    sb.append("\n");
    NavigableMap<Double,EvalStatsTagging> map = getByThresholdEvalStats();
    for(double th : map.navigableKeySet()) {
      sb.append(map.get(th));
      sb.append("\n");
    }
    return sb.toString();
  }
  
  
}
