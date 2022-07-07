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
package io.gravitee.rest.api.model.v4.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.FlowMode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString
public class NewApiEntity {

    @NotNull
    @NotEmpty(message = "Api's name must not be empty")
    @Schema(description = "Api's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @NotNull
    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String apiVersion;

    @Schema(description = "API's gravitee definition version")
    private DefinitionVersion definitionVersion = DefinitionVersion.V4;

    @Schema(description = "API's type", example = "async")
    private ApiType type;

    @NotNull
    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @Schema(description = "The list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags = new HashSet<>();

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @Schema(description = "A list of listeners used to describe our you api could be reached.")
    @NotNull
    private List<Listener> listeners;

    @Schema(description = "A list of endpoint describing the endpoints to contact.")
    @NotNull
    private List<EndpointGroup> endpointGroups;

    @Schema(description = "API's flow mode.", example = "BEST_MATCH")
    private FlowMode flowMode;

    @Schema(description = "A list of flows containing the policies configuration.")
    @DeploymentRequired
    private List<Flow> flows;
}
