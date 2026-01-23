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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(of = { "id", "name", "version" })
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ApiProduct implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version = "undefined";

    @JsonProperty("gravitee")
    private DefinitionVersion definitionVersion;

    @JsonProperty(value = "definition_context")
    private DefinitionContext definitionContext;

    @JsonProperty("apiIds")
    private List<String> apiIds = new ArrayList<>();

    @JsonProperty("plans")
    private Map<String, Plan> plans = new HashMap<>();

    public ApiProduct(ApiProduct other) {
        this.id = other.id;
        this.name = other.name;
        this.version = other.version;
        this.definitionVersion = other.definitionVersion;
        this.definitionContext = other.definitionContext;

        this.apiIds = other.apiIds;
        this.plans = other.plans;
    }

    public Plan getPlan(String plan) {
        return plans.get(plan);
    }

    public List<Plan> getPlans() {
        if (plans != null) {
            return new ArrayList<>(plans.values());
        }
        return new ArrayList<>();
    }

    public void setPlans(List<Plan> plans) {
        this.plans.clear();
        if (plans != null) {
            for (Plan plan : plans) {
                this.plans.put(plan.getId(), plan);
            }
        }
    }

    public ApiProduct plans(List<Plan> plans) {
        setPlans(plans);
        return this;
    }
}
