package gate.plugin.evaluation.tests;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import static org.junit.Assert.*;
import org.junit.Test;

import gate.plugin.evaluation.api.AnnotationDifferOld;
import gate.plugin.evaluation.api.*;
import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;

/**
 * First simple test for the annotation differ. 
 * For now this is used to develop or own strategy for finding and counting
 * differences, based on the behavior of the gate.util.AnnotationDiffer
 * 
 * @author Petrak
 */
public class TestDiffer1 {

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
  public void testFindPairings01() {
    System.out.println("Running test testFindPairings01");
    // Get the differences, using the old method
    AnnotationDifferOld annDiffer = new AnnotationDifferOld();
    Set<String> features = new HashSet<String>();
    features.add("id");
    annDiffer.setSignificantFeaturesSet(features);
    AnnotationSet doc1KeyAnns = doc1.getAnnotations("Key");
    System.out.println("testFindPairings01 doc1 anns from Key: "+doc1KeyAnns);
    AnnotationSet doc1Resp1Anns = doc1.getAnnotations("Resp1");    
    System.out.println("testFindPairings01 doc1 anns from Resp1: "+doc1Resp1Anns);
    List<AnnotationDifferOld.Pairing> pairings = annDiffer.calculateDiff(doc1KeyAnns, doc1Resp1Anns);
    System.out.println("testFindPairings01 doc1 keys: "+annDiffer.getKeysCount());
    System.out.println("testFindPairings01 doc1 resp: "+annDiffer.getResponsesCount());
    System.out.println("testFindPairings01 doc1 features: "+annDiffer.getSignificantFeaturesSet());
    System.out.println("testFindPairings01 doc1 correct: "+annDiffer.getCorrectMatches());
    System.out.println("testFindPairings01 doc1 partially correct: "+annDiffer.getPartiallyCorrectMatches());
    System.out.println("testFindPairings01 doc1 spurious: "+annDiffer.getSpurious());
    System.out.println("testFindPairings01 doc1 missing: "+annDiffer.getMissing());
    System.out.println("testFindPairings01 doc1 true missing: "+annDiffer.getTrueMissing());
    System.out.println("testFindPairings01 doc1 true spurious: "+annDiffer.getTrueSpurious());
    System.out.println("testFindPairings01 doc1 incorrect strict: "+annDiffer.getIncorrectStrict());
    System.out.println("testFindPairings01 doc1 incorrect partial: "+annDiffer.getIncorrectPartial());
    System.out.println("testFindPairings01 doc1 incorrect lenient: "+annDiffer.getIncorrectLenient());
    System.out.println("testFindPairings01 doc1 FP strict: "+annDiffer.getFalsePositivesStrict());
    System.out.println("testFindPairings01 doc1 FP lenient: "+annDiffer.getFalsePositivesLenient());
    System.out.println("testFindPairings01 doc1 precision strict: "+annDiffer.getPrecisionStrict());
    System.out.println("testFindPairings01 doc1 recall strict: "+annDiffer.getRecallStrict());
    System.out.println("testFindPairings01 doc1 fmeas strict: "+annDiffer.getFMeasureStrict(1.0));
    System.out.println("testFindPairings01 doc1 precision lenient: "+annDiffer.getPrecisionLenient());
    System.out.println("testFindPairings01 doc1 recall lenient: "+annDiffer.getRecallLenient());
    System.out.println("testFindPairings01 doc1 fmeas lenient: "+annDiffer.getFMeasureLenient(1.0));
    System.out.println("testFindPairings01 doc1 correct anns: "+annDiffer.correctAnnotations);
    System.out.println("testFindPairings01 doc1 partial anns: "+annDiffer.partiallyCorrectAnnotations);
    System.out.println("testFindPairings01 doc1 missing anns: "+annDiffer.missingAnnotations);
    System.out.println("testFindPairings01 doc1 spurious anns: "+annDiffer.spuriousAnnotations);
    System.out.println("testFindPairings01 doc1 incorrect strict anns: "+annDiffer.incorrectStrictAnnotations);
    
  }
  
  
  ///////////////////////////
  /// MAIN
  //////////////////////////
  
  
  // so we can run this test from the command line 
  //public static void main(String args[]) {
  //  org.junit.runner.JUnitCore.main(TestDiffer1.class.getCanonicalName());
  //}  
  
  
}
