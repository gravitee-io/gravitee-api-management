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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.MessageListener;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    @Test
    void should_persist_stopped_event_before_stop_completes_when_topic_delivers_asynchronously() throws Exception {
        DeferredTopic deferredTopic = new DeferredTopic();
        when(clusterManager.<Event>topic("heartbeats")).thenReturn(deferredTopic);
        List<String> persisted = recordPersistedEvents();
        heartbeatEvent.setId("gateway-id");
        heartbeatEvent.setType(EventType.GATEWAY_STARTED);
        cut = new HeartbeatEventScheduler(clusterManager, eventRepository, 1, TimeUnit.HOURS, heartbeatEvent);

        cut.start();
        cut.preStop();
        cut.stop();
        // Messages still in flight on the topic only reach the listener once the service is stopped
        deferredTopic.deliverPending();

        assertThat(persisted).containsExactly(EventType.GATEWAY_STOPPED + " stopped_at=" + heartbeatEvent.getUpdatedAt().getTime());
    }

    @Test
    void should_ignore_heartbeat_delivered_after_stopped_event_is_persisted() throws Exception {
        DeferredTopic deferredTopic = new DeferredTopic();
        when(clusterManager.<Event>topic("heartbeats")).thenReturn(deferredTopic);
        List<String> persisted = recordPersistedEvents();
        heartbeatEvent.setId("gateway-id");
        heartbeatEvent.setType(EventType.GATEWAY_STARTED);
        cut = new HeartbeatEventScheduler(clusterManager, eventRepository, 1, TimeUnit.HOURS, heartbeatEvent);

        cut.start();
        deferredTopic.discardPending();
        Event lateHeartbeat = new Event();
        lateHeartbeat.setId("gateway-id");
        lateHeartbeat.setType(EventType.GATEWAY_STARTED);
        deferredTopic.publish(lateHeartbeat);
        cut.preStop();
        deferredTopic.deliverPending();
        awaitListenerIdle();
        cut.stop();

        assertThat(persisted).containsExactly(EventType.GATEWAY_STOPPED + " stopped_at=" + heartbeatEvent.getUpdatedAt().getTime());
    }

    @Test
    void should_publish_stopped_event_on_topic_without_persisting_it_when_node_is_not_primary() throws Exception {
        when(member.primary()).thenReturn(false);
        heartbeatEvent.setType(EventType.GATEWAY_STARTED);
        List<EventType> published = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> published.add(invocation.<Event>getArgument(0).getType()))
            .when(topic)
            .publish(any());

        cut.start();
        cut.preStop();
        cut.stop();

        assertThat(published).containsExactly(EventType.GATEWAY_STARTED, EventType.GATEWAY_STOPPED);
        verify(eventRepository, never()).createOrPatch(any());
    }

    /**
     * The listener writes on a single thread: once a no-op task submitted after the delivered messages has run, every
     * write they triggered has run too.
     */
    private void awaitListenerIdle() throws Exception {
        ExecutorService heartbeatExecutor = (ExecutorService) ReflectionUtils.tryToReadFieldValue(
            HeartbeatEventListener.class,
            "heartbeatExecutor",
            cut.getHeartbeatEventListener()
        ).get();
        heartbeatExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);
    }

    /**
     * The scheduler keeps mutating the same event instance, so what was persisted has to be captured when the repository
     * is called, not verified afterwards.
     */
    private List<String> recordPersistedEvents() throws Exception {
        List<String> persisted = new CopyOnWriteArrayList<>();
        lenient()
            .when(eventRepository.createOrPatch(any()))
            .thenAnswer(invocation -> {
                Event event = invocation.getArgument(0);
                String stoppedAt = event.getProperties() == null ? null : event.getProperties().get(EVENT_STOPPED_AT_PROPERTY);
                persisted.add(event.getType() + " stopped_at=" + stoppedAt);
                return event;
            });
        return persisted;
    }

    /**
     * Mimics the standalone cluster topic, which hands messages to listeners on another thread, never within
     * {@link Topic#publish(Object)}: messages are only delivered when {@link #deliverPending()} is called.
     */
    private static class DeferredTopic implements Topic<Event> {

        private final Map<String, MessageListener<Event>> listeners = new ConcurrentHashMap<>();
        private final List<Event> pending = new CopyOnWriteArrayList<>();

        @Override
        public void publish(Event event) {
            pending.add(event);
        }

        @Override
        public String addMessageListener(MessageListener<Event> messageListener) {
            String subscriptionId = UUID.randomUUID().toString();
            listeners.put(subscriptionId, messageListener);
            return subscriptionId;
        }

        @Override
        public boolean removeMessageListener(String subscriptionId) {
            return listeners.remove(subscriptionId) != null;
        }

        void discardPending() {
            pending.clear();
        }

        void deliverPending() {
            for (Event event : pending) {
                pending.remove(event);
                for (MessageListener<Event> listener : listeners.values()) {
                    listener.onMessage(new Message<>("heartbeats", event));
                }
            }
        }
    }
}
