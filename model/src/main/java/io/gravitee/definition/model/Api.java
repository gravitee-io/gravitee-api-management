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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api implements Serializable {

    private String id;
    private String name;
    private String version = "undefined";
    private FlowMode flowMode = FlowMode.DEFAULT;
    private DefinitionVersion definitionVersion;
    private Proxy proxy;
    private Services services = new Services();
    private List<Resource> resources = new ArrayList<>();
    private Map<String, List<Rule>> paths;
    private List<Flow> flows;
    private Properties properties;
    private Set<String> tags = new HashSet<>();
    private Map<String, Pattern> pathMappings = new LinkedHashMap<>();
    private Map<String, Map<String, ResponseTemplate>> responseTemplates = new LinkedHashMap<>();
    private Map<String, Plan> plans = new HashMap<>();

    public Api() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;

    }

    @JsonGetter("paths")
    public Map<String, List<Rule>> getPathsJson() {
        if (this.getDefinitionVersion() == DefinitionVersion.V1) {
            return getPaths();
        }
        return null;
    }

    @JsonIgnore
    public Map<String, List<Rule>> getPaths() {
        if (paths != null) {
            Map<String, List<Rule>> filteredPaths = new HashMap<>();
            for (Map.Entry<String, List<Rule>> entry : paths.entrySet()) {
                filteredPaths.put(entry.getKey(), entry.getValue().stream().filter(rule -> rule.getPolicy() != null).collect(Collectors.toList()));
            }
            return filteredPaths;
        }
        return null;
    }

    @JsonSetter("paths")
    public void setPaths(Map<String, List<Rule>> paths) {
        this.paths = paths;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Properties getProperties() {
        return properties;
    }

    @JsonIgnore
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @JsonSetter("properties")
    @JsonDeserialize(using = PropertiesDeserializer.class)
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

    @JsonIgnore
    public Services getServices() {
        return services;
    }

    @JsonGetter("services")
    public Services getServicesJson() {
        if(services != null && !services.isEmpty()) {
            return services;
        }
        return null;
    }

    @JsonSetter
    public void setServices(Services services) {
        this.services = services;
        if (services != null) {
            // Add compatibility with Definition 1.22
            EndpointDiscoveryService discoveryService = services.get(EndpointDiscoveryService.class);
            if (discoveryService != null) {
                services.remove(EndpointDiscoveryService.class);
                Set<EndpointGroup> endpointGroups = proxy.getGroups();
                if (endpointGroups != null && !endpointGroups.isEmpty()) {
                    EndpointGroup defaultGroup = endpointGroups.iterator().next();
                    if (defaultGroup.getServices() == null) {
                        defaultGroup.setServices(new Services());
                    }
                    defaultGroup.getServices().put(EndpointDiscoveryService.class, discoveryService);
                }
            }
        }
    }

    public <T extends Service> T getService(Class<T> serviceClass) {
        return this.services.get(serviceClass);
    }

    @JsonIgnore
    public Set<String> getTags() {
        return tags;
    }

    @JsonGetter("tags")
    public Set<String> getTagsForJson() {
        if (tags != null && tags.isEmpty()) {
            return null;
        }
        return tags;
    }

    @JsonSetter
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    public List<Resource> getResources() {
        return resources;
    }

    @JsonGetter("resources")
    public List<Resource> getResourcesForJson() {
        if (resources != null && resources.isEmpty()) {
            return null;
        }
        return resources;
    }

    @JsonSetter
    public void setResources(List<Resource> resources) {
        Set<String> distinct = new HashSet<>(resources.size());
        resources.removeIf(p -> !distinct.add(p.getName()));
        this.resources = resources;
    }

    @JsonIgnore
    public Map<String, Pattern> getPathMappings() {
        return pathMappings;
    }

    @JsonIgnore
    public void setPathMappings(Map<String, Pattern> pathMappings) {
        this.pathMappings = pathMappings;
    }

    @JsonGetter("path_mappings")
    public Set<String> getPathMappingsJson() {
        if (pathMappings != null && !pathMappings.isEmpty()) {
            return pathMappings.keySet();
        }
        return null;
    }

    @JsonSetter("path_mappings")
    public void setPathMappingsJson(Set<String> pathMappings) {
        if (pathMappings == null) {
            this.pathMappings = null;
            return;
        }
        this.pathMappings = new LinkedHashMap<>();
        pathMappings.forEach(pathMapping -> this.pathMappings.put(
                pathMapping,
                Pattern.compile(pathMapping.replaceAll(":\\w*", "[^\\/]*") + "/*")
        ));
    }

    @JsonGetter("response_templates")
    public Map<String, Map<String, ResponseTemplate>> getResponseTemplatesJson() {
        if (responseTemplates != null && !responseTemplates.isEmpty()) {
            return responseTemplates;
        }
        return null;
    }

    @JsonIgnore
    public Map<String, Map<String, ResponseTemplate>> getResponseTemplates() {
        return responseTemplates;
    }

    @JsonSetter("response_templates")
    public void setResponseTemplates(Map<String, Map<String, ResponseTemplate>> responseTemplates) {
        this.responseTemplates = responseTemplates;
    }

    @JsonGetter("flow_mode")
    public FlowMode getFlowMode() {
        return flowMode;
    }

    @JsonSetter("flow_mode")
    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }

    @JsonGetter("flows")
    public List<Flow> getFlowsJson() {
        if (this.getDefinitionVersion() == DefinitionVersion.V2) {
            return flows;
        }
        return null;
    }

    @JsonIgnore
    public List<Flow> getFlows() {
        return flows;
    }

    @JsonSetter("flows")
    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    @JsonGetter("gravitee")
    public DefinitionVersion getDefinitionVersion() {
        return definitionVersion == null ? DefinitionVersion.V1 : definitionVersion;
    }

    public void setDefinitionVersion(DefinitionVersion definitionVersion) {
        this.definitionVersion = definitionVersion == null ? DefinitionVersion.V1 : definitionVersion;
    }

    public Plan getPlan(String plan) {
        return plans.get(plan);
    }

    @JsonIgnore
    public List<Plan> getPlans() {
        return new ArrayList<>(plans.values());
    }

    @JsonGetter("plans")
    public List<Plan> getPlansForJson() {
        if (this.getDefinitionVersion() == DefinitionVersion.V2 && plans != null && !plans.isEmpty()) {
            return new ArrayList<>(plans.values());
        }
        return null;
    }

    @JsonSetter
    public void setPlans(List<Plan> plans) {
        this.plans.clear();
        for (Plan plan : plans) {
            this.plans.put(plan.getId(), plan);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Api api = (Api) o;
        return Objects.equals(id, api.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toString() {
        return "Api{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
