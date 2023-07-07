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
package io.gravitee.gateway.services.heartbeat.spring.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class HeartbeatStrategyConfiguration {

    private final boolean enabled;
    private final int delay;
    private final TimeUnit unit;
    private final boolean storeSystemProperties;
    private final String port;
    private final ObjectMapper objectMapper;
    private final Node node;
    private final EnvironmentRepository environmentRepository;
    private final OrganizationRepository organizationRepository;
    private final ClusterManager clusterManager;
    private final EventRepository eventRepository;
    private final GatewayConfiguration gatewayConfiguration;
    private final PluginRegistry pluginRegistry;
}
