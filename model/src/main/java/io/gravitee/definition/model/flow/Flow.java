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

    private String name;
    private PathOperator pathOperator = new PathOperator();
    private List<Step> pre = new ArrayList<>();
    private List<Step> post = new ArrayList<>();
    private boolean enabled = true;
    private Set<HttpMethod> methods = new HashSet<>();
    private String condition;

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
        if (methods instanceof TreeSet) {
            return methods;
        }
        return new TreeSet<>(methods);
    }

    public void setMethods(Set<HttpMethod> methods) {
        if (methods instanceof TreeSet) {
            this.methods = methods;
        } else {
            this.methods = new TreeSet<>(methods);
        }
    }

    @JsonIgnore
    public String getPath() {
        return pathOperator == null ? null : pathOperator.getPath();
    }

    public void setPath(String path) {
        if (pathOperator == null) {
            pathOperator = new PathOperator();
        }
        pathOperator.setPath(path);
    }

    @JsonIgnore
    public Operator getOperator() {
        return pathOperator == null ? null : pathOperator.getOperator();
    }

    public void setOperator(Operator operator) {
        if (pathOperator == null) {
            pathOperator = new PathOperator();
        }
        pathOperator.setOperator(operator);
    }

    @JsonProperty("path-operator")
    private PathOperator getPathOperator() {
        return pathOperator;
    }

    private void setPathOperator(PathOperator pathOperator) {
        this.pathOperator = pathOperator;
    }

    private static class PathOperator implements Serializable {

        private String path = "";
        private Operator operator = Operator.STARTS_WITH;

        public String getPath() {
            return path;
        }

        public PathOperator setPath(String path) {
            this.path = path;
            return this;
        }

        public Operator getOperator() {
            return operator;
        }

        public PathOperator setOperator(Operator operator) {
            this.operator = operator;
            return this;
        }
    }
}
