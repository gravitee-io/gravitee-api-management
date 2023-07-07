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
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HeartbeatConfiguration {

    @Bean
    public HeartbeatStrategyConfiguration heartbeatDependencies(
        ObjectMapper objectMapper,
        Node node,
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository,
        ClusterManager clusterManager,
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
            clusterManager,
            eventRepository,
            gatewayConfiguration,
            pluginRegistry
        );
    }
}
