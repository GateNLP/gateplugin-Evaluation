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
package gate.plugin.evaluation.tests;

import gate.CorpusController;
import gate.Document;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import org.junit.Test;
import gate.test.GATEPluginTests;

import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import gate.creole.ExecutionException;
import gate.persist.PersistenceException;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.resources.EvaluateTagging;
import gate.plugin.evaluation.resources.EvaluateTagging4Lists;
import static gate.plugin.evaluation.tests.TestUtils.*;
import gate.util.GateRuntimeException;
import gate.util.persistence.PersistenceManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;

/**
 * Third set of tests, mainly testing the PR within pipelines.
 * 
 * @author Johann Petrak
 */
public class TestTagging3 extends GATEPluginTests {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private File pluginHome;
  private File testingDir;
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
      Gate.init();
    }
    pluginHome = new File(".");
    pluginHome = pluginHome.getCanonicalFile();
    testingDir = new File(pluginHome,"test");
    
  }
  
  /////////////////////////
  /// Tests
  /////////////////////////
  
  @Test
  public void testTagging3Corp1() throws ResourceInstantiationException, ExecutionException, PersistenceException, IOException {
    logger.debug("Running test testCorp1");
    File pipelineFile = new File(testingDir,"test-eval-corp1.xgapp");
    CorpusController controller = (CorpusController)PersistenceManager.loadObjectFromFile(pipelineFile);
    // access the PRs that are in the controller
    EvaluateTagging corp1orig = null;
    EvaluateTagging corp1renamed = null;
    
    for(ProcessingResource pr : controller.getPRs()) {
      if(pr.getName().equals("EvaluateTagging:corp1:orig")) {
        corp1orig = (EvaluateTagging)pr;
      }
      if(pr.getName().equals("EvaluateTagging:corp1:renamed")) {
        corp1renamed = (EvaluateTagging)pr;
      }
    }
    assertNotNull(corp1orig);
    assertNotNull(corp1renamed);
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
    
    // now check if the second PR, where we carry out the evaluation on the set with the 
    // renamed types also gets the same values.
    es_allTypes = corp1renamed.getEvalStatsTagging("");
    assertEquals("precision strict",0.982652,es_allTypes.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.774683,es_allTypes.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.866362,es_allTypes.getFMeasureStrict(1.0),EPS4);
    assertEquals("precision lenient", 0.985614,es_allTypes.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.7770180,es_allTypes.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient", 0.86897323,es_allTypes.getFMeasureLenient(1.0),EPS4);
    es_Anatomy = corp1renamed.getEvalStatsTagging("Anatomy");
    assertEquals("precision strict",0.98913043,es_Anatomy.getPrecisionStrict(),EPS4);
    assertEquals("recall lenient",0.869980,es_Anatomy.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.925737,es_Anatomy.getFMeasureStrict(1.0),EPS4);
    es_Drug = corp1renamed.getEvalStatsTagging("Drug");
    assertEquals("precision strict",0.9740740,es_Drug.getPrecisionStrict(),EPS4);
    assertEquals("recall lenient",0.6743589,es_Drug.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.796969,es_Drug.getFMeasureStrict(1.0),EPS4);
  }
  
  @Test
  public void testTagging3Corp2() throws ResourceInstantiationException, ExecutionException, PersistenceException, IOException {
    logger.debug("Running test testCorp2");
    File pipelineFile = new File(testingDir,"test-eval-corp2.xgapp");
    CorpusController controller = (CorpusController)PersistenceManager.loadObjectFromFile(pipelineFile);
    // access the PRs that are in the controller
    EvaluateTagging corp2normal = null;
    EvaluateTagging corp2score  = null;
    EvaluateTagging4Lists corp2list = null;
    
    for(ProcessingResource pr : controller.getPRs()) {
      if(pr.getName().equals("EvaluateTagging:corp2-normal")) {
        corp2normal = (EvaluateTagging)pr;
      }
      if(pr.getName().equals("EvaluateTagging:corp2-score")) {
        corp2score = (EvaluateTagging)pr;
      }
      if(pr.getName().equals("EvaluateTagging4Lists:corp2-list")) {
        corp2list = (EvaluateTagging4Lists)pr;
      }
    }
    // make sure we clear all document features before running the evaluation!
    for(Document d : controller.getCorpus()) {
      d.getFeatures().clear();
    }
    assertNotNull(corp2normal);
    assertNotNull(corp2score);
    assertNotNull(corp2list);
    controller.execute();
    
    Document doc11 = null;
    for(Document d : controller.getCorpus()) {
      // need startsWith because GATE appends that random nonsense
      if(d.getName().startsWith("doc11.xml")) {
        doc11 = d;
      }
    }
    assertNotNull(doc11);
    
    
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
    
    EvalStatsTagging es_score = corp2score.getEvalStatsTagging("");
    assertEquals("precision strict",0.46739130434783,es_score.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.18587896253602,es_score.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.2659793814433,es_score.getFMeasureStrict(1.0),EPS4);
    assertEquals("accuracy strict",0.18587896253602,es_score.getSingleCorrectAccuracyStrict(),EPS4);
    assertEquals("precision lenient",0.51449275362319,es_score.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.20461095100865,es_score.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient",0.29278350515464,es_score.getFMeasureLenient(1.0),EPS4);
    assertEquals("accuracy lenient",0.20461095100865,es_score.getSingleCorrectAccuracyLenient(),EPS4);
    
    es_score = corp2score.getByThEvalStatsTagging("").get(0.94);
    assertNotNull(es_score);
    assertEquals("precision strict",0.48387096774194,es_score.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.06484149855908,es_score.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.1143583227446,es_score.getFMeasureStrict(1.0),EPS4);
    // assertEquals("accuracy strict",0,es_score.getSingleCorrectAccuracyStrict(),EPS4);  // BUG???
    assertEquals("precision lenient",0.53763440860215,es_score.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.07204610951009,es_score.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient",0.12706480304956,es_score.getFMeasureLenient(1.0),EPS4);
    // assertEquals("accuracy lenient",0,es_score.getSingleCorrectAccuracyLenient(),EPS4); // BUG?

    // also make sure that the document features are set correctly
    // get some arbitrary document
    FeatureMap fmd1 = doc11.getFeatures();
    assertEquals(0.33333333333333,(Double)fmd1.get("evaluateTagging.response.corp2-normal.Shef.Mention.PrecisionStrict"),EPS4);
    assertEquals(0.17647058823529,(Double)fmd1.get("evaluateTagging.response.corp2-normal.Shef.Mention.RecallStrict"),EPS4);
    assertEquals(0.23076923076923,(Double)fmd1.get("evaluateTagging.response.corp2-normal.Shef.Mention.FMeasureStrict"),EPS4);

    assertEquals(0.75,(Double)fmd1.get("evaluateTagging.reference.corp2-normal.Ref.Mention.PrecisionStrict"),EPS4);
    assertEquals(0.35294117647059,(Double)fmd1.get("evaluateTagging.reference.corp2-normal.Ref.Mention.RecallStrict"),EPS4);
    assertEquals(0.48,(Double)fmd1.get("evaluateTagging.reference.corp2-normal.Ref.Mention.FMeasureStrict"),EPS4);

    // BEGINNING OF CHECKING THE ACTUAL LIST-BASED EVALUATION VALUES
    
    EvalStatsTagging es_list_default = corp2list.getEvalStatsTagging();
    assertEquals("precision strict",0.39404352806415,es_list_default.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.49567723342939,es_list_default.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.43905552010211,es_list_default.getFMeasureStrict(1.0),EPS4);
    assertEquals("accuracy strict",0.49567723342939,es_list_default.getSingleCorrectAccuracyStrict(),EPS4);
    assertEquals("precision lenient",0.42497136311569,es_list_default.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.53458213256484,es_list_default.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient",0.47351627313338,es_list_default.getFMeasureLenient(1.0),EPS4);
    assertEquals("accuracy lenient",0.53458213256484,es_list_default.getSingleCorrectAccuracyLenient(),EPS4);
    
    es_list_default = corp2list.getByThEvalStatsTagging().get(0.81);
    assertNotNull(es_score);
    assertEquals("precision strict",0.63434343434343,es_list_default.getPrecisionStrict(),EPS4);
    assertEquals("recall strict",0.45244956772334,es_list_default.getRecallStrict(),EPS4);
    assertEquals("f1.0 strict",0.52817493692178,es_list_default.getFMeasureStrict(1.0),EPS4);
    // assertEquals("accuracy strict",0,es_score.getSingleCorrectAccuracyStrict(),EPS4);  // BUG???
    assertEquals("precision lenient",0.67474747474747,es_list_default.getPrecisionLenient(),EPS4);
    assertEquals("recall lenient",0.48126801152738,es_list_default.getRecallLenient(),EPS4);
    assertEquals("f1.0 lenient",0.56181665264929,es_list_default.getFMeasureLenient(1.0),EPS4);
    // assertEquals("accuracy lenient",0,es_score.getSingleCorrectAccuracyLenient(),EPS4); // BUG?
  }  
  
}
