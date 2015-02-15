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
package gate.plugin.evaluation.tests;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import org.junit.Test;

import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import static gate.Utils.*;
import gate.creole.ExecutionException;
import gate.plugin.evaluation.resources.EvaluateTagging;
import static gate.plugin.evaluation.tests.TestUtils.*;

/**
 * Second test, mainly testing the PR, not the back-end API.
 * 
 * @author Johann Petrak
 */
public class TestTagging2 {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private File pluginHome;
  private File testingDir;
  private EvaluateTagging prListEval1;
  @Before
  public void setup() throws GateException, IOException {
    if(!Gate.isInitialised()) {
      Gate.runInSandbox(true);
      Gate.init();
    }
    // load the plugin
    pluginHome = new File(".");
    pluginHome = pluginHome.getCanonicalFile();
    testingDir = new File(pluginHome,"test");
    Gate.getCreoleRegister().registerDirectories(
              pluginHome.toURI().toURL());
    // create a number of pre-configured PR instances
    FeatureMap parms = Factory.newFeatureMap();
    parms.put("annotationTypes", newStringList("M"));
    parms.put("evaluationId","EvaluataTagging1");
    parms.put("featureNames",newStringList("id")); // our only feature for matching is "id"
    parms.put("keyASName", "Key");
    parms.put("outputASName", "Evaluate");
    parms.put("outputDirectoryUrl",testingDir.toURI().toURL());
    parms.put("responseASName","Resp");
    parms.put("scoreFeatureName", "s");  // score feature is "s"
    parms.put("listIdFeatureName","ids");
    prListEval1 = (EvaluateTagging)Factory.createResource(
            "gate.plugin.evaluation.resources.EvaluateTagging", 
            parms, 
            Factory.newFeatureMap(), 
            "EvaluateTagging1");
    
  }
  
  /////////////////////////
  /// Tests
  /////////////////////////
  
  @Test
  public void testListEval01() throws ResourceInstantiationException, ExecutionException {
    System.out.println("Running test testListEval01");
    Document doc = newD();
    AnnotationSet keys = doc.getAnnotations("Key");
    AnnotationSet resp = doc.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.8)));
    addListAnn(resp,0,10,"M",ids);
    pln("DEBUG: key annotation set is: "+keys);
    pln("DEBUG: resp annotation set is: "+resp);
    runETPR(prListEval1,doc);
  }
  
  
}
