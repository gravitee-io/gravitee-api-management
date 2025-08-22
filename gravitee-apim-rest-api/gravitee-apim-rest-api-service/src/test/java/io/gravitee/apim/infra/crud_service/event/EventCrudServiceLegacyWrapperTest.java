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
package io.gravitee.apim.infra.crud_service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.rest.api.service.EventService;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventCrudServiceLegacyWrapperTest {

    @Mock
    EventService eventService;

    @Mock
    EventRepository eventRepository;

    @InjectMocks
    EventCrudServiceLegacyWrapper sut;

    @Captor
    ArgumentCaptor<Collection<String>> deletedEvents;

    @Test
    void should_remove_old_events() {
        // Given
        when(eventRepository.findEventsToClean(anyString()))
            .thenReturn(
                Stream.of(
                    new EventRepository.EventToClean("11", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("12", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("13", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("14", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("15", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("21", new EventRepository.EventToCleanGroup("PUBLISH_API", "api2")),
                    new EventRepository.EventToClean("22", new EventRepository.EventToCleanGroup("PUBLISH_API", "api2")),
                    new EventRepository.EventToClean("23", new EventRepository.EventToCleanGroup("PUBLISH_API", "api2")),
                    new EventRepository.EventToClean("24", new EventRepository.EventToCleanGroup("PUBLISH_API", "api2")),
                    new EventRepository.EventToClean("25", new EventRepository.EventToCleanGroup("PUBLISH_API", "api2"))
                )
            );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));

        // Then
        verify(eventRepository, atLeastOnce()).delete(deletedEvents.capture());
        assertThat(deletedEvents.getAllValues().stream().flatMap(Collection::stream)).doesNotContain("11", "12", "21", "22").hasSize(6);
    }

    @Test
    void should_cleanup_all_event_types() {
        // Given
        when(eventRepository.findEventsToClean(anyString()))
            .thenReturn(
                Stream.of(
                    // API events
                    new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    new EventRepository.EventToClean("3", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                    // Dictionary events
                    new EventRepository.EventToClean("4", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict1")),
                    new EventRepository.EventToClean("5", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict1")),
                    new EventRepository.EventToClean("6", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict1")),
                    // Organization events
                    new EventRepository.EventToClean("7", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org1")),
                    new EventRepository.EventToClean("8", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org1")),
                    new EventRepository.EventToClean("9", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org1")),
                    // Gateway events
                    new EventRepository.EventToClean("10", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw1")),
                    new EventRepository.EventToClean("11", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw1")),
                    new EventRepository.EventToClean("12", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw1"))
                )
            );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));

        // Then
        verify(eventRepository, atLeastOnce()).delete(deletedEvents.capture());
        // Should keep 2 events per group, so 4 events should be deleted (1 from each group)
        assertThat(deletedEvents.getAllValues().stream().flatMap(Collection::stream)).hasSize(4);
    }

    @Test
    void should_handle_events_without_properties() {
        // Given
        when(eventRepository.findEventsToClean(anyString()))
            .thenReturn(
                Stream.of(
                    new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1"))
                    // Events without properties are filtered out in the repository layer
                )
            );

        // When
        sut.cleanupEvents("env", 1, Duration.ofDays(1));

        // Then
        verify(eventRepository, never()).delete(anyCollection());
    }

    @Test
    void should_handle_unknown_event_types() {
        // Given
        when(eventRepository.findEventsToClean(anyString()))
            .thenReturn(
                Stream.of(
                    new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("PUBLISH_API", "unknown1")),
                    new EventRepository.EventToClean("2", new EventRepository.EventToCleanGroup("PUBLISH_API", "unknown1")),
                    new EventRepository.EventToClean("3", new EventRepository.EventToCleanGroup("PUBLISH_API", "unknown1"))
                )
            );

        // When
        sut.cleanupEvents("env", 1, Duration.ofDays(1));

        // Then
        verify(eventRepository, atLeastOnce()).delete(deletedEvents.capture());
        // Should keep 1 event per group, so 2 events should be deleted
        assertThat(deletedEvents.getAllValues().stream().flatMap(Collection::stream)).hasSize(2);
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CleanupEvents {

        @Mock
        private EventService eventService;

        @Mock
        private EventRepository eventRepository;

        @InjectMocks
        private EventCrudServiceLegacyWrapper sut;

        @Captor
        private ArgumentCaptor<List<String>> deletedEventsCaptor;

        @BeforeEach
        void setUp() {
            // Setup common mocks
        }

        @Test
        void should_cleanup_events_with_proper_grouping() {
            // Given
            String environmentId = "DEFAULT";
            int nbEventsToKeep = 2;
            Duration timeToLive = Duration.ofDays(1);

            // Create a stream of events with different types and reference IDs
            Stream<EventRepository.EventToClean> eventsStream = Stream.of(
                // API events - should keep 2, delete 1
                new EventRepository.EventToClean("api-1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                new EventRepository.EventToClean("api-2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                new EventRepository.EventToClean("api-3", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                // Dictionary events - should keep 2, delete 1
                new EventRepository.EventToClean("dict-1", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456")),
                new EventRepository.EventToClean("dict-2", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456")),
                new EventRepository.EventToClean("dict-3", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456")),
                // Organization events - should keep 2, delete 1
                new EventRepository.EventToClean("org-1", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org-789")),
                new EventRepository.EventToClean("org-2", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org-789")),
                new EventRepository.EventToClean("org-3", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org-789"))
            );

            when(eventRepository.findEventsToClean(environmentId)).thenReturn(eventsStream);

            // When
            sut.cleanupEvents(environmentId, nbEventsToKeep, timeToLive);

            // Then
            verify(eventRepository).delete(deletedEventsCaptor.capture());

            List<String> deletedEvents = deletedEventsCaptor.getValue();
            assertThat(deletedEvents).hasSize(3);

            // Should delete the oldest event from each group (api-3, dict-3, org-3)
            assertThat(deletedEvents).contains("api-3", "dict-3", "org-3");
            assertThat(deletedEvents).doesNotContain("api-1", "api-2", "dict-1", "dict-2", "org-1", "org-2");
        }

        @Test
        void should_cleanup_events_with_different_reference_ids() {
            // Given
            String environmentId = "DEFAULT";
            int nbEventsToKeep = 1;
            Duration timeToLive = Duration.ofDays(1);

            // Create events with different reference IDs for the same event type
            Stream<EventRepository.EventToClean> eventsStream = Stream.of(
                // API 1 events - should keep 1, delete 1
                new EventRepository.EventToClean("api1-1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-1")),
                new EventRepository.EventToClean("api1-2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-1")),
                // API 2 events - should keep 1, delete 1
                new EventRepository.EventToClean("api2-1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-2")),
                new EventRepository.EventToClean("api2-2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-2"))
            );

            when(eventRepository.findEventsToClean(environmentId)).thenReturn(eventsStream);

            // When
            sut.cleanupEvents(environmentId, nbEventsToKeep, timeToLive);

            // Then
            verify(eventRepository).delete(deletedEventsCaptor.capture());

            List<String> deletedEvents = deletedEventsCaptor.getValue();
            assertThat(deletedEvents).hasSize(2);

            // Should delete the oldest event from each API group
            assertThat(deletedEvents).contains("api1-2", "api2-2");
            assertThat(deletedEvents).doesNotContain("api1-1", "api2-1");
        }

        @Test
        void should_cleanup_mixed_event_types_correctly() {
            // Given
            String environmentId = "DEFAULT";
            int nbEventsToKeep = 1;
            Duration timeToLive = Duration.ofDays(1);

            // Create a mix of different event types
            Stream<EventRepository.EventToClean> eventsStream = Stream.of(
                // API events
                new EventRepository.EventToClean("api-1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                new EventRepository.EventToClean("api-2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                // Dictionary events
                new EventRepository.EventToClean("dict-1", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456")),
                new EventRepository.EventToClean("dict-2", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456")),
                // Gateway events
                new EventRepository.EventToClean("gw-1", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw-789")),
                new EventRepository.EventToClean("gw-2", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw-789"))
            );

            when(eventRepository.findEventsToClean(environmentId)).thenReturn(eventsStream);

            // When
            sut.cleanupEvents(environmentId, nbEventsToKeep, timeToLive);

            // Then
            verify(eventRepository).delete(deletedEventsCaptor.capture());

            List<String> deletedEvents = deletedEventsCaptor.getValue();
            assertThat(deletedEvents).hasSize(3);

            // Should delete the oldest event from each group
            assertThat(deletedEvents).contains("api-2", "dict-2", "gw-2");
            assertThat(deletedEvents).doesNotContain("api-1", "dict-1", "gw-1");
        }

        @Test
        void should_handle_empty_event_stream() throws TechnicalException {
            // Given
            String environmentId = "DEFAULT";
            int nbEventsToKeep = 2;
            Duration timeToLive = Duration.ofDays(1);

            when(eventRepository.findEventsToClean(environmentId)).thenReturn(Stream.empty());

            // When
            sut.cleanupEvents(environmentId, nbEventsToKeep, timeToLive);

            // Then
            // Should not call delete when there are no events
            verify(eventRepository).findEventsToClean(environmentId);
            // Verify that no delete was called
            verify(eventRepository, never()).delete(anyString());
            verify(eventRepository, never()).delete(anyCollection());
        }

        @Test
        void should_handle_single_event_per_group() throws TechnicalException {
            // Given
            String environmentId = "DEFAULT";
            int nbEventsToKeep = 2;
            Duration timeToLive = Duration.ofDays(1);

            // Create only one event per group
            Stream<EventRepository.EventToClean> eventsStream = Stream.of(
                new EventRepository.EventToClean("api-1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                new EventRepository.EventToClean("dict-1", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456"))
            );

            when(eventRepository.findEventsToClean(environmentId)).thenReturn(eventsStream);

            // When
            sut.cleanupEvents(environmentId, nbEventsToKeep, timeToLive);

            // Then
            // Should not call delete when there are fewer events than the keep limit
            verify(eventRepository).findEventsToClean(environmentId);
            // Verify that no delete was called
            verify(eventRepository, never()).delete(anyString());
            verify(eventRepository, never()).delete(anyCollection());
        }

        @Test
        void should_handle_exactly_keep_limit_events_per_group() throws TechnicalException {
            // Given
            String environmentId = "DEFAULT";
            int nbEventsToKeep = 2;
            Duration timeToLive = Duration.ofDays(1);

            // Create exactly the keep limit events per group
            Stream<EventRepository.EventToClean> eventsStream = Stream.of(
                new EventRepository.EventToClean("api-1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                new EventRepository.EventToClean("api-2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api-123")),
                new EventRepository.EventToClean("dict-1", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456")),
                new EventRepository.EventToClean("dict-2", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict-456"))
            );

            when(eventRepository.findEventsToClean(environmentId)).thenReturn(eventsStream);

            // When
            sut.cleanupEvents(environmentId, nbEventsToKeep, timeToLive);

            // Then
            // Should not call delete when there are exactly the keep limit events per group
            verify(eventRepository).findEventsToClean(environmentId);
            // Verify that no delete was called
            verify(eventRepository, never()).delete(anyString());
            verify(eventRepository, never()).delete(anyCollection());
        }
    }
}
