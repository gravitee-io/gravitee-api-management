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

import static io.gravitee.gateway.services.heartbeat.impl.HeartbeatEventScheduler.HEARTBEATS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HeartbeatServiceTest {

    protected static final String NODE_ID = "NODE_ID";

    @Mock
    private HeartbeatStrategyConfiguration heartbeatStrategyConfiguration;

    @Mock
    private Node node;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private Member member;

    @Mock
    private Topic topic;

    private HeartbeatService cut;

    @BeforeEach
    void init() {
        when(node.id()).thenReturn(NODE_ID);
        when(member.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(member);
        when(clusterManager.topic(HEARTBEATS)).thenReturn(topic);

        heartbeatStrategyConfiguration =
            new HeartbeatStrategyConfiguration(
                true,
                1,
                TimeUnit.SECONDS,
                true,
                "8888",
                new ObjectMapper(),
                node,
                environmentRepository,
                organizationRepository,
                clusterManager,
                eventRepository,
                gatewayConfiguration,
                pluginRegistry
            );
        cut = new HeartbeatService(heartbeatStrategyConfiguration);
    }

    @Test
    @SneakyThrows
    void should_create_and_push_gateway_start_event_to_topic_when_starting_service() {
        cut.doStart();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(topic).publish(eventCaptor.capture());

        final Event gatewayStartedEvent = eventCaptor.getValue();

        assertThat(gatewayStartedEvent.getId()).isNotNull();
        assertThat(gatewayStartedEvent.getType()).isEqualTo(EventType.GATEWAY_STARTED);
        assertThat(gatewayStartedEvent.getProperties()).containsEntry("id", NODE_ID);
        assertThat(gatewayStartedEvent.getPayload()).isNotBlank();
        assertThat(gatewayStartedEvent.getOrganizations()).isNull();
        assertThat(gatewayStartedEvent.getEnvironments()).isNull();
        assertThat(gatewayStartedEvent.getProperties().keySet()).allMatch(k -> !k.toUpperCase(Locale.ROOT).startsWith("GRAVITEE"));
    }

    @Test
    @SneakyThrows
    void should_create_gateway_start_event_with_org_and_env_ids_and_hrids_when_gateway_is_configured_with_org_and_env_hrids() {
        when(node.metadata())
            .thenReturn(
                Map.of(
                    Node.META_ORGANIZATIONS,
                    new TreeSet<>(List.of("org-id1", "org-id2")),
                    Node.META_ENVIRONMENTS,
                    new TreeSet<>(List.of("env-id1", "env-id2"))
                )
            );

        cut.doStart();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(topic).publish(eventCaptor.capture());

        final Event gatewayStartedEvent = eventCaptor.getValue();

        assertThat(gatewayStartedEvent.getId()).isNotNull();
        assertThat(gatewayStartedEvent.getType()).isEqualTo(EventType.GATEWAY_STARTED);
        assertThat(gatewayStartedEvent.getOrganizations()).isEqualTo(Set.of("org-id1", "org-id2"));
        assertThat(gatewayStartedEvent.getEnvironments()).isEqualTo(Set.of("env-id1", "env-id2"));
    }
}
