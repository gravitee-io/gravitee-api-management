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

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Proxy {

    public static boolean DEFAULT_DUMP_REQUEST = false;

    public static boolean DEFAULT_MULTI_TENANT = false;

    private String contextPath;

    private List<Endpoint> endpoints = new ArrayList<>();

    private LoadBalancer loadBalancer = new LoadBalancer();

    private Failover failover;

    private boolean stripContextPath = false;

    private boolean dumpRequest = DEFAULT_DUMP_REQUEST;

    private boolean multiTenant = DEFAULT_MULTI_TENANT;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public boolean isStripContextPath() {
        return stripContextPath;
    }

    public void setStripContextPath(boolean stripContextPath) {
        this.stripContextPath = stripContextPath;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
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

    public boolean isDumpRequest() {
        return dumpRequest;
    }

    public void setDumpRequest(boolean dumpRequest) {
        this.dumpRequest = dumpRequest;
    }

    public boolean isMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(boolean multiTenant) {
        this.multiTenant = multiTenant;
    }
}
