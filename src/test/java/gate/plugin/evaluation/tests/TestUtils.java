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
import static gate.Utils.addAnn;
import static gate.Utils.featureMap;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.plugin.evaluation.api.FeatureComparison;
import gate.plugin.evaluation.resources.EvaluateTagging;
import gate.plugin.evaluation.resources.EvaluateTaggingBase;
import java.util.ArrayList;
import java.util.Arrays;
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
  public static final double EPS4 = 1e-4;
  
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
  
  public static List<Integer> newIntList(Integer... vals)  {
    List<Integer> ret = new ArrayList<Integer>();
    ret.addAll(Arrays.asList(vals));
    return ret;
  }
  public static List<String> newStringList(String... vals)  {
    List<String> ret = new ArrayList<String>();
    ret.addAll(Arrays.asList(vals));
    return ret;
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
  
  public static void addListAnn(AnnotationSet set, int from, int to, String typeWithoutList, List<Integer> annIds) {
    addAnn(set,from,to,typeWithoutList,featureMap("ids",annIds));
  }
    
  public static void runETPR(EvaluateTaggingBase pr, Document... docs) throws ExecutionException {
    pr.controllerExecutionStarted(null);
    for(Document d : docs) {
      pr.setDocument(d);
      pr.execute();
    }
    pr.controllerExecutionFinished(null);
  }
  
  
}
