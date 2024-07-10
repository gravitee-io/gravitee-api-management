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
package io.gravitee.rest.api.management.v2.rest.model;

import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class ApiCRDSpec {

    @NotNull
    @NotEmpty
    private String id;

    @NotNull
    @NotEmpty
    private String crossId;

    @NotNull
    @Valid
    private DefinitionContext definitionContext;

    @NotEmpty
    private String name;

    @NotEmpty
    private String version;

    @NotNull
    private ApiType type;

    private String description;

    private Set<String> tags = new HashSet<>();

    private List<@Valid Listener> listeners;

    @NotNull
    @Size(min = 1)
    private List<@Valid EndpointGroupV4> endpointGroups;

    private Analytics analytics;

    private List<@Valid Property> properties = new ArrayList<>();

    private List<@Valid Resource> resources = new ArrayList<>();

    private Map<String, @Valid PlanCRD> plans = new HashMap<>();

    private FlowExecution flowExecution;

    private List<FlowV4> flows;

    private Map<String, Map<String, ResponseTemplate>> responseTemplates = new LinkedHashMap<>();

    private ApiServices services;

    private Set<String> groups;

    private Visibility visibility;

    private GenericApi.StateEnum state;

    private PrimaryOwner primaryOwner;

    private List<String> labels = new ArrayList<>();

    private List<Metadata> metadata = new ArrayList<>();

    private ApiLifecycleState lifecycleState;

    private Set<String> categories;

    private Set<MemberCRD> members;

    private boolean notifyMembers;

    private Map<String, PageCRD> pages;

    public DefinitionVersion getDefinitionVersion() {
        return DefinitionVersion.V4;
    }
}
