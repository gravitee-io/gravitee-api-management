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
package io.gravitee.gateway.services.heartbeat.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.heartbeat.HeartbeatStrategy;
import io.gravitee.gateway.services.heartbeat.impl.hazelcast.HazelcastHeartbeatStrategy;
import io.gravitee.gateway.services.heartbeat.impl.standalone.StandaloneHeartbeatStrategy;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.message.MessageProducer;
import io.gravitee.node.cluster.spring.HazelcastClusterConfiguration;
import io.gravitee.node.cluster.spring.StandaloneClusterConfiguration;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HeartbeatConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean("heartbeatStrategy")
    @Conditional(StandaloneClusterConfiguration.StandaloneModeEnabled.class)
    public HeartbeatStrategy heartbeatStandaloneStrategy(HeartbeatStrategyConfiguration heartbeatStrategyConfiguration) {
        return new StandaloneHeartbeatStrategy(heartbeatStrategyConfiguration);
    }

    @Bean("heartbeatStrategy")
    @Conditional(HazelcastClusterConfiguration.ClusterModeEnabled.class)
    public HeartbeatStrategy heartbeatHazelcastStrategy(
        HeartbeatStrategyConfiguration heartbeatStrategyConfiguration,
        ClusterManager clusterManager,
        MessageProducer messageProducer
    ) {
        return new HazelcastHeartbeatStrategy(heartbeatStrategyConfiguration, clusterManager, messageProducer);
    }

    @Bean
    public HeartbeatStrategyConfiguration heartbeatDependencies(
        ObjectMapper objectMapper,
        Node node,
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository,
        EventRepository eventRepository,
        GatewayConfiguration gatewayConfiguration,
        PluginRegistry pluginRegistry,
        io.gravitee.node.api.configuration.Configuration configuration
    ) {
        return new HeartbeatStrategyConfiguration(
            configuration.getProperty("services.heartbeat.enabled", Boolean.class, true),
            configuration.getProperty("services.heartbeat.delay", Integer.class, 5000),
            configuration.getProperty("services.heartbeat.unit", TimeUnit.class, TimeUnit.MILLISECONDS),
            configuration.getProperty("services.heartbeat.storeSystemProperties", Boolean.class, true),
            configuration.getProperty("http.port", String.class, "8082"),
            objectMapper,
            node,
            environmentRepository,
            organizationRepository,
            eventRepository,
            gatewayConfiguration,
            pluginRegistry
        );
    }
}
