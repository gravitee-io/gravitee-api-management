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
package io.gravitee.definition.model.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.Service;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class Services implements Serializable {

    @JsonIgnore
    private final Map<Class<? extends Service>, Service> services = new HashMap<>();

    @JsonIgnore
    public Collection<Service> getAll() {
        return services.values();
    }

    @JsonIgnore
    public <T extends Service> T get(Class<T> serviceType) {
        //noinspection unchecked
        return (T) services.get(serviceType);
    }

    @JsonIgnore
    public void set(Collection<? extends Service> services) {
        services.forEach((Consumer<Service>) service -> Services.this.services.put(service.getClass(), service));
    }

    @JsonIgnore
    public void put(Class<? extends Service> clazz, Service service) {
        this.services.put(clazz, service);
    }

    @JsonIgnore
    public void remove(Class<? extends Service> clazz) {
        this.services.remove(clazz);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return services.isEmpty();
    }

    @JsonProperty("discovery")
    public EndpointDiscoveryService getDiscoveryService() {
        return get(EndpointDiscoveryService.class);
    }

    public void setDiscoveryService(EndpointDiscoveryService discoveryService) {
        if (discoveryService != null) {
            put(EndpointDiscoveryService.class, discoveryService);
        }
    }

    @JsonProperty("health-check")
    public HealthCheckService getHealthCheckService() {
        return get(HealthCheckService.class);
    }

    public void setHealthCheckService(HealthCheckService healthCheckService) {
        if (healthCheckService != null) {
            put(HealthCheckService.class, healthCheckService);
        }
    }

    @JsonProperty("dynamic-property")
    public DynamicPropertyService getDynamicPropertyService() {
        return get(DynamicPropertyService.class);
    }

    public void setDynamicPropertyService(DynamicPropertyService dynamicPropertyService) {
        if (dynamicPropertyService != null) {
            put(DynamicPropertyService.class, dynamicPropertyService);
        }
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream.of(
            Optional.ofNullable(this.getDiscoveryService())
                .filter(Service::isEnabled)
                .map(s -> new Plugin("service_discovery", s.getProvider())),
            Optional.ofNullable(this.getHealthCheckService())
                .filter(Service::isEnabled)
                .map(s -> new Plugin("service", "healthcheck")),
            Optional.ofNullable(this.getDynamicPropertyService())
                .filter(Service::isEnabled)
                .map(s -> new Plugin("service", "mgmt-service-dynamicproperties"))
        )
            .filter(Optional::isPresent)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }
}
