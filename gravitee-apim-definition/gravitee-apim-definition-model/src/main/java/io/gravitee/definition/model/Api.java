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
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@ToString(of = { "id", "name", "version" })
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Api implements Serializable, ApiDefinition {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version = "undefined";

    @JsonProperty("flow_mode")
    private FlowMode flowMode = FlowMode.DEFAULT;

    @JsonProperty("gravitee")
    private DefinitionVersion definitionVersion;

    @JsonProperty(value = "definition_context")
    private DefinitionContext definitionContext;

    @JsonProperty("proxy")
    private Proxy proxy;

    @JsonProperty("services")
    private Services services = new Services();

    @JsonProperty("resources")
    private List<Resource> resources = new ArrayList<>();

    @JsonProperty("paths")
    private Map<String, List<Rule>> paths;

    @JsonProperty("flows")
    private List<Flow> flows;

    @JsonProperty("properties")
    private Properties properties;

    @JsonProperty("tags")
    private Set<String> tags = new HashSet<>();

    @JsonProperty("path_mappings")
    private Map<String, Pattern> pathMappings = new LinkedHashMap<>();

    @JsonProperty("response_templates")
    private Map<String, Map<String, ResponseTemplate>> responseTemplates = new LinkedHashMap<>();

    @JsonProperty("plans")
    private Map<String, Plan> plans = new HashMap<>();

    @JsonProperty("execution_mode")
    private ExecutionMode executionMode;

    public Api(Api other) {
        this.id = other.id;
        this.name = other.name;
        this.version = other.version;
        this.flowMode = other.flowMode;
        this.definitionVersion = other.definitionVersion;
        this.definitionContext = other.definitionContext;
        this.proxy = other.proxy;
        this.services = other.services;
        this.resources = other.resources;
        this.paths = other.paths;
        this.flows = other.flows;
        this.properties = other.properties;
        this.tags = other.tags;
        this.pathMappings = other.pathMappings;
        this.responseTemplates = other.responseTemplates;
        this.plans = other.plans;
        this.executionMode = other.executionMode;
    }

    public <T extends Service> T getService(Class<T> serviceClass) {
        return this.services.get(serviceClass);
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

    public Api plans(List<Plan> plans) {
        setPlans(plans);
        return this;
    }

    public Api flows(List<Flow> flows) {
        this.flows = flows;
        return this;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream.of(
            Optional.ofNullable(this.getResources())
                .map(r -> r.stream().filter(Resource::isEnabled).map(Resource::getPlugins).flatMap(List::stream).toList())
                .orElse(List.of()),
            Optional.ofNullable(this.getFlows())
                .map(f -> f.stream().filter(Flow::isEnabled).map(Flow::getPlugins).flatMap(List::stream).toList())
                .orElse(List.of()),
            Optional.ofNullable(this.getPlans())
                .map(p -> p.stream().map(Plan::getPlugins).flatMap(List::stream).toList())
                .orElse(List.of()),
            Optional.ofNullable(this.getServices()).map(Services::getPlugins).orElse(List.of()),
            Optional.ofNullable(this.proxy).map(Proxy::getPlugins).orElse(List.of())
        )
            .flatMap(List::stream)
            .toList();
    }
}
