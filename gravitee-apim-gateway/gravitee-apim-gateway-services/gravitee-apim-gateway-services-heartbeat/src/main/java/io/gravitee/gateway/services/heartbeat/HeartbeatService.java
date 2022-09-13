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
package io.gravitee.gateway.services.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Version;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.heartbeat.event.InstanceEventPayload;
import io.gravitee.gateway.services.heartbeat.event.Plugin;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Organization;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author GraviteeSource Team
 */
public class HeartbeatService extends AbstractService<HeartbeatService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    public static final String EVENT_LAST_HEARTBEAT_PROPERTY = "last_heartbeat_at";
    public static final String EVENT_STARTED_AT_PROPERTY = "started_at";
    public static final String EVENT_STOPPED_AT_PROPERTY = "stopped_at";
    public static final String EVENT_ID_PROPERTY = "id";

    private final HeartbeatStrategy heartbeatStrategy;
    private final boolean enabled;
    private final Node node;
    private final GatewayConfiguration gatewayConfiguration;
    private final PluginRegistry pluginRegistry;
    private final ObjectMapper objectMapper;
    private final OrganizationRepository organizationRepository;
    private final EnvironmentRepository environmentRepository;
    private final String port;
    private final boolean storeSystemProperties;

    public HeartbeatService(HeartbeatStrategy heartbeatStrategy, HeartbeatStrategyConfiguration heartbeatStrategyConfiguration) {
        this.enabled = heartbeatStrategyConfiguration.isEnabled();
        this.heartbeatStrategy = heartbeatStrategy;
        this.node = heartbeatStrategyConfiguration.getNode();
        this.gatewayConfiguration = heartbeatStrategyConfiguration.getGatewayConfiguration();
        this.pluginRegistry = heartbeatStrategyConfiguration.getPluginRegistry();
        this.objectMapper = heartbeatStrategyConfiguration.getObjectMapper();
        this.organizationRepository = heartbeatStrategyConfiguration.getOrganizationRepository();
        this.environmentRepository = heartbeatStrategyConfiguration.getEnvironmentRepository();
        this.port = heartbeatStrategyConfiguration.getPort();
        this.storeSystemProperties = heartbeatStrategyConfiguration.isStoreSystemProperties();
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            heartbeatStrategy.doStart(prepareEvent());
        }
    }

    @Override
    public HeartbeatService preStop() throws Exception {
        if (enabled) {
            heartbeatStrategy.preStop();
        }
        return this;
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            heartbeatStrategy.doStop();
        }
    }

    @Override
    protected String name() {
        return "Gateway Heartbeat";
    }

    private Event prepareEvent() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setType(EventType.GATEWAY_STARTED);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());
        final Map<String, String> properties = new HashMap<>();
        properties.put(EVENT_ID_PROPERTY, node.id());

        final String now = Long.toString(event.getCreatedAt().getTime());
        properties.put(EVENT_STARTED_AT_PROPERTY, now);
        properties.put(EVENT_LAST_HEARTBEAT_PROPERTY, now);

        prepareOrganizationsAndEnvironmentsProperties(event, properties);

        event.setProperties(properties);

        InstanceEventPayload instance = createInstanceInfo();

        try {
            String payload = objectMapper.writeValueAsString(instance);
            event.setPayload(payload);
        } catch (JsonProcessingException jsex) {
            LOGGER.error("An error occurs while transforming instance information into JSON", jsex);
        }
        return event;
    }

    private void prepareOrganizationsAndEnvironmentsProperties(final Event event, final Map<String, String> properties)
        throws TechnicalException {
        final Optional<List<String>> optOrganizationsList = gatewayConfiguration.organizations();
        final Optional<List<String>> optEnvironmentsList = gatewayConfiguration.environments();

        Set<String> organizationsHrids = optOrganizationsList.map(HashSet::new).orElseGet(HashSet::new);
        Set<String> environmentsHrids = optEnvironmentsList.map(HashSet::new).orElseGet(HashSet::new);

        Set<String> organizationsIds = new HashSet<>();
        if (!organizationsHrids.isEmpty()) {
            final Set<Organization> orgs = organizationRepository.findByHrids(organizationsHrids);
            organizationsIds = orgs.stream().map(Organization::getId).collect(Collectors.toSet());
        }

        Set<Environment> environments;
        if (organizationsIds.isEmpty() && environmentsHrids.isEmpty()) {
            environments = environmentRepository.findAll();
        } else {
            environments = environmentRepository.findByOrganizationsAndHrids(organizationsIds, environmentsHrids);
        }

        Set<String> environmentsIds = environments.stream().map(Environment::getId).collect(Collectors.toSet());

        // The first time APIM starts, if the Gateway is launched before the environments collection is created by the Rest API, then environmentsIds will be empty.
        // We must put at least "DEFAULT" environment.
        if (environmentsIds.isEmpty()) {
            environmentsIds.add("DEFAULT");
            environmentsHrids = environmentsHrids.isEmpty() ? Collections.singleton("DEFAULT") : environmentsHrids;
        }
        event.setEnvironments(environmentsIds);

        properties.put(Event.EventProperties.ENVIRONMENTS_HRIDS_PROPERTY.getValue(), String.join(", ", environmentsHrids));
        properties.put(Event.EventProperties.ORGANIZATIONS_HRIDS_PROPERTY.getValue(), String.join(", ", organizationsHrids));
    }

    private InstanceEventPayload createInstanceInfo() {
        InstanceEventPayload instanceInfo = new InstanceEventPayload();

        instanceInfo.setId(node.id());
        instanceInfo.setVersion(Version.RUNTIME_VERSION.toString());

        Optional<List<String>> shardingTags = gatewayConfiguration.shardingTags();
        instanceInfo.setTags(shardingTags.orElse(null));

        instanceInfo.setPlugins(plugins());
        instanceInfo.setSystemProperties(getSystemProperties());
        instanceInfo.setPort(port);

        Optional<String> tenant = gatewayConfiguration.tenant();
        instanceInfo.setTenant(tenant.orElse(null));

        try {
            instanceInfo.setHostname(InetAddress.getLocalHost().getHostName());
            instanceInfo.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException uhe) {
            LOGGER.warn("Could not get hostname / IP", uhe);
        }

        return instanceInfo;
    }

    private Set<Plugin> plugins() {
        return pluginRegistry
            .plugins()
            .stream()
            .map(
                regPlugin -> {
                    Plugin plugin = new Plugin();
                    plugin.setId(regPlugin.id());
                    plugin.setName(regPlugin.manifest().name());
                    plugin.setDescription(regPlugin.manifest().description());
                    plugin.setVersion(regPlugin.manifest().version());
                    plugin.setType(regPlugin.type().toLowerCase());
                    plugin.setPlugin(regPlugin.clazz());
                    return plugin;
                }
            )
            .collect(Collectors.toSet());
    }

    private Map getSystemProperties() {
        if (storeSystemProperties) {
            return System
                .getProperties()
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().toString().toUpperCase().startsWith("GRAVITEE"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return Collections.emptyMap();
    }
}
