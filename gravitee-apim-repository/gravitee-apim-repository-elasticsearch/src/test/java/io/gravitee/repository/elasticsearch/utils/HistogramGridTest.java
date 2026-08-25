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
package io.gravitee.repository.elasticsearch.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HistogramGridTest {

    static Stream<Duration> intervals_that_do_not_amount_to_a_millisecond() {
        return Stream.of(null, Duration.ZERO, Duration.ofMinutes(-10), Duration.ofNanos(500));
    }

    @Nested
    class OffsetMillis {

        static Stream<Instant> window_starts() {
            return Stream.of(
                Instant.parse("2026-08-24T10:17:42.123Z"),
                Instant.parse("2026-08-24T10:30:00Z"),
                Instant.parse("2026-08-24T00:00:00.001Z"),
                Instant.parse("1969-12-31T23:52:30Z")
            );
        }

        @ParameterizedTest
        @MethodSource("window_starts")
        void should_place_a_bucket_boundary_exactly_on_the_window_start(final Instant windowStart) {
            // Given
            final Duration interval = Duration.ofMinutes(10);

            // When
            final long offset = HistogramGrid.offsetMillis(windowStart, interval);

            // Then
            assertThat(offset).isGreaterThanOrEqualTo(0).isLessThan(interval.toMillis());
            assertThat((windowStart.toEpochMilli() - offset) % interval.toMillis()).isZero();
        }

        @Test
        void should_not_shift_the_grid_when_the_window_already_starts_on_a_boundary() {
            // Given
            final Instant windowStart = Instant.parse("2026-08-24T10:30:00Z");

            // When
            final long offset = HistogramGrid.offsetMillis(windowStart, Duration.ofMinutes(30));

            // Then
            assertThat(offset).isZero();
        }

        @Test
        void should_return_a_positive_offset_for_a_window_starting_before_the_epoch() {
            // Given
            final Instant windowStart = Instant.parse("1969-12-31T23:52:30Z");

            // When
            final long offset = HistogramGrid.offsetMillis(windowStart, Duration.ofMinutes(10));

            // Then
            assertThat(offset).isEqualTo(Duration.ofMinutes(2).plusSeconds(30).toMillis());
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.repository.elasticsearch.utils.HistogramGridTest#intervals_that_do_not_amount_to_a_millisecond")
        void should_not_shift_the_grid_when_the_interval_is_not_usable(final Duration interval) {
            // Given
            final Instant windowStart = Instant.parse("2026-08-24T10:17:42.123Z");

            // When
            final long offset = HistogramGrid.offsetMillis(windowStart, interval);

            // Then
            assertThat(offset).isZero();
        }
    }

    @Nested
    class EndOfLastBucket {

        @Test
        void should_stop_one_interval_past_the_window_when_it_spans_whole_intervals() {
            // Given a window of exactly four intervals
            final Instant from = Instant.parse("2026-08-24T10:00:00Z");
            final Instant to = Instant.parse("2026-08-24T11:00:00Z");

            // When
            final Instant end = HistogramGrid.endOfLastBucket(from, to, Duration.ofMinutes(15));

            // Then the last emitted bucket starts on the window end, so its own end is one interval further
            assertThat(end).isEqualTo(Instant.parse("2026-08-24T11:15:00Z"));
        }

        @Test
        void should_stop_at_the_end_of_the_bucket_holding_the_window_end_when_it_does_not() {
            // Given a window of 59m53s bucketed by 15s: the grid sits on :07, so the last emitted bucket is
            // 09:59:52 rather than one starting on the window end
            final Instant from = Instant.parse("2026-08-24T09:00:07Z");
            final Instant to = Instant.parse("2026-08-24T10:00:00Z");

            // When
            final Instant end = HistogramGrid.endOfLastBucket(from, to, Duration.ofSeconds(15));

            // Then the bound is the end of that bucket, which leaves out a document at 10:00:10
            assertThat(end).isEqualTo(Instant.parse("2026-08-24T10:00:07Z"));
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.repository.elasticsearch.utils.HistogramGridTest#intervals_that_do_not_amount_to_a_millisecond")
        void should_return_the_window_end_when_the_interval_is_not_usable(final Duration interval) {
            // Given
            final Instant from = Instant.parse("2026-08-24T09:00:07Z");
            final Instant to = Instant.parse("2026-08-24T10:00:00Z");

            // When
            final Instant end = HistogramGrid.endOfLastBucket(from, to, interval);

            // Then
            assertThat(end).isEqualTo(to);
        }
    }
}
