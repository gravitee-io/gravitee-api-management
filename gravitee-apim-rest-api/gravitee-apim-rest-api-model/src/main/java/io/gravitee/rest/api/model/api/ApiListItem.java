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
package io.gravitee.rest.api.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApiListItem {

    @Schema(description = "Api's uuid.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    @EqualsAndHashCode.Include
    private String id;

    @Schema(description = "Api's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    @EqualsAndHashCode.Include
    private String version;

    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String description;

    @JsonProperty("created_at")
    @Schema(description = "The date (as a timestamp) when the API was created.", example = "1581256457163")
    private Date createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "The last date (as a timestamp) when the API was updated.", example = "1581256457163")
    private Date updatedAt;

    @Schema(description = "The visibility of the API regarding the portal.", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "The status of the API regarding the gateway.", example = "STARTED")
    private Lifecycle.State state;

    @JsonProperty("owner")
    @Schema(description = "The user with role PRIMARY_OWNER on this API.")
    private PrimaryOwnerEntity primaryOwner;

    private String role;

    @JsonProperty(value = "picture_url")
    @Schema(
        description = "the API logo url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/picture"
    )
    private String pictureUrl;

    @JsonProperty(value = "virtual_hosts")
    private List<VirtualHost> virtualHosts;

    @Schema(description = "the list of categories associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @Schema(description = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    @Schema(description = "How consumers have evaluated the API (between 0 to 5)", example = "4")
    private Double rate;

    @Schema(description = "How many consumers have evaluated the API", example = "4")
    private int numberOfRatings;

    @Schema(description = "the list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    @JsonProperty(value = "workflow_state")
    private WorkflowState workflowState;

    @JsonProperty(value = "context_path")
    @Schema(description = "API's context path.", example = "/my-awesome-api")
    private String contextPath;

    @JsonProperty(value = "healthcheck_enabled")
    @Schema(description = "true if HealthCheck is enabled globally or on one endpoint")
    private boolean hasHealthCheckEnabled;

    @JsonProperty(value = "definition_context")
    @Schema(description = "the context where the api definition was created from")
    private DefinitionContext definitionContext;

    @JsonProperty(value = "gravitee")
    @Schema(description = "API's gravitee definition version")
    private String graviteeDefinitionVersion;
}
