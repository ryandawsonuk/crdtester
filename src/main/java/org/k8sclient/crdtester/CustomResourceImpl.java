/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.k8sclient.crdtester;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.fabric8.kubernetes.client.CustomResource;

/**
 */
public class CustomResourceImpl extends CustomResource {
  private CustomResourceSpecImpl spec;

  Map<String, String> unknownFields = new HashMap<>();

  // Capture all other fields that Jackson do not match other members
  @JsonAnyGetter
  public Map<String, String> otherFields() {
    return unknownFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, String value) {
    unknownFields.put(name, value);
  }

  public CustomResourceSpecImpl getSpec() {
    return spec;
  }

  public void setSpec(CustomResourceSpecImpl spec) {
    this.spec = spec;
  }
}