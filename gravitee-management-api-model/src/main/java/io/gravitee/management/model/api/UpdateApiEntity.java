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
package io.gravitee.management.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.ResponseTemplates;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.management.model.Visibility;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class UpdateApiEntity {

    @NotNull
    private String name;

    @NotNull
    private String version;

    @NotNull
    private String description;

    @NotNull
    @JsonProperty(value = "proxy", required = true)
    private Proxy proxy;

    @JsonProperty(value = "paths", required = true)
    private Map<String, Path> paths = new HashMap<>();

    private Services services;

    private List<Resource> resources = new ArrayList<>();

    @JsonProperty(value = "properties")
    private io.gravitee.definition.model.Properties properties;

    @NotNull
    private Visibility visibility;

    private Set<String> tags;
    
    private String picture;

    private Set<String> views;

    private List<String> labels;

    private Set<String> groups;

    @JsonProperty(value = "path_mappings")
    private Set<String> pathMappings;

    @JsonProperty(value = "response_templates")
    private Map<String, ResponseTemplates> responseTemplates;

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

    public Map<String, Path> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Path> paths) {
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

    public Set<String> getViews() {
        return views;
    }

    public void setViews(Set<String> views) {
        this.views = views;
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

    public Map<String, ResponseTemplates> getResponseTemplates() {
        return responseTemplates;
    }

    public void setResponseTemplates(Map<String, ResponseTemplates> responseTemplates) {
        this.responseTemplates = responseTemplates;
    }
}
