/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.evaluation.api;

/**
 * A single type specification.
 * In cases where we still use the String type, the key type is used by convention.
 */
public class AnnotationTypeSpec {
  private String keyType = "";
  private String responseType = "";

  public AnnotationTypeSpec(String keyType, String responseType) {
    this.keyType = keyType;
    this.responseType = responseType;
  }

  public String getKeyType() {
    return keyType;
  }

  public String getResponseType() {
    return responseType;
  }

  @Override
  public String toString() {
    String k = keyType;
    String r = responseType;
    if (k.equals(r)) {
      return k;
    } else {
      return k + "=" + r;
    }
  }

}
