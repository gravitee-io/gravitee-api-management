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
package io.gravitee.gateway.services.heartbeat.impl.hazelcast;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.message.Message;
import io.gravitee.node.api.message.MessageProducer;
import io.gravitee.node.api.message.Topic;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HazelcastHeartbeatStrategyTest {

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HeartbeatStrategyConfiguration heartbeatStrategyConfiguration;

    @Mock
    private Topic<Event> topic;

    @Mock
    private Message<Event> message;

    private HazelcastHeartbeatStrategy cut;

    @BeforeEach
    public void setUp() {
        when(heartbeatStrategyConfiguration.getDelay()).thenReturn(5000);
        when(heartbeatStrategyConfiguration.getUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(heartbeatStrategyConfiguration.getEventRepository()).thenReturn(eventRepository);

        when(messageProducer.<Event>getTopic(HazelcastHeartbeatStrategy.HEARTBEATS)).thenReturn(topic);
        when(topic.addMessageConsumer(any())).thenReturn(null);

        cut = new HazelcastHeartbeatStrategy(heartbeatStrategyConfiguration, clusterManager, messageProducer);
    }

    @Test
    void shouldCreateOrPatchEvent() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        final Map<String, String> properties = new HashMap<>();
        event.setProperties(properties);

        when(clusterManager.isMasterNode()).thenReturn(true);
        when(message.getMessageObject()).thenReturn(event);
        cut.onMessage(message);

        verify(eventRepository).createOrPatch(event);
    }

    @Test
    void shouldNotCreateOrPatchEventWhenNotMasterNode() throws TechnicalException {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        final Map<String, String> properties = new HashMap<>();
        event.setProperties(properties);

        when(clusterManager.isMasterNode()).thenReturn(false);
        cut.onMessage(message);

        verify(eventRepository, never()).createOrPatch(event);
    }
}
