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
    private static final Duration TICK = Duration.ofMinutes(1);
    private static final PerformanceTargetSchedule SCHEDULE = new PerformanceTargetSchedule(3, Duration.ofHours(1), 288, TICK);
    private static final PerformanceTarget TARGET = PerformanceTargetFixtures.aTarget().toBuilder().interval(INTERVAL).build();

    /** A slot boundary of every interval used here: a multiple of five and of ten minutes since the epoch. */
    private static final Instant BOUNDARY = Instant.parse("2021-06-01T10:00:00Z");

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
    class Slots {

        /** The slot is the same for every node, every on-demand run and every timeline: cut on the epoch, not on the target. */
        @Test
        void should_cut_time_on_the_epoch_at_the_effective_interval() {
            assertThat(SCHEDULE.slotStart(TARGET, 0, BOUNDARY)).isEqualTo(BOUNDARY);
            assertThat(SCHEDULE.slotStart(TARGET, 0, BOUNDARY.plus(INTERVAL).minusSeconds(1))).isEqualTo(BOUNDARY);
            assertThat(SCHEDULE.slotStart(TARGET, 0, BOUNDARY.plus(INTERVAL))).isEqualTo(BOUNDARY.plus(INTERVAL));
            // Three misses double the interval to ten minutes: 10:05 falls in the slot that started at 10:00.
            assertThat(SCHEDULE.slotStart(TARGET, 3, BOUNDARY.plus(INTERVAL))).isEqualTo(BOUNDARY);
            assertThat(SCHEDULE.slotStart(TARGET, 0, BOUNDARY.plusSeconds(37))).isEqualTo(BOUNDARY);
        }

        /** Where in the slot the target is due: its phase, held back so a tick still falls in the slot. */
        @Test
        void should_be_due_at_the_target_s_phase_into_the_slot_leaving_room_for_a_tick() {
            var dueAt = SCHEDULE.dueAt(TARGET, 0, BOUNDARY);

            assertThat(dueAt).isEqualTo(BOUNDARY.plus(SCHEDULE.jitter(TARGET)));
            assertThat(Duration.between(BOUNDARY, dueAt)).isLessThanOrEqualTo(INTERVAL.minus(TICK));
        }

        @Test
        void should_be_due_at_the_slot_start_when_the_interval_is_no_longer_than_the_tick() {
            var everyMinute = TARGET.toBuilder().id("a-target-with-a-non-zero-jitter").interval(TICK).build();

            assertThat(SCHEDULE.jitter(everyMinute)).isPositive();
            assertThat(SCHEDULE.dueAt(everyMinute, 0, BOUNDARY)).isEqualTo(BOUNDARY);
            assertThat(SCHEDULE.isDue(everyMinute, BOUNDARY.minusSeconds(50), 0, BOUNDARY)).isTrue();
        }

        @Test
        void should_cap_a_late_phase_so_the_last_tick_of_the_slot_still_catches_the_target() {
            var late = IntStream.range(0, 1000)
                .mapToObj(i -> TARGET.toBuilder().id("late-" + i).build())
                .filter(candidate -> SCHEDULE.jitter(candidate).compareTo(INTERVAL.minus(TICK)) > 0)
                .findFirst()
                .orElseThrow();

            assertThat(SCHEDULE.dueAt(late, 0, BOUNDARY)).isEqualTo(BOUNDARY.plus(INTERVAL).minus(TICK));
        }
    }

    @Nested
    class IsDue {

        private final Duration phase = Duration.between(BOUNDARY, SCHEDULE.dueAt(TARGET, 0, BOUNDARY));

        @Test
        void should_be_due_when_never_evaluated() {
            assertThat(SCHEDULE.isDue(TARGET, null, 0, BOUNDARY.minusSeconds(1))).isTrue();
        }

        @Test
        void should_be_due_from_its_phase_on_once_a_new_slot_has_started_since_the_last_evaluation() {
            assertThat(SCHEDULE.isDue(TARGET, BOUNDARY.minusSeconds(1), 0, BOUNDARY.plus(phase))).isTrue();
            assertThat(SCHEDULE.isDue(TARGET, BOUNDARY.minusSeconds(1), 0, BOUNDARY.plus(INTERVAL).minusSeconds(1))).isTrue();
            if (phase.isPositive()) {
                assertThat(SCHEDULE.isDue(TARGET, BOUNDARY.minusSeconds(1), 0, BOUNDARY.plus(phase).minusSeconds(1))).isFalse();
            }
        }

        /** An evaluation run on demand early in a slot is that slot's evaluation: the scheduler adds none. */
        @Test
        void should_not_be_due_inside_the_slot_of_the_last_evaluation() {
            assertThat(SCHEDULE.isDue(TARGET, BOUNDARY, 0, BOUNDARY.plus(phase))).isFalse();
            assertThat(SCHEDULE.isDue(TARGET, BOUNDARY.plusSeconds(10), 0, BOUNDARY.plus(INTERVAL).minusSeconds(1))).isFalse();
        }

        /** And the very next slot is evaluated in turn: the strip a reader draws per slot has no hole. */
        @Test
        void should_be_due_in_the_slot_after_the_one_evaluated_on_demand() {
            var onDemand = BOUNDARY.plusSeconds(10);

            assertThat(SCHEDULE.isDue(TARGET, onDemand, 0, BOUNDARY.plus(INTERVAL).plus(phase))).isTrue();
        }

        @Test
        void should_wait_for_the_effective_interval_of_an_idle_target() {
            var backedOff = Duration.between(BOUNDARY, SCHEDULE.dueAt(TARGET, 3, BOUNDARY));

            assertThat(SCHEDULE.isDue(TARGET, BOUNDARY, 3, BOUNDARY.plus(INTERVAL).plus(phase))).isFalse();
            assertThat(SCHEDULE.isDue(TARGET, BOUNDARY, 3, BOUNDARY.plus(INTERVAL.multipliedBy(2)).plus(backedOff))).isTrue();
        }
    }

    @Test
    void should_tick_every_minute_unless_told_otherwise() {
        assertThat(new PerformanceTargetSchedule(3, Duration.ofHours(1), 288).tick()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void should_reject_a_meaningless_configuration() {
        assertThatThrownBy(() -> new PerformanceTargetSchedule(0, Duration.ofHours(1), 288)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerformanceTargetSchedule(3, Duration.ZERO, 288)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerformanceTargetSchedule(3, Duration.ofHours(1), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerformanceTargetSchedule(3, Duration.ofHours(1), 288, Duration.ZERO)).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
