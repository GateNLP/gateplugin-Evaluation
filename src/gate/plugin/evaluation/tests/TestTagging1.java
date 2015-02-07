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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import static org.junit.Assert.*;
import static gate.Utils.*;
import gate.plugin.evaluation.api.ByThEvalStatsTagging;
import static gate.plugin.evaluation.tests.TestUtils.*;

/**
 * First simple test for the annotation differ. 
 * For now this is used to develop or own strategy for finding and counting
 differences, based on the behavior of the gate.util.AnnotationDifferTagging
 * 
 * @author Petrak
 */
public class TestTagging1 {

  ////////////////////////////
  // Initialization
  ////////////////////////////
  private Document doc1;
  private Document doc2;
  private File pluginHome;
  private File testingDir;
  @Before
  public void setup() throws GateException, IOException {
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
    System.out.println("Running test testTagging01");
    // Get the differences, using the old method
    //AnnotationDifferOld annDiffer = new AnnotationDifferOld();
    gate.util.AnnotationDiffer annDiffer = new gate.util.AnnotationDiffer();
    Set<String> features = new HashSet<String>();
    features.add("id");
    annDiffer.setSignificantFeaturesSet(features);
    AnnotationSet doc1KeyAnns = doc1.getAnnotations("Key");
    //System.out.println("testTagging01 doc1 anns from Key: "+doc1KeyAnns);
    AnnotationSet doc1Resp1Anns = doc1.getAnnotations("Resp1");    
    //System.out.println("testTagging01 doc1 anns from Resp1: "+doc1Resp1Anns);
    List<gate.util.AnnotationDiffer.Pairing> pairings = annDiffer.calculateDiff(doc1KeyAnns, doc1Resp1Anns);
    
    List<String> featureList = new ArrayList<String>();
    featureList.add("id");
    AnnotationDifferTagging newDiffer = new AnnotationDifferTagging(doc1KeyAnns,doc1Resp1Anns,featureList,null,null);
    EvalStatsTagging newDifferES = newDiffer.getEvalStatsTagging();
    
    System.out.println("OLD testFindPairings01 doc1 keys: "+annDiffer.getKeysCount());
    System.out.println("NEW testFindPairings01 doc1 keys: "+newDifferES.getTargets());
    System.out.println("OLD testFindPairings01 doc1 resp: "+annDiffer.getResponsesCount());
    System.out.println("NEW testFindPairings01 doc1 resp: "+newDifferES.getResponses());
    System.out.println("OLD testFindPairings01 doc1 correct: "+annDiffer.getCorrectMatches());
    System.out.println("NEW testFindPairings01 doc1 correct: "+newDifferES.getCorrectStrict());
    System.out.println("OLD testFindPairings01 doc1 correct anns: "+annDiffer.correctAnnotations.size());
    System.out.println("OLD testFindPairings01 doc1 partially correct: "+annDiffer.getPartiallyCorrectMatches());
    System.out.println("NEW testFindPairings01 doc1 partially correct: "+newDifferES.getCorrectPartial());
    System.out.println("OLD testFindPairings01 doc1 partial anns: "+annDiffer.partiallyCorrectAnnotations.size());
    System.out.println("OLD testFindPairings01 doc1 spurious: "+annDiffer.getSpurious());
    System.out.println("NEW testFindPairings01 doc1 spurious strict: "+newDifferES.getSpuriousStrict());
    System.out.println("NEW testFindPairings01 doc1 spurious lenient: "+newDifferES.getSpuriousLenient());
    System.out.println("NEW testFindPairings01 doc1 true spurious strict: "+newDifferES.getTrueSpuriousStrict());
    System.out.println("NEW testFindPairings01 doc1 true spurious lenient: "+newDifferES.getTrueSpuriousLenient());
    System.out.println("OLD testFindPairings01 doc1 missing: "+annDiffer.getMissing());
    System.out.println("NEW testFindPairings01 doc1 missing strict: "+newDifferES.getMissingStrict());
    System.out.println("NEW testFindPairings01 doc1 missing lenient: "+newDifferES.getMissingLenient());
    System.out.println("NEW testFindPairings01 doc1 true missing strict: "+newDifferES.getTrueMissingStrict());
    System.out.println("NEW testFindPairings01 doc1 true missing lenient: "+newDifferES.getTrueMissingLenient());
    System.out.println("NEW testFindPairings01 doc1 incorrect strict: "+newDifferES.getIncorrectStrict());
    System.out.println("NEW testFindPairings01 doc1 incorrect lenient: "+newDifferES.getIncorrectLenient());
    System.out.println("OLD testFindPairings01 doc1 precision strict: "+annDiffer.getPrecisionStrict());
    System.out.println("NEW testFindPairings01 doc1 precision strict: "+newDifferES.getPrecisionStrict());
    System.out.println("OLD testFindPairings01 doc1 recall strict: "+annDiffer.getRecallStrict());
    System.out.println("NEW testFindPairings01 doc1 recall strict: "+newDifferES.getRecallStrict());
    System.out.println("OLD testFindPairings01 doc1 fmeas strict: "+annDiffer.getFMeasureStrict(1.0));
    System.out.println("NEW testFindPairings01 doc1 fmeas strict: "+newDifferES.getFMeasureStrict(1.0));
    System.out.println("OLD testFindPairings01 doc1 precision lenient: "+annDiffer.getPrecisionLenient());
    System.out.println("NEW testFindPairings01 doc1 precision lenient: "+newDifferES.getPrecisionLenient());
    System.out.println("OLD testFindPairings01 doc1 recall lenient: "+annDiffer.getRecallLenient());
    System.out.println("NEW testFindPairings01 doc1 recall lenient: "+newDifferES.getRecallLenient());
    System.out.println("OLD testFindPairings01 doc1 fmeas lenient: "+annDiffer.getFMeasureLenient(1.0));
    System.out.println("NEW testFindPairings01 doc1 fmeas lenient: "+newDifferES.getFMeasureLenient(1.0));
    System.out.println("OLD testFindPairings01 doc1 missing anns: "+annDiffer.missingAnnotations.size());
    System.out.println("OLD testFindPairings01 doc1 spurious anns: "+annDiffer.spuriousAnnotations.size());
    System.out.println("NEW testFindPairings01 doc1 true missing lenient anns: "+newDiffer.getTrueMissingLenientAnnotations().size());
    System.out.println("NEW testFindPairings01 doc1 true spurious lenient anns: "+newDiffer.getTrueSpuriousLenientAnnotations().size());
    
    System.out.println("NEW Stats object");
    System.out.println(newDiffer.getEvalStatsTagging());
    
    
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
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FL_ID);
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
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FL_ID);
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
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FL_ID);
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
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FL_ID);
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
    ByThEvalStatsTagging bth = new ByThEvalStatsTagging(ByThEvalStatsTagging.WhichThresholds.USE_ALL);
    AnnotationDifferTagging ad = new AnnotationDifferTagging(t, r, FL_ID,"s",bth);
    EvalStatsTagging es = ad.getEvalStatsTagging();
    
    // now actually perform the tests on the values ....
    assertEquals("F1.0 strict, th=NaN",1.0,es.getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 strict, th=0.1",1.0,bth.get(0.1).getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 strict, th=0.2",0.75,bth.get(0.2).getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 strict, th=0.3",0.50,bth.get(0.3).getFMeasureStrict(1.0),EPS);
    assertEquals("F1.0 strict, th=0.4",0.25,bth.get(0.4).getFMeasureStrict(1.0),EPS);
  }
  


///////////////////////////
  /// HELPER METHODS
  ///////////////////////////
  
  
  ///////////////////////////
  /// MAIN
  //////////////////////////
  
  
  // so we can run this test from the command line 
  //public static void main(String args[]) {
  //  org.junit.runner.JUnitCore.main(TestTagging1.class.getCanonicalName());
  //}  
  
  
}
