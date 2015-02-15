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

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.plugin.evaluation.api.AnnotationDifferTagging;
import gate.plugin.evaluation.api.EvalStatsTagging;
import org.junit.Test;

import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import static org.junit.Assert.*;
import static gate.Utils.*;
import gate.plugin.evaluation.api.ByThEvalStatsTagging;
import gate.plugin.evaluation.api.ThresholdsToUse;
import static gate.plugin.evaluation.tests.TestUtils.*;
import java.io.OutputStreamWriter;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Tests, mainly for the back-end API.
 * 
 * @author Johann Petrak
 */
public class TestTagging1 {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private Document doc1;
  private Document doc2;
  private File pluginHome;
  private File testingDir;
  private static final Logger logger = Logger.getLogger(TestTagging1.class);
  @Before
  public void setup() throws GateException, IOException {
    /*
    logger.setLevel(Level.DEBUG);
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.DEBUG);
    //logger.setLevel(Level.INFO);
    ConsoleAppender appender = new ConsoleAppender();
    appender.setWriter(new OutputStreamWriter(System.out));
    appender.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
    //logger.addAppender(appender);
    rootLogger.addAppender(appender);
    */
    if(!Gate.isInitialised()) {
      Gate.runInSandbox(true);
      Gate.init();
    }
    pluginHome = new File(".");
    pluginHome = pluginHome.getCanonicalFile();
    Gate.getCreoleRegister().registerDirectories(
              pluginHome.toURI().toURL());
    testingDir = new File(pluginHome,"test");
    URL doc1Url = new File(testingDir,"doc1.xml").toURI().toURL();
    URL doc2Url = new File(testingDir,"doc2.xml").toURI().toURL();
    FeatureMap parms = Factory.newFeatureMap();
    parms.put("sourceUrl", doc1Url);
    doc1 = (Document)Factory.createResource("gate.corpora.DocumentImpl",parms);
    doc2 = (Document)Factory.createResource("gate.corpora.DocumentImpl",parms);
  }
  
  /////////////////////////
  /// Tests
  /////////////////////////
  
