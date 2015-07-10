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
public class ByRankEvalStatsTagging implements NavigableMap<Integer,EvalStatsTagging4Rank> {
  protected NavigableMap<Integer,EvalStatsTagging4Rank> byRankEvalStats = new TreeMap<Integer,EvalStatsTagging4Rank>();
  protected ThresholdsOrRanksToUse whichThresholds = ThresholdsOrRanksToUse.USE_RANKS_ALL;
  public ThresholdsOrRanksToUse getWhichThresholds() { return whichThresholds; }
  

  /**
   * By default, all scores will be used.
   */
  public ByRankEvalStatsTagging() {
    
  }
  
  public ByRankEvalStatsTagging(ThresholdsOrRanksToUse which) {
    whichThresholds = which;
    if(whichThresholds == null) {
      whichThresholds = ThresholdsOrRanksToUse.USE_RANKS_ALL;
    } else {
      if(which.getThresholdsToUse() != null) {
        throw new GateRuntimeException("ByRankEvalStatsTagging must be created with a rank, not threshold");
      }
    }
  }
  
  /**
   * Add another ByThEvalStatsTagging object to this one.
   * The purpose of this method is to add a per-document object which already has the correct
   * by-rank threshold stats to a global object. 
   * This works in the following way: if both this and the other stats object exists for rank k,
   * then the other object gets added to this object.
   * If this object does not have a stats object at some rank, but the other has one, then a
   * new object is created for this object and it gets initialized with the values of the 
   * object with the next lower rank before the other object was added. 
   * If there is a stats object in this object without one in the other object, the next lower
   * rank object from the other object gets added. 
   * If some next-lower rank object needs to get added but it does not exist, this is an error.
   * It is also an error if the set of ranks for both objects is different.
   * 
   * @param other 
   */
  public void add(ByRankEvalStatsTagging other) {
    if(!this.whichThresholds.equals(other.whichThresholds)) {
      System.err.println("SERIOUS WARNING Cannot add if the thresholds settings do not match this="+this.whichThresholds+" other="+other.whichThresholds);
    }
    
    // Create an ordered set with all the rank thresholds from both this and other
    NavigableSet<Integer> allRanks = new TreeSet<Integer>();
    allRanks.addAll(byRankEvalStats.keySet());
    allRanks.addAll(other.getByRankEvalStats().keySet());
    
    // in order to prevent that counts are added twice, we process the thresholds starting 
    // with the highest. That way, if we add some next-lower element of this it will not already
    // have anything added by other.
    for(int rank : allRanks.descendingSet()) {
      //System.out.println("DEBUG: merging rank="+rank);
      EvalStatsTagging thisES = byRankEvalStats.get(rank);
      EvalStatsTagging otherES = other.getByRankEvalStats().get(rank);
      if(thisES != null && otherES != null) {
        //System.out.println("DEBUG: same th in both, th="+th);
        thisES.add(otherES);
      } else if(otherES != null && thisES == null) {
        // The other stats object exists, but this does not. In that case we create a new
        // this object, initialized with the counts from the next lower this obejct and then 
        // add the other object.
        NavigableMap.Entry<Integer,EvalStatsTagging4Rank> thisLowerEntry = byRankEvalStats.lowerEntry(rank);
        EvalStatsTagging4Rank newES = null;
        if(thisLowerEntry == null) {
          // if we do not have a lower rank entry, simply add from other: this could happen if we
          // start with an empty this.
          newES = new EvalStatsTagging4Rank(otherES);          
        } else {
          newES = new EvalStatsTagging4Rank(thisLowerEntry.getValue());
          newES.add(otherES);
        }
        newES.setRank(rank);
        
        byRankEvalStats.put(rank, newES);
      } else if(otherES == null && thisES != null) {
        // if the other stats object does not exist, then we need to add the next lower 
        // other object
        NavigableMap.Entry<Integer,EvalStatsTagging4Rank> otherLowerEntry = other.byRankEvalStats.lowerEntry(rank);
        if(otherLowerEntry == null) {
          System.err.println("ERROR: Cannot add stats if we have a rank and the other object does not have the same or lower rank, rank is "+rank);
        } else {
          thisES.add(otherLowerEntry.getValue());
        }
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
  public NavigableMap.Entry<Integer,EvalStatsTagging4Rank> lowerEntry(Integer th) {
    return byRankEvalStats.lowerEntry(th);
  }

  public EvalStatsTagging get(Integer oth) {
    return byRankEvalStats.get(oth);
  }

  @Override
  public Integer floorKey(Integer th) {
    return byRankEvalStats.floorKey(th);
  }

  @Override
  public Integer lowerKey(Integer oth) {
    return byRankEvalStats.lowerKey(oth);
  }


  @Override
  public int size() {
    return byRankEvalStats.size();
  }

  @Override
  public Integer firstKey() {
    return byRankEvalStats.firstKey();
  }

  @Override
  public Integer higherKey(Integer th) {
    return byRankEvalStats.higherKey(th);
  }
  
  
  public NavigableMap<Integer,EvalStatsTagging4Rank> getByRankEvalStats() { return byRankEvalStats; }

  @Override
  public NavigableMap.Entry<Integer,EvalStatsTagging4Rank> higherEntry(Integer th) {
    return byRankEvalStats.higherEntry(th);
  }

  @Override
  public Entry<Integer, EvalStatsTagging4Rank> floorEntry(Integer key) {
    return byRankEvalStats.floorEntry(key);
  }

  @Override
  public Entry<Integer, EvalStatsTagging4Rank> ceilingEntry(Integer key) {
    return byRankEvalStats.ceilingEntry(key);
  }

  @Override
  public Integer ceilingKey(Integer key) {
    return byRankEvalStats.ceilingKey(key);
  }

  @Override
  public Entry<Integer, EvalStatsTagging4Rank> firstEntry() {
    return byRankEvalStats.firstEntry();
  }

  @Override
  public Entry<Integer, EvalStatsTagging4Rank> lastEntry() {
    return byRankEvalStats.lastEntry();
  }

  @Override
  public Entry<Integer, EvalStatsTagging4Rank> pollFirstEntry() {
    return byRankEvalStats.pollFirstEntry();
  }

  @Override
  public Entry<Integer, EvalStatsTagging4Rank> pollLastEntry() {
    return byRankEvalStats.pollFirstEntry();
  }

  @Override
  public NavigableMap<Integer, EvalStatsTagging4Rank> descendingMap() {
    return byRankEvalStats.descendingMap();
  }

  @Override
  public NavigableSet<Integer> navigableKeySet() {
    return byRankEvalStats.navigableKeySet();
  }

  @Override
  public NavigableSet<Integer> descendingKeySet() {
    return byRankEvalStats.descendingKeySet();
  }

  @Override
  public NavigableMap<Integer, EvalStatsTagging4Rank> subMap(Integer fromKey, boolean fromInclusive, Integer toKey, boolean toInclusive) {
    return byRankEvalStats.subMap(fromKey, fromInclusive, toKey, toInclusive);
  }

  @Override
  public NavigableMap<Integer, EvalStatsTagging4Rank> headMap(Integer toKey, boolean inclusive) {
    return byRankEvalStats.headMap(toKey, inclusive);
  }

  @Override
  public NavigableMap<Integer, EvalStatsTagging4Rank> tailMap(Integer fromKey, boolean inclusive) {
    return byRankEvalStats.tailMap(fromKey, inclusive);
  }

  @Override
  public SortedMap<Integer, EvalStatsTagging4Rank> subMap(Integer fromKey, Integer toKey) {
    return byRankEvalStats.subMap(fromKey, toKey);
  }

  @Override
  public SortedMap<Integer, EvalStatsTagging4Rank> headMap(Integer toKey) {
    return byRankEvalStats.headMap(toKey);
  }

  @Override
  public SortedMap<Integer, EvalStatsTagging4Rank> tailMap(Integer fromKey) {
    return byRankEvalStats.tailMap(fromKey);
  }

  @Override
  public Comparator<? super Integer> comparator() {
    return byRankEvalStats.comparator();
  }

  @Override
  public Integer lastKey() {
    return byRankEvalStats.lastKey();
  }

  @Override
  public Set<Integer> keySet() {
    return byRankEvalStats.keySet();
  }

  @Override
  public Collection<EvalStatsTagging4Rank> values() {
    return byRankEvalStats.values();
  }

  @Override
  public Set<Entry<Integer, EvalStatsTagging4Rank>> entrySet() {
    return byRankEvalStats.entrySet();
  }

  @Override
  public boolean isEmpty() {
    return byRankEvalStats.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return byRankEvalStats.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return byRankEvalStats.containsValue(value);
  }

  @Override
  public EvalStatsTagging4Rank get(Object key) {
    return byRankEvalStats.get(key);
  }


  @Override
  public EvalStatsTagging4Rank remove(Object key) {
    return byRankEvalStats.remove(key);
  }

  @Override
  public void putAll(Map<? extends Integer, ? extends EvalStatsTagging4Rank> m) {
    byRankEvalStats.putAll(m);
  }

  @Override
  public void clear() {
    byRankEvalStats.clear();
  }

  @Override
  public EvalStatsTagging4Rank put(Integer key, EvalStatsTagging4Rank value) {
      return byRankEvalStats.put(key, (EvalStatsTagging4Rank)value);
  }

  public EvalStatsTagging4Rank put(Integer key, EvalStatsTagging value) {
    if(value instanceof EvalStatsTagging4Rank) {
      return byRankEvalStats.put(key, (EvalStatsTagging4Rank)value);
    } else {
      throw new ClassCastException("Cannot add a EvalStatsTagging to ByRankEvalStatsTagging if it is not a EvalStatsTagging4Rank");
    }
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Thresholds to use: ");
    sb.append(getWhichThresholds());
    sb.append("\n");
    NavigableMap<Integer,EvalStatsTagging4Rank> map = getByRankEvalStats();
    for(Integer th : map.navigableKeySet()) {
      sb.append(map.get(th));
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public String toString4Debug() {
    StringBuilder sb = new StringBuilder();
    sb.append("Thresholds to use: ");
    sb.append(getWhichThresholds());
    sb.append("\n");
    NavigableMap<Integer,EvalStatsTagging4Rank> map = getByRankEvalStats();
    for(Integer th : map.navigableKeySet()) {
      sb.append(map.get(th).toString4Debug());
      sb.append("\n");
    }
    return sb.toString();    
  }
  
}
