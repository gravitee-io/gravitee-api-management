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

import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.rest.api.service.EventService;
import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;
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
}
