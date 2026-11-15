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
package inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Pins the behaviour of the MongoDB and JDBC event repositories, which this double stands for. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EventQueryServiceInMemoryTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private EventQueryServiceInMemory cut;

    @BeforeEach
    void setUp() {
        cut = new EventQueryServiceInMemory();
    }

    @Test
    void should_match_the_id_property_and_not_the_primary_key() {
        cut.initWith(List.of(event("history-id", T0, Map.of()), event("latest-id", T0, Map.of(Event.EventProperties.ID, "history-id"))));

        var response = cut.search(query(Optional.of("history-id"), Optional.empty(), Optional.empty()), new PageableImpl(1, 10));

        assertThat(response.events())
            .as("events whose id property is the searched id")
            .extracting(Event::getId)
            .containsExactly("latest-id");
    }

    @Test
    void should_sort_by_update_date_then_id_newest_first() {
        cut.initWith(
            List.of(
                event("a", T0.plusSeconds(1), Map.of()).toBuilder().createdAt(T0.plusSeconds(9).atZone(ZoneOffset.UTC)).build(),
                event("c", T0, Map.of()),
                event("b", T0.plusSeconds(2), Map.of()),
                event("d", T0, Map.of())
            )
        );

        var response = cut.search(query(Optional.empty(), Optional.empty(), Optional.empty()), new PageableImpl(1, 10));

        assertThat(response.events()).as("events by updatedAt desc, id desc").extracting(Event::getId).containsExactly("b", "a", "d", "c");
    }

    @Test
    void should_filter_on_the_update_date_from_inclusive_to_exclusive() {
        cut.initWith(
            List.of(
                event("before", T0.minusMillis(1), Map.of()),
                event("from", T0, Map.of()).toBuilder().createdAt(T0.minusSeconds(9).atZone(ZoneOffset.UTC)).build(),
                event("inside", T0.plusSeconds(1), Map.of()),
                event("to", T0.plusSeconds(2), Map.of())
            )
        );

        var response = cut.search(
            query(Optional.empty(), Optional.of(T0.toEpochMilli()), Optional.of(T0.plusSeconds(2).toEpochMilli())),
            new PageableImpl(1, 10)
        );

        assertThat(response.events()).as("events updated in [from, to)").extracting(Event::getId).containsExactly("inside", "from");
    }

    private static EventQueryService.SearchQuery query(Optional<String> id, Optional<Long> from, Optional<Long> to) {
        return new EventQueryService.SearchQuery(ENVIRONMENT_ID, id, Optional.empty(), List.of(), Map.of(), from, to);
    }

    private static Event event(String id, Instant updatedAt, Map<Event.EventProperties, String> properties) {
        var eventProperties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        eventProperties.putAll(properties);
        return Event.builder()
            .id(id)
            .environments(Set.of(ENVIRONMENT_ID))
            .properties(eventProperties)
            .createdAt(updatedAt.atZone(ZoneOffset.UTC))
            .updatedAt(updatedAt.atZone(ZoneOffset.UTC))
            .build();
    }
}
