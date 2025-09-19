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

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_CLUSTER_PRIMARY_NODE_PROPERTY;
import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.repository.exceptions.TechnicalException;
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
class HeartbeatEventPublisherTest {

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private Member member;

    @Mock
    private Topic<Event> topic;

    private HeartbeatEventPublisher cut;

    @BeforeEach
    public void beforeEach() {
        when(member.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(member);
        Event event = new Event();
        event.setId("id");
        cut = new HeartbeatEventPublisher(clusterManager, topic, event);
    }

    @Test
    void should_publish_light_event() {
        cut.run();
        verify(topic).publish(
            argThat(eventPublish -> {
                assertThat(eventPublish.getCreatedAt()).isNull();
                assertThat(eventPublish.getUpdatedAt()).isNotNull();
                assertThat(eventPublish.getProperties()).containsOnly(
                    entry(EVENT_LAST_HEARTBEAT_PROPERTY, Long.toString(eventPublish.getUpdatedAt().getTime())),
                    entry(EVENT_CLUSTER_PRIMARY_NODE_PROPERTY, Boolean.TRUE.toString())
                );
                return true;
            })
        );
    }

    @Test
    void should_ignore_any_error_when_creating_event() {
        doThrow(new RuntimeException()).when(topic).publish(any());
        assertDoesNotThrow(() -> cut.run());
    }
}
