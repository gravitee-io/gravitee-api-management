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
import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.definition.model.Plugin;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Flow implements Serializable, ConditionSupplier {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path-operator")
    @Builder.Default
    private PathOperator pathOperator = new PathOperator();

    @JsonProperty("pre")
    @Builder.Default
    private List<Step> pre = new ArrayList<>();

    @JsonProperty("post")
    @Builder.Default
    private List<Step> post = new ArrayList<>();

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
     * @return the stage of the Flow, see {@link FlowStage}.
     */
    @JsonIgnore
    private FlowStage stage;

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

    public List<Step> getPost() {
        return post;
    }

    public void setPost(List<Step> post) {
        this.post = post;
    }

    public List<Step> getPre() {
        return pre;
    }

    public void setPre(List<Step> pre) {
        this.pre = pre;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Set<HttpMethod> getMethods() {
        return methods;
    }

    public void setMethods(Set<HttpMethod> methods) {
        this.methods = methods;
    }

    public FlowStage getStage() {
        return stage;
    }

    public void setStage(FlowStage stage) {
        this.stage = stage;
    }

    @JsonIgnore
    public String getPath() {
        return pathOperator != null ? pathOperator.getPath() : null;
    }

    @JsonIgnore
    public Operator getOperator() {
        return pathOperator != null ? pathOperator.getOperator() : null;
    }

    public PathOperator getPathOperator() {
        return pathOperator;
    }

    public void setPathOperator(PathOperator pathOperator) {
        this.pathOperator = pathOperator;
    }

    public List<Consumer> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<Consumer> consumers) {
        this.consumers = consumers;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream.of(computePlugins(this.post), computePlugins(this.pre)).flatMap(List::stream).collect(Collectors.toList());
    }

    @JsonIgnore
    private List<Plugin> computePlugins(List<Step> steps) {
        return Optional.ofNullable(steps)
            .map(s -> s.stream().filter(Step::isEnabled).map(Step::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
            .orElse(List.of());
    }
}
