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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Proxy implements Serializable {

    @JsonProperty("virtual_hosts")
    private List<VirtualHost> virtualHosts;

    @JsonProperty("groups")
    private Set<EndpointGroup> groups;

    @JsonProperty("failover")
    private Failover failover;

    @JsonProperty("cors")
    private Cors cors;

    @JsonProperty("logging")
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

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Set<EndpointGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<EndpointGroup> groups) {
        this.groups = groups;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public boolean isPreserveHost() {
        return preserveHost;
    }

    public void setPreserveHost(boolean preserveHost) {
        this.preserveHost = preserveHost;
    }
}
