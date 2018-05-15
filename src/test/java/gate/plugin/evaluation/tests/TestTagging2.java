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

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import org.junit.Test;
import gate.test.GATEPluginTests;

import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import static gate.Utils.*;
import gate.creole.ExecutionException;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.resources.EvaluateTagging;
import gate.plugin.evaluation.resources.EvaluateTagging4Lists;
import static gate.plugin.evaluation.tests.TestUtils.*;
import java.io.OutputStreamWriter;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import static org.junit.Assert.*;

/**
 * Second test, mainly testing the PR, not the back-end API.
 * 
 * @author Johann Petrak
 */
public class TestTagging2 extends GATEPluginTests {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private File pluginHome;
  private File testingDir;
  private EvaluateTagging4Lists prListEval1;
  private static final Logger logger = Logger.getLogger(TestTagging2.class);
  @Before
  public void setup() throws GateException, IOException {
    /*
    logger.setLevel(Level.DEBUG);
    ConsoleAppender appender = new ConsoleAppender();
    appender.setWriter(new OutputStreamWriter(System.out));
    appender.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
    logger.addAppender(appender);
    */
    logger.setLevel(Level.DEBUG);
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.DEBUG);
    ConsoleAppender appender = new ConsoleAppender();
    appender.setWriter(new OutputStreamWriter(System.out));
    appender.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
    rootLogger.addAppender(appender);
    
