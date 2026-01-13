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
package io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.helper;

import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.KubernetesServiceDiscoveryServiceConfiguration;
import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.factory.EndpointFactory;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.kubernetes.client.model.v1.EndpointAddress;
import io.gravitee.kubernetes.client.model.v1.EndpointPort;
import io.gravitee.kubernetes.client.model.v1.EndpointSubset;
import io.gravitee.kubernetes.client.model.v1.Endpoints;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.KubernetesEventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;

@CustomLog
public class KubernetesEventHandler {

    private final EndpointManager endpointManager;
    private final EndpointGroup group;
    private final KubernetesServiceDiscoveryServiceConfiguration configuration;
    private final long pendingRequestsTimeout;
    private final Map<String, Set<String>> discoveredEndpoints;

    public KubernetesEventHandler(
        EndpointManager endpointManager,
        EndpointGroup group,
        KubernetesServiceDiscoveryServiceConfiguration configuration,
        long pendingRequestsTimeout,
        Map<String, Set<String>> discoveredEndpoints
    ) {
        this.endpointManager = endpointManager;
        this.group = group;
        this.configuration = configuration;
        this.pendingRequestsTimeout = pendingRequestsTimeout;
        this.discoveredEndpoints = discoveredEndpoints;
    }

    public void handleSnapshot(List<Endpoints> endpointsList) {
        Set<String> next = new HashSet<>();
        endpointsList.forEach(endpoints -> next.addAll(upsertFromEndpoints(endpoints)));
        if (next.isEmpty()) {
            // Avoid wiping all endpoints during transient empty snapshots.
            return;
        }
        updateDiscovered(next);
    }

    public void handle(Event<Endpoints> event) {
        if (event == null || event.getObject() == null) {
            return;
        }
        if (KubernetesEventType.BOOKMARK.name().equals(event.getType())) {
            return;
        }

        if (KubernetesEventType.DELETED.name().equals(event.getType())) {
            Set<String> next = new HashSet<>(currentDiscovered());
            next.removeAll(endpointNames(event.getObject()));
            updateDiscovered(next);
            return;
        }

        Set<String> next = endpointNames(event.getObject());
        upsertFromEndpoints(event.getObject());
        if (next.isEmpty()) {
            // Avoid clearing endpoints when the update has no ready addresses.
            return;
        }
        updateDiscovered(next);
    }

    private Set<String> upsertFromEndpoints(Endpoints endpoints) {
        Set<String> names = new HashSet<>();
        forEachEndpoint(endpoints, (address, port) -> {
            var endpoint = EndpointFactory.build(group, address, port, configuration);
            endpointManager.addOrUpdateEndpoint(group.getName(), endpoint);
            names.add(EndpointFactory.endpointName(address, port));
        });
        return names;
    }

    private Set<String> endpointNames(Endpoints endpoints) {
        Set<String> names = new HashSet<>();
        forEachEndpoint(endpoints, (address, port) -> names.add(EndpointFactory.endpointName(address, port)));
        return names;
    }

    private void forEachEndpoint(Endpoints endpoints, EndpointConsumer consumer) {
        if (endpoints.getSubsets() == null) {
            return;
        }

        Integer configuredPort = configuration.getPort();
        for (EndpointSubset subset : endpoints.getSubsets()) {
            List<EndpointPort> ports = subset.getPorts();
            List<EndpointAddress> addresses = subset.getAddresses();
            if (ports == null || addresses == null) {
                continue;
            }
            if (configuredPort != null) {
                if (!containsPort(ports, configuredPort)) {
                    continue;
                }
                for (EndpointAddress address : addresses) {
                    consumer.accept(address, configuredPort);
                }
            } else {
                for (EndpointPort port : ports) {
                    int resolvedPort = port.getPort();
                    if (resolvedPort <= 0) {
                        continue;
                    }
                    for (EndpointAddress address : addresses) {
                        consumer.accept(address, resolvedPort);
                    }
                }
            }
        }
    }

    private boolean containsPort(List<EndpointPort> ports, int desiredPort) {
        return ports.stream().anyMatch(port -> port.getPort() == desiredPort);
    }

    private Set<String> currentDiscovered() {
        return new HashSet<>(discoveredEndpoints.getOrDefault(group.getName(), new HashSet<>()));
    }

    private void updateDiscovered(Set<String> next) {
        Set<String> previous = currentDiscovered();
        Set<String> removed = new HashSet<>(previous);
        removed.removeAll(next);

        removed.forEach(endpointManager::disable);
        Completable.defer(() -> {
            removed.forEach(endpointManager::removeEndpoint);
            return Completable.complete();
        })
            .onErrorComplete()
            .delaySubscription(pendingRequestsTimeout, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribe();

        discoveredEndpoints.put(group.getName(), next);
    }

    @FunctionalInterface
    private interface EndpointConsumer {
        void accept(EndpointAddress address, int port);
    }
}
