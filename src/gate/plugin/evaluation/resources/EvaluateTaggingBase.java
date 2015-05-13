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
import gate.Utils;
import gate.annotation.ImmutableAnnotationSetImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.evaluation.api.ContingencyTableInteger;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.plugin.evaluation.api.FeatureComparison;
import gate.plugin.evaluation.api.ThresholdsToUse;
import gate.util.Files;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 * Common base class for the Evaluation PRs for Tagging. 
 * This processes the parameters that are common to both PRs and also does some basic
 * processing that is common.
 * @author Johann Petrak
 */
public abstract class EvaluateTaggingBase extends AbstractLanguageAnalyser  {

  /////////////////////////////////////////////////////////////////////////
  /// PR PARAMETERS COMMON TO EvaluateTagging and EvaluateTagging4Lists
  /////////////////////////////////////////////////////////////////////////
  
  
  protected String keyASName;
  @CreoleParameter (comment="The name of the annotation set that contains the target/key annotations (gold standard)", defaultValue="Key")
  @RunTime
  public void setKeyASName(String name) { keyASName = name; }
  public String getKeyASName() { return keyASName; }
  public String getExpandedKeyASName() { return Utils.replaceVariablesInString(getKeyASName()); }
  
  protected String responseASName;
  @CreoleParameter (comment="The name of the annotation set that contains the response annotations",defaultValue ="Response")
  @RunTime
  public void setResponseASName(String name) { responseASName = name; }
  public String getResponseASName() { return responseASName; }
  public String getExpandedResponseASName() { return Utils.replaceVariablesInString(getResponseASName()); }
  
  protected String referenceASName;
  @CreoleParameter (comment="The name of the annotation set that contains the reference/old response annotations. Empty means no reference set.")
  @Optional
  @RunTime
  public void setReferenceASName(String name) { referenceASName = name; }
  public String getReferenceASName() { return referenceASName; }
  public String getExpandedReferenceASName() { return Utils.replaceVariablesInString(getReferenceASName()); }
  
  protected String containingASNameAndType;
  @CreoleParameter (comment="The name of the restricting annotation set and the name of the type in the form asname:typename")
  @Optional
  @RunTime
  public void setContainingASNameAndType(String name) { containingASNameAndType = name; }
  public String getContainingASNameAndType() { return containingASNameAndType; }
  public String getExpandedContainingASNameAndType() { return Utils.replaceVariablesInString(getContainingASNameAndType()); }
  
  protected ContainmentType containmentType;
  @CreoleParameter (comment="How the responses are restricted to the annotations of the containingASNameAndType",defaultValue="OVERLAPPING")
  @Optional
  @RunTime
  public void setContainmentType(ContainmentType ct) { ct = containmentType; }
  public ContainmentType getContainmentType() { return containmentType; }
  
  protected List<String> annotationTypes;
  @CreoleParameter (comment="The annotation types to use for evaluations, at least one type must be given",defaultValue="Mention")
  @RunTime
  public void setAnnotationTypes(List<String> name) { annotationTypes = name; }
  public List<String> getAnnotationTypes() { return annotationTypes; }
  
  protected List<String> featureNames;
  protected Set<String> featureSet;
  @CreoleParameter (comment="A list of feature names to use for comparison, can be empty. First is used as the id feature, if necessary.")
  @RunTime
  @Optional
  public void setFeatureNames(List<String> names) { 
    featureNames = names; 
    if(featureNames != null) {
      featureSet = new HashSet<String>(featureNames);
    }
  }
  public List<String> getFeatureNames() { return featureNames; }
  
  public FeatureComparison featureComparison;
  @CreoleParameter(comment="",defaultValue="FEATURE_EQUALITY")
  @RunTime
  @Optional  
  public void setFeatureComparison(FeatureComparison value) { featureComparison = value; }
  public FeatureComparison getFeatureComparison() { return featureComparison; }
     
  
  // For the list-based PR this would maybe either be a score (bigger is better), or a rank (smaller
  // is better) but then we would need a seperate setting to control which it is.
  protected String scoreFeatureName;
  @CreoleParameter (comment="The name of the feature which contains a numeric score or confidence. If specified will generated P/R curve.")
  @Optional
  @RunTime
  public void setScoreFeatureName(String name) { scoreFeatureName = name; }
  public String getScoreFeatureName() { return scoreFeatureName; }
  public String getExpandedScoreFeatureName() { return Utils.replaceVariablesInString(getScoreFeatureName()); }
  
