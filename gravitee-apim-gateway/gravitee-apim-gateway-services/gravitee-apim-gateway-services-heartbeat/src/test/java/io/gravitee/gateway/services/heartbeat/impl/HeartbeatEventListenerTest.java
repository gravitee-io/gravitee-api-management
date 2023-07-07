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
package io.gravitee.gateway.services.heartbeat.impl;

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class HeartbeatEventListenerTest {

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Member member;

    private HeartbeatEventListener cut;

    @BeforeEach
    public void beforeEach() {
        when(member.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(member);
        cut = new HeartbeatEventListener(clusterManager, eventRepository);
    }

    @Test
    void should_create_event_on_primary_node() throws TechnicalException {
        when(member.primary()).thenReturn(true);
        Event event = new Event();
        event.setId("1");
        event.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", event));
        verify(eventRepository).createOrPatch(event);
    }

    @Test
    void should_not_create_event_on_secondary_node() {
        when(member.primary()).thenReturn(false);
        Event event = new Event();
        event.setId("1");
        event.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", event));
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_ignore_any_error_when_creating_event() throws TechnicalException {
        when(member.primary()).thenReturn(true);
        when(eventRepository.createOrPatch(any())).thenThrow(new RuntimeException());
        assertDoesNotThrow(() -> cut.onMessage(new Message<>("topic", new Event())));
    }
}
