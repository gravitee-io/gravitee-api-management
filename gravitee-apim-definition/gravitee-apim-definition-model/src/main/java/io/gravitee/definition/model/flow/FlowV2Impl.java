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
package io.gravitee.definition.model.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Plugin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FlowV2Impl implements Serializable, FlowV2 {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path-operator")
    @Builder.Default
    private PathOperator pathOperator = new PathOperator();

    @JsonProperty("pre")
    @Builder.Default
    private List<StepV2> pre = new ArrayList<>();

    @JsonProperty("post")
    @Builder.Default
    private List<StepV2> post = new ArrayList<>();

    @JsonProperty("enabled")
    @Builder.Default
    private boolean enabled = true;

    @JsonProperty("methods")
    @Builder.Default
    private Set<HttpMethod> methods = new HashSet<>();

    @JsonProperty("condition")
    private String condition;

    @JsonProperty("consumers")
    private List<Consumer> consumers;

    /**
     * In which stage the flow is configured.
     * This data is useful to debug or improve logging.
     * Ignored because only used internally.
     */
    @JsonIgnore
    private FlowStage stage;

    @JsonIgnore
    @Override
    public String getPath() {
        return pathOperator != null ? pathOperator.getPath() : null;
    }

    @JsonIgnore
    @Override
    public Operator getOperator() {
        return pathOperator != null ? pathOperator.getOperator() : null;
    }

    @JsonIgnore
    @Override
    public List<Plugin> getPlugins() {
        return Stream.of(computePlugins(this.post), computePlugins(this.pre)).flatMap(List::stream).collect(Collectors.toList());
    }

    @JsonIgnore
    private List<Plugin> computePlugins(List<? extends Step> steps) {
        return Optional
            .ofNullable(steps)
            .map(s -> s.stream().filter(Step::isEnabled).map(Step::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
            .orElse(List.of());
    }
}
