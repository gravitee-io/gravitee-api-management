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
package io.gravitee.gateway.services.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Version;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.services.heartbeat.event.InstanceEventPayload;
import io.gravitee.gateway.services.heartbeat.event.Plugin;
import io.gravitee.gateway.services.heartbeat.impl.HeartbeatEventScheduler;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class HeartbeatService extends AbstractService<HeartbeatService> {

    public static final String EVENT_LAST_HEARTBEAT_PROPERTY = "last_heartbeat_at";
    public static final String EVENT_STARTED_AT_PROPERTY = "started_at";
    public static final String EVENT_STOPPED_AT_PROPERTY = "stopped_at";
    public static final String EVENT_ID_PROPERTY = "id";
    public static final String EVENT_CLUSTER_PRIMARY_NODE_PROPERTY = "cluster_primary_node";

    private final HeartbeatStrategyConfiguration heartbeatStrategyConfiguration;
    private HeartbeatEventScheduler heartbeatStrategyScheduler;

    @Override
    protected void doStart() throws Exception {
        if (heartbeatStrategyConfiguration.enabled()) {
            heartbeatStrategyScheduler =
                new HeartbeatEventScheduler(
                    heartbeatStrategyConfiguration.clusterManager(),
                    heartbeatStrategyConfiguration.eventRepository(),
                    heartbeatStrategyConfiguration.delay(),
                    heartbeatStrategyConfiguration.unit(),
                    prepareEvent()
                );
            heartbeatStrategyScheduler.start();
        }
    }

    @Override
    public HeartbeatService preStop() throws Exception {
        if (heartbeatStrategyConfiguration.enabled()) {
            heartbeatStrategyScheduler.preStop();
        }
        return this;
    }

    @Override
    protected void doStop() throws Exception {
        if (heartbeatStrategyConfiguration.enabled()) {
            heartbeatStrategyScheduler.stop();
        }
    }

    @Override
    protected String name() {
        return "Gateway Heartbeat";
    }

    private Event prepareEvent() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.random().toString());
        event.setType(EventType.GATEWAY_STARTED);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());
        final Map<String, String> properties = new HashMap<>();
        properties.put(EVENT_ID_PROPERTY, heartbeatStrategyConfiguration.node().id());
        properties.put(
            EVENT_CLUSTER_PRIMARY_NODE_PROPERTY,
            Boolean.toString(heartbeatStrategyConfiguration.clusterManager().self().primary())
        );

        final String now = Long.toString(event.getCreatedAt().getTime());
        properties.put(EVENT_STARTED_AT_PROPERTY, now);
        properties.put(EVENT_LAST_HEARTBEAT_PROPERTY, now);

        prepareOrganizationsAndEnvironmentsProperties(event, properties);

        event.setProperties(properties);

        InstanceEventPayload instance = createInstanceInfo();

        try {
            String payload = heartbeatStrategyConfiguration.objectMapper().writeValueAsString(instance);
            event.setPayload(payload);
        } catch (JsonProcessingException jsex) {
            log.error("An error occurs while transforming instance information into JSON", jsex);
        }
        return event;
    }

    private void prepareOrganizationsAndEnvironmentsProperties(final Event event, final Map<String, String> properties) {
        event.setEnvironments((Set<String>) heartbeatStrategyConfiguration.node().metadata().get(Node.META_ENVIRONMENTS));
        event.setOrganizations((Set<String>) heartbeatStrategyConfiguration.node().metadata().get(Node.META_ORGANIZATIONS));
    }

    private InstanceEventPayload createInstanceInfo() {
        InstanceEventPayload instanceInfo = new InstanceEventPayload();

        instanceInfo.setId(heartbeatStrategyConfiguration.node().id());
        instanceInfo.setVersion(Version.RUNTIME_VERSION.toString());

        Optional<List<String>> shardingTags = heartbeatStrategyConfiguration.gatewayConfiguration().shardingTags();
        instanceInfo.setTags(shardingTags.orElse(null));

        instanceInfo.setPlugins(plugins());
        instanceInfo.setSystemProperties(getSystemProperties());
        instanceInfo.setPort(heartbeatStrategyConfiguration.port());

        Optional<String> tenant = heartbeatStrategyConfiguration.gatewayConfiguration().tenant();
        instanceInfo.setTenant(tenant.orElse(null));

        try {
            instanceInfo.setHostname(InetAddress.getLocalHost().getHostName());
            instanceInfo.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException uhe) {
            log.warn("Could not get hostname / IP", uhe);
        }

        instanceInfo.setClusterId(heartbeatStrategyConfiguration.clusterManager().clusterId());
        return instanceInfo;
    }

    private Set<Plugin> plugins() {
        return heartbeatStrategyConfiguration
            .pluginRegistry()
            .plugins()
            .stream()
            .map(regPlugin -> {
                Plugin plugin = new Plugin();
                plugin.setId(regPlugin.id());
                plugin.setName(regPlugin.manifest().name());
                plugin.setDescription(regPlugin.manifest().description());
                plugin.setVersion(regPlugin.manifest().version());
                plugin.setType(regPlugin.type().toLowerCase());
                plugin.setPlugin(regPlugin.clazz());
                return plugin;
            })
            .collect(Collectors.toSet());
    }

    private Map<String, String> getSystemProperties() {
        if (heartbeatStrategyConfiguration.storeSystemProperties()) {
            return System
                .getProperties()
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().toString().toUpperCase().startsWith("GRAVITEE"))
                .collect(Collectors.toMap(o -> o.getKey().toString(), o -> o.getValue().toString()));
        }

        return Collections.emptyMap();
    }
}
