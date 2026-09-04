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
package io.gravitee.apim.core.performance_target.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PerformanceTargetFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PerformanceTargetScheduleTest {

    private static final Duration INTERVAL = Duration.ofMinutes(5);
    private static final PerformanceTargetSchedule SCHEDULE = new PerformanceTargetSchedule(3, Duration.ofHours(1), 288);
    private static final PerformanceTarget TARGET = PerformanceTargetFixtures.aTarget().toBuilder().interval(INTERVAL).build();

    @Nested
    class EffectiveInterval {

        @ParameterizedTest(name = "{0} consecutive not evaluable evaluations -> {1} minutes")
        @CsvSource({ "0, 5", "1, 5", "2, 5", "3, 10", "4, 20", "5, 40", "6, 60", "7, 60", "50, 60" })
        void should_double_the_interval_per_miss_past_the_backoff_threshold_up_to_the_cap(int consecutiveNotEvaluable, long minutes) {
            assertThat(SCHEDULE.effectiveInterval(TARGET, consecutiveNotEvaluable)).isEqualTo(Duration.ofMinutes(minutes));
        }

        @Test
        void should_keep_a_declared_interval_longer_than_the_cap() {
            var daily = TARGET.toBuilder().interval(Duration.ofDays(1)).build();

            assertThat(SCHEDULE.effectiveInterval(daily, 10)).isEqualTo(Duration.ofDays(1));
        }

        @Test
        void should_know_how_many_evaluations_back_the_cap_is_reached() {
            assertThat(SCHEDULE.historyDepth(TARGET)).isEqualTo(6);
            assertThat(SCHEDULE.historyDepth(TARGET.toBuilder().interval(Duration.ofDays(1)).build())).isEqualTo(3);
        }
    }

    @Nested
    class Jitter {

        @Test
        void should_be_stable_and_spread_across_the_declared_interval() {
            var jitters = IntStream.range(0, 200)
                .mapToObj(i -> SCHEDULE.jitter(TARGET.toBuilder().id("target-" + i).build()))
                .toList();

            assertThat(jitters).allSatisfy(jitter -> assertThat(jitter).isBetween(Duration.ZERO, INTERVAL.minusSeconds(1)));
            assertThat(jitters.stream().distinct().count()).isGreaterThan(100);
            assertThat(SCHEDULE.jitter(TARGET)).isEqualTo(SCHEDULE.jitter(PerformanceTargetFixtures.aTarget()));
        }

        @Test
        void should_not_depend_on_the_backoff() {
            assertThat(SCHEDULE.jitter(TARGET)).isEqualTo(new PerformanceTargetSchedule(1, Duration.ofDays(1), 10).jitter(TARGET));
        }
    }

    @Nested
    class IsDue {

        private final Instant boundary = Instant.parse("2021-06-01T10:00:00Z").plus(SCHEDULE.jitter(TARGET));

        @Test
        void should_be_due_when_never_evaluated() {
            assertThat(SCHEDULE.isDue(TARGET, null, 0, boundary.minusSeconds(1))).isTrue();
        }

        @Test
        void should_be_due_once_a_slot_boundary_has_passed_since_the_last_evaluation() {
            assertThat(SCHEDULE.isDue(TARGET, boundary.minusSeconds(1), 0, boundary)).isTrue();
            assertThat(SCHEDULE.isDue(TARGET, boundary.minusSeconds(1), 0, boundary.plusSeconds(59))).isTrue();
        }

        @Test
        void should_not_be_due_inside_the_slot_of_the_last_evaluation() {
            assertThat(SCHEDULE.isDue(TARGET, boundary, 0, boundary)).isFalse();
            assertThat(SCHEDULE.isDue(TARGET, boundary, 0, boundary.plus(INTERVAL).minusSeconds(1))).isFalse();
            assertThat(SCHEDULE.isDue(TARGET, boundary.plusSeconds(30), 0, boundary.plus(INTERVAL).minusSeconds(1))).isFalse();
        }

        @Test
        void should_be_due_exactly_one_interval_after_the_slot_of_the_last_evaluation() {
            assertThat(SCHEDULE.isDue(TARGET, boundary, 0, boundary.plus(INTERVAL))).isTrue();
        }

        @Test
        void should_name_the_slot_an_instant_belongs_to_by_its_start() {
            assertThat(SCHEDULE.slotStart(TARGET, 0, boundary)).isEqualTo(boundary);
            assertThat(SCHEDULE.slotStart(TARGET, 0, boundary.plus(INTERVAL).minusSeconds(1))).isEqualTo(boundary);
            assertThat(SCHEDULE.slotStart(TARGET, 0, boundary.plus(INTERVAL))).isEqualTo(boundary.plus(INTERVAL));
            assertThat(SCHEDULE.slotStart(TARGET, 3, boundary.plus(INTERVAL))).isEqualTo(boundary);
        }

        @Test
        void should_wait_for_the_effective_interval_of_an_idle_target() {
            var backedOffBoundary = Instant.parse("2021-06-01T10:00:00Z").plus(SCHEDULE.jitter(TARGET));

            assertThat(SCHEDULE.isDue(TARGET, backedOffBoundary, 3, backedOffBoundary.plus(INTERVAL))).isFalse();
            assertThat(SCHEDULE.isDue(TARGET, backedOffBoundary, 3, backedOffBoundary.plus(INTERVAL.multipliedBy(2)))).isTrue();
        }
    }

    @Test
    void should_reject_a_meaningless_configuration() {
        assertThatThrownBy(() -> new PerformanceTargetSchedule(0, Duration.ofHours(1), 288)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerformanceTargetSchedule(3, Duration.ZERO, 288)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerformanceTargetSchedule(3, Duration.ofHours(1), 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
