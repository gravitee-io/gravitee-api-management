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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.gravitee.common.http.HttpMethod;
import java.io.Serializable;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Rule implements Serializable {

    private Set<HttpMethod> methods = EnumSet.allOf(HttpMethod.class);

    private Policy policy;

    private String description;

    private boolean enabled = true;

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
    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // for compatibility
    @JsonAnySetter
    private void setPolicyJson(String name, JsonNode jsonNode) {
        policy = new Policy();
        policy.setName(name);
        policy.setConfiguration(jsonNode.toString());
    }

    @JsonSerialize(contentUsing = RawSerializer.class)
    @JsonAnyGetter
    public Map<String, Object> getPolicyJson() {
        if (policy == null) {
            return null;
        }
        return Collections.singletonMap(policy.getName(), policy.getConfiguration());
    }
}
