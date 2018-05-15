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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A version of EvalStatsTagging that represents the macro average over several EvalStatsTagging objects.
 * Note: all counts are simply the sum of all the EvalStatsTagging counts that are added, 
 * but measures are the averages of the measures from all the EvalStatsTagging measures.
 * @author Johann Petrak
 */
public class EvalStatsTaggingMacro extends EvalStatsTagging {

  
  public static final double EPS = 1.7763568394002505e-15;
  
  public EvalStatsTaggingMacro() {
    
  }
  
  public EvalStatsTaggingMacro(Collection<EvalStatsTagging> others) {
    for(EvalStatsTagging es : others) {
      add(es);
    }
  }
  
  protected List<Double> precisionStrictVals = new ArrayList<>();
  protected List<Double> precisionLenientVals = new ArrayList<>();
  protected List<Double> recallStrictVals = new ArrayList<>();
  protected List<Double> recallLenientVals = new ArrayList<>();
  protected List<Double> fMeasureStrictVals = new ArrayList<>();
  protected List<Double> fMeasureLenientVals = new ArrayList<>();
  protected List<Double> singleCorrectAccuracyStrictVals = new ArrayList<>();
  protected List<Double> singleCorrectAccuracyLenientVals = new ArrayList<>();
  protected List<Integer> targetsVals = new ArrayList<>();
  protected List<Integer> responsesVals = new ArrayList<>();
  
  
  @Override
  public void add(EvalStatsTagging other) {
    nTargets += other.nTargets;
    nResponses += other.nResponses;
    nCorrectStrict += other.nCorrectStrict;
    nCorrectPartial += other.nCorrectPartial;
    nIncorrectStrict += other.nIncorrectStrict;
    nIncorrectPartial += other.nIncorrectPartial;
    nSingleCorrectStrict += other.nSingleCorrectStrict;
    nSingleCorrectPartial += other.nSingleCorrectPartial;
    // now in addition to the counters, we also need to remember the basic measures from which 
    // to calculate the macro averages:
    precisionStrictVals.add(other.getPrecisionStrict());
    precisionLenientVals.add(other.getPrecisionLenient());
    recallStrictVals.add(other.getRecallStrict());
    recallLenientVals.add(other.getRecallLenient());
    fMeasureStrictVals.add(other.getFMeasureStrict(1.0));
    fMeasureLenientVals.add(other.getFMeasureLenient(1.0));
    singleCorrectAccuracyLenientVals.add(other.getSingleCorrectAccuracyLenient());
    singleCorrectAccuracyStrictVals.add(other.getSingleCorrectAccuracyStrict());
    targetsVals.add(other.getTargets());
    responsesVals.add(other.getResponses());
  }
  
  @Override
  public double getPrecisionStrict() {
    double tmp = averageOf(precisionStrictVals);
    if(Double.isNaN(tmp)) {
      return 1.0;
    } else {
      return tmp;
    }
  }
  
  @Override
  public double getPrecisionLenient() {
    double tmp = averageOf(precisionLenientVals);
    if(Double.isNaN(tmp)) {
      return 1.0;
    } else {
      return tmp;
    }
  }
  
  @Override
  public double getRecallStrict() {
    double tmp = averageOf(recallStrictVals);
    if(Double.isNaN(tmp)) {
      return 1.0;
    } else {
      return tmp;
    }
  }
  
  @Override
  public double getRecallLenient() {
    double tmp = averageOf(recallLenientVals);
    if(Double.isNaN(tmp)) {
      return 1.0;
    } else {
      return tmp;
    }
  }
  
  /**
   * At the moment we only allow to calculate the FMeasure for beta 1.0. 
   * @param beta TODO
   * @return  TODO
   */
  @Override
  public double getFMeasureStrict(double beta) {
    if(Math.abs(beta - 1.0) > EPS) {
      throw new GateRuntimeException("Macro average for the FMeasure can only be calculated for beta=1.0 at the moment");
    }
    double tmp = averageOf(fMeasureStrictVals);
    if(Double.isNaN(tmp)) {
      return 1.0;
    } else {
      return tmp;
    }
  }
  
  /**
   * At the moment we only allow to calculate the FMeasure for beta 1.0. 
   * @param beta TODO
   * @return  TODO
   */
  @Override
  public double getFMeasureLenient(double beta) {
    if(Math.abs(beta - 1.0) > EPS) {
      throw new GateRuntimeException("Macro average for the FMeasure can only be calculated for beta=1.0 at the moment");
    }
    double tmp = averageOf(fMeasureLenientVals);
    if(Double.isNaN(tmp)) {
      return 1.0;
    } else {
      return tmp;
    }
  }
  
  
  /**
   * Return the number of EvalStatsTagging objects from which this macro average object was 
   * built.
   * @return TODO
   */
  public int getN() {
    return precisionLenientVals.size();
  }
  
  /** 
   * Calculate the average over the values.
   * @param values TODO
   * @return  TODO
   */
  protected double averageOf(List<Double> values) {
    double sum = 0.0;
    int n = 0;
    for(Double val : values) {
      //if(targetsVals.get(i) > 0 || responsesVals.get(i) > 0) {
        sum += val;
        n++;
      //}
    }
    if(n > 0) {
      return sum / n;
    } else {
      return Double.NaN;
    }
  }
  
  
}
