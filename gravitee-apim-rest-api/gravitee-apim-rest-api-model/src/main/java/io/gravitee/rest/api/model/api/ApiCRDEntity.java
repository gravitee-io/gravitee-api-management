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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PropertiesEntity;
import io.gravitee.rest.api.model.PropertyEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.jackson.PropertiesEntityAsListDeserializer;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class ApiCRDEntity {

    @Schema(description = "API's ID.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    @NotNull
    @NotEmpty
    private String id;

    @Schema(description = "API's crossId. Identifies API across environments.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    @NotNull
    @NotEmpty
    private String crossId;

    @NotNull
    @NotEmpty(message = "Api's name must not be empty")
    @Schema(description = "Api's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @NotNull
    @NotEmpty
    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String version;

    @NotNull
    @NotEmpty
    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @Schema(description = "Api's execution mode. Define if the execution mode should use v3 or jupiter mode.", example = "v3")
    @DeploymentRequired
    @JsonProperty(value = "execution_mode")
    private ExecutionMode executionMode;

    @JsonProperty(value = "proxy", required = true)
    @Schema(description = "API's definition.")
    private Proxy proxy;

    @JsonProperty(value = "flows")
    @Schema(description = "a list of flows (the policies configuration)")
    private List<Flow> flows = new ArrayList<>();

    @JsonProperty(value = "plans")
    @Schema(description = "a list of plans with flows (the policies configuration)")
    private Set<PlanEntity> plans = new HashSet<>();

    @Schema(description = "The configuration of API services like the dynamic properties, the endpoint discovery or the healthcheck.")
    private Services services;

    @Schema(description = "The list of API resources used by policies like cache resources or oauth2")
    private List<Resource> resources = new ArrayList<>();

    @JsonProperty(value = "properties")
    @Schema(description = "A dictionary (could be dynamic) of properties available in the API context.")
    private PropertiesEntity properties;

    @Schema(description = "The visibility of the API regarding the portal.", example = "PUBLIC")
    private Visibility visibility = Visibility.PRIVATE;

    @Schema(description = "the list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags;

    @Schema(description = "the API logo encoded in base64")
    private String picture;

    @DeploymentRequired
    @JsonProperty(value = "gravitee")
    @Schema(description = "API's gravitee definition version")
    private String graviteeDefinitionVersion;

    @DeploymentRequired
    @JsonProperty(value = "flow_mode")
    @Schema(description = "API's flow mode.", example = "BEST_MATCH")
    private FlowMode flowMode;

    @JsonProperty("picture_url")
    @Schema(description = "the API logo URL")
    private String pictureUrl;

    @Schema(description = "the list of categories associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @Schema(description = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @JsonProperty(value = "path_mappings")
    @Schema(
        description = "A list of paths used to aggregate data in analytics",
        example = "/products/:productId, /products/:productId/media"
    )
    private Set<String> pathMappings;

    @JsonProperty(value = "response_templates")
    @Schema(
        description = "A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-ky is missing, ..."
    )
    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    private List<ApiMetadataEntity> metadata;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    private boolean notifyMembers;

    @Schema(description = "the API background encoded in base64")
    private String background;

    @JsonProperty("background_url")
    @Schema(description = "the API background URL")
    private String backgroundUrl;

    @Schema(hidden = true, description = "The API's lifecycle state has been added for the kubernetes operator only.")
    private Lifecycle.State state;

    @JsonProperty("definition_context")
    private DefinitionContext definitionContext;

    @Schema(description = "List of API members")
    private List<Member> members;

    @Schema(description = "List of API pages")
    private Map<String, PageCRD> pages;

    public void setName(String name) {
        this.name = HtmlSanitizer.sanitize(name);
    }

    public void setDescription(String description) {
        this.description = HtmlSanitizer.sanitize(description);
    }

    @JsonIgnore
    public void setProperties(PropertiesEntity properties) {
        this.properties = properties;
    }

    public boolean isDisableMembershipNotifications() {
        return !this.notifyMembers;
    }

    @JsonProperty("disable_membership_notifications")
    public void setDisableMembershipNotifications(boolean disableMembershipNotifications) {
        this.notifyMembers = !disableMembershipNotifications;
    }

    public List<PageCRD> getPages() {
        return pages == null ? List.of() : new ArrayList<>(pages.values());
    }

    public Map<String, PageCRD> getPagesMap() {
        return pages == null ? Map.of() : pages;
    }

    @JsonSetter("properties")
    @JsonDeserialize(using = PropertiesEntityAsListDeserializer.class)
    public void setPropertyList(List<PropertyEntity> properties) {
        this.properties = new PropertiesEntity(properties);
    }

    @JsonGetter("properties")
    public List<PropertyEntity> getPropertyList() {
        if (properties != null) {
            return properties.getProperties();
        }
        return Collections.emptyList();
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Member {

        private String source;
        private String sourceId;
        private String role;
    }

    @Setter
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class PageCRD extends PageEntity {

        private DefinitionContext definitionContext;
    }
}
