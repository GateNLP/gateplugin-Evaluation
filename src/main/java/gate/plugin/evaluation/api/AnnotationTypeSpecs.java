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

import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A class representing all type specifications to use.
 */
public class AnnotationTypeSpecs {

  /**
   * Create an instance of this class from the PR parameter.
   * 
   * @param types TODO
   */
  public AnnotationTypeSpecs(List<String> types) {
    HashSet<String> seen = new HashSet<String>();
    for (String t : types) {
      if (t == null || t.isEmpty()) {
        throw new GateRuntimeException("List of annotation types to use contains a null or empty type name!");
      } else {
        // convert the entry to a AnnotationTypeSpec object
        String k;
        String r;
        if (t.contains("=")) {
          String[] tmp = t.split("=", 2);
          k = tmp[0];
          r = tmp[1];
        } else {
          k = t;
          r = t;
        }
        if(seen.contains(k)) {
          throw new GateRuntimeException("Key type cannot be used twice: "+k+"="+r);
        }
        if(seen.contains(r)) {
          throw new GateRuntimeException("Response type cannot be used twice: "+k+"="+r);
        }
        seen.add(k);
        seen.add(r);
        AnnotationTypeSpec as = new AnnotationTypeSpec(k, r);
        specs.add(as);
        key2response.put(k, r);
        response2key.put(r, k);
        keyTypes.add(k);
        responseTypes.add(r);
      }
    }
  }
  private final List<AnnotationTypeSpec> specs = new ArrayList<AnnotationTypeSpec>();
  private final List<String> responseTypes = new ArrayList<String>();
  private final List<String> keyTypes = new ArrayList<String>();
  private final Map<String, String> key2response = new HashMap<String, String>();
  private final Map<String, String> response2key = new HashMap<String, String>();

  public List<AnnotationTypeSpec> getSpecs() {
    return specs;
  }

  public String getResponseType(String keyType) {
    return key2response.get(keyType);
  }

  public List<String> getResponseTypes() {
    return responseTypes;
  }

  public String getKeyType(String responseType) {
    return response2key.get(responseType);
  }

  public List<String> getKeyTypes() {
    return keyTypes;
  }
  
  public int size() { return specs.size(); }
  
  @Override
  public String toString() {
    return specs.toString();
  }

}
