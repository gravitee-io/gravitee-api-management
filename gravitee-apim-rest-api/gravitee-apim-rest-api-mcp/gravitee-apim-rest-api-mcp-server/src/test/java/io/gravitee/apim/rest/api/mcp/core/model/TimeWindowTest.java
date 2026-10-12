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
package io.gravitee.apim.rest.api.mcp.core.model;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimeWindowTest {

    @ParameterizedTest
    @CsvSource({ "15m, 2026-01-31T13:45:00Z", "2h, 2026-01-31T12:00:00Z", "1d, 2026-01-30T14:00:00Z" })
    void should_end_now_and_start_one_length_before(String window, String expectedFrom) {
        assertThat(TimeWindow.parse(window, null, NOW)).isEqualTo(new TimeWindow(Instant.parse(expectedFrom), NOW));
    }

    @Test
    void should_default_to_the_last_fifteen_minutes() {
        assertThat(TimeWindow.parse(null, null, NOW)).isEqualTo(new TimeWindow(Instant.parse("2026-01-31T13:45:00Z"), NOW));
    }

    @Test
    void should_end_on_a_whole_second() {
        assertThat(TimeWindow.parse("15m", null, NOW.plusNanos(159_861_000)).to()).isEqualTo(NOW);
    }

    @Test
    void should_end_at_the_given_instant() {
        assertThat(TimeWindow.parse("30m", "2026-01-31T10:00:00Z", NOW)).isEqualTo(
            new TimeWindow(Instant.parse("2026-01-31T09:30:00Z"), Instant.parse("2026-01-31T10:00:00Z"))
        );
    }

    @Test
    void should_take_the_preceding_window_of_equal_length_as_baseline() {
        assertThat(TimeWindow.parse("15m", null, NOW).baseline()).isEqualTo(
            new TimeWindow(Instant.parse("2026-01-31T13:30:00Z"), Instant.parse("2026-01-31T13:45:00Z"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "15", "m", "15 minutes", "0m", "1w", "-5m", "1769868000000" })
    void should_reject_a_malformed_window_and_name_the_format(String window) {
        assertThatThrownBy(() -> TimeWindow.parse(window, null, NOW))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("for example 15m, 2h or 1d");
    }

    @Test
    void should_reject_a_window_longer_than_thirty_days() {
        assertThatThrownBy(() -> TimeWindow.parse("31d", null, NOW))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("longest window is 30d");
    }

    @Test
    void should_reject_epoch_milliseconds_as_until_and_name_the_format() {
        assertThatThrownBy(() -> TimeWindow.parse("15m", "1769868000000", NOW))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("ISO-8601");
    }

    @Test
    void should_reject_an_until_in_the_future() {
        assertThatThrownBy(() -> TimeWindow.parse("15m", "2026-01-31T15:00:00Z", NOW))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("in the future");
    }
}
