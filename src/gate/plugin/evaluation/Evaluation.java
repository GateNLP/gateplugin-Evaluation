/*
 *  Evaluation.java
 *
 * Copyright (c) 2000-2012, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 3, 29 June 2007.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  johann, 27/9/2014
 *
 * For details on the configuration options, see the user guide:
 * http://gate.ac.uk/cgi-bin/userguide/sec:creole-model:config
 */

package gate.plugin.evaluation;

import gate.*;
import gate.creole.*;
import gate.creole.metadata.*;
import gate.util.*;
import java.util.List;

// for now we simply calculate P/R/F lenient and strict for all the 
// types/features specified and at the same time we calculat accuracy
// We calculate accuracy based on the key:
// key accuracy: each key is used to count
// = if there is one overlapping/exact response, it is counted as true or
//   false, depending on the feature value.
// = if there is no overlapping response, it is counted as wrong
// = if more than one is overlapping, the best is counted and all others
//   are counted a wrong
// response accuracy: same as key accuracy, but in addition, all responses
// not overlapping with a key are also counted as wrong. (but see: nils)
// If nils are blank is enabled then if a feature is specified and the feature
// is an empty string, it is treated as a "NIL". In that case the following 
// applies: a key/response pair matches if both are nil, or a response that is
// nil matches a key which is not present or a key that is nil matches a 
// response which is missing
// NOTE: not sure what to do about NILs with an ID: probably we should treat
// them like any other URI -- the values have to match and anything can be
// a value to identify a NIL with an ID.
// If it is a classification corpus, we still also calculate PRF

// NOTES: 
// If there are several key sets, calculate average F1 between all pairs, kappa,
// maybe othermeasures. Also calculate consensus set and then calculate 
// average and indvidual measures for each key to the consensus.
// Same if there are several response sets. If several response sets and 
// several key sets, calculate both the average measures between each response
// set and all key sets and the measures between each response set and the
// key consensus set, and the measures for response consensus set and key
// consensus set. 
// Consensus set: there may be different ways to calculate the set we may 
// want to parametrize (same for key and response? only one?)



/** 
 * This class is the implementation of the resource EVALUATION.
 */
@CreoleResource(name = "Evaluation",
        comment = "Add a descriptive comment about this resource")
public class Evaluation  extends AbstractLanguageAnalyser
  implements ControllerAwarePR {

  // Parameters: at the moment we can specify one set for the key and target
  // and several combinations of type and optional feature name. Each of these
  // gets evaluated separately and then macro and micro averages are calculated
  // over all of them.
  
  private String keyASName;
  @CreoleParameter ( defaultValue="Key")
  @RunTime
  public void setKeyASName(String name) { keyASName = name; }
  public String getKeyASName() { return keyASName; }
  
  
  
  private String responseASName;
  @CreoleParameter (defaultValue ="Response")
  @RunTime
  public void setResponseASName(String name) { responseASName = name; }
  public String getResponseASName() { return responseASName; }
  
  private List<String> typesAndFeatures;
  @CreoleParameter (defaultValue = "Mention.inst")
  @RunTime
  public void setTypesAndFeatures(List<String> value) { typesAndFeatures = value; }
  public List<String> getTypesAndFeatures() { return typesAndFeatures; }
  
  
  @Override
  public Resource init() {
    // check if we are a duplicated copy getting initialzed: we can se this
    // by the shared stats object that got initialized in the the original.
    return this;
  }
  
  @Override
  public void reInit() {
    init();
  }
  
  @Override
  public void cleanup() {
    //
  }
  
  @Override
  public void execute() {
    
  }
  
  @Override
  public void controllerExecutionStarted(Controller cntrlr) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void controllerExecutionFinished(Controller cntrlr) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void controllerExecutionAborted(Controller cntrlr, Throwable thrwbl) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  

} // class Evaluation
