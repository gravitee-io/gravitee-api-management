/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.gateway.core.definition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.core.definition.jackson.PathDefinitionDeserializer;
import io.gravitee.gateway.core.definition.jackson.SubPathDefinitionDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@JsonDeserialize(using = SubPathDefinitionDeserializer.class)
public class SubPathDefinition {

    private HttpMethod [] methods;

    private Map<String, Policy> policies = new HashMap<>();

    public HttpMethod[] getMethods() {
        return methods;
    }

    public void setMethods(HttpMethod[] methods) {
        this.methods = methods;
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, Policy> policies) {
        this.policies = policies;
    }
}
