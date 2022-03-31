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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.endpoint.EndpointStatusListener;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Endpoint implements Serializable {

    private static final String DEFAULT_TYPE = "http";

    private final Set<EndpointStatusListener> listeners = new HashSet<>();

    public static final int DEFAULT_WEIGHT = 1;

    @JsonProperty("name")
    private String name;

    @JsonProperty("target")
    private String target;

    @JsonProperty("weight")
    private int weight = DEFAULT_WEIGHT;

    @JsonProperty("backup")
    private boolean backup;

    @JsonIgnore
    private Status status = Status.UP;

    @JsonProperty("tenants")
    private List<String> tenants; // TODO was not serialized

    @JsonProperty("type")
    private final String type;

    @JsonProperty("inherit")
    private Boolean inherit;

    @JsonProperty("healthcheck")
    private EndpointHealthCheckService healthCheck;

    @JsonIgnore
    private transient String configuration;

    public Endpoint(String name, String target) {
        this(null, name, target);
    }

    public Endpoint(String type, String name, String target) {
        this.type = type != null ? type : DEFAULT_TYPE;
        this.name = name;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        listeners.forEach(endpointStatusListener -> endpointStatusListener.onStatusChanged(status));
    }

    public List<String> getTenants() {
        return tenants;
    }

    public void setTenants(List<String> tenants) {
        this.tenants = tenants;
    }

    public String getType() {
        return type;
    }

    public Boolean getInherit() {
        return inherit;
    }

    public void setInherit(Boolean inherit) {
        this.inherit = inherit;
    }

    public void addEndpointAvailabilityListener(EndpointStatusListener listener) {
        listeners.add(listener);
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public EndpointHealthCheckService getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(EndpointHealthCheckService healthCheck) {
        this.healthCheck = healthCheck;
    }

    public Set<EndpointStatusListener> getEndpointAvailabilityListeners() {
        return this.listeners;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoint endpoint = (Endpoint) o;

        return name.equals(endpoint.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public enum Status {
        UP(3),
        DOWN(0),
        TRANSITIONALLY_DOWN(1),
        TRANSITIONALLY_UP(2);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }

        public boolean isDown() {
            return this == DOWN || this == TRANSITIONALLY_UP;
        }
    }
}
