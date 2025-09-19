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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventCrudServiceLegacyWrapperGroupKeyTest {

    @Mock
    EventRepository eventRepository;

    @InjectMocks
    EventCrudServiceLegacyWrapper sut;

    @Test
    void should_group_api_events_by_api_id() {
        // Given
        when(eventRepository.findEventsToClean(anyString())).thenReturn(
            Stream.of(
                new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api123")),
                new EventRepository.EventToClean("2", new EventRepository.EventToCleanGroup("PUBLISH_API", "api123")),
                new EventRepository.EventToClean("3", new EventRepository.EventToCleanGroup("PUBLISH_API", "api123"))
            )
        );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));
        // Then
        verify(eventRepository).findEventsToClean("env");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(eventRepository).delete(idsCaptor.capture());

        Collection<String> deletedIds = idsCaptor.getValue();
        assertThat(deletedIds.size()).isEqualTo(1);
        assertThat(deletedIds).containsExactly("3"); // depending on cleanup strategy
    }

    @Test
    void should_group_dictionary_events_by_dictionary_id() {
        // Given
        when(eventRepository.findEventsToClean(anyString())).thenReturn(
            Stream.of(
                new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict456")),
                new EventRepository.EventToClean("2", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict456")),
                new EventRepository.EventToClean("3", new EventRepository.EventToCleanGroup("PUBLISH_DICTIONARY", "dict456"))
            )
        );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));
        // Then
        verify(eventRepository).findEventsToClean("env");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(eventRepository).delete(idsCaptor.capture());

        Collection<String> deletedIds = idsCaptor.getValue();
        assertThat(deletedIds.size()).isEqualTo(1);
        assertThat(deletedIds).containsExactly("3"); // depending on cleanup strategy
    }

    @Test
    void should_group_gateway_events_by_gateway_id() {
        // Given
        when(eventRepository.findEventsToClean(anyString())).thenReturn(
            Stream.of(
                new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw101")),
                new EventRepository.EventToClean("2", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw101")),
                new EventRepository.EventToClean("3", new EventRepository.EventToCleanGroup("GATEWAY_STARTED", "gw101"))
            )
        );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));
        // Then
        verify(eventRepository).findEventsToClean("env");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(eventRepository).delete(idsCaptor.capture());

        Collection<String> deletedIds = idsCaptor.getValue();
        assertThat(deletedIds.size()).isEqualTo(1);
        assertThat(deletedIds).containsExactly("3"); // depending on cleanup strategy
    }

    @Test
    void should_handle_mixed_event_types() {
        // Given
        when(eventRepository.findEventsToClean(anyString())).thenReturn(
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
                new EventRepository.EventToClean("9", new EventRepository.EventToCleanGroup("PUBLISH_ORGANIZATION", "org1"))
            )
        );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));
        // Then
        verify(eventRepository).findEventsToClean("env");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(eventRepository).delete(idsCaptor.capture());

        Collection<String> deletedIds = idsCaptor.getValue();
        assertThat(deletedIds.size()).isEqualTo(3);
        assertThat(deletedIds).containsExactly("3", "6", "9"); // depending on cleanup strategy
    }

    @Test
    void should_handle_events_with_null_group_keys() throws TechnicalException {
        // Given
        when(eventRepository.findEventsToClean(anyString())).thenReturn(
            Stream.of(
                new EventRepository.EventToClean("1", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1")),
                new EventRepository.EventToClean("2", null), // Event that can't be grouped
                new EventRepository.EventToClean("3", new EventRepository.EventToCleanGroup("PUBLISH_API", "api1"))
            )
        );

        // When
        sut.cleanupEvents("env", 0, Duration.ofDays(1));
        // Then
        verify(eventRepository).findEventsToClean("env");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(eventRepository).delete(idsCaptor.capture());

        Collection<String> deletedIds = idsCaptor.getValue();

        assertThat(deletedIds.size()).isEqualTo(2);
        assertThat(deletedIds).containsExactly("1", "3"); // depending on cleanup strategy
    }
}