  protected String outputASPrefix;
  @CreoleParameter (comment="The name of the annotation set for creating descriptive annotations. If empty, no annotations are created.")
  @Optional
  @RunTime
  public void setOutputASPrefix(String name) { outputASPrefix = name; }
  public String getOutputASPrefix() { return outputASPrefix; }
  public String getExpandedOutputASPrefix() { return Utils.replaceVariablesInString(getOutputASPrefix()); }
  
  protected NilTreatment nilTreatment;
  @CreoleParameter(comment="",defaultValue="NO_NILS")
  @RunTime
  @Optional  
  public void setNilTreatment(NilTreatment value) { nilTreatment = value; }
  public NilTreatment getNilTreatment() { return nilTreatment; }
     
  protected String nilValue;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setNilValue(String value) { nilValue = value; }
  public String getNilValue() { return nilValue; }
  public String getExpandedNilValue() { return Utils.replaceVariablesInString(getNilValue()); }
     
  protected URL outputDirectoryUrl;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setOutputDirectoryUrl(URL value) { outputDirectoryUrl = value; }
  public URL getOutputDirectoryUrl() { return outputDirectoryUrl; }
     
  protected String evaluationId;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setEvaluationId(String value) { evaluationId = value; }
  public String getEvaluationId() { return evaluationId == null ? "" : evaluationId; }
  public String getExpandedEvaluationId() { return Utils.replaceVariablesInString(getEvaluationId()); }
     
  // Maybe add an option to specify your own and then allow a list of values in a separate field?
  // Or always use the list if it is non-empty, ignoring the setting here?
  // If we allow thresholds to be specified, then we could also do single-thresholds analyses
  // especially for lists, where we analyse e.g. what we get if we take the best for threshold
  // > something or for rank < N. 
  protected ThresholdsToUse whichThresholds;
  @CreoleParameter(comment="",defaultValue="USE_ALL")
  @RunTime
  @Optional  
  public void setWhichThresholds(ThresholdsToUse value) { whichThresholds = value; }
  public ThresholdsToUse getWhichThresholds() { return whichThresholds; }
  
  
  protected List<AnnotationTypeSpec> annotationTypeSpecs;
  
  protected String expandedKeySetName;
  protected String expandedResponseSetName;
  protected String expandedReferenceSetName;
  protected String expandedContainingNameAndType;
  protected String expandedScoreFeatureName;
  protected String expandedOutputASPrefix;
  protected String outputASResName = "";
  protected String outputASRefName = "";
  protected String outputASDiffName = "";
  protected String expandedNilValue;
  protected String expandedEvaluationId;
  
  protected static final String initialFeaturePrefixResponse = "evaluateTagging.response.";
  protected static final String initialFeaturePrefixReference = "evaluateTagging.reference.";
  
  protected String featurePrefixResponse;
  protected String featurePrefixReference;
  
  
  protected static final Logger logger = Logger.getLogger(EvaluateTaggingBase.class);
  
  
  
  ////////////////////////////////////////////
  /// HELPER METHODS
  ////////////////////////////////////////////
  
  
  protected PrintStream mainTsvPrintStream;
  
