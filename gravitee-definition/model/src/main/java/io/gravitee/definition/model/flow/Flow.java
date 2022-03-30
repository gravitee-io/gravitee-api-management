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
package io.gravitee.definition.model.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpMethod;
import java.io.Serializable;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
public class Flow implements Serializable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("path-operator")
    private PathOperator pathOperator = new PathOperator();

    @JsonProperty("pre")
    private List<Step> pre = new ArrayList<>();

    @JsonProperty("post")
    private List<Step> post = new ArrayList<>();

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("methods")
    private Set<HttpMethod> methods = new HashSet<>();

    @JsonProperty("condition")
    private String condition;

    @JsonProperty("consumers")
    private List<Consumer> consumers;

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
}