    if(!Gate.isInitialised()) {
      Gate.init();
    }
    // load the plugin
    pluginHome = new File(".");
    pluginHome = pluginHome.getCanonicalFile();
    testingDir = new File(pluginHome,"test");
    // create a number of pre-configured PR instances
    FeatureMap parms = Factory.newFeatureMap();
    parms.put("listType", "L");
    parms.put("elementType", "M");
    parms.put("keyType", "M");
    parms.put("evaluationId","EvaluataTagging1");
    parms.put("featureNames",newStringList("id")); // our only feature for matching is "id"
    parms.put("keyASName", "Key");
    parms.put("outputASPrefix", "Evaluate");
    parms.put("outputDirectoryUrl",testingDir.toURI().toURL());
    parms.put("responseASName","Resp");
    parms.put("scoreFeatureName", "s");  // score feature is "s"
    parms.put("edgeFeatureName","ids");
    prListEval1 = (EvaluateTagging4Lists)Factory.createResource(
            "gate.plugin.evaluation.resources.EvaluateTagging4Lists", 
            parms, 
            Factory.newFeatureMap(), 
            "EvaluateTagging1");
    
  }
  
  /////////////////////////
  /// Tests
  /////////////////////////
  
  @Test
  public void testTagging2ListEval01() throws ResourceInstantiationException, ExecutionException {
    logger.debug("Running test testTagging2ListEval01");
    Document doc = newD();
    AnnotationSet keys = doc.getAnnotations("Key");
    AnnotationSet resp = doc.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.8)));
    addListAnn(resp,0,10,"L",ids);
    runETPR(prListEval1,doc);
    EvalStatsTagging es = prListEval1.getEvalStatsTagging();
    assertNotNull(es);
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",1,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",0.0,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.9);
    assertNotNull(es);
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",1,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",0.0,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.8);
    assertNotNull(es);
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",1,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",1.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",1.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);    
    
  }

  @Test
  public void testTagging2ListEval02() throws ResourceInstantiationException, ExecutionException {
    logger.debug("Running test testTagging2ListEval02");
    Document doc = newD();
    AnnotationSet keys = doc.getAnnotations("Key");
    AnnotationSet resp = doc.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is partially correct, the third correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 2, "M", featureMap("id","x","s",0.8)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.7)));
    addListAnn(resp,0,1,"L",ids);
    runETPR(prListEval1,doc);
    EvalStatsTagging es = prListEval1.getEvalStatsTagging();
    assertNotNull(es);
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",1,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",0.0,es.getRecallLenient(),EPS);    

    es = prListEval1.getByThEvalStatsTagging().get(0.9);
    assertNotNull(es);
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",1,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",0.0,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.8);
    assertNotNull(es);
    //logger.debug("DEBUG: test02 for 0.8: \n"+es.toString());
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",1,es.getCorrectPartial());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",1,es.getTrueMissingStrict());
    assertEquals("true spurious strict",1,es.getTrueSpuriousStrict());
    assertEquals("true missing lenient",0,es.getTrueMissingLenient());
    assertEquals("true spurious lenient",0,es.getTrueSpuriousLenient());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.7);
    assertNotNull(es);
    //logger.debug("DEBUG: test02 for 0.7: \n"+es.toString());
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",1,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",1.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",1.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);    
    
  
  }

  @Test
  public void testTagging2ListEval03() throws ResourceInstantiationException, ExecutionException {
    
    logger.debug("Running test testTagging2ListEval03");
    Document doc1 = newD();
    AnnotationSet keys = doc1.getAnnotations("Key");
    AnnotationSet resp = doc1.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is partially correct, the third correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 2, "M", featureMap("id","x","s",0.8)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.7)));
    addListAnn(resp,0,1,"L",ids);
    
    // Another document, with another list: this time the responses at 0.9 and 0.8 are both partial and the
    // there is a response at 0.6 which is correct. Of the two partial responses, only the one 0.9
    // should be taken, because the one at 0.8 does not improve that!
    Document doc2 = newD();
    keys = doc2.getAnnotations("Key");
    resp = doc2.getAnnotations("Resp");    
    addAnn(keys,0,1,"M",featureMap("id","x"));
    ids = newIntList();
    ids.add(addAnn(resp, 0, 2, "M", featureMap("id","x","s",0.9)));
    ids.add(addAnn(resp, 0, 2, "M", featureMap("id","x","s",0.8)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.6)));
    addListAnn(resp,0,1,"L",ids);
    runETPR(prListEval1,doc1,doc2);
    
    EvalStatsTagging es = prListEval1.getEvalStatsTagging();
    //logger.debug("All documents, first response: \n"+es);
    
    //logger.debug("All documents, all ths: \n"+prListEval1.getByThEvalStatsTagging()+"\n");
    

    es = prListEval1.getByThEvalStatsTagging().get(0.9);
    assertNotNull(es);
    assertEquals("targets",2,es.getTargets());
    assertEquals("responses",2,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",1,es.getCorrectPartial());
    assertEquals("incorrect strict",1,es.getIncorrectStrict());
    assertEquals("true missing strict",1,es.getTrueMissingStrict());
    assertEquals("true spurious strict",1,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.5,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",0.5,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.8);
    assertNotNull(es);
    logger.debug("DEBUG: test02 for 0.8: \n"+es.toString());
    assertEquals("targets",2,es.getTargets());
    assertEquals("responses",2,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("correct partial",2,es.getCorrectPartial());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",2,es.getTrueMissingStrict());
    assertEquals("true spurious strict",2,es.getTrueSpuriousStrict());
    assertEquals("true missing lenient",0,es.getTrueMissingLenient());
    assertEquals("true spurious lenient",0,es.getTrueSpuriousLenient());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.7);
    assertNotNull(es);
    logger.debug("DEBUG: test02 for 0.7: \n"+es.toString());
    assertEquals("targets",2,es.getTargets());
    assertEquals("responses",2,es.getResponses());
    assertEquals("correct strict",1,es.getCorrectStrict());
    assertEquals("correct partial",1,es.getCorrectPartial());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",1,es.getTrueMissingStrict());
    assertEquals("true spurious strict",1,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.5,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.5,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);    
    
    es = prListEval1.getByThEvalStatsTagging().get(0.6);
    assertNotNull(es);
    assertEquals("targets",2,es.getTargets());
    assertEquals("responses",2,es.getResponses());
    assertEquals("correct strict",2,es.getCorrectStrict());
    assertEquals("correct partial",0,es.getCorrectPartial());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",1.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",1.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);    

  }
  
  
  
}
