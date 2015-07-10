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
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import org.junit.Test;

import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import static gate.Utils.*;
import gate.creole.ExecutionException;
import gate.persist.PersistenceException;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.resources.EvaluateTagging;
import gate.plugin.evaluation.resources.EvaluateTagging4Lists;
import static gate.plugin.evaluation.tests.TestUtils.*;
import gate.util.GateRuntimeException;
import gate.util.persistence.PersistenceManager;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import static org.junit.Assert.*;

/**
 * Third set of tests, mainly detailed tests for list evaluation statistics
 * 
 * @author Johann Petrak
 */
public class TestTagging4 {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private File pluginHome;
  private File testingDir;
  private EvaluateTagging4Lists prListEval1;
  private CorpusController controller;
  private Corpus corpus;
  private static final Logger logger = Logger.getLogger(TestTagging1.class);
  @Before
  public void setup() throws GateException, IOException {
    /*
    logger.setLevel(Level.DEBUG);
    ConsoleAppender appender = new ConsoleAppender();
    appender.setWriter(new OutputStreamWriter(System.out));
    appender.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
    logger.addAppender(appender);
    */
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.INFO);
    logger.setLevel(Level.DEBUG);

    if(!Gate.isInitialised()) {
      if(System.getProperty("gate.home") != null) {
        Gate.setGateHome(new File(System.getProperty("gate.home")));
      }
      Gate.runInSandbox(false);
      Gate.init();
    }
    File home = Gate.getGateHome();
    if(home==null) {
      throw new GateRuntimeException("GATE home is not set, cannot run test");
    } else {
      System.out.println("GATE home is "+home);
    }
    // load the plugin
    pluginHome = new File(".");
    pluginHome = pluginHome.getCanonicalFile();
    testingDir = new File(pluginHome,"test");
    Gate.getCreoleRegister().registerDirectories(
              pluginHome.toURI().toURL());
    
    // TODO: create a PR, change PR so we can access all the list-based data structures via the 
    // API, then print AND retrieve them and check for correct values in the tests!
    
    // create the PR 
    FeatureMap parms = Factory.newFeatureMap();
    parms.put("addDocumentFeatures", false);
    parms.put("edgeFeatureName","ids");
    parms.put("elementType","M");
    parms.put("evaluationId","TestTagging3");
    List<String> featureList = new ArrayList<String>();
    featureList.add("id");
    parms.put("featureNames",featureList);
    parms.put("keyASName", "Key");
    parms.put("responseASName", "Resp");
    parms.put("keyType","M");
    parms.put("listType","L");
    parms.put("outputDirectoryString",".");
    parms.put("scoreFeatureName", "s");
    //parms.put("whichThresholds","USE_RANKS_11FROM0TO100");
    parms.put("whichThresholds","USE_TH11FROM0TO1");
    prListEval1 = (EvaluateTagging4Lists)Factory.createResource(EvaluateTagging4Lists.class.getCanonicalName(), parms);
    
    parms = Factory.newFeatureMap();
    controller = (CorpusController)Factory.createResource(gate.creole.SerialAnalyserController.class.getCanonicalName(), parms);
    
    List<ProcessingResource> prs = new ArrayList<ProcessingResource>();
    prs.add(prListEval1);
    controller.setPRs(prs);
    
    corpus = Factory.newCorpus("Corpus");
    controller.setCorpus(corpus);
  }
  
  /////////////////////////
  /// Tests
  /////////////////////////
  
  @Test
  public void testTagging4Lists1() throws ResourceInstantiationException, ExecutionException, PersistenceException, IOException {
    logger.debug("Running test testTagging4Lists1");
    
    Document doc1 = newD();
    AnnotationSet keys = doc1.getAnnotations("Key");
    AnnotationSet resp = doc1.getAnnotations("Resp");
    
    List<Integer> ids;
    
    // We create 4 list annotations, with 2, 3, 4 and 6 candidates
    // One list does not have any correct candidate
    // The rest has:
    // correct in position 0
    // correct in position 1
    // correct in position 2
    
    // first one: 2 candidates, none correct
    addAnn(keys,0,1,"M",featureMap("id","x"));   // key
    ids = newIntList();  
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y1","s",0.9)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y2","s",0.8)));
    addListAnn(resp,0,1,"L",ids);
    
    // second one 3 candidates, correct one at position 0
    addAnn(keys,2,3,"M",featureMap("id","x"));   // key
    ids = newIntList();  
    ids.add(addAnn(resp, 2, 3, "M", featureMap("id","x","s",0.95)));
    ids.add(addAnn(resp, 2, 3, "M", featureMap("id","y1","s",0.81)));
    ids.add(addAnn(resp, 2, 3, "M", featureMap("id","y2","s",0.72)));
    addListAnn(resp,2,3,"L",ids);
    
    // third one, 4 candidates, second one is correct
    addAnn(keys,4,5,"M",featureMap("id","x"));   // key
    ids = newIntList();  
    ids.add(addAnn(resp, 4, 5, "M", featureMap("id","y1","s",0.96)));
    ids.add(addAnn(resp, 4, 5, "M", featureMap("id","x","s",0.81)));
    ids.add(addAnn(resp, 4, 5, "M", featureMap("id","y2","s",0.76)));
    ids.add(addAnn(resp, 4, 5, "M", featureMap("id","y3","s",0.72)));
    addListAnn(resp,4,5,"L",ids);

    // fourth one, 6 candidates, third one is correct
    addAnn(keys,6,7,"M",featureMap("id","x"));   // key
    ids = newIntList();  
    ids.add(addAnn(resp, 6, 7, "M", featureMap("id","y1","s",0.96)));
    ids.add(addAnn(resp, 6, 7, "M", featureMap("id","y2","s",0.96)));
    ids.add(addAnn(resp, 6, 7, "M", featureMap("id","x","s",0.81)));
    ids.add(addAnn(resp, 6, 7, "M", featureMap("id","y3","s",0.76)));
    ids.add(addAnn(resp, 6, 7, "M", featureMap("id","y4","s",0.72)));
    ids.add(addAnn(resp, 6, 7, "M", featureMap("id","y5","s",0.72)));
    addListAnn(resp,6,7,"L",ids);
    
    corpus.add(doc1);
    controller.execute();
    
    // get the stuff we have calculated ...
    
    
  }  
  
}
