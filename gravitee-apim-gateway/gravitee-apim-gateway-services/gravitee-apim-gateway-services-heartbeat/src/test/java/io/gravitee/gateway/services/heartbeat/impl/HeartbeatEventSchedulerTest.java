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
import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_STOPPED_AT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class HeartbeatEventSchedulerTest {

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private Member member;

    @Mock
    private EventRepository eventRepository;

    private HeartbeatStrategyConfiguration heartbeatStrategyConfiguration;

    @Mock
    private Topic<Event> topic;

    private HeartbeatEventScheduler cut;
    private Event heartbeatEvent;

    @BeforeEach
    public void setUp() {
        heartbeatStrategyConfiguration = new HeartbeatStrategyConfiguration(
            true,
            5000,
            TimeUnit.MILLISECONDS,
            true,
            null,
            new ObjectMapper(),
            null,
            null,
            null,
            clusterManager,
            eventRepository,
            null,
            null
        );
        lenient().when(member.primary()).thenReturn(true);
        lenient().when(clusterManager.self()).thenReturn(member);
        when(clusterManager.<Event>topic("heartbeats")).thenReturn(topic);
        heartbeatEvent = new Event();
        cut = new HeartbeatEventScheduler(
            clusterManager,
            eventRepository,
            heartbeatStrategyConfiguration.delay(),
            heartbeatStrategyConfiguration.unit(),
            heartbeatEvent
        );
    }

    @Test
    void should_start_and_publish_initial_event_and_start_scheduler() throws Exception {
        cut.start();
        ScheduledExecutorService executorService = (ScheduledExecutorService) ReflectionUtils.tryToReadFieldValue(
            HeartbeatEventScheduler.class,
            "executorService",
            cut
        ).get();

        assertThat(executorService.isShutdown()).isFalse();
        verify(topic).publish(heartbeatEvent);
    }

    @Test
    void should_prestop_and_publish_final_event() throws Exception {
        cut.start();
        cut.preStop();
        ScheduledExecutorService executorService = (ScheduledExecutorService) ReflectionUtils.tryToReadFieldValue(
            HeartbeatEventScheduler.class,
            "executorService",
            cut
        ).get();

        assertThat(executorService.isShutdown()).isFalse();
        verify(topic, times(2)).publish(
            argThat(argument -> {
                if (argument.getType() == EventType.GATEWAY_STARTED) {
                    return true;
                } else if (argument.getType() == EventType.GATEWAY_STOPPED) {
                    assertThat(argument.getProperties()).containsOnly(
                        entry(EVENT_STOPPED_AT_PROPERTY, Long.toString(argument.getUpdatedAt().getTime())),
                        entry(EVENT_CLUSTER_PRIMARY_NODE_PROPERTY, Boolean.TRUE.toString())
                    );
                    return true;
                }
                return false;
            })
        );
    }

    @Test
    void should_stop_scheduler() throws Exception {
        cut.start();
        cut.stop();
        ScheduledExecutorService executorService = (ScheduledExecutorService) ReflectionUtils.tryToReadFieldValue(
            HeartbeatEventScheduler.class,
            "executorService",
            cut
        ).get();

        // This call is made during the start
        verify(topic, times(1)).publish(any());
        assertThat(executorService.isShutdown()).isTrue();
        ExecutorService heartbeatExecutor = (ExecutorService) ReflectionUtils.tryToReadFieldValue(
            HeartbeatEventListener.class,
            "heartbeatExecutor",
            cut.getHeartbeatEventListener()
        ).get();

        assertThat(heartbeatExecutor.isShutdown()).isTrue();
    }
}
