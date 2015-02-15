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
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.resources.EvaluateTagging;
import static gate.plugin.evaluation.tests.TestUtils.*;
import java.io.OutputStreamWriter;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;

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
    //logger.setLevel(Level.INFO);
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
    logger.debug("Running test testListEval01");
    Document doc = newD();
    AnnotationSet keys = doc.getAnnotations("Key");
    AnnotationSet resp = doc.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.8)));
    addListAnn(resp,0,10,"M",ids);
    runETPR(prListEval1,doc);
    EvalStatsTagging es = prListEval1.getEvalStatsTagging("");
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
    
    es = prListEval1.getByThEvalStatsTagging("").get(0.9);
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
    
    es = prListEval1.getByThEvalStatsTagging("").get(0.8);
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
  public void testListEval02() throws ResourceInstantiationException, ExecutionException {
    logger.debug("Running test testListEval02");
    Document doc = newD();
    AnnotationSet keys = doc.getAnnotations("Key");
    AnnotationSet resp = doc.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is partially correct, the third correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 2, "M", featureMap("id","x","s",0.8)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.7)));
    addListAnn(resp,0,1,"M",ids);
    runETPR(prListEval1,doc);
    EvalStatsTagging es = prListEval1.getEvalStatsTagging("");
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

    es = prListEval1.getByThEvalStatsTagging("").get(0.9);
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
    
    es = prListEval1.getByThEvalStatsTagging("").get(0.8);
    logger.debug("DEBUG: test02 for 0.8: \n"+es.toString());
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
    
    es = prListEval1.getByThEvalStatsTagging("").get(0.7);
    logger.debug("DEBUG: test02 for 0.7: \n"+es.toString());
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
  public void testListEval03() throws ResourceInstantiationException, ExecutionException {
    logger.debug("Running test testListEval03");
    Document doc1 = newD();
    AnnotationSet keys = doc1.getAnnotations("Key");
    AnnotationSet resp = doc1.getAnnotations("Resp");
    
    // Simple list: the first (highest) response is wrong, the second is partially correct, the third correct
    addAnn(keys,0,1,"M",featureMap("id","x"));
    List<Integer> ids = newIntList();
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","y","s",0.9)));
    ids.add(addAnn(resp, 0, 2, "M", featureMap("id","x","s",0.8)));
    ids.add(addAnn(resp, 0, 1, "M", featureMap("id","x","s",0.7)));
    addListAnn(resp,0,1,"M",ids);
    
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
    addListAnn(resp,0,1,"M",ids);
    runETPR(prListEval1,doc1,doc2);
    
    EvalStatsTagging es = prListEval1.getEvalStatsTagging("");
    logger.debug("All documents, first response: \n"+es.toString());
    es = prListEval1.getByThEvalStatsTagging("").get(0.9);
    logger.debug("All documents, th=0.9: \n"+es.toString());
    es = prListEval1.getByThEvalStatsTagging("").get(0.8);
    logger.debug("All documents, th=0.8: \n"+es.toString());
    es = prListEval1.getByThEvalStatsTagging("").get(0.7);
    logger.debug("All documents, th=0.7: \n"+es.toString());
    es = prListEval1.getByThEvalStatsTagging("").get(0.6);
    logger.debug("All documents, th=0.6: \n"+es.toString());
    
    /*
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

    es = prListEval1.getByThEvalStatsTagging("").get(0.9);
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
    
    es = prListEval1.getByThEvalStatsTagging("").get(0.8);
    logger.debug("DEBUG: test02 for 0.8: \n"+es.toString());
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
    
    es = prListEval1.getByThEvalStatsTagging("").get(0.7);
    logger.debug("DEBUG: test02 for 0.7: \n"+es.toString());
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
    */
  
  }
  
  
  
}
