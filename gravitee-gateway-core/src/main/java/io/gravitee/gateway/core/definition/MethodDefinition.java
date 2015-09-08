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
import io.gravitee.gateway.core.definition.jackson.MethodDefinitionDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@JsonDeserialize(using = MethodDefinitionDeserializer.class)
public class MethodDefinition {

    private HttpMethod [] methods = new HttpMethod[] {
            HttpMethod.CONNECT, HttpMethod.DELETE, HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS,
            HttpMethod.PATCH, HttpMethod.POST, HttpMethod.PUT, HttpMethod.TRACE
    };

    private List<PolicyDefinition> policies = new ArrayList<>();

    public HttpMethod[] getMethods() {
        return methods;
    }

    public void setMethods(HttpMethod[] methods) {
        this.methods = methods;
    }

    public List<PolicyDefinition> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyDefinition> policies) {
        this.policies = policies;
    }
}
