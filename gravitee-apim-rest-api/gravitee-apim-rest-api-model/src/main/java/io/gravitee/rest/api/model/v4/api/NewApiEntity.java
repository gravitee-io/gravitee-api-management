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
package io.gravitee.rest.api.model.v4.api;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
@Schema(name = "NewApiEntityV4")
public class NewApiEntity {

    @NotBlank
    @NotEmpty(message = "API's name must not be empty")
    @Schema(description = "API's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @NotBlank
    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String apiVersion;

    @NotNull
    @Schema(description = "API's gravitee definition version")
    private DefinitionVersion definitionVersion = DefinitionVersion.V4;

    @Schema(description = "API's origin context")
    private OriginContext originContext = new OriginContext.Management();

    @NotNull
    @Schema(description = "API's type", example = "async")
    private ApiType type;

    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @Schema(description = "The list of sharding tags associated with this API.", example = "public, private")
    private Set<@NotBlank String> tags;

    @NotNull
    @Valid
    @Schema(description = "A list of listeners used to describe our you api could be reached.")
    private List<@NotNull Listener> listeners;

    @NotNull
    @Valid
    @Schema(description = "A list of endpoint describing the endpoints to contact.")
    private List<EndpointGroup> endpointGroups;

    @Schema(description = "Analytics configuration")
    private Analytics analytics;

    @Schema(description = "Failover configuration")
    private Failover failover;

    @Schema(description = "API's flow execution.")
    private FlowExecution flowExecution;

    @Valid
    @Schema(description = "A list of flows containing the policies configuration.")
    private List<Flow> flows;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<@NotBlank String> groups;

    public void setName(String name) {
        this.name = HtmlSanitizer.sanitize(name);
    }

    public void setDescription(String description) {
        this.description = HtmlSanitizer.sanitize(description);
    }
}
