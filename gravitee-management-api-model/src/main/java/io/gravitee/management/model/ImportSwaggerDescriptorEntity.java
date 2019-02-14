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
package io.gravitee.management.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportSwaggerDescriptorEntity {

    private Type type = Type.INLINE;

    @NotNull
    private String payload;
    @JsonProperty("with_documentation")
    private boolean withDocumentation;
    @JsonProperty("with_path_mapping")
    private boolean withPathMapping;
    @JsonProperty("with_policy_paths")
    private boolean withPolicyPaths;
    @JsonProperty("with_policy_mocks")
    private boolean withPolicyMocks;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isWithDocumentation() {
        return withDocumentation;
    }

    public void setWithDocumentation(boolean withDocumentation) {
        this.withDocumentation = withDocumentation;
    }

    public boolean isWithPathMapping() {
        return withPathMapping;
    }

    public void setWithPathMapping(boolean withPathMapping) {
        this.withPathMapping = withPathMapping;
    }

    public boolean isWithPolicyPaths() {
        return withPolicyPaths;
    }

    public void setWithPolicyPaths(boolean withPolicyPaths) {
        this.withPolicyPaths = withPolicyPaths;
    }

    public boolean isWithPolicyMocks() {
        return withPolicyMocks;
    }

    public void setWithPolicyMocks(boolean withPolicyMocks) {
        this.withPolicyMocks = withPolicyMocks;
    }

    public enum Type {
        INLINE,
        URL
    }
}