  @Test
  public void testTagging01() {
    logger.debug("Running test testTagging01");
    // Get the differences, using the old method
    //AnnotationDifferOld annDiffer = new AnnotationDifferOld();
    gate.util.AnnotationDiffer annDiffer = new gate.util.AnnotationDiffer();
    Set<String> features = new HashSet<String>();
    features.add("id");
    annDiffer.setSignificantFeaturesSet(features);
    AnnotationSet doc1KeyAnns = doc1.getAnnotations("Key");
    //logger.debug("testTagging01 doc1 anns from Key: "+doc1KeyAnns);
    AnnotationSet doc1Resp1Anns = doc1.getAnnotations("Resp1");    
    //logger.debug("testTagging01 doc1 anns from Resp1: "+doc1Resp1Anns);
    List<gate.util.AnnotationDiffer.Pairing> pairings = annDiffer.calculateDiff(doc1KeyAnns, doc1Resp1Anns);
    
    Set<String> featureSet = new HashSet<String>();
    featureSet.add("id");
    AnnotationDifferTagging newDiffer = new AnnotationDifferTagging(doc1KeyAnns,doc1Resp1Anns,featureSet,FC_EQU);
    EvalStatsTagging newDifferES = newDiffer.getEvalStatsTagging();
    
    logger.debug("OLD testFindPairings01 doc1 keys: "+annDiffer.getKeysCount());
    logger.debug("NEW testFindPairings01 doc1 keys: "+newDifferES.getTargets());
    logger.debug("OLD testFindPairings01 doc1 resp: "+annDiffer.getResponsesCount());
    logger.debug("NEW testFindPairings01 doc1 resp: "+newDifferES.getResponses());
    logger.debug("OLD testFindPairings01 doc1 correct: "+annDiffer.getCorrectMatches());
    logger.debug("NEW testFindPairings01 doc1 correct: "+newDifferES.getCorrectStrict());
    logger.debug("OLD testFindPairings01 doc1 correct anns: "+annDiffer.correctAnnotations.size());
    logger.debug("OLD testFindPairings01 doc1 partially correct: "+annDiffer.getPartiallyCorrectMatches());
    logger.debug("NEW testFindPairings01 doc1 partially correct: "+newDifferES.getCorrectPartial());
    logger.debug("OLD testFindPairings01 doc1 partial anns: "+annDiffer.partiallyCorrectAnnotations.size());
    logger.debug("OLD testFindPairings01 doc1 spurious: "+annDiffer.getSpurious());
    logger.debug("NEW testFindPairings01 doc1 spurious strict: "+newDifferES.getSpuriousStrict());
    logger.debug("NEW testFindPairings01 doc1 spurious lenient: "+newDifferES.getSpuriousLenient());
    logger.debug("NEW testFindPairings01 doc1 true spurious strict: "+newDifferES.getTrueSpuriousStrict());
    logger.debug("NEW testFindPairings01 doc1 true spurious lenient: "+newDifferES.getTrueSpuriousLenient());
    logger.debug("OLD testFindPairings01 doc1 missing: "+annDiffer.getMissing());
    logger.debug("NEW testFindPairings01 doc1 missing strict: "+newDifferES.getMissingStrict());
    logger.debug("NEW testFindPairings01 doc1 missing lenient: "+newDifferES.getMissingLenient());
    logger.debug("NEW testFindPairings01 doc1 true missing strict: "+newDifferES.getTrueMissingStrict());
    logger.debug("NEW testFindPairings01 doc1 true missing lenient: "+newDifferES.getTrueMissingLenient());
    logger.debug("NEW testFindPairings01 doc1 incorrect strict: "+newDifferES.getIncorrectStrict());
    logger.debug("NEW testFindPairings01 doc1 incorrect lenient: "+newDifferES.getIncorrectLenient());
    logger.debug("OLD testFindPairings01 doc1 precision strict: "+annDiffer.getPrecisionStrict());
    logger.debug("NEW testFindPairings01 doc1 precision strict: "+newDifferES.getPrecisionStrict());
    logger.debug("OLD testFindPairings01 doc1 recall strict: "+annDiffer.getRecallStrict());
    logger.debug("NEW testFindPairings01 doc1 recall strict: "+newDifferES.getRecallStrict());
    logger.debug("OLD testFindPairings01 doc1 fmeas strict: "+annDiffer.getFMeasureStrict(1.0));
    logger.debug("NEW testFindPairings01 doc1 fmeas strict: "+newDifferES.getFMeasureStrict(1.0));
    logger.debug("OLD testFindPairings01 doc1 precision lenient: "+annDiffer.getPrecisionLenient());
    logger.debug("NEW testFindPairings01 doc1 precision lenient: "+newDifferES.getPrecisionLenient());
    logger.debug("OLD testFindPairings01 doc1 recall lenient: "+annDiffer.getRecallLenient());
    logger.debug("NEW testFindPairings01 doc1 recall lenient: "+newDifferES.getRecallLenient());
    logger.debug("OLD testFindPairings01 doc1 fmeas lenient: "+annDiffer.getFMeasureLenient(1.0));
    logger.debug("NEW testFindPairings01 doc1 fmeas lenient: "+newDifferES.getFMeasureLenient(1.0));
    logger.debug("OLD testFindPairings01 doc1 missing anns: "+annDiffer.missingAnnotations.size());
    logger.debug("OLD testFindPairings01 doc1 spurious anns: "+annDiffer.spuriousAnnotations.size());
    logger.debug("NEW testFindPairings01 doc1 true missing lenient anns: "+newDiffer.getTrueMissingLenientAnnotations().size());
    logger.debug("NEW testFindPairings01 doc1 true spurious lenient anns: "+newDiffer.getTrueSpuriousLenientAnnotations().size());
    
    logger.debug("NEW Stats object");
    logger.debug(newDiffer.getEvalStatsTagging());
    
    
    assertEquals("precision strict",annDiffer.getPrecisionStrict(),newDifferES.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",annDiffer.getPrecisionLenient(),newDifferES.getPrecisionLenient(),EPS);
    assertEquals("recall strict",annDiffer.getRecallStrict(),newDifferES.getRecallStrict(),EPS);
    assertEquals("recall lenient",annDiffer.getRecallLenient(),newDifferES.getRecallLenient(),EPS);
    
  }
  
  @Test
  public void testTaggingD01() throws ResourceInstantiationException {
    Document doc = newD();
    AnnotationSet t = addA(doc,"Keys",0,10,"M","x");
    AnnotationSet r = addA(doc,"Resp",0,10,"M","x");
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",1,es.getCorrectStrict());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",1.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",1.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);
    assertEquals("F1.0 strict",1.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 lenient",1.0,es.getFMeasureLenient(1.0),EPS);
  }
  
  @Test
  public void testTaggingD02() throws ResourceInstantiationException {
    Document doc = newD();
    AnnotationSet t = addA(doc,"Keys",0,10,"M","x");
    AnnotationSet r = addA(doc,"Resp",0,10,"M","y");
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",1,es.getResponses());
    assertEquals("correct strict",0,es.getCorrectStrict());
    assertEquals("incorrect strict",1,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",0.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",0.0,es.getRecallLenient(),EPS);
    assertEquals("F1.0 strict",0.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 lenient",0.0,es.getFMeasureLenient(1.0),EPS);
  }
  
  @Test
  public void testTaggingD03() throws ResourceInstantiationException {
    Document doc = newD();
    AnnotationSet t = addA(doc,"Keys",0,10,"M","x");
    addA(doc,"Resp",0,10,"M","x");
    AnnotationSet r = addA(doc,"Resp",0,10,"M","y");
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    assertEquals("targets",1,es.getTargets());
    assertEquals("responses",2,es.getResponses());
    assertEquals("correct strict",1,es.getCorrectStrict());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",1,es.getTrueSpuriousStrict());
    assertEquals("precision strict",0.5,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",0.5,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",1.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);
    assertEquals("F1.0 strict",2/3.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 lenient",2/3.0,es.getFMeasureLenient(1.0),EPS);
    // also check if we create the correct annotations
    AnnotationSet os = doc.getAnnotations("O");
    ad.addIndicatorAnnotations(os);
    assertEquals("outset size",2,os.size());
    AnnotationSet tmpSet = os.get("M_CS");
    assertEquals("M_CS size",1,tmpSet.size());
    Annotation tmpAnn = getOnlyAnn(tmpSet);
    assertEquals("M_CS ann start",0,(long)start(tmpAnn));
  }

  @Test
  public void testTaggingD04() throws ResourceInstantiationException {
    Document doc = newD();
    addA(doc,"Keys",0,10,"M","x");
    AnnotationSet t = addA(doc,"Keys",0,10,"M","y");
    addA(doc,"Resp",0,10,"M","x");
    AnnotationSet r = addA(doc,"Resp",0,10,"M","y");
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    assertEquals("targets",2,es.getTargets());
    assertEquals("responses",2,es.getResponses());
    assertEquals("correct strict",2,es.getCorrectStrict());
    assertEquals("incorrect strict",0,es.getIncorrectStrict());
    assertEquals("true missing strict",0,es.getTrueMissingStrict());
    assertEquals("true spurious strict",0,es.getTrueSpuriousStrict());
    assertEquals("precision strict",1.0,es.getPrecisionStrict(),EPS);
    assertEquals("precision lenient",1.0,es.getPrecisionLenient(),EPS);
    assertEquals("recall strict",1.0,es.getRecallStrict(),EPS);
    assertEquals("recall lenient",1.0,es.getRecallLenient(),EPS);
    assertEquals("F1.0 strict",1.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 lenient",1.0,es.getFMeasureLenient(1.0),EPS);
    // also check if we create the correct annotations
    AnnotationSet os = doc.getAnnotations("O");
    ad.addIndicatorAnnotations(os);
    assertEquals("outset size",2,os.size());
    AnnotationSet tmpSet = os.get("M_CS");
    assertEquals("M_CS size",2,tmpSet.size());
    for(Annotation a : tmpSet) {
      assertEquals("M_CS ann start",0,(long)start(a));
      assertEquals("M_CS ann end",10,(long)end(a));
    }
  }

  // Test P/R curve, 01
  @Test
  public void testTaggingPR01() throws ResourceInstantiationException {
    Document doc = newD();
    // add 2 targets to the keys
    addA(doc,"Keys",0, 10,"M",featureMap("id","x"));
    AnnotationSet t = addA(doc,"Keys",60,70,"M",featureMap("id","x"));
    // add 2 correct responses with 4 different scores
    addA(doc,"Resp",0,10,"M",featureMap("id","x","s","0.1"));
    AnnotationSet r = addA(doc,"Resp",60,70,"M",featureMap("id","x","s","0.5"));
    logger.debug("DEBUG: Running test PR01");
    ByThEvalStatsTagging bth = 
            AnnotationDifferTagging.calculateByThEvalStatsTagging(t, r, FS_ID, FC_EQU,"s",ThresholdsToUse.USE_ALL,null);
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();

    // Disregarding the thresholds, all responses must be correct
    logger.debug("Stats for th=0.5:");
    logger.debug(bth.get(0.5));
    logger.debug("Stats for th=0.1:");
    logger.debug(bth.get(0.1));
    assertEquals("F1.0 strict, th=NaN",1.0,es.getFMeasureStrict(1.0),EPS);
    // At the higher threshold, only one of the two correct ones is still there, so precision
    // must be 1.0 and recall 0.5
    assertEquals("Prec strict, th=0.5",1.0,bth.get(0.5).getPrecisionStrict(),EPS);
    assertEquals("Rec strict, th=0.5",0.5,bth.get(0.5).getRecallStrict(),EPS);
    // At the lower threshold, both responses are correct, so we should get 1.0
    assertEquals("F1.0 strict, th=0.1",1.0,bth.get(0.1).getFMeasureStrict(1.0),EPS);
  }
  
  // Test P/R curve, 02
  @Test
  public void testTaggingPR02() throws ResourceInstantiationException {
    Document doc = newD();
    // add 4 targets to the keys
    addA(doc,"Keys",0, 10,"M",featureMap("id","x"));
    addA(doc,"Keys",20,30,"M",featureMap("id","x"));
    addA(doc,"Keys",40,50,"M",featureMap("id","x"));
    AnnotationSet t = addA(doc,"Keys",60,70,"M",featureMap("id","x"));
    // add 4 correct responses with 4 different scores
    addA(doc,"Resp",0,10,"M",featureMap("id","x","s","0.1"));
    addA(doc,"Resp",20,30,"M",featureMap("id","x","s","0.2"));
    addA(doc,"Resp",40,50,"M",featureMap("id","x","s","0.3"));
    AnnotationSet r = addA(doc,"Resp",60,70,"M",featureMap("id","x","s","0.4"));
    ByThEvalStatsTagging bth = 
            AnnotationDifferTagging.calculateByThEvalStatsTagging(t, r, FS_ID, FC_EQU,"s",ThresholdsToUse.USE_ALL,null);
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    
    // now actually perform the tests on the values ....
    logger.debug("PR02, th=0.1: "+bth.get(0.1).shortCounts());
    logger.debug("PR02, th=0.2: "+bth.get(0.2).shortCounts());
    logger.debug("PR02, th=0.3: "+bth.get(0.3).shortCounts());
    logger.debug("PR02, th=0.4: "+bth.get(0.4).shortCounts());
    assertEquals("F1.0 strict, th=NaN",1.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("Prec strict, th=0.1",1.0,bth.get(0.1).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.1",1.0,bth.get(0.1).getRecallStrict(),EPS);
    assertEquals("Prec strict, th=0.2",1.0,bth.get(0.2).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.2",0.75,bth.get(0.2).getRecallStrict(),EPS);
    assertEquals("Prec strict, th=0.3",1.0,bth.get(0.3).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.3",0.5,bth.get(0.3).getRecallStrict(),EPS);
    assertEquals("Prec strict, th=0.4",1.0,bth.get(0.4).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.4",0.25,bth.get(0.4).getRecallStrict(),EPS);
  }
  
  // Test P/R curve, 03
  @Test
  public void testTaggingPR03() throws ResourceInstantiationException {
    // same as 02, but with two different documents, and we check the 
    // combined statistics
    Document doc1 = newD();
    Document doc2 = newD();
    // add 4 targets to the keys
    addA(doc1,"Keys",0, 10,"M",featureMap("id","x"));
    addA(doc2,"Keys",20,30,"M",featureMap("id","x"));
    AnnotationSet t1 = addA(doc1,"Keys",40,50,"M",featureMap("id","x"));
    AnnotationSet t2 = addA(doc2,"Keys",60,70,"M",featureMap("id","x"));
    // add 4 correct responses with 4 different scores
    addA(doc1,"Resp",0,10,"M",featureMap("id","x","s","0.1"));
    addA(doc2,"Resp",20,30,"M",featureMap("id","x","s","0.2"));
    AnnotationSet r1 = addA(doc1,"Resp",40,50,"M",featureMap("id","x","s","0.3"));
    AnnotationSet r2 = addA(doc2,"Resp",60,70,"M",featureMap("id","x","s","0.4"));

    ByThEvalStatsTagging bth = 
            AnnotationDifferTagging.calculateByThEvalStatsTagging(t1, r1, FS_ID, FC_EQU,"s",ThresholdsToUse.USE_ALL,null);
    bth = AnnotationDifferTagging.calculateByThEvalStatsTagging(t2, r2, FS_ID, FC_EQU,"s",ThresholdsToUse.USE_ALL,bth);
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t1, r1, FS_ID, FC_EQU);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    ad = new AnnotationDifferTagging(t2, r2, FS_ID, FC_EQU);
    es.add(ad.getEvalStatsTagging());
    
    // now actually perform the tests on the values ....
    logger.debug("PR02, th=0.1: "+bth.get(0.1).shortCounts());
    logger.debug("PR02, th=0.2: "+bth.get(0.2).shortCounts());
    logger.debug("PR02, th=0.3: "+bth.get(0.3).shortCounts());
    logger.debug("PR02, th=0.4: "+bth.get(0.4).shortCounts());
    assertEquals("F1.0 strict, th=NaN",1.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("Prec strict, th=0.1",1.0,bth.get(0.1).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.1",1.0,bth.get(0.1).getRecallStrict(),EPS);
    assertEquals("Prec strict, th=0.2",1.0,bth.get(0.2).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.2",0.75,bth.get(0.2).getRecallStrict(),EPS);
    assertEquals("Prec strict, th=0.3",1.0,bth.get(0.3).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.3",0.5,bth.get(0.3).getRecallStrict(),EPS);
    assertEquals("Prec strict, th=0.4",1.0,bth.get(0.4).getPrecisionStrict(),EPS);
    assertEquals("Rec strict,  th=0.4",0.25,bth.get(0.4).getRecallStrict(),EPS);
  }
  
  
}
