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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    public void afterEach() {
        cut.shutdownNow();
    }

    @Test
    void should_create_event_on_primary_node() throws TechnicalException, InterruptedException {
        when(member.primary()).thenReturn(true);
        Event event = new Event();
        event.setId("1");
        event.setType(EventType.GATEWAY_STARTED);

        CountDownLatch latch = new CountDownLatch(1);
        when(eventRepository.createOrPatch(any())).thenAnswer(invocation -> {
            latch.countDown();
            return null;
        });

        cut.onMessage(new Message<>("topic", event));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
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
    void should_ignore_any_error_when_creating_event() throws TechnicalException, InterruptedException {
        when(member.primary()).thenReturn(true);

        CountDownLatch latch = new CountDownLatch(1);
        when(eventRepository.createOrPatch(any())).thenAnswer(invocation -> {
            latch.countDown();
            throw new RuntimeException();
        });

        assertDoesNotThrow(() -> cut.onMessage(new Message<>("topic", new Event())));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(eventRepository).createOrPatch(any());
    }

    @Test
    void should_discard_heartbeat_event_when_another_is_already_being_processed() throws InterruptedException, TechnicalException {
        when(member.primary()).thenReturn(true);

        // Create a latch that will block the first event processing
        CountDownLatch processingLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(1);

        when(eventRepository.createOrPatch(any())).thenAnswer(invocation -> {
            // Signal that processing has started
            completionLatch.countDown();
            // Block until we release it
            processingLatch.await();
            return null;
        });

        // First event - this will block
        Event firstEvent = new Event();
        firstEvent.setId("1");
        firstEvent.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", firstEvent));

        // Wait for the first event to start processing
        assertThat(completionLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Second event - this should be discarded
        Event secondEvent = new Event();
        secondEvent.setId("2");
        secondEvent.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", secondEvent));

        // Verify that only the first event was processed
        verify(eventRepository, times(1)).createOrPatch(firstEvent);
        verify(eventRepository, times(1)).createOrPatch(any());

        // Release the first event to complete
        processingLatch.countDown();
    }

    @Test
    void should_process_new_event_after_previous_one_completes() throws TechnicalException, InterruptedException {
        when(member.primary()).thenReturn(true);

        // First event
        Event firstEvent = new Event();
        firstEvent.setId("1");
        firstEvent.setType(EventType.GATEWAY_STARTED);

        CountDownLatch firstLatch = new CountDownLatch(1);
        when(eventRepository.createOrPatch(firstEvent)).thenAnswer(invocation -> {
            firstLatch.countDown();
            return null;
        });

        cut.onMessage(new Message<>("topic", firstEvent));
        assertThat(firstLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Second event - should be processed after first completes
        Event secondEvent = new Event();
        secondEvent.setId("2");
        secondEvent.setType(EventType.GATEWAY_STARTED);

        CountDownLatch secondLatch = new CountDownLatch(1);
        when(eventRepository.createOrPatch(secondEvent)).thenAnswer(invocation -> {
            secondLatch.countDown();
            return null;
        });

        cut.onMessage(new Message<>("topic", secondEvent));
        assertThat(secondLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Verify both events were processed
        verify(eventRepository).createOrPatch(firstEvent);
        verify(eventRepository).createOrPatch(secondEvent);
        verify(eventRepository, times(2)).createOrPatch(any());
    }

    @Test
    void should_discard_multiple_events_when_one_is_being_processed() throws InterruptedException, TechnicalException {
        when(member.primary()).thenReturn(true);

        // Create a latch that will block the first event processing
        CountDownLatch processingLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(1);

        when(eventRepository.createOrPatch(any())).thenAnswer(invocation -> {
            // Signal that processing has started
            completionLatch.countDown();
            // Block until we release it
            processingLatch.await();
            return null;
        });

        // First event - this will block
        Event firstEvent = new Event();
        firstEvent.setId("1");
        firstEvent.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", firstEvent));

        // Wait for the first event to start processing
        assertThat(completionLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Multiple additional events - all should be discarded
        Event secondEvent = new Event();
        secondEvent.setId("2");
        secondEvent.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", secondEvent));

        Event thirdEvent = new Event();
        thirdEvent.setId("3");
        thirdEvent.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", thirdEvent));

        Event fourthEvent = new Event();
        fourthEvent.setId("4");
        fourthEvent.setType(EventType.GATEWAY_STARTED);
        cut.onMessage(new Message<>("topic", fourthEvent));

        // Verify that only the first event was processed
        verify(eventRepository, times(1)).createOrPatch(firstEvent);
        verify(eventRepository, times(1)).createOrPatch(any());

        // Release the first event to complete
        processingLatch.countDown();
    }
}
