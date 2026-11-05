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
package io.gravitee.apim.core.generic_event.use_case;

import static io.gravitee.apim.core.event.model.Event.EventProperties.GENERIC_EVENT_ID;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PurgeGenericEventsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final PurgeGenericEventsUseCase.Input INPUT = new PurgeGenericEventsUseCase.Input(Duration.ofDays(7));

    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final EventLatestCrudInMemory eventLatestCrudService = new EventLatestCrudInMemory();
    private PurgeGenericEventsUseCase purge;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        purge = new PurgeGenericEventsUseCase(eventLatestQueryService, eventLatestCrudService);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_delete_an_expired_event_without_unpublishing_it() {
        var expired = published("expired", NOW.minusSeconds(1));
        var active = published("active", NOW.plusSeconds(1));
        given(expired, active);

        var output = purge.execute(INPUT);

        assertThat(output.expired()).isEqualTo(1);
        assertThat(eventLatestCrudService.storage()).containsExactly(active);
    }

    @Test
    void should_purge_an_unpublish_only_after_the_retention() {
        var old = unpublished("old", NOW.minus(Duration.ofDays(8)));
        var recent = unpublished("recent", NOW.minus(Duration.ofDays(6)));
        given(old, recent);

        var output = purge.execute(INPUT);

        assertThat(output.purged()).isEqualTo(1);
        assertThat(eventLatestCrudService.storage()).containsExactly(recent);
    }

    private void given(Event... events) {
        eventLatestQueryService.initWith(List.of(events));
        eventLatestCrudService.initWith(List.of(events));
    }

    private static Event published(String id, Instant expiresAt) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(GENERIC_EVENT_ID, id);
        return Event.builder()
            .id(id)
            .type(EventType.PUBLISH_GENERIC_EVENT)
            .payload(
                "{\"kind\":\"aihub.key\",\"id\":\"%s\",\"environmentId\":\"env\",\"expiresAt\":\"%s\",\"payload\":{}}".formatted(
                    id,
                    expiresAt
                )
            )
            .properties(properties)
            .updatedAt(NOW.minus(Duration.ofDays(1)).atZone(ZoneOffset.UTC))
            .expiresAt(expiresAt.atZone(ZoneOffset.UTC))
            .build();
    }

    private static Event unpublished(String id, Instant updatedAt) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(GENERIC_EVENT_ID, id);
        return Event.builder()
            .id(id)
            .type(EventType.UNPUBLISH_GENERIC_EVENT)
            .properties(properties)
            .updatedAt(updatedAt.atZone(ZoneOffset.UTC))
            .build();
    }
}
