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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties({ "endpoint", "multiTenant" }) // TODO this is currently tested?!
public class Proxy implements Serializable {

    @JsonProperty("virtual_hosts")
    private List<VirtualHost> virtualHosts;

    private Set<EndpointGroup> groups;

    @JsonUnwrapped
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private EndpointGroup commonEndpointSettings;

    private Failover failover;

    private Cors cors;

    private Logging logging;

    @JsonProperty("strip_context_path")
    private boolean stripContextPath = false;

    @JsonProperty("preserve_host")
    private boolean preserveHost = false;

    public boolean isStripContextPath() {
        return stripContextPath;
    }

    public void setStripContextPath(boolean stripContextPath) {
        this.stripContextPath = stripContextPath;
    }

    public List<VirtualHost> getVirtualHosts() {
        return virtualHosts;
    }

    public void setVirtualHosts(List<VirtualHost> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }

    public boolean failoverEnabled() {
        return failover != null;
    }

    public Failover getFailover() {
        return failover;
    }

    public void setFailover(Failover failover) {
        this.failover = failover;
    }

    @JsonIgnore
    public Cors getCors() {
        return cors;
    }

    @JsonGetter("cors")
    public Cors getCorsJson() {
        if (cors != null && cors.isEnabled()) {
            return cors;
        }
        return null;
    }

    @JsonSetter("cors")
    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Set<EndpointGroup> getGroups() {
        return groups;
    }

    public void setGroups(Collection<EndpointGroup> groups) {
        if (groups == null) {
            this.groups = null;
            return;
        }
        this.groups = new LinkedHashSet<>(groups.size());
        for (EndpointGroup group : groups) {
            if (!this.groups.add(group)) {
                throw new IllegalArgumentException("[api] API must have single endpoint group names");
            }
        }

        //check that endpoint groups and endpoints don't have the same name
        //deser have already check that group names are unique
        // and endpoint names too (in the same group)
        Set<String> endpointNames = groups.stream()
                .map(EndpointGroup::getName)
                .collect(Collectors.toSet());
        for (EndpointGroup group : groups) {
            if (group.getEndpoints() != null) {
                for (Endpoint endpoint : group.getEndpoints()) {
                    if (endpointNames.contains(endpoint.getName())) {
                        throw new IllegalArgumentException("[api] API endpoint names and group names must be unique");
                    }
                    endpointNames.add(endpoint.getName());
                }
            }
        }
    }

    @JsonSetter("endpoints")
    public void setEndpoints(List<Endpoint> endpoints) {
        EndpointGroup group = new EndpointGroup();
        group.setName("default-group");
        group.setEndpoints(endpoints);
        this.setGroups(Collections.singleton(group));
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    @JsonSetter
    public void setLoggingMode(LoggingMode loggingMode) {
        Logging logging = new Logging();
        logging.setMode(loggingMode);
        this.logging = logging;
    }

    public boolean isPreserveHost() {
        return preserveHost;
    }

    public void setPreserveHost(boolean preserveHost) {
        this.preserveHost = preserveHost;
    }

    public EndpointGroup getCommonEndpointSettings() {
        return commonEndpointSettings;
    }

    public Proxy setCommonEndpointSettings(EndpointGroup commonEndpointSettings) {
        this.commonEndpointSettings = commonEndpointSettings;
        return this;
    }

    // To ensure backward compatibility
    @JsonSetter(value = "context_path")
    private void setContextPath(String contextPath) {
        String[] parts = contextPath.split("/");
        StringBuilder finalPath = new StringBuilder("/");

        for (String part : parts) {
            if (!part.isEmpty()) {
                finalPath.append(part).append('/');
            }
        }

        String sContextPath = finalPath.deleteCharAt(finalPath.length() - 1).toString();
        VirtualHost defaultHost = new VirtualHost();
        defaultHost.setPath(sContextPath);
        setVirtualHosts(Collections.singletonList(defaultHost));
    }

}
