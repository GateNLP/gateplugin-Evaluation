package gate.plugin.evaluation.tests;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.plugin.evaluation.api.AnnotationDiffer;
import org.junit.Test;

import gate.plugin.evaluation.api.AnnotationDifferOld;
import gate.util.GateException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
    //AnnotationDifferOld annDiffer = new AnnotationDifferOld();
    gate.util.AnnotationDiffer annDiffer = new gate.util.AnnotationDiffer();
    Set<String> features = new HashSet<String>();
    features.add("id");
    annDiffer.setSignificantFeaturesSet(features);
    AnnotationSet doc1KeyAnns = doc1.getAnnotations("Key");
    //System.out.println("testFindPairings01 doc1 anns from Key: "+doc1KeyAnns);
    AnnotationSet doc1Resp1Anns = doc1.getAnnotations("Resp1");    
    //System.out.println("testFindPairings01 doc1 anns from Resp1: "+doc1Resp1Anns);
    List<gate.util.AnnotationDiffer.Pairing> pairings = annDiffer.calculateDiff(doc1KeyAnns, doc1Resp1Anns);
    
    List<String> featureList = new ArrayList<String>();
    featureList.add("id");
    AnnotationDiffer newDiffer = new AnnotationDiffer(doc1KeyAnns,doc1Resp1Anns,featureList,null,null);
    
    
    System.out.println("OLD testFindPairings01 doc1 keys: "+annDiffer.getKeysCount());
    System.out.println("NEW testFindPairings01 doc1 keys: "+newDiffer.getKeysCount());
    System.out.println("OLD testFindPairings01 doc1 resp: "+annDiffer.getResponsesCount());
    System.out.println("NEW testFindPairings01 doc1 resp: "+newDiffer.getResponsesCount());
    System.out.println("OLD testFindPairings01 doc1 correct: "+annDiffer.getCorrectMatches());
    System.out.println("NEW testFindPairings01 doc1 correct: "+newDiffer.getCorrectMatches());
    System.out.println("OLD testFindPairings01 doc1 correct anns: "+annDiffer.correctAnnotations.size());
    System.out.println("OLD testFindPairings01 doc1 partially correct: "+annDiffer.getPartiallyCorrectMatches());
    System.out.println("NEW testFindPairings01 doc1 partially correct: "+newDiffer.getPartiallyCorrectMatches());
    System.out.println("OLD testFindPairings01 doc1 partial anns: "+annDiffer.partiallyCorrectAnnotations.size());
    System.out.println("OLD testFindPairings01 doc1 spurious: "+annDiffer.getSpurious());
    System.out.println("NEW testFindPairings01 doc1 spurious: "+newDiffer.getSpurious());
    System.out.println("NEW testFindPairings01 doc1 spurious strict: "+newDiffer.getEvalStats().getSpuriousStrict());
    System.out.println("NEW testFindPairings01 doc1 spurious lenient: "+newDiffer.getEvalStats().getSpuriousLenient());
    System.out.println("NEW testFindPairings01 doc1 true spurious strict: "+newDiffer.getEvalStats().getTrueSpuriousStrict());
    System.out.println("NEW testFindPairings01 doc1 true spurious lenient: "+newDiffer.getEvalStats().getTrueSpuriousLenient());
    System.out.println("OLD testFindPairings01 doc1 missing: "+annDiffer.getMissing());
    System.out.println("NEW testFindPairings01 doc1 missing: "+newDiffer.getMissing());
    System.out.println("NEW testFindPairings01 doc1 missing strict: "+newDiffer.getEvalStats().getMissingStrict());
    System.out.println("NEW testFindPairings01 doc1 missing lenient: "+newDiffer.getEvalStats().getMissingLenient());
    System.out.println("NEW testFindPairings01 doc1 true missing strict: "+newDiffer.getEvalStats().getTrueMissingStrict());
    System.out.println("NEW testFindPairings01 doc1 true missing lenient: "+newDiffer.getEvalStats().getTrueMissingLenient());
    System.out.println("NEW testFindPairings01 doc1 incorrect strict: "+newDiffer.getEvalStats().getIncorrectStrict());
    System.out.println("NEW testFindPairings01 doc1 incorrect lenient: "+newDiffer.getEvalStats().getIncorrectLenient());
    System.out.println("OLD testFindPairings01 doc1 precision strict: "+annDiffer.getPrecisionStrict());
    System.out.println("NEW testFindPairings01 doc1 precision strict: "+newDiffer.getPrecisionStrict());
    System.out.println("OLD testFindPairings01 doc1 recall strict: "+annDiffer.getRecallStrict());
    System.out.println("NEW testFindPairings01 doc1 recall strict: "+newDiffer.getRecallStrict());
    System.out.println("OLD testFindPairings01 doc1 fmeas strict: "+annDiffer.getFMeasureStrict(1.0));
    System.out.println("NEW testFindPairings01 doc1 fmeas strict: "+newDiffer.getFMeasureStrict(1.0));
    System.out.println("OLD testFindPairings01 doc1 precision lenient: "+annDiffer.getPrecisionLenient());
    System.out.println("NEW testFindPairings01 doc1 precision lenient: "+newDiffer.getPrecisionLenient());
    System.out.println("OLD testFindPairings01 doc1 recall lenient: "+annDiffer.getRecallLenient());
    System.out.println("NEW testFindPairings01 doc1 recall lenient: "+newDiffer.getRecallLenient());
    System.out.println("OLD testFindPairings01 doc1 fmeas lenient: "+annDiffer.getFMeasureLenient(1.0));
    System.out.println("NEW testFindPairings01 doc1 fmeas lenient: "+newDiffer.getFMeasureLenient(1.0));
    System.out.println("OLD testFindPairings01 doc1 missing anns: "+annDiffer.missingAnnotations.size());
    System.out.println("OLD testFindPairings01 doc1 spurious anns: "+annDiffer.spuriousAnnotations.size());
    System.out.println("NEW testFindPairings01 doc1 true missing lenient anns: "+newDiffer.getTrueMissingLenientAnnotations().size());
    System.out.println("NEW testFindPairings01 doc1 true spurious lenient anns: "+newDiffer.getTrueSpuriousLenientAnnotations().size());
    
  }
  
  
  ///////////////////////////
  /// MAIN
  //////////////////////////
  
  
  // so we can run this test from the command line 
  //public static void main(String args[]) {
  //  org.junit.runner.JUnitCore.main(TestDiffer1.class.getCanonicalName());
  //}  
  
  
}
