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
package io.gravitee.rest.api.model.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.Visibility;
import io.swagger.annotations.ApiModelProperty;
import java.util.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class UpdateApiEntity {

    @NotNull
    @NotEmpty(message = "Api's name must not be empty")
    @ApiModelProperty(value = "Api's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @NotNull
    @ApiModelProperty(value = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String version;

    @NotNull
    @ApiModelProperty(
        value = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @NotNull
    @JsonProperty(value = "proxy", required = true)
    @ApiModelProperty(value = "API's definition.")
    private Proxy proxy;

    @JsonProperty(value = "paths", required = true)
    @ApiModelProperty(value = "a map where you can associate a path to a configuration (the policies configuration)")
    private Map<String, List<Rule>> paths = new HashMap<>();

    @JsonProperty(value = "flows", required = true)
    @ApiModelProperty(value = "a list of flows (the policies configuration)")
    private List<Flow> flows = new ArrayList<>();

    @JsonProperty(value = "plans", required = true)
    @ApiModelProperty(value = "a list of plans with flows (the policies configuration)")
    private List<Plan> plans = new ArrayList<>();

    @ApiModelProperty(value = "The configuration of API services like the dynamic properties, the endpoint discovery or the healthcheck.")
    private Services services;

    @ApiModelProperty(value = "The list of API resources used by policies like cache resources or oauth2")
    private List<Resource> resources = new ArrayList<>();

    @JsonProperty(value = "properties")
    @ApiModelProperty(value = "A dictionary (could be dynamic) of properties available in the API context.")
    private io.gravitee.definition.model.Properties properties;

    @NotNull
    @ApiModelProperty(value = "The visibility of the API regarding the portal.", example = "PUBLIC", allowableValues = "PUBLIC, PRIVATE")
    private Visibility visibility;

    @ApiModelProperty(value = "the list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags;

    @ApiModelProperty(value = "the API logo encoded in base64")
    private String picture;

    @DeploymentRequired
    @JsonProperty(value = "gravitee", required = false)
    @ApiModelProperty(value = "API's gravitee definition version")
    private String graviteeDefinitionVersion;

    @DeploymentRequired
    @JsonProperty(value = "flow_mode")
    @ApiModelProperty(value = "API's flow mode.", example = "BEST_MATCH")
    private FlowMode flowMode;

    @JsonProperty("picture_url")
    @ApiModelProperty(value = "the API logo encoded in base64")
    private String pictureUrl;

    @ApiModelProperty(value = "the list of categories associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @ApiModelProperty(value = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    @ApiModelProperty(value = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @JsonProperty(value = "path_mappings")
    @ApiModelProperty(
        value = "A list of paths used to aggregate data in analytics",
        example = "/products/:productId, /products/:productId/media"
    )
    private Set<String> pathMappings;

    @JsonProperty(value = "response_templates")
    @ApiModelProperty(
        value = "A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-ky is missing, ..."
    )
    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    private List<ApiMetadataEntity> metadata;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    @JsonProperty("disable_membership_notifications")
    private boolean disableMembershipNotifications;

    @ApiModelProperty(value = "the API background encoded in base64")
    private String background;

    public Visibility getVisibility() {
        return visibility;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Map<String, List<Rule>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, List<Rule>> paths) {
        this.paths = paths;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @JsonSetter("properties")
    public void setPropertyList(List<Property> properties) {
        this.properties = new Properties();
        this.properties.setProperties(properties);
    }

    @JsonGetter("properties")
    public List<Property> getPropertyList() {
        if (properties != null) {
            return properties.getProperties();
        }
        return Collections.emptyList();
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Set<String> getPathMappings() {
        return pathMappings;
    }

    public void setPathMappings(Set<String> pathMappings) {
        this.pathMappings = pathMappings;
    }

    public Map<String, Map<String, ResponseTemplate>> getResponseTemplates() {
        return responseTemplates;
    }

    public void setResponseTemplates(Map<String, Map<String, ResponseTemplate>> responseTemplates) {
        this.responseTemplates = responseTemplates;
    }

    public List<ApiMetadataEntity> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<ApiMetadataEntity> metadata) {
        this.metadata = metadata;
    }

    public ApiLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(ApiLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public boolean isDisableMembershipNotifications() {
        return disableMembershipNotifications;
    }

    public void setDisableMembershipNotifications(boolean disableMembershipNotifications) {
        this.disableMembershipNotifications = disableMembershipNotifications;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public void setPlans(List<Plan> plans) {
        this.plans = plans;
    }

    public String getGraviteeDefinitionVersion() {
        return graviteeDefinitionVersion;
    }

    public void setGraviteeDefinitionVersion(String graviteeDefinitionVersion) {
        this.graviteeDefinitionVersion = graviteeDefinitionVersion;
    }

    public void addPlan(Plan plan) {
        this.plans.add(plan);
    }

    public FlowMode getFlowMode() {
        return flowMode;
    }

    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }
}
