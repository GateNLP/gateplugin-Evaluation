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
package gate.plugin.evaluation.resources;

import gate.plugin.evaluation.api.ContainmentType;
import gate.plugin.evaluation.api.NilTreatment;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.FeatureMap;
import gate.Resource;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.evaluation.api.AnnotationDifferTagging;
import gate.plugin.evaluation.api.AnnotationDifferTagging.CandidateList;
import gate.plugin.evaluation.api.AnnotationTypeSpec;
import gate.plugin.evaluation.api.AnnotationTypeSpecs;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.util.GateRuntimeException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 *
 * @author Johann Petrak
 */
@CreoleResource(
        name = "EvaluateMaxRecall",
        helpURL ="https://github.com/johann-petrak/gateplugin-Evaluation/wiki/EvaluateMaxRecall-PR",
        comment = "Calculate maximum recall for annotations with candidate lists")
public class EvaluateMaxRecall extends EvaluateTaggingBase 
  implements ControllerAwarePR
  //, CustomDuplication 
{
  
  ///////////////////
  /// PR PARAMETERS 
  ///////////////////
  
  

  protected String edgeFeatureName;
  @CreoleParameter(comment="The name of the feature that contains the list of candidate/element annotation ids",defaultValue="")
  @RunTime
  @Optional  
  public void setEdgeFeatureName(String value) { edgeFeatureName = value; }
  public String getEdgeFeatureName() { return edgeFeatureName; }
  public String getExpandedEdgeFeatureName() { return Utils.replaceVariablesInString(getEdgeFeatureName()); }
  
  protected String listType;
  @CreoleParameter(comment="The annotation type of the list annotations",defaultValue="LookupList")
  @RunTime
  public void setListType(String value) { listType = value; }
  public String getListType() { return listType; }
  public String getExpandedListType() { return Utils.replaceVariablesInString(getListType()); }
  
  protected String keyType;
  @CreoleParameter(comment="The annotation type of the key/target annotations",defaultValue="Mention")
  @RunTime
  public void setKeyType(String value) { keyType = value; }
  public String getKeyType() { return keyType; }
  public String getExpandedKeyType() { return Utils.replaceVariablesInString(getKeyType()); }
  
  protected String elementType;
  @CreoleParameter(comment="The annotation type of the list element annotations",defaultValue="Lookup")
  @RunTime
  public void setElementType(String value) { elementType = value; }
  public String getElementType() { return elementType; }
  public String getExpandedElementType() { return Utils.replaceVariablesInString(getElementType()); }
  
  
  //////////////////// 
  // PR METHODS 
  ///////////////////

  @Override
  public Resource init() {    
    return this;
  }

  
  @Override
  public void reInit() {
    init();
  }
  
  @Override
  public void cleanup() {
    //
  }
  
  /// API methods to return the crucial measurements and values
  
  
  private int nTargets;
  /**
   * Number of total targets over all processed documents
   * @return 
   */
  public int getTargets() { return nTargets; }

  private int nTargetsWithList;
  /**
   * Number of total targets that have at least one non-empty response list 
   */
  public int getTargetsWithList() { return nTargetsWithList; }
  
  private List<Integer> nCorrectStrictByRank;
  /**
   * Number of correct strict responses for a target at rank i
   */
  public List<Integer> getCorrectStrictByRank() { 
    return new ArrayList<Integer>(nCorrectStrictByRank); 
  }

  // convenience method to increment the count for a specific 
  // index by the given value and make sure that all 
  // new elements are set to zero.
  // returns the new value
  private int incCorrectStrictByRank(int index, int byValue) {
    return incHelper(nCorrectStrictByRank,index,byValue);
  }
  private int incCorrectLenientByRank(int index, int byValue) {
    return incHelper(nCorrectLenientByRank,index,byValue);
  }
  private int incHelper(List<Integer> what, int index, int byValue) {
    int val = 0;
    // if the index is not in the array yet, then we need to add index-size elements
    if(index >= what.size()) {
      for(int i = what.size(); i <= index; i++) {
        what.add(0);
      }
      what.set(index, byValue);
      val = byValue;
    } else {
      val = what.get(index);
      val += byValue;
      what.set(index,val);
    }
    return val;
  }
  
  // convenience method to initialize all counts 
  private void initializeCounts() {
    nTargets = 0;
    nTargetsWithList = 0;
    nCorrectLenient = 0;
    nCorrectLenientByRank = new ArrayList<Integer>(1000);
    nCorrectStrict = 0;
    nCorrectStrictByRank = new ArrayList<Integer>(1000);
    nResponseLists = 0;
    nResponseListsWithTarget = 0;
  }
  
  
  
  private List<Integer> nCorrectLenientByRank;
  /**
   * Number of crrect lenient responses for a target at rank i 
   * @return 
   */
  private List<Integer> getCorrectLenientByRank() {
    return new ArrayList<Integer>(nCorrectLenientByRank);
  }
  
  private int nCorrectStrict;
  private int nCorrectLenient;
  
  public double getMaxRecallStrict() { 
    return recall(nTargets,nCorrectStrict);
  }
  public double getMaxRecallLenient() { 
    return recall(nTargets,nCorrectLenient);
  }
  public double getMaxRecall4ListsStrict() { 
    return recall(nTargetsWithList,nCorrectStrict);
  }
  public double getMaxRecall4ListsLenient() { 
    return recall(nTargetsWithList,nCorrectLenient);
  }
  
  public List<Double> getMaxRecallStrictByRank() {
    List<Double> ret = new ArrayList<Double>(nCorrectStrictByRank.size());
    int sum = 0;
    for(int corr : nCorrectStrictByRank) {
      sum += corr;
      ret.add(recall(nTargets,sum));
    }
    return ret;
  }
  
  public List<Double> getMaxRecallLenientByRank() {
    List<Double> ret = new ArrayList<Double>(nCorrectLenientByRank.size());
    int sum = 0;
    for(int corr : nCorrectLenientByRank) {
      sum += corr;
      ret.add(recall(nTargets,sum));
    }
    return ret;
  }
  
  public List<Double> getMaxRecallStrict4ListByRank() {
    List<Double> ret = new ArrayList<Double>(nCorrectStrictByRank.size());
    int sum = 0;
    for(int corr : nCorrectStrictByRank) {
      sum += corr;
      ret.add(recall(nTargetsWithList,sum));
    }
    return ret;
  }

  public List<Double> getMaxRecallLenient4ListByRank() {
    List<Double> ret = new ArrayList<Double>(nCorrectLenientByRank.size());
    int sum = 0;
    for(int corr : nCorrectLenientByRank) {
      sum += corr;
      ret.add(recall(nTargetsWithList,sum));
    }
    return ret;
  }
  
  private int nResponseLists;
  private int nResponseListsWithTarget;
  
  /// Helper functions
  
  private double recall(int targets, int correct) {
    if(targets == 0 && correct == 0) {
      return 1.0;
    } else if(targets == 0) {
      return 0.0;
    } else {
      return (double)correct / targets;
    }
  }
  
  
  
  AnnotationTypeSpecs annotationTypeSpecs4Best;
  
  String expandedEdgeName;
  
  protected static final String initialFeaturePrefixResponse = "evaluateTagging4Lists.response.";
  protected static final String initialFeaturePrefixReference = "evaluateTagging4Lists.reference.";
  
  protected static final Logger logger = Logger.getLogger(EvaluateMaxRecall.class);
  
  protected PrintStream maxrecallTsvPrintStream;
    
  @Override
  public void execute() throws ExecutionException {
    //System.out.println("DEBUG: running tagging4lists execute");
    if(needInitialization) {
      needInitialization = false;
      initializeCounts();
      initializeForRunning();
    }
    if(isInterrupted()) {
      throw new ExecutionException("PR was interrupted!");       
    }
    
    //System.out.println("DOC: "+document);
        
    // This should iterate exactly once because we created the annotationTypeSpecs from 
    // single key and list annotation types this PR uses.
    for(AnnotationTypeSpec typeSpec : annotationTypeSpecs.getSpecs()) {
      AnnotationSet keySet = document.getAnnotations(expandedKeySetName).get(typeSpec.getKeyType());
      //System.out.println("DEBUG: getting anns for set "+expandedResponseSetName+" and type "+typeSpec.getResponseType());
      AnnotationSet responseSet = document.getAnnotations(expandedResponseSetName).get(typeSpec.getResponseType());
      // for now, no support for a reference set!!
      evaluateForType(keySet,responseSet,null,typeSpec);      
    }
    
    
  }
  
  protected void evaluateForType(
          AnnotationSet keySet, AnnotationSet responseSet, AnnotationSet referenceSet, AnnotationTypeSpec typeSpec) {
    String type = typeSpec.getKeyType();
    //System.out.println("DEBUG: evaluating for type "+typeSpec+" keysize="+keySet.size()+" resSize="+responseSet.size());
    if(!expandedContainingNameAndType.isEmpty()) {
      String[] setAndType = expandedContainingNameAndType.split(":",2);
      if(setAndType.length != 2 || setAndType[0].isEmpty() || setAndType[1].isEmpty()) {
        throw new GateRuntimeException("Runtime Parameter containingASAndName not of the form setname:typename");
      }      
      String containingSetName = setAndType[0];
      String containingType = setAndType[1];
      AnnotationSet containingSet = document.getAnnotations(setAndType[0]).get(setAndType[1]);
      // now filter the keys and responses. If the containing set/type is the same as the key set/type,
      // do not filter the keys.
      ContainmentType ct = containmentType;
      if(ct == null) ct = ContainmentType.OVERLAPPING;
      responseSet = selectOverlappingBy(responseSet,containingSet,ct);
      if(containingSetName.equals(expandedKeySetName) && containingType.equals(type)) {
        // no need to do anything for the key set
      } else {
        keySet = selectOverlappingBy(keySet,containingSet,ct);
      }
      // if we have a reference set, we need to apply the same filtering to that one too
      if(referenceSet != null) {
        referenceSet = selectOverlappingBy(referenceSet,containingSet,ct);
      }
    } // have a containing set and type
    
    boolean filterNils = false;
    if(getNilTreatment().equals(NilTreatment.NIL_IS_ABSENT)) {
      filterNils = true;
      removeNilAnns(keySet);
    }
    //System.out.println("DEBUG: after NIL filtering, keysize="+keySet.size());
    
    // counts for measuring overall (not by rank) document max recall
    int nDocTargets = 0;
    int nDocTargetsWithList = 0;
    int nDocStrictMatches = 0;
    int nDocLenientMatches = 0;
    int nDocResponseLists = 0;
    int nDocResponseListsWithList = 0;
    
    AnnotationSet listAnns = responseSet;
    
    // Create the candidate lists, with the candidates sorted by the given score feature,
    // and with empty response lists removed.
    List<CandidateList> candLists = 
              AnnotationDifferTagging.createCandidateLists(
                      document.getAnnotations(expandedResponseSetName),
                      listAnns, 
                      expandedEdgeName, 
                      expandedScoreFeatureName, // this should be null if we evaluate for ranks                      
                      getExpandedElementType(),
                      filterNils,getNilValue(),getFeatureNames().get(0));
    
    nResponseLists += candLists.size();
    nDocResponseLists += candLists.size();
    
    // This set is used to record the listAnns that do have a target so we can count them later
    Set<Annotation> listAnnsWithTarget = new HashSet<Annotation>(listAnns.size());
    
    for(Annotation keyAnn : keySet.inDocumentOrder()) {
      // each key annotation is a target so count it
      nTargets += 1;
      nDocTargets += 1;
      
      // get all the candidate lists that overlap with the key. At the moment this
      // is done a bit clumsily by checking the list annotations for all candidate lists
      // separatel and putting all that overlap in a list.
      List<CandidateList> overlaps = new ArrayList<CandidateList>();
      for(CandidateList cl : candLists) {
        if(cl.getListAnnotation().overlaps(keyAnn)) {
          overlaps.add(cl);
        }
      }

      // if there are no overlapping lists, we are done, all the interesting stuff
      // only happens if we have at least one response list
      if(overlaps.size() > 0) {
        // count as a target that has at least one response list
        nTargetsWithList += 1;
        nDocTargetsWithList += 1;
        
        // find the lowest index among all response lists where we have a strict or a lenient (strict or partial)
        // match. For this we initialize an index for those matches with MAXINT and 
        // lower it for each match we find with a lower index in any of the overlapping lists.
        int strictMatchIndex = Integer.MAX_VALUE;
        int lenientMatchIndex = Integer.MAX_VALUE;
        for(CandidateList cl : overlaps) {
          // record that this list overlaps with a target
          listAnnsWithTarget.add(cl.getListAnnotation());
          for(int i = 0; i < cl.sizeAll(); i++) {
            // first check if the response overlaps with the key at all, only do something
            // if that is the case            
            Annotation resp = cl.get(i);
            if(resp.overlaps(keyAnn)) {
              //System.out.println("DEBUG: got an overlap, matching "+keyAnn.getFeatures().get("inst")+" and "+resp.getFeatures().get("inst"));
              boolean match = AnnotationDifferTagging.isAnnotationsMatch(keyAnn, resp, 
                    featureSet, featureComparison, true, annotationTypeSpecs);
              // if we have a match, it is definitely lenient (strict or partial)
              // but we have to check if the response is also coextensive
              if(match) {
                //System.out.println("DEBUG: got match");
                if(i < lenientMatchIndex) lenientMatchIndex = i;
                if(resp.coextensive(keyAnn)) {
                  if(i < strictMatchIndex) strictMatchIndex = i;
                  // we have found and checked a strict match, so any other match at a higher
                  // index will not change anything any more, we can terminate the loop
                  break;
                }
              } else { // if match
                //System.out.println("DEBUG: got NO match");
              }
            }
          } // for i=0..cl.size
        } // for cl : overlaps
        // Now the match indices are either still MAXINT because no match was found, or 
        // they are the index of the lowest match found. If not maxint, we can also 
        // increase the overall (not by rank) max recall counts
        // We only can have a strict match if there was a lenient match because each strict
        // match is also a lenient one
        if(lenientMatchIndex < Integer.MAX_VALUE) {
          nCorrectLenient += 1;
          nDocLenientMatches += 1;
          //System.out.println("DEBUG: incrementing lenient at index "+lenientMatchIndex);
          incCorrectLenientByRank(lenientMatchIndex, 1);
          if(strictMatchIndex < Integer.MAX_VALUE) {
            nCorrectStrict += 1;
            nDocStrictMatches += 1;
            //System.out.println("DEBUG: incrementing strict at index "+strictMatchIndex);
            incCorrectStrictByRank(strictMatchIndex, 1);
          } else {
            // TODO: maybe set to -1 instead?
          }
        } 
        
        
        // create annotation in the output set, if we have one 
        if(!outputASResName.isEmpty()) {
          // figure out which suffix to use for the indicator annotation depending on the kind of
          // match we found:
          //  _NM : we had a response list but no match at all
          //  _SM : we found a strict match somewhere
          //  _PM : we found a partial match but no strict match
          //  _NR : there is not even a response list
          String suf = "NM";
          if(strictMatchIndex < Integer.MAX_VALUE) {
            suf = "SM";
          } else if(lenientMatchIndex < Integer.MAX_VALUE) {
            suf = "PM";
          } 
          AnnotationSet outSet = document.getAnnotations(outputASResName);
          Utils.addAnn(outSet, keyAnn, keyAnn.getType()+"_"+suf, Utils.toFeatureMap(keyAnn.getFeatures()));
        }      
        
        // TODO: maybe replace MAX_VALUE with -1 for the tsv output.
        
        // TODO: output line for this target to TSV file if we have one
        
        
        
      } else {  // if we have candidate lists that overlap with the target
        // no we do not have a list, so create an indicator annotation if wanted
        if(!outputASResName.isEmpty()) {
          AnnotationSet outSet = document.getAnnotations(outputASResName);
          Utils.addAnn(outSet, keyAnn, keyAnn.getType()+"_NR", Utils.toFeatureMap(keyAnn.getFeatures()));
        }      
        
        
      }
      
    } // end for keyAnn in keySet

    nResponseListsWithTarget += listAnnsWithTarget.size();
    nDocResponseListsWithList += listAnnsWithTarget.size();

    if(mainTsvPrintStream != null) {
      String line = outputTsvLine(
                "list-maxrecall",
                document.getName(),
                annotationTypeSpecs.getSpecs().get(0), 
                expandedResponseSetName, 
                -1, 
                nDocTargets, 
                nDocTargetsWithList, 
                nDocStrictMatches, 
                nDocLenientMatches, 
                nDocResponseLists, 
                nDocResponseListsWithList);
      mainTsvPrintStream.println(line);
    }
    
    // TODO: maybe do the same for the reference set!
    
    // Store the counts and measures as document feature values
    FeatureMap docFm = document.getFeatures();
    
    // TODO: store as document features!
    
    /*
    if (getAddDocumentFeatures()) {      
      String featurePrefixResponseT = featurePrefixResponse;
      featurePrefixResponseT += type;
      docFm.put(featurePrefixResponseT + "FMeasureStrict", es.getFMeasureStrict(1.0));
      docFm.put(featurePrefixResponseT + "FMeasureLenient", es.getFMeasureLenient(1.0));
      docFm.put(featurePrefixResponseT + "PrecisionStrict", es.getPrecisionStrict());
      docFm.put(featurePrefixResponseT + "PrecisionLenient", es.getPrecisionLenient());
      docFm.put(featurePrefixResponseT + "RecallStrict", es.getRecallStrict());
      docFm.put(featurePrefixResponseT + "RecallLenient", es.getRecallLenient());
      docFm.put(featurePrefixResponseT + "SingleCorrectAccuracyStrict", es.getSingleCorrectAccuracyStrict());
      docFm.put(featurePrefixResponseT + "SingleCorrectAccuracyLenient", es.getSingleCorrectAccuracyLenient());
      docFm.put(featurePrefixResponseT + "CorrectStrict", es.getCorrectStrict());
      docFm.put(featurePrefixResponseT + "CorrectPartial", es.getCorrectPartial());
      docFm.put(featurePrefixResponseT + "IncorrectStrict", es.getIncorrectStrict());
      docFm.put(featurePrefixResponseT + "IncorrectPartial", es.getIncorrectPartial());
      docFm.put(featurePrefixResponseT + "TrueMissingStrict", es.getTrueMissingStrict());
      docFm.put(featurePrefixResponseT + "TrueMissingLenient", es.getTrueMissingLenient());
      docFm.put(featurePrefixResponseT + "TrueSpuriousStrict", es.getTrueSpuriousStrict());
      docFm.put(featurePrefixResponseT + "TrueSpuriousLenient", es.getTrueSpuriousLenient());
      docFm.put(featurePrefixResponseT + "Targets", es.getTargets());
      docFm.put(featurePrefixResponseT + "Responses", es.getResponses());
    }
    */
    
  }
  
  
  
  ////////////////////////////////////////////
  /// HELPER METHODS
  ////////////////////////////////////////////
  
  /**
   * Return a new set with all the NIL annotations removed.
   * @param set 
   */
  private AnnotationSet removeNilAnns(AnnotationSet set) {
    String nilStr = "";
    if(getNilValue() != null) { nilStr = getNilValue(); }
    // NOTE: this method only gets invoked if feature names is non-null and contains at least
    // one element (does not make sense to invoke it otherwise!)
    String idFeature = getFeatureNames().get(0);
    Set<Annotation> nils = new HashSet<Annotation>();
    for (Annotation ann : set) {
      Object val = ann.getFeatures().get(idFeature);
      String valStr = val == null ? "" : val.toString();
      if (valStr.equals(nilStr)) {
        nils.add(ann);
      }
    }
    AnnotationSet newset = new AnnotationSetImpl(set);
    newset.removeAll(nils);
    return newset;
  }
  

  // This needs to run as part of the first execute, since at the moment, the parametrization
  // does not work correctly with the controller callbacks. 
  @Override
  protected void initializeForRunning() {
    //System.out.println("DEBUG: reinitializing");
    super.initializeForRunning();
    //System.out.println("DEBUG: running tagging4lists initialize");
    expandedEdgeName = getStringOrElse(getExpandedEdgeFeatureName(),"");
    expandedScoreFeatureName = getStringOrElse(getExpandedScoreFeatureName(),"");
    
    
    if(getExpandedListType() == null || getExpandedListType().isEmpty()) {
      throw new GateRuntimeException("List annotation type is not specified or empty!");
    }
    if(getExpandedElementType() == null || getExpandedElementType().isEmpty()) {
      throw new GateRuntimeException("Element annotation type is not specified or empty!");
    }
        
    if(getExpandedKeyType() == null || getExpandedKeyType().isEmpty()) {
      throw new GateRuntimeException("Key annotation type is not specified or empty!");
    }
    
    if(getFeatureNames() != null) {
      for(String t : getFeatureNames()) {
        if(t == null || t.isEmpty()) {
          throw new GateRuntimeException("List of feature names to use contains a null or empty type name!");
        }      
      }
    }
      
    // If the featureNames list is null, this has the special meaning that the features in 
    // the key/target annotation should be used. In that case the featureNameSet will also 
    // be left null. Otherwise the list will get converted to a set.
    // Convert the feature list into a set
    if(featureNames != null) {
      if(0 == featureNames.size()) {
        throw new GateRuntimeException("Need at least one feature for list evaluation");
      }
      for(String t : getFeatureNames()) {
        if(t == null || t.isEmpty()) {
          throw new GateRuntimeException("List of feature names to use contains a null or empty type name!");
        }      
      }
      Set<String> featureNameSet = new HashSet<String>();
      featureNameSet.addAll(featureNames);
      // check if we have duplicate entries in the featureNames
      if(featureNameSet.size() != featureNames.size()) {
        throw new GateRuntimeException("Duplicate feature in the feature name list");
      }
    } else {
      throw new GateRuntimeException("Need at least one feature for list evaluation");
    }
    
    List<String> types = new ArrayList<String>();
    types.add(getExpandedKeyType()+"="+getExpandedListType());
    annotationTypeSpecs = new AnnotationTypeSpecs(types);
    
    types = new ArrayList<String>();
    types.add(getExpandedKeyType()+"="+getExpandedElementType());
    annotationTypeSpecs4Best = new AnnotationTypeSpecs(types);
    
    // Establish the default containment type if it was not specified. 
    if(getContainmentType() == null) {
      containmentType = ContainmentType.OVERLAPPING;
    }
    if(getNilTreatment() == null) {
      nilTreatment = NilTreatment.NO_NILS;
    }
    
    if(!(getExpandedReferenceASName() == null || getExpandedReferenceASName().isEmpty())) {
      System.err.println("WARNING: Reference set cannot be used with List evaluation (yet?), ignored!");
      expandedReferenceSetName = null;
      referenceASName = null;
    } 
    
    featurePrefixResponse = initialFeaturePrefixResponse + getExpandedEvaluationId() + "." + getResponseASName() + "." ;
    featurePrefixReference = initialFeaturePrefixReference + getExpandedEvaluationId() + "." + getReferenceASName() + ".";
    
    if(!expandedOutputASPrefix.isEmpty()) {
      outputASResName = expandedOutputASPrefix+"_MaxRecall";
    } else {
      outputASResName = "";
    }

    mainTsvPrintStream = getOutputStream("maxrec");

    if(mainTsvPrintStream != null) {
      mainTsvPrintStream.print("evaluationId"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("evaluationType"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("docName"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("setName"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("annotationType"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("thresholdType"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("threshold"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("maxRecallStrict"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("maxRecallLenient"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("maxRecallListStrict"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("maxRecallListLenient"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("targets"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("targets"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("targetsWithList"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("correctStrict"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("correctLenient"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("lists"); mainTsvPrintStream.print("\t");
      mainTsvPrintStream.print("listsWithTarget"); mainTsvPrintStream.print("\t");      
    }

    
  }
  
  
  
  public void finishRunning() {
    outputDefaultResults();
    if(mainTsvPrintStream != null) {
      mainTsvPrintStream.close();    
    }
  }
  
  protected String outputTsvLine(
          String evalType,
          String docName,
          AnnotationTypeSpec typeSpec,
          String setName,
          int rank,  // the rank or -1 for overall counts/measures
          int targets, int targetsWithList, int correctStrict, int correctLenient,
          int lists, int listsWithTarget
  ) {
    StringBuilder sb = new StringBuilder();
    sb.append(expandedEvaluationId); sb.append("\t");
    sb.append(evalType); sb.append("\t");
    if(docName == null) {
      sb.append("[doc:all:micro]");
    } else {
      sb.append(docName);
    }
    sb.append("\t");
    if(setName == null || setName.isEmpty()) {
      sb.append(expandedResponseSetName);
    } else {
      sb.append(setName);
    }
    sb.append("\t");
    if(typeSpec == null) {
      sb.append("[type:all:micro]");
    } else {
      sb.append(typeSpec);
    }
    sb.append("\t");
    sb.append("rank"); sb.append("\t");  // the threshold type is always rank!
    sb.append(rank); sb.append("\t");   // -1 for overall
    // the stuff that is specific to max recall
    sb.append(recall(targets,correctStrict)); sb.append("\t");
    sb.append(recall(targets,correctLenient)); sb.append("\t");
    sb.append(recall(targetsWithList,correctStrict)); sb.append("\t");
    sb.append(recall(targetsWithList,correctLenient)); sb.append("\t");
    sb.append(targets); sb.append("\t");
    sb.append(targetsWithList); sb.append("\t");
    sb.append(correctStrict); sb.append("\t");
    sb.append(correctLenient); sb.append("\t");
    sb.append(lists); sb.append("\t");
    sb.append(listsWithTarget); sb.append("\t");
    return sb.toString();    
  }
  
  
  public void outputDefaultResults() {
    
    AnnotationTypeSpec typeSpecNormal = annotationTypeSpecs.getSpecs().get(0);
    AnnotationTypeSpec typeSpecList   = annotationTypeSpecs.getSpecs().get(0);
    
    System.out.println(evaluationId+" MaxRecall Recall Strict: "+r4(recall(nTargets,nCorrectStrict)));
    System.out.println(evaluationId+" MaxRecall Recall Lenient: "+r4(recall(nTargets,nCorrectLenient)));
    System.out.println(evaluationId+" MaxRecall Recall In Responses Strict: "+r4(recall(nTargetsWithList,nCorrectStrict)));
    System.out.println(evaluationId+" MaxRecall Recall In Responses Lenient: "+r4(recall(nTargetsWithList,nCorrectLenient)));
    System.out.println(evaluationId+" Targets: "+nTargets);
    System.out.println(evaluationId+" Targets without responses: "+(nTargets-nTargetsWithList));
    System.out.println(evaluationId+" Targets with strict match: "+nCorrectStrict);
    System.out.println(evaluationId+" Targets with only partial match: "+(nCorrectLenient-nCorrectStrict));
    System.out.println(evaluationId+" Targets with responses: "+nTargetsWithList);  
    System.out.println(evaluationId+" Lists: "+nResponseLists);
    System.out.println(evaluationId+" Lists with target: "+nResponseListsWithTarget);
    System.out.println(evaluationId+" Spurious lists (no target): "+(nResponseLists-nResponseListsWithTarget));
    
    
    // Now also output the by rank recall strict and recall lenient for lists and overall
    
    List<Double> maxrecS = getMaxRecallStrictByRank();
    for(int i = 0; i<maxrecS.size(); i++) {
      System.out.println(evaluationId+" MaxRecall strict at rank: "+i+" "+r4(maxrecS.get(i)));
    }
    List<Double> maxrecL = getMaxRecallLenientByRank();
    for(int i = 0; i<maxrecL.size(); i++) {
      System.out.println(evaluationId+" MaxRecall lenient at rank: "+i+" "+r4(maxrecL.get(i)));
    }
    List<Double> maxrecSL = getMaxRecallStrict4ListByRank();
    for(int i = 0; i<maxrecSL.size(); i++) {
      System.out.println(evaluationId+" MaxRecall4List strict at rank: "+i+" "+r4(maxrecSL.get(i)));
    }
    List<Double> maxrecLL = getMaxRecallLenient4ListByRank();
    for(int i = 0; i<maxrecLL.size(); i++) {
      System.out.println(evaluationId+" MaxRecall4List lenient at rank: "+i+" "+r4(maxrecLL.get(i)));
    }

    if(mainTsvPrintStream != null) {
      // The lines for each possible rank ....
      int n = Math.max(nCorrectLenientByRank.size(),nCorrectStrictByRank.size());
      int nSumCS = 0;
      int nSumCL = 0;      
      for(int i = 0; i<n; i++) {
        if(i<nCorrectStrictByRank.size()) {
          nSumCS += nCorrectStrictByRank.get(i);
        }
        if(i<nCorrectLenientByRank.size()) {
          nSumCL += nCorrectLenientByRank.get(i);
        }
        String line = outputTsvLine(
                "list-maxrecall",
                null,
                typeSpecList, 
                expandedResponseSetName, 
                i, 
                nTargets, 
                nTargetsWithList, 
                nSumCS, 
                nSumCL, 
                nResponseLists, 
                nResponseListsWithTarget);
        mainTsvPrintStream.println(line);
      }
      // Now one line for overall
      String line = outputTsvLine(
                "list-maxrecall",
                null,
                typeSpecList, 
                expandedResponseSetName, 
                -1, 
                nTargets, 
                nTargetsWithList, 
                nCorrectStrict, 
                nCorrectLenient, 
                nResponseLists, 
                nResponseListsWithTarget);
      mainTsvPrintStream.println(line);
    }
    
    
    
  }
  
  
  ////////////////////////////////////////////
  /// CONTROLLER AWARE PR methods
  ////////////////////////////////////////////
  
  @Override
  public void controllerExecutionStarted(Controller cntrlr) throws ExecutionException {
    needInitialization = true;
  }

  @Override
  public void controllerExecutionFinished(Controller cntrlr) throws ExecutionException {
    // only do anything at all if we had actually been executed once. The callback gets also
    // invoked if the PR was disabled, so we have to check ...
    // needInitialization is set in the started callback and reset in execute, so if it is still
    // on, we never were in execute.
    if(!needInitialization) {
      finishRunning();
      needInitialization = true;
    }
  }

  @Override
  public void controllerExecutionAborted(Controller cntrlr, Throwable thrwbl) throws ExecutionException {
    if(!needInitialization) {
      System.err.println("Processing was aborted: "+thrwbl.getMessage());
      thrwbl.printStackTrace(System.err);
      System.err.println("Here are the summary stats for what was processed: ");
      finishRunning();
      needInitialization = true;
    }
  }

  /*
  @Override
  public Resource duplicate(Factory.DuplicationContext dc) throws ResourceInstantiationException {
    throw new UnsupportedOperationException("At the moment, this PR may not be duplicated and must be run single-threaded"); 
    // TODO: duplicate such that all duplicates get a flag set which indicates that they are 
    // duplicates, and only the original has the flag not set.
    // Also, use a shared object to give all PRs access to everyone elses's statistics objects.
    // Also, use a shared obejcts to give all PRs access to a singleton object for writing to
    // the output files.
    // Finally, implement the controllerExecutionFinished() method such that only the original
    // will do the actual summarization: it will access all stats objects from all other PRs and
    // summarize them and 
  }
  */
  
  
}
