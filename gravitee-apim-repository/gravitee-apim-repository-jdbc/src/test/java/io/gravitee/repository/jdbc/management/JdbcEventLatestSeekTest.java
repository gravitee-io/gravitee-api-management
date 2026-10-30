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
package io.gravitee.repository.jdbc.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.EventLatestCursor;
import io.gravitee.repository.management.api.search.EventLatestPage;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The update date column is finer than {@link Date} and has no zone, which only a SQL repository can run into.
 *
 * @author GraviteeSource Team
 */
public class JdbcEventLatestSeekTest extends AbstractManagementRepositoryTest {

    private static final int MAX_PAGES = 20;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Properties graviteeProperties;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_read_each_event_once_when_more_events_than_a_page_share_a_sub_millisecond_date() throws Exception {
        var date = LocalDateTime.of(2026, 1, 15, 12, 0, 0, 123_456_000);
        storeEvents(Map.of("a", date, "b", date, "c", date, "d", date, "e", date));

        assertThat(seekAll(2)).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }

    @Test
    public void should_read_each_event_once_when_a_millisecond_mixes_exact_and_sub_millisecond_dates() throws Exception {
        var millisecond = LocalDateTime.of(2026, 1, 15, 12, 0, 0, 123_000_000);
        storeEvents(
            Map.of(
                "m",
                millisecond,
                "s0",
                millisecond.plusNanos(100_000),
                "z",
                millisecond.plusNanos(200_000),
                "a",
                millisecond.plusNanos(300_000),
                "later",
                millisecond.plusNanos(1_000_000)
            )
        );

        assertThat(seekAll(3)).containsExactlyInAnyOrder("m", "s0", "z", "a", "later");
    }

    @Test
    public void should_read_each_event_once_when_dates_fall_in_a_daylight_saving_gap_of_the_gateway_zone() throws Exception {
        var gap = LocalDateTime.of(2026, 3, 29, 2, 10, 0);
        storeEvents(
            Map.of("a", gap, "b", gap.plusMinutes(10), "c", gap.plusMinutes(20), "d", gap.plusMinutes(30), "e", gap.plusMinutes(40))
        );

        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Warsaw"));
            assertThat(seekAll(2)).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private void storeEvents(Map<String, LocalDateTime> updatedAtById) throws Exception {
        String table = graviteeProperties.getProperty("management.jdbc.prefix", "") + "events_latest";
        for (var entry : updatedAtById.entrySet()) {
            eventLatestRepository.createOrUpdate(anEvent(entry.getKey()));
            jdbcTemplate.update("update " + table + " set updated_at = ? where id = ?", entry.getValue(), entry.getKey());
        }
    }

    private List<String> seekAll(int size) {
        List<String> ids = new ArrayList<>();
        EventLatestCursor cursor = null;
        for (int pages = 0; pages < MAX_PAGES; pages++) {
            EventLatestPage page = eventLatestRepository.searchAfter(
                EventCriteria.builder().build(),
                Event.EventProperties.API_ID,
                cursor,
                size
            );
            page.events().forEach(event -> ids.add(event.getId()));
            if (page.events().size() < size) {
                return ids;
            }
            cursor = page.next();
        }
        return fail("Paging did not end after %d pages, read %s", MAX_PAGES, ids);
    }

    private static Event anEvent(String id) {
        return Event.builder()
            .id(id)
            .type(EventType.PUBLISH_API)
            .payload("{}")
            .properties(Map.of(Event.EventProperties.API_ID.getValue(), id))
            .environments(Set.of("DEFAULT"))
            .organizations(Set.of("DEFAULT"))
            .createdAt(new Date(1000))
            .updatedAt(new Date(1000))
            .build();
    }
}
