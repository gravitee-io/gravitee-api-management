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
package io.gravitee.definition.model.v4;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.FlowMode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.responsetemplate.ResponseTemplate;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Api implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @JsonProperty(required = true)
    @NotBlank
    private ApiType type;

    @JsonProperty(required = true)
    @NotBlank
    private String apiVersion;

    @JsonProperty(required = true)
    @NotNull
    private DefinitionVersion definitionVersion = DefinitionVersion.V4;

    private Set<@NotBlank String> tags;

    @JsonProperty(required = true)
    @NotEmpty
    private List<@NotNull Listener> listeners;

    @JsonProperty(required = true)
    @NotEmpty
    private List<EndpointGroup> endpointGroups;

    private List<Property> properties;

    private List<Resource> resources;

    @JsonProperty(required = true)
    @NotNull
    private Map<@NotEmpty String, @NotNull Plan> plans;

    private FlowMode flowMode = FlowMode.DEFAULT;

    private List<Flow> flows;

    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    private ApiServices services;

    public Plan getPlan(final String plan) {
        return plans.get(plan);
    }

    public void setPlans(List<Plan> plans) {
        this.plans.clear();
        this.plans = plans.stream().collect(Collectors.toMap(Plan::getId, Function.identity()));
    }
}