  /** 
   * Create and open an print stream to the file where the Tsv rows should get written to.
   * If no output directory was specified, this returns null.
   * Otherwise it returns a stream that writes to a file in the output directory that has
   * the name "EvaluateTagging-ID.tsv" where "ID" is the value of the evaluationId parameter.
   * If the evaluationId parameter is not set, the file name is "EvaluateTagging.tsv".
   * @return 
   */
  protected PrintStream getOutputStream(String suffix) {
    if(getOutputDirectoryUrl() == null) {
      return null;
    }
    File dir = Files.fileFromURL(getOutputDirectoryUrl());
    if(!dir.exists()) {
      throw new GateRuntimeException("Output directory does not exists: "+getOutputDirectoryUrl());
    }
    if(!dir.isDirectory()) {
      throw new GateRuntimeException("Not a directory: "+getOutputDirectoryUrl());
    }
    String fname = getStringOrElse(getEvaluationId(), "").equals("") 
            ? "EvaluateTagging.tsv" : "EvaluateTagging-"+getEvaluationId();
    if(suffix != null && !suffix.isEmpty()) {
      fname += "-"+suffix;
    }
    fname += ".tsv";
    File outFile = new File(dir,fname);
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(outFile);
    } catch (FileNotFoundException ex) {
      throw new GateRuntimeException("Could not open output file "+outFile,ex);
    }    
    return new PrintStream(os);
  }
  
  protected boolean needInitialization = true;
  
  
  // This needs to run as part of the first execute, since at the moment, the parametrization
  // does not work correctly with the controller callbacks. 
  protected void initializeForRunning() {

    
    expandedKeySetName = getStringOrElse(getExpandedKeyASName(), "");
    expandedResponseSetName = getStringOrElse(getExpandedResponseASName(),"");
    expandedReferenceSetName = getStringOrElse(getExpandedReferenceASName(),"");
    expandedContainingNameAndType = getStringOrElse(getExpandedContainingASNameAndType(),"");
    expandedEvaluationId = getStringOrElse(getExpandedEvaluationId(),"");
    expandedNilValue = getStringOrElse(getExpandedNilValue(),"");
    expandedOutputASPrefix = getStringOrElse(getExpandedOutputASPrefix(),"");
    if(!expandedOutputASPrefix.isEmpty()) {
      outputASResName = expandedOutputASPrefix+"_Res";
      if(!expandedReferenceSetName.isEmpty()) {
        outputASRefName = expandedOutputASPrefix+"_Ref";
        outputASDiffName = expandedOutputASPrefix+"_Diff";
      }
    }
    expandedScoreFeatureName = getStringOrElse(getExpandedScoreFeatureName(),"");
    
    if(getAnnotationTypes() == null || getAnnotationTypes().isEmpty()) {
      throw new GateRuntimeException("List of annotation types to use is not specified or empty!");
    }
    annotationTypeSpecs = new ArrayList<AnnotationTypeSpec>(getAnnotationTypes().size());
    for(String t : getAnnotationTypes()) {
      if(t == null || t.isEmpty()) {
        throw new GateRuntimeException("List of annotation types to use contains a null or empty type name!");
      } else {
        // convert the entry to a AnnotationTypeSpec object
        String k = "";
        String r = "";
        if(t.contains("|")) {
          String tmp[] = t.split("\\|",2);
          k = tmp[0];
          r = tmp[1];
        } else {
          k = t;
          r = t;
        }
        // TODO: left off here!!
      }
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
      Set<String> featureNameSet = new HashSet<String>();
      featureNameSet.addAll(featureNames);
      // check if we have duplicate entries in the featureNames
      if(featureNameSet.size() != featureNames.size()) {
        throw new GateRuntimeException("Duplicate feature in the feature name list");
      }
    }
    
    
    // Establish the default containment type if it was not specified. 
    if(getContainmentType() == null) {
      containmentType = ContainmentType.OVERLAPPING;
    }
    if(getNilTreatment() == null) {
      nilTreatment = NilTreatment.NO_NILS;
    }
    
    featurePrefixResponse = initialFeaturePrefixResponse + getResponseASName() + ".";
    featurePrefixReference = initialFeaturePrefixReference + getReferenceASName() + ".";

  }
  
  
  protected static String getStringOrElse(String value, String elseValue) {
    if(value == null) return elseValue; else return value;
  }
  
  /**
   * Filter the annotations in the set toFilter and select only those which 
   * overlap with any annotation in set by.
   * 
   * @param toFilter
   * @param by
   * @return 
   */
  protected static AnnotationSet selectOverlappingBy(AnnotationSet toFilterSet, AnnotationSet bySet, ContainmentType how) {
    if(toFilterSet.isEmpty()) return toFilterSet;
    if(bySet.isEmpty()) return new ImmutableAnnotationSetImpl(toFilterSet.getDocument(),null);
    Set<Annotation> selected = new HashSet<Annotation>();
    for(Annotation byAnn : bySet) {
      AnnotationSet tmp = null;
      if(how == ContainmentType.OVERLAPPING) {
        tmp = gate.Utils.getOverlappingAnnotations(toFilterSet, byAnn);
      } else if(how == ContainmentType.CONTAINING) {
        tmp = gate.Utils.getContainedAnnotations(toFilterSet, byAnn);          
      } else if(how == ContainmentType.COEXTENSIVE) {
        tmp = gate.Utils.getCoextensiveAnnotations(toFilterSet, byAnn);        
      } else {
        throw new GateRuntimeException("Odd ContainmentType parameter value: "+how);
      }
      selected.addAll(tmp);
    }
    return new ImmutableAnnotationSetImpl(toFilterSet.getDocument(), selected);    
  }
  
  /**
   * Create an output line for a TSV file representation of the evaluation results.
   * This prefixes the TSV line created by the EvalStatsTagging object with the evaluation id,
   * the given annotation type, and the given document name. 
   * If an null/empty annotation type is passed, the type "[type:all:micro]" is used instead.
   * If a null document name is passed, the name "[doc:all:micro]" is used instead.
   * @param docName
   * @param annotationType
   * @param es
   * @return 
   */
  protected String outputTsvLine(
          String docName,
          String annotationType,
          String setName,
          EvalStatsTagging es
  ) {
    StringBuilder sb = new StringBuilder();
    sb.append(expandedEvaluationId); sb.append("\t");
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
    if(annotationType == null || annotationType.isEmpty()) {
      sb.append("[type:all:micro]");
    } else {
      sb.append(annotationType);
    }
    sb.append("\t");
    sb.append(es.getTSVLine());
    return sb.toString();    
  }
  
  
  protected static double r4(double x) {
    return ((double) Math.round(x * 10000.0) / 10000.0);
  }
  
  // Output the complete EvalStats object, but in a format that makes it easier to grep
  // out the lines one is interested in based on threshold and type
  public void outputEvalStatsForType(PrintStream out, EvalStatsTagging es, String type, String set) {
    EvaluateTaggingBase.outputEvalStatsForType(out,es,type,set,expandedEvaluationId);
  }
    
  public static void outputEvalStatsForType(PrintStream out, EvalStatsTagging es, String type, String set, String expandedEvaluationId) {
    
    String ts = "none";
    double th = es.getThreshold();
    if(!Double.isNaN(th)) {
      if(Double.isInfinite(th)) {
        ts="inf";
      } else {
        ts = "" + r4(th);
      }
    }
    ts = ", th="+ts+", ";
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Precision Strict: "+r4(es.getPrecisionStrict()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Recall Strict: "+r4(es.getRecallStrict()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"F1.0 Strict: "+r4(es.getFMeasureStrict(1.0)));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Accuracy Strict: "+r4(es.getSingleCorrectAccuracyStrict()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Precision Lenient: "+r4(es.getPrecisionLenient()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Recall Lenient: "+r4(es.getRecallLenient()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"F1.0 Lenient: "+r4(es.getFMeasureLenient(1.0)));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Accuracy Lenient: "+r4(es.getSingleCorrectAccuracyLenient()));
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Targets: "+es.getTargets());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Responses: "+es.getResponses());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Correct Strict: "+es.getCorrectStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Correct Partial: "+es.getCorrectPartial());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Incorrect Strict: "+es.getIncorrectStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Incorrect Partial: "+es.getIncorrectPartial());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Missing Strict: "+es.getMissingStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Missing Strict: "+es.getTrueMissingStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Missing Lenient: "+es.getMissingLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Missing Lenient: "+es.getTrueMissingLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Spurious Strict: "+es.getSpuriousStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Spurious Strict: "+es.getTrueSpuriousStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Spurious Lenient: "+es.getSpuriousLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"True Spurious Lenient: "+es.getTrueSpuriousLenient());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Single Correct Strict: "+es.getSingleCorrectStrict());
    out.println(expandedEvaluationId+" set="+set+", type="+type+ts+"Single Correct Lenient: "+es.getSingleCorrectLenient());
  }
  
  // TODO: make this work per type once we collect the tables per type!
  public void outputContingencyTable(PrintStream out, ContingencyTableInteger table) {
    out.println(expandedEvaluationId+" "+table.getName()+"correct/correct: "+table.get(0, 0));
    out.println(expandedEvaluationId+" "+table.getName()+"correct/wrong: "+table.get(0, 1));
    out.println(expandedEvaluationId+" "+table.getName()+"wrong/correct: "+table.get(1, 0));
    out.println(expandedEvaluationId+" "+table.getName()+"wrong/wrong: "+table.get(1, 1));    
  }
  
  
  
  
  protected static class AnnotationTypeSpec {
    public String keyType = "";
    public String keyElementType = "";
    public String responseType = "";
    public String responseElementType = "";
    public String toString() {
      String k = keyType;
      String r = responseType;
      if(!keyElementType.isEmpty()) {
        k += "["+keyElementType+"]";
      }
      if(!responseElementType.isEmpty()) {
        r += "["+responseElementType+"]";
      }
      if(k.equals(r)) {
        return k;
      } else {
        return k+"|"+r;
      }
    }
  }

  
  
}
