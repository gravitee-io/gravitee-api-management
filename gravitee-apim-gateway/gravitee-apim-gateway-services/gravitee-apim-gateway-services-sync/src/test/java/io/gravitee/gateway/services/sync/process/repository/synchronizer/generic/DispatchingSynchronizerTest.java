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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.generic;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.core.v4.generic.GenericEvent;
import io.gravitee.gateway.reactive.core.v4.generic.GenericEventObserver;
import io.gravitee.gateway.reactive.core.v4.generic.GenericEventObservers;
import io.gravitee.gateway.services.sync.process.repository.fetcher.GenericEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DispatchingSynchronizerTest {

    private final List<Event> stored = new ArrayList<>();
    private RuntimeException fetchError;
    private final GenericEventFetcher fetcher = new GenericEventFetcher(null) {
        @Override
        public Flowable<List<Event>> fetchLatest(final Long from, final Long to, final Set<String> environments) {
            return fetchError != null ? Flowable.error(fetchError) : Flowable.just(List.copyOf(stored));
        }
    };
    private final GenericEventObservers observers = new GenericEventObservers();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final DispatchingSynchronizer synchronizer = new DispatchingSynchronizer(fetcher, observers, executor);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void should_hand_the_raw_payload_and_its_routing_properties_to_the_observer_of_its_kind() {
        // Given
        var received = observe("runtime-overlay", true);
        observe("agent-guardian", true);
        stored.add(event("e1", "runtime-overlay", "apply", "doc-1", "{\"any\":\"json\"}"));

        // When
        sync();

        // Then
        assertThat(received).containsExactly(new GenericEvent("e1", "runtime-overlay", "apply", "doc-1", "{\"any\":\"json\"}"));
    }

    @Test
    void should_not_hand_out_again_an_event_marked_done() {
        // Given
        var received = observe("runtime-overlay", true);
        stored.add(event("e1", "runtime-overlay", "apply", "doc-1", "{}"));

        // When
        sync();
        sync();

        // Then
        assertThat(received).hasSize(1);
    }

    @Test
    void should_hand_out_again_an_event_not_done_while_the_window_covers_it() {
        // Given
        var received = observe("runtime-overlay", false);
        stored.add(event("e1", "runtime-overlay", "apply", "doc-1", "{}"));

        // When
        sync();
        sync();

        // Then
        assertThat(received).hasSize(2);
    }

    @Test
    void should_never_fail_the_sync_cycle() {
        // Given
        observers.register(
            new GenericEventObserver() {
                @Override
                public String kind() {
                    return "broken";
                }

                @Override
                public boolean onEvent(final GenericEvent event) {
                    throw new IllegalStateException("boom");
                }
            }
        );
        stored.add(event("e1", "broken", "apply", "doc-1", "{}"));
        stored.add(event("e2", "nobody-listens", "apply", "doc-2", "{}"));

        // When / Then
        synchronizer.synchronize(0L, 1L, Set.of("DEFAULT")).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        fetchError = new IllegalStateException("bridge refuses GENERIC_EVENT");
        synchronizer.synchronize(0L, 1L, Set.of("DEFAULT")).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
    }

    private void sync() {
        synchronizer.synchronize(0L, 1L, Set.of("DEFAULT")).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
    }

    private List<GenericEvent> observe(final String kind, final boolean done) {
        final List<GenericEvent> received = new ArrayList<>();
        observers.register(
            new GenericEventObserver() {
                @Override
                public String kind() {
                    return kind;
                }

                @Override
                public boolean onEvent(final GenericEvent event) {
                    received.add(event);
                    return done;
                }
            }
        );
        return received;
    }

    private static Event event(final String id, final String kind, final String action, final String documentId, final String payload) {
        final Event event = new Event();
        event.setId(id);
        event.setType(EventType.GENERIC_EVENT);
        event.setCreatedAt(new Date());
        event.setPayload(payload);
        event.setProperties(
            Map.of(
                Event.EventProperties.GENERIC_EVENT_KIND.getValue(),
                kind,
                Event.EventProperties.GENERIC_EVENT_ACTION.getValue(),
                action,
                Event.EventProperties.ID.getValue(),
                documentId
            )
        );
        return event;
    }
}
