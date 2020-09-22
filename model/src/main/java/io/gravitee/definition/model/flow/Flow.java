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

import io.gravitee.common.http.HttpMethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
public class Flow implements Serializable {

    private String name;
    private List<Step> pre = new ArrayList<>();
    private List<Step> post = new ArrayList<>();
    private boolean enabled;
    private Set<HttpMethod> methods;
    private String path;
    private Operator operator = Operator.STARTS_WITH;
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
        return methods;
    }

    public void setMethods(Set<HttpMethod> methods) {
        this.methods = methods;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }
}
