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
package io.gravitee.apim.core.event.use_case;

import fixtures.core.model.EventFixtures;
import inmemory.EventQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.model.EventWithInitiator;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEventsUseCaseTest {

    public static final String API_ID = "api-id";
    public static final String ORGANIZATION_ID = "organization-id";
    public static final String ENVIRONMENT_ID = "environment-id";

    private static final BaseUserEntity USER = BaseUserEntity.builder()
        .id("user-id")
        .firstname("John")
        .lastname("Doe")
        .email("john.doe@gravitee.io")
        .build();

    EventQueryServiceInMemory eventQueryService = new EventQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    SearchEventsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchEventsUseCase(eventQueryService, userCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(eventQueryService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_events_with_their_initiators_of_the_requested_api() {
        // Given
        userCrudService.initWith(List.of(USER));
        var expected = EventFixtures.anApiEvent(API_ID).toBuilder().environments(Set.of(ENVIRONMENT_ID)).build();
        eventQueryService.initWith(
            List.of(
                expected,
                Event.builder().id("event2").properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, "other-api"))).build(),
                Event.builder().id("event3").properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, "other-api"))).build()
            )
        );

        var result = useCase.execute(
            new SearchEventsUseCase.Input(
                new EventQueryService.SearchQuery(
                    ENVIRONMENT_ID,
                    Optional.empty(),
                    Optional.of(API_ID),
                    List.of(),
                    Map.of(),
                    Optional.of(0L),
                    Optional.of(Instant.now().toEpochMilli())
                )
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft.assertThat(result.data()).containsExactly(new EventWithInitiator(expected, USER));
        });
    }

    @Test
    void should_return_events_sorted_by_desc_createdAt() {
        // Given
        eventQueryService.initWith(
            List.of(
                EventFixtures.anEvent()
                    .toBuilder()
                    .id("1")
                    .environments(Set.of(ENVIRONMENT_ID))
                    .createdAt(ZonedDateTime.parse("2020-02-01T20:22:02.00Z"))
                    .build(),
                EventFixtures.anEvent()
                    .toBuilder()
                    .id("2")
                    .environments(Set.of(ENVIRONMENT_ID))
                    .createdAt(ZonedDateTime.parse("2020-02-02T20:22:02.00Z"))
                    .build(),
                EventFixtures.anEvent()
                    .toBuilder()
                    .id("3")
                    .environments(Set.of(ENVIRONMENT_ID))
                    .createdAt(ZonedDateTime.parse("2020-02-03T20:22:02.00Z"))
                    .build()
            )
        );

        var result = useCase.execute(
            new SearchEventsUseCase.Input(
                new EventQueryService.SearchQuery(
                    ENVIRONMENT_ID,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    Map.of(),
                    Optional.of(0L),
                    Optional.of(Instant.now().toEpochMilli())
                )
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(3);
            soft.assertThat(result.data()).extracting(Event::getId).containsExactly("3", "2", "1");
        });
    }

    @Test
    void should_return_the_page_requested() {
        // Given
        var expectedTotal = 15;
        var pageNumber = 2;
        var pageSize = 5;
        eventQueryService.initWith(
            IntStream.range(0, expectedTotal)
                .mapToObj(i -> EventFixtures.anEvent().toBuilder().id(String.valueOf(i)).environments(Set.of(ENVIRONMENT_ID)).build())
                .collect(Collectors.toList())
        );

        var result = useCase.execute(
            new SearchEventsUseCase.Input(
                new EventQueryService.SearchQuery(
                    ENVIRONMENT_ID,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    Map.of(),
                    Optional.of(0L),
                    Optional.of(Instant.now().toEpochMilli())
                ),
                new PageableImpl(pageNumber, pageSize)
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft.assertThat(result.data()).extracting(Event::getId).containsExactly("5", "6", "7", "8", "9");
        });
    }
}
