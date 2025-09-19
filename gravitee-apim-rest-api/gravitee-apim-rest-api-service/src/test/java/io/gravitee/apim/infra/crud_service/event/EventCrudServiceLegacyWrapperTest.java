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
import static org.mockito.Mockito.atLeastOnce;
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
        when(eventRepository.findGatewayEvents(anyString())).thenReturn(
            Stream.of(
                new EventRepository.EventToClean("11", "api1"),
                new EventRepository.EventToClean("12", "api1"),
                new EventRepository.EventToClean("13", "api1"),
                new EventRepository.EventToClean("14", "api1"),
                new EventRepository.EventToClean("15", "api1"),
                new EventRepository.EventToClean("21", "api2"),
                new EventRepository.EventToClean("22", "api2"),
                new EventRepository.EventToClean("23", "api2"),
                new EventRepository.EventToClean("24", "api2"),
                new EventRepository.EventToClean("25", "api2")
            )
        );

        // When
        sut.cleanupEvents("env", 2, Duration.ofDays(1));

        // Then
        verify(eventRepository, atLeastOnce()).delete(deletedEvents.capture());
        assertThat(deletedEvents.getAllValues().stream().flatMap(Collection::stream)).doesNotContain("11", "12", "21", "22").hasSize(6);
    }
}
