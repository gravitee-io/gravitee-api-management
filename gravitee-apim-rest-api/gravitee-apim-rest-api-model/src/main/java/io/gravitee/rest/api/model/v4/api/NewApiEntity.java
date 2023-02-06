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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString
@Schema(name = "NewApiEntityV4")
public class NewApiEntity {

    /**
     * OWASP HTML sanitizer to prevent XSS attacks.
     */
    private static final PolicyFactory HTML_SANITIZER = new HtmlPolicyBuilder().toFactory();

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

    @NotNull
    @Schema(description = "API's type", example = "async")
    private ApiType type;

    @NotNull
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

    @Schema(description = "API's flow execution.")
    private FlowExecution flowExecution;

    @Valid
    @Schema(description = "A list of flows containing the policies configuration.")
    private List<Flow> flows;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<@NotBlank String> groups;

    public void setName(String name) {
        this.name = HTML_SANITIZER.sanitize(name);
    }
}
