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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.core.v4.endpoint.loadbalancer.LoadBalancerStrategy;
import io.gravitee.gateway.reactive.core.v4.endpoint.loadbalancer.LoadBalancerStrategyFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultManagedEndpointGroup implements ManagedEndpointGroup {

    private final EndpointGroup definition;
    private final List<ManagedEndpoint> primaries;
    private final List<ManagedEndpoint> secondaries;
    private final LoadBalancerStrategy primaryLB;
    private final LoadBalancerStrategy secondaryLB;

    private final Map<String, ManagedEndpoint> primariesByName;
    private final Map<String, ManagedEndpoint> secondariesByName;
    private Set<ConnectorMode> supportedModes;
    private ApiType supportedApi;

    public DefaultManagedEndpointGroup(final EndpointGroup definition) {
        this.definition = definition;
        this.primaries = new CopyOnWriteArrayList<>();
        this.secondaries = new CopyOnWriteArrayList<>();
        this.primariesByName = new ConcurrentHashMap<>(1);
        this.secondariesByName = new ConcurrentHashMap<>(1);

        final LoadBalancerType loadBalancerType = definition.getLoadBalancer() != null
            ? definition.getLoadBalancer().getType()
            : LoadBalancerType.ROUND_ROBIN;
        this.primaryLB = LoadBalancerStrategyFactory.create(loadBalancerType, primaries);
        this.secondaryLB = LoadBalancerStrategyFactory.create(loadBalancerType, secondaries);
    }

    @Override
    public ManagedEndpoint next() {
        final ManagedEndpoint next = primaryLB.next();

        if (next == null) {
            return secondaryLB.next();
        }

        return next;
    }

    @Override
    public ManagedEndpoint addManagedEndpoint(ManagedEndpoint managedEndpoint) {
        final Endpoint endpointDefinition = managedEndpoint.getDefinition();

        if (endpointDefinition.isSecondary()) {
            secondaries.add(managedEndpoint);
            secondariesByName.put(endpointDefinition.getName(), managedEndpoint);
            secondaryLB.refresh();
        } else {
            primaries.add(managedEndpoint);
            primariesByName.put(endpointDefinition.getName(), managedEndpoint);
            primaryLB.refresh();
        }

        if (supportedModes == null) {
            supportedModes = managedEndpoint.getConnector().supportedModes();
            supportedApi = managedEndpoint.getConnector().supportedApi();
        }
        return managedEndpoint;
    }

    @Override
    public ManagedEndpoint removeManagedEndpoint(ManagedEndpoint managedEndpoint) {
        return this.removeManagedEndpoint(managedEndpoint.getDefinition().getName());
    }

    @Override
    public ManagedEndpoint removeManagedEndpoint(String name) {
        ManagedEndpoint managedEndpoint = primariesByName.remove(name);

        if (managedEndpoint != null) {
            primaries.remove(managedEndpoint);
            primaryLB.refresh();
        } else {
            managedEndpoint = secondariesByName.remove(name);

            if (managedEndpoint != null) {
                secondaries.remove(managedEndpoint);
            }
            secondaryLB.refresh();
        }

        return managedEndpoint;
    }

    @Override
    public EndpointGroup getDefinition() {
        return definition;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return supportedModes == null ? Collections.emptySet() : supportedModes;
    }

    @Override
    public ApiType supportedApi() {
        return supportedApi;
    }
}
