/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.evaluation.api;

/**
 * A class to hold all the numbers for an evaluation and for calculating
 * various measures from the numbers.
 * In addition to the numbers, a EvalStats object also contains a confidence
 * or score threshold. This is a double value such the the stats object
 * represents the numbers if the evaluation was carried out with that 
 * threshold, i.e. a response is only considered if the score of the response
 * is >= the threshold. If the threshold is NaN then the stats object is not
 * associated with a threshold (usually that means it is a stats object that
 * represents all responses found).
 * 
 * @author Johann Petrak
 */
public class EvalStats {
  
  // responses that are coextensive and equal
  protected int nCorrectStrict;  
  public int getNCorrectStrict() { return nCorrectStrict; }
  // responses that are overlapping but not coextensive and that equal
  protected int nCorrectPartial;
  public int getNCorrectPartial() { return nCorrectPartial; }
  public int getNCorrectLenient() { return nCorrectStrict + nCorrectPartial; }
  
  // responses that are coextensive but not equal 
  protected int nIncorrectStrict;
  protected 
}
