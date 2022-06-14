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

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_STATE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.message.Message;
import io.gravitee.node.api.message.Topic;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HeartbeatServiceTest {

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Topic<Event> topic;

    @Mock
    private Message<Event> message;

    @InjectMocks
    private HeartbeatService cut;

    @Test
    public void shouldCreateEvent() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        final Map<String, String> properties = new HashMap<>();
        properties.put(EVENT_STATE_PROPERTY, "create");
        event.setProperties(properties);

        when(clusterManager.isMasterNode()).thenReturn(true);
        when(message.getMessageObject()).thenReturn(event);
        cut.onMessage(message);

        verify(eventRepository).create(event);
    }

    @Test
    public void shouldUpdateEvent() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setProperties(new HashMap<>());

        when(clusterManager.isMasterNode()).thenReturn(true);
        when(message.getMessageObject()).thenReturn(event);
        cut.onMessage(message);

        verify(eventRepository).update(event);
    }

    @Test
    public void shouldNotRepublishEventWhenIllegalArgumentExceptionIsThrown() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setProperties(new HashMap<>());

        when(clusterManager.isMasterNode()).thenReturn(true);
        when(message.getMessageObject()).thenReturn(event);
        when(eventRepository.update(event))
            .thenThrow(new IllegalStateException(String.format("No event found with id [%s]", event.getId())));
        cut.onMessage(message);

        verifyNoInteractions(topic);
        assertTrue(event.getProperties().isEmpty());
    }

    @Test
    public void shouldNotRepublishEventWhenTechnicalExceptionIsThrown() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setProperties(new HashMap<>());

        when(clusterManager.isMasterNode()).thenReturn(true);
        when(message.getMessageObject()).thenReturn(event);
        when(eventRepository.update(event)).thenThrow(new TechnicalException("Connection timeout"));
        cut.onMessage(message);

        verifyNoInteractions(topic);
        assertTrue(event.getProperties().isEmpty());
    }
}
