package gate.plugin.evaluation.tests;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import static gate.Utils.addAnn;
import static gate.Utils.featureMap;
import gate.creole.ResourceInstantiationException;
import gate.plugin.evaluation.api.FeatureComparison;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Some utils and predefined values used in the tests.
 * @author Johann Petrak
 */
public class TestUtils {
  // For the comparison of doubles, we use an epsilon of approximately
  // 1.7E-15 which is 1.0 (the maximum expected number) divided through the value of the maximum 
  // mantissa of double (64 bit), but with 3 bits taken away, i.e. 52-3 bits for the mantissa,
  // i.e. 2^49 
  public static final double EPS = 1.7763568394002505e-15;
  
  // create a string with 1000 blanks which we will use as document content for many documents
  // dynamically created in the tests
  public static final String STR1000 = new String(new char[1000]).replace("\0", " ");
  
  // predefined lists with the names of features to use
  public static final List<String> FL_ID = new ArrayList<String>();
  static { FL_ID.add("id"); }

  // predefined setss with the names of features to use
  public static final Set<String> FS_ID = new HashSet<String>();
  static { FS_ID.add("id"); }
  
  public static final FeatureComparison FC_EQU = FeatureComparison.FEATURE_EQUALITY;
  public static final FeatureComparison FC_SUB = FeatureComparison.FEATURE_SUBSUMPTION;
  
  public static Document newD() throws ResourceInstantiationException {
    return Factory.newDocument(STR1000);
  }
  
  public static AnnotationSet addA(Document doc, String setName, int from, int to, String type, Object idFeatureValue) {
    AnnotationSet set = doc.getAnnotations(setName);
    addAnn(set, from, to, type, featureMap("id",idFeatureValue));
    return set;
  }
  
  public static AnnotationSet addA(Document doc, String setName, int from, int to, String type, FeatureMap fm) {
    AnnotationSet set = doc.getAnnotations(setName);
    addAnn(set, from, to, type, fm);
    return set;
  }
  

}
