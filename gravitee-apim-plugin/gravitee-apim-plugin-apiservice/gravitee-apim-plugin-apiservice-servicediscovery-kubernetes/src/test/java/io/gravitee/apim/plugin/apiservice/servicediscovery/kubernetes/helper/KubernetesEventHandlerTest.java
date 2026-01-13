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
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointCriteria;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.kubernetes.client.model.v1.EndpointAddress;
import io.gravitee.kubernetes.client.model.v1.EndpointPort;
import io.gravitee.kubernetes.client.model.v1.EndpointSubset;
import io.gravitee.kubernetes.client.model.v1.Endpoints;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.KubernetesEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KubernetesEventHandlerTest {

    private static final String GROUP_NAME = "default";
    private static final String GROUP_TYPE = "http-proxy";
    private static final String DEFAULT_IP = "10.0.0.1";
    private static final int DEFAULT_PORT = 8080;

    @Test
    void shouldAddEndpointsFromSnapshot() {
        RecordingEndpointManager manager = new RecordingEndpointManager();
        KubernetesServiceDiscoveryServiceConfiguration config = new KubernetesServiceDiscoveryServiceConfiguration();
        config.setPort(DEFAULT_PORT);

        KubernetesEventHandler handler = createHandler(manager, config);
        handler.handleSnapshot(List.of(endpoints(DEFAULT_IP, DEFAULT_PORT)));

        Assertions.assertTrue(manager.endpoints.containsKey(endpointName(DEFAULT_IP, DEFAULT_PORT)));
    }

    @Test
    void shouldRemoveEndpointsOnDeleteEvent() throws InterruptedException {
        RecordingEndpointManager manager = new RecordingEndpointManager();
        KubernetesServiceDiscoveryServiceConfiguration config = new KubernetesServiceDiscoveryServiceConfiguration();
        config.setPort(DEFAULT_PORT);

        KubernetesEventHandler handler = createHandler(manager, config);
        Endpoints endpoints = endpoints(DEFAULT_IP, DEFAULT_PORT);
        handler.handleSnapshot(List.of(endpoints));

        handler.handle(new Event<>(KubernetesEventType.DELETED.name(), endpoints));

        Thread.sleep(50);

        Assertions.assertTrue(manager.disabled.contains(endpointName(DEFAULT_IP, DEFAULT_PORT)));
        Assertions.assertTrue(manager.removed.contains(endpointName(DEFAULT_IP, DEFAULT_PORT)));
    }

    @Test
    void shouldAddAllPortsWhenNoPortConfigured() {
        RecordingEndpointManager manager = new RecordingEndpointManager();
        KubernetesServiceDiscoveryServiceConfiguration config = new KubernetesServiceDiscoveryServiceConfiguration();

        KubernetesEventHandler handler = createHandler(manager, config);
        handler.handleSnapshot(List.of(endpoints(DEFAULT_IP, DEFAULT_PORT, 9090)));

        Assertions.assertTrue(manager.endpoints.containsKey(endpointName(DEFAULT_IP, DEFAULT_PORT)));
        Assertions.assertTrue(manager.endpoints.containsKey(endpointName(DEFAULT_IP, 9090)));
    }

    private KubernetesEventHandler createHandler(RecordingEndpointManager manager, KubernetesServiceDiscoveryServiceConfiguration config) {
        EndpointGroup group = EndpointGroup.builder().name(GROUP_NAME).type(GROUP_TYPE).build();
        return new KubernetesEventHandler(manager, group, config, 0, new HashMap<>());
    }

    private static String endpointName(String ip, int port) {
        return "kubernetes#" + ip + "#" + port;
    }

    private static Endpoints endpoints(String ip, int... ports) {
        EndpointAddress address = new EndpointAddress();
        address.setIp(ip);

        List<EndpointPort> endpointPorts = new ArrayList<>();
        for (int port : ports) {
            EndpointPort endpointPort = new EndpointPort();
            endpointPort.setPort(port);
            endpointPorts.add(endpointPort);
        }

        EndpointSubset subset = new EndpointSubset();
        subset.setAddresses(Collections.singletonList(address));
        subset.setPorts(endpointPorts);

        Endpoints endpoints = new Endpoints();
        endpoints.setSubsets(Collections.singletonList(subset));
        return endpoints;
    }

    private static class RecordingEndpointManager implements EndpointManager {

        private final Map<String, Endpoint> endpoints = new HashMap<>();
        private final Set<String> disabled = new HashSet<>();
        private final Set<String> removed = new HashSet<>();
        private io.gravitee.common.component.Lifecycle.State state = io.gravitee.common.component.Lifecycle.State.STARTED;

        @Override
        public void addOrUpdateEndpoint(String groupName, Endpoint endpoint) {
            endpoints.put(endpoint.getName(), endpoint);
        }

        @Override
        public void removeEndpoint(String name) {
            removed.add(name);
            endpoints.remove(name);
        }

        @Override
        public ManagedEndpoint next() {
            return null;
        }

        @Override
        public List<ManagedEndpoint> all() {
            return List.of();
        }

        @Override
        public String addListener(java.util.function.BiConsumer<EndpointManager.Event, ManagedEndpoint> endpointConsumer) {
            return "listener";
        }

        @Override
        public void removeListener(String listenerId) {}

        @Override
        public ManagedEndpoint next(EndpointCriteria criteria) {
            return null;
        }

        @Override
        public void disable(ManagedEndpoint endpoint) {
            if (endpoint != null) {
                disabled.add(endpoint.getDefinition().getName());
            }
        }

        @Override
        public void disable(String name) {
            disabled.add(name);
        }

        @Override
        public void enable(ManagedEndpoint endpoint) {}

        @Override
        public io.gravitee.common.component.Lifecycle.State lifecycleState() {
            return state;
        }

        @Override
        public EndpointManager start() {
            state = io.gravitee.common.component.Lifecycle.State.STARTED;
            return this;
        }

        @Override
        public EndpointManager stop() {
            state = io.gravitee.common.component.Lifecycle.State.STOPPED;
            return this;
        }
    }
}
