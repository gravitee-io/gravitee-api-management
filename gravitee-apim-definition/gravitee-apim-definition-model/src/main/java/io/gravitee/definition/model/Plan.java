/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.flow.Flow;
import java.io.Serializable;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Plan implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("security")
    private String security;

    @JsonProperty("securityDefinition")
    private String securityDefinition;

    @Builder.Default
    @JsonProperty("paths")
    private Map<String, List<Rule>> paths = new HashMap<>();

    @JsonProperty("api")
    private String api;

    @JsonProperty("selectionRule")
    private String selectionRule;

    @Builder.Default
    @JsonProperty("flows")
    private List<Flow> flows = new ArrayList<>();

    @JsonProperty("tags")
    private Set<String> tags;

    @JsonProperty("status")
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, List<Rule>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, List<Rule>> paths) {
        this.paths = paths;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getSecurityDefinition() {
        return securityDefinition;
    }

    public void setSecurityDefinition(String securityDefinition) {
        this.securityDefinition = securityDefinition;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getSelectionRule() {
        return selectionRule;
    }

    public void setSelectionRule(String selectionRule) {
        this.selectionRule = selectionRule;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plan plan = (Plan) o;

        return id.equals(plan.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Optional.ofNullable(this.getFlows())
            .map(f -> f.stream().map(Flow::getPlugins).flatMap(List::stream).toList())
            .orElse(List.of());
    }

    @JsonIgnore
    public final boolean isApiKey() {
        return (
            this.getSecurity() != null && ("API_KEY".equalsIgnoreCase(this.getSecurity()) || "api-key".equalsIgnoreCase(this.getSecurity()))
        );
    }
}
