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
package io.gravitee.gateway.services.heartbeat.spring.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import java.util.concurrent.TimeUnit;

public class HeartbeatStrategyConfiguration {

    private boolean enabled;
    private int delay;
    private TimeUnit unit;
    private boolean storeSystemProperties;
    private String port;
    private ObjectMapper objectMapper;
    private Node node;
    private EnvironmentRepository environmentRepository;
    private OrganizationRepository organizationRepository;
    private EventRepository eventRepository;
    private GatewayConfiguration gatewayConfiguration;
    private PluginRegistry pluginRegistry;

    public HeartbeatStrategyConfiguration(
        boolean enabled,
        int delay,
        TimeUnit unit,
        boolean storeSystemProperties,
        String port,
        ObjectMapper objectMapper,
        Node node,
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository,
        EventRepository eventRepository,
        GatewayConfiguration gatewayConfiguration,
        PluginRegistry pluginRegistry
    ) {
        this.enabled = enabled;
        this.delay = delay;
        this.unit = unit;
        this.storeSystemProperties = storeSystemProperties;
        this.port = port;
        this.objectMapper = objectMapper;
        this.node = node;
        this.environmentRepository = environmentRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.gatewayConfiguration = gatewayConfiguration;
        this.pluginRegistry = pluginRegistry;
    }

    public HeartbeatStrategyConfiguration() {}

    public boolean isEnabled() {
        return enabled;
    }

    public int getDelay() {
        return delay;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public boolean isStoreSystemProperties() {
        return storeSystemProperties;
    }

    public String getPort() {
        return port;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public Node getNode() {
        return node;
    }

    public EnvironmentRepository getEnvironmentRepository() {
        return environmentRepository;
    }

    public OrganizationRepository getOrganizationRepository() {
        return organizationRepository;
    }

    public EventRepository getEventRepository() {
        return eventRepository;
    }

    public GatewayConfiguration getGatewayConfiguration() {
        return gatewayConfiguration;
    }

    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }
}
