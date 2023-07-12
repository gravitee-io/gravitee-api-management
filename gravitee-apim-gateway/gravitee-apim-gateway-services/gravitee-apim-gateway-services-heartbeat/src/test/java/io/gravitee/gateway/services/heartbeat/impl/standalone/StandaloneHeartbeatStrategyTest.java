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
package io.gravitee.gateway.services.heartbeat.impl.standalone;

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class StandaloneHeartbeatStrategyTest {

    public static final String EVENT_ID = "event#1";
    public static final String EVENT_NODE_ID_PROPERTIES = "event_node_id";
    public static final String ORGANIZATIONS_PROPERTIES = "organizations";
    public static final String ENVIRONMENTS_PROPERTIES = "environments";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HeartbeatStrategyConfiguration heartbeatStrategyConfiguration;

    @Captor
    ArgumentCaptor<Event> eventCaptor;

    private StandaloneHeartbeatStrategy cut;

    @BeforeEach
    void setUp() {
        when(heartbeatStrategyConfiguration.getDelay()).thenReturn(5000);
        when(heartbeatStrategyConfiguration.getUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(heartbeatStrategyConfiguration.getEventRepository()).thenReturn(eventRepository);

        cut = new StandaloneHeartbeatStrategy(heartbeatStrategyConfiguration);
    }

    @Test
    @DisplayName("Should handle exceptions and retry create of the event")
    void shouldHandleExceptions() throws Exception {
        Event event = prepareEventToCreate();
        when(eventRepository.createOrPatch(any(Event.class))).thenThrow(new TechnicalException("Timeout"));

        // First call is done it should fail with timeout but assign the internal heartbeatEvent
        cut.eventToCreate = event;
        cut.sendHeartbeatEvent();
        verify(eventRepository, times(1)).createOrPatch(event);

        // Second should succeed and update the internal heartbeatEvent
        clearInvocations(eventRepository);
        when(eventRepository.createOrPatch(any(Event.class))).thenReturn(event);
        cut.sendHeartbeatEvent();
        verify(eventRepository, times(1)).createOrPatch(eventCaptor.capture());
        final Event savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getId()).isEqualTo(EVENT_ID);
        assertThat(savedEvent.getType()).isEqualTo(EventType.GATEWAY_STARTED);
        assertThat(savedEvent.getCreatedAt()).isEqualTo(event.getCreatedAt());
        assertThat(savedEvent.getUpdatedAt()).isEqualTo(event.getUpdatedAt());
        assertThat(savedEvent.getProperties())
            .contains(
                entry(EVENT_NODE_ID_PROPERTIES, "node_id"),
                entry(ORGANIZATIONS_PROPERTIES, "org1"),
                entry(ENVIRONMENTS_PROPERTIES, "env1")
            );
    }

    @Test
    @DisplayName("Should update event with a lite version if already created")
    void shouldUpdateWhenCreated() throws Exception {
        Event event = prepareEventToCreate();
        when(eventRepository.createOrPatch(any(Event.class))).thenReturn(event);

        cut.eventToCreate = event;
        cut.sendHeartbeatEvent();
        verify(eventRepository, times(1)).createOrPatch(eventCaptor.capture());
        final Event savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getId()).isEqualTo(EVENT_ID);
        assertThat(savedEvent.getType()).isEqualTo(EventType.GATEWAY_STARTED);
        assertThat(savedEvent.getCreatedAt()).isEqualTo(event.getCreatedAt());
        assertThat(savedEvent.getUpdatedAt()).isEqualTo(event.getUpdatedAt());
        assertThat(savedEvent.getProperties())
            .contains(
                entry(EVENT_NODE_ID_PROPERTIES, "node_id"),
                entry(ORGANIZATIONS_PROPERTIES, "org1"),
                entry(ENVIRONMENTS_PROPERTIES, "env1")
            );

        // Second should succeed and update the internal heartbeatEvent
        clearInvocations(eventRepository);
        cut.sendHeartbeatEvent();
        verify(eventRepository, times(1)).createOrPatch(eventCaptor.capture());
        final Event updatedEvent = eventCaptor.getValue();
        assertThat(updatedEvent.getId()).isEqualTo(EVENT_ID);
        assertThat(updatedEvent.getUpdatedAt()).isAfter(event.getUpdatedAt());
        assertThat(updatedEvent.getProperties()).hasSize(1).containsOnlyKeys(EVENT_LAST_HEARTBEAT_PROPERTY);
        assertThat(updatedEvent.getType()).isEqualTo(EventType.GATEWAY_STARTED);
        assertThat(updatedEvent.getEnvironments()).isEqualTo(event.getEnvironments());
        assertThat(updatedEvent.getPayload()).isNull();
    }

    private Event prepareEventToCreate() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setType(EventType.GATEWAY_STARTED);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());
        final Map<String, String> properties = new HashMap<>();
        properties.put(EVENT_NODE_ID_PROPERTIES, "node_id");
        properties.put(ORGANIZATIONS_PROPERTIES, "org1");
        properties.put(ENVIRONMENTS_PROPERTIES, "env1");
        event.setProperties(properties);
        return event;
    }
}
