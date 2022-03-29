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

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api implements Serializable {

    private String id;
    private String name;
    private String version;
    private FlowMode flowMode;
    private DefinitionVersion definitionVersion;
    private Proxy proxy;
    private Services services = new Services();
    private List<Resource> resources = new ArrayList<>();
    private Map<String, Path> paths;
    private List<Flow> flows;
    private Properties properties;
    private Set<String> tags = new HashSet<>();
    private Map<String, Pattern> pathMappings = new HashMap<>();
    private Map<String, ResponseTemplates> responseTemplates = new HashMap<>();
    private Map<String, Plan> plans = new HashMap<>();

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

    public Map<String, Path> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Path> paths) {
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

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public <T extends Service> T getService(Class<T> serviceClass) {
        return this.services.get(serviceClass);
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Map<String, Pattern> getPathMappings() {
        return pathMappings;
    }

    public void setPathMappings(Map<String, Pattern> pathMappings) {
        this.pathMappings = pathMappings;
    }

    public Map<String, ResponseTemplates> getResponseTemplates() {
        return responseTemplates;
    }

    public void setResponseTemplates(Map<String, ResponseTemplates> responseTemplates) {
        this.responseTemplates = responseTemplates;
    }

    public FlowMode getFlowMode() {
        return flowMode;
    }

    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public DefinitionVersion getDefinitionVersion() {
        return definitionVersion;
    }

    public void setDefinitionVersion(DefinitionVersion definitionVersion) {
        this.definitionVersion = definitionVersion;
    }

    public Plan getPlan(String plan) {
        return plans.get(plan);
    }

    public List<Plan> getPlans() {
        return new ArrayList<>(plans.values());
    }

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
        return "Api{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", version='" + version + '\'' + '}';
    }
}
