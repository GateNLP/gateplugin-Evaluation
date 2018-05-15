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
