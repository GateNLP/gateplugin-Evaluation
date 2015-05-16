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
import static gate.plugin.evaluation.tests.TestUtils.*;
import gate.util.GateRuntimeException;
import gate.util.persistence.PersistenceManager;
import java.io.OutputStreamWriter;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import static org.junit.Assert.*;

/**
 * Third set of tests, mainly testing the PR within pipelines.
 * 
 * @author Johann Petrak
 */
public class TestTagging3 {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private File pluginHome;
  private File testingDir;
  private EvaluateTagging prListEval1;
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
    
  }
  
  /////////////////////////
  /// Tests
  /////////////////////////
  
  @Test
  public void testCorp1() throws ResourceInstantiationException, ExecutionException, PersistenceException, IOException {
    logger.debug("Running test testCorp1");
    File pipelineFile = new File(testingDir,"test-eval-corp1.xgapp");
    CorpusController controller = (CorpusController)PersistenceManager.loadObjectFromFile(pipelineFile);
    // access the PRs that are in the controller
    EvaluateTagging corp1orig = null;
    
    for(ProcessingResource pr : controller.getPRs()) {
      if(pr.getName().equals("EvaluateTagging:corp1:orig")) {
        corp1orig = (EvaluateTagging)pr;
      }
    }
    assertNotNull(corp1orig);
    controller.execute();
    // now check if the expected evaluation statistics are there
    EvalStatsTagging es_allTypes = corp1orig.getEvalStatsTagging("");
    assertEquals("precision strict",0.982652,es_allTypes.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.774683,es_allTypes.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.866362,es_allTypes.getFMeasureStrict(1.0),EPS4);
    assertEquals("precision lenient", 0.985614,es_allTypes.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.7770180,es_allTypes.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient", 0.86897323,es_allTypes.getFMeasureLenient(1.0),EPS4);
    EvalStatsTagging es_Anatomy = corp1orig.getEvalStatsTagging("Anatomy");
    assertEquals("precision strict",0.98913043,es_Anatomy.getPrecisionStrict(),EPS4);
    assertEquals("recall lenient",0.869980,es_Anatomy.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.925737,es_Anatomy.getFMeasureStrict(1.0),EPS4);
    EvalStatsTagging es_Drug = corp1orig.getEvalStatsTagging("Drug");
    assertEquals("precision strict",0.9740740,es_Drug.getPrecisionStrict(),EPS4);
    assertEquals("recall lenient",0.6743589,es_Drug.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.796969,es_Drug.getFMeasureStrict(1.0),EPS4);
    
    // 
  }
  
  @Test
  public void testCorp2() throws ResourceInstantiationException, ExecutionException, PersistenceException, IOException {
    logger.debug("Running test testCorp2");
    File pipelineFile = new File(testingDir,"test-eval-corp2.xgapp");
    CorpusController controller = (CorpusController)PersistenceManager.loadObjectFromFile(pipelineFile);
    // access the PRs that are in the controller
    EvaluateTagging corp2normal = null;
    EvaluateTagging corp2score  = null;
    EvaluateTagging corp2list = null;
    
    for(ProcessingResource pr : controller.getPRs()) {
      if(pr.getName().equals("EvaluateTagging:corp2-normal")) {
        corp2normal = (EvaluateTagging)pr;
      }
      if(pr.getName().equals("EvaluateTagging:corp2-score")) {
        corp2score = (EvaluateTagging)pr;
      }
      if(pr.getName().equals("EvaluateTagging:corp2-list")) {
        corp2list = (EvaluateTagging)pr;
      }
    }
    assertNotNull(corp2normal);
    assertNotNull(corp2score);
    assertNotNull(corp2list);
    controller.execute();
    // now check if the expected evaluation statistics are there
    EvalStatsTagging es_normal = corp2normal.getEvalStatsTagging("");
    assertEquals("precision strict",0.46739130434783,es_normal.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.18587896253602,es_normal.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.2659793814433,es_normal.getFMeasureStrict(1.0),EPS4);
    assertEquals("accuracy strict",0.18587896253602,es_normal.getSingleCorrectAccuracyStrict(),EPS4);
    assertEquals("precision lenient",0.51449275362319,es_normal.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.20461095100865,es_normal.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient",0.29278350515464,es_normal.getFMeasureLenient(1.0),EPS4);
    assertEquals("accuracy lenient",0.20461095100865,es_normal.getSingleCorrectAccuracyLenient(),EPS4);
    
    // 

    
  }  
  
}
