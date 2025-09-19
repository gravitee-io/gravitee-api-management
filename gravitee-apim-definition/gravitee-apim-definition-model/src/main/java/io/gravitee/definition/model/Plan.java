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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
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

    public Plan flows(List<Flow> flows) {
        this.flows = flows;
        return this;
    }
}
