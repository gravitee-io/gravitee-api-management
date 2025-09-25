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
import io.gravitee.apim.core.user.model.BaseUserEntity;
import java.util.*;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SearchApiEventUseCaseTest {

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

    SearchApiEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchApiEventUseCase(eventQueryService, userCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(eventQueryService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_event_with_initiator_of_the_requested_api() {
        // Given
        userCrudService.initWith(List.of(USER));
        var expected = EventFixtures.anApiEvent(API_ID).toBuilder().environments(Set.of(ENVIRONMENT_ID)).build();
        eventQueryService.initWith(
            List.of(
                expected,
                Event.builder()
                    .id("event2")
                    .environments(Set.of(ENVIRONMENT_ID))
                    .properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, "other-api")))
                    .build(),
                Event.builder()
                    .id("event3")
                    .environments(Set.of(ENVIRONMENT_ID))
                    .properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, "other-api")))
                    .build()
            )
        );

        var result = useCase.execute(new SearchApiEventUseCase.Input("event-id", ENVIRONMENT_ID, API_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.apiEvent()).isNotEmpty();
            soft.assertThat(result.apiEvent()).contains(new EventWithInitiator(expected, USER));
        });
    }
}
