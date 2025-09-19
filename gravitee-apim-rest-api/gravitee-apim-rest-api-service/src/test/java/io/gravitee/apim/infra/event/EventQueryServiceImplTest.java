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
package io.gravitee.apim.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.infra.query_service.event.EventQueryServiceImpl;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class EventQueryServiceImplTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    EventRepository eventRepository;

    EventQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);

        service = new EventQueryServiceImpl(eventRepository);
    }

    @Nested
    class Search {

        @Test
        @SneakyThrows
        void should_query_for_events() {
            // Given
            when(eventRepository.search(any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

            var query = new EventQueryService.SearchQuery(
                "environment-id",
                Optional.of("id"),
                Optional.of("api-id"),
                Set.of(EventType.PUBLISH_API),
                Map.of(Event.EventProperties.USER, "user-id"),
                Optional.of(2L),
                Optional.of(100L)
            );
            var pageable = new PageableImpl(0, 50);

            // When
            service.search(query, pageable);

            // Then
            var queryCaptor = ArgumentCaptor.forClass(EventCriteria.class);
            var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(eventRepository).search(queryCaptor.capture(), pageableCaptor.capture());
            assertThat(queryCaptor.getValue()).satisfies(criteria -> {
                assertThat(criteria.getEnvironments()).isEqualTo(Set.of("environment-id"));
                assertThat(criteria.getTypes()).isEqualTo(Set.of(io.gravitee.repository.management.model.EventType.PUBLISH_API));
                assertThat(criteria.getProperties()).containsExactlyInAnyOrderEntriesOf(
                    Map.ofEntries(
                        Map.entry(Event.EventProperties.ID.getLabel(), "id"),
                        Map.entry(Event.EventProperties.API_ID.getLabel(), "api-id"),
                        Map.entry(Event.EventProperties.USER.getLabel(), "user-id")
                    )
                );
                assertThat(criteria.getFrom()).isEqualTo(2L);
                assertThat(criteria.getTo()).isEqualTo(100L);
            });

            assertThat(pageableCaptor.getValue()).satisfies(pageParam -> {
                assertThat(pageParam.pageNumber()).isZero();
                assertThat(pageParam.pageSize()).isEqualTo(50);
            });
        }

        @SneakyThrows
        @Test
        void should_return_events() {
            // Given
            when(eventRepository.search(any(), any())).thenAnswer(invocation -> {
                var criteria = invocation.getArgument(0, EventCriteria.class);
                var list = List.of(anEvent().environments(new HashSet<>(criteria.getEnvironments())).build());
                return new Page<>(list, 0, list.size(), list.size());
            });

            var query = new EventQueryService.SearchQuery(
                "environment-id",
                Optional.empty(),
                Optional.empty(),
                Set.of(EventType.PUBLISH_API),
                Map.of(),
                Optional.empty(),
                Optional.empty()
            );
            var pageable = new PageableImpl(0, 50);

            // When
            var result = service.search(query, pageable);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result.total()).isOne();
                softly
                    .assertThat(result.events())
                    .containsExactly(
                        Event.builder()
                            .id("event-id")
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .environments(Set.of("environment-id"))
                            .type(EventType.PUBLISH_API)
                            .payload("event-payload")
                            .parentId("parent-id")
                            .properties(
                                new EnumMap<>(
                                    Map.ofEntries(
                                        Map.entry(Event.EventProperties.API_ID, "api-id"),
                                        Map.entry(Event.EventProperties.USER, "user-id")
                                    )
                                )
                            )
                            .build()
                    );
            });
        }

        private io.gravitee.repository.management.model.Event.EventBuilder anEvent() {
            return io.gravitee.repository.management.model.Event.builder()
                .id("event-id")
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(INSTANT_NOW))
                .environments(Set.of("environment-id"))
                .type(io.gravitee.repository.management.model.EventType.PUBLISH_API)
                .payload("event-payload")
                .parentId("parent-id")
                .properties(
                    Map.ofEntries(
                        Map.entry(Event.EventProperties.API_ID.getLabel(), "api-id"),
                        Map.entry(Event.EventProperties.USER.getLabel(), "user-id")
                    )
                );
        }

        @Test
        @SneakyThrows
        void should_return_event_by_id() {
            when(eventRepository.findById(any())).thenAnswer(invocation -> Optional.of(anEvent().build()));

            var result = service.findByIdForEnvironmentAndApi("event-id", "environment-id", "api-id");

            assertThat(result).isNotEmpty();
        }
    }
}
