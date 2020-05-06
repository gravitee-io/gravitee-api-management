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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportSwaggerDescriptorEntity {

    private Type type = Type.INLINE;

    @NotNull
    @ApiModelProperty(
            value = "The swagger/openapi content.")
    private String payload;

    @JsonProperty("with_documentation")
    @ApiModelProperty(
            value = "Do you want to create a swagger page in addition to the API ?",
            example = "true")
    private boolean withDocumentation;

    @JsonProperty("with_path_mapping")
    @ApiModelProperty(
            value = "Do you want to create a path mapping for each declared swagger paths in addition to the API ?",
            example = "true")
    private boolean withPathMapping;
    
    @JsonProperty("with_policy_paths")
    @ApiModelProperty(
            value = "Do you want to create a path (in order to add policies under) for each declared swagger paths in addition to the API ?",
            example = "true")
    private boolean withPolicyPaths;
    @JsonProperty("with_policies")
    private List<String> withPolicies;

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

    public List<String> getWithPolicies() {
        return withPolicies;
    }

    public void setWithPolicies(List<String> withPolicies) {
        this.withPolicies = withPolicies;
    }

    public enum Type {
        INLINE,
        URL
    }
}
