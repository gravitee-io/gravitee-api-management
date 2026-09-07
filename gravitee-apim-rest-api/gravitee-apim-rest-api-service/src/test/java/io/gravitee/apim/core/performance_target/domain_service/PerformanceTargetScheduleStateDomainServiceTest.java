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
package io.gravitee.apim.core.performance_target.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PerformanceTargetFixtures;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetScheduleStateDomainService.State;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PerformanceTargetScheduleStateDomainServiceTest {

    private static final Instant T0 = Instant.parse("2021-06-01T10:00:00Z");

    private final PerformanceTargetScheduleStateDomainService service = new PerformanceTargetScheduleStateDomainService();

    @Test
    void should_seed_a_target_once_and_keep_the_seeded_state() {
        var seeded = new State(T0, 2);

        var first = service.stateOf("t", () -> seeded);
        var second = service.stateOf("t", () -> State.FRESH);

        assertThat(first).isEqualTo(seeded);
        assertThat(second).isEqualTo(seeded);
    }

    @Test
    void should_count_consecutive_not_evaluable_evaluations_and_reset_the_count_on_an_evaluable_one() {
        service.record("t", evaluation(PerformanceTargetEvaluation.Status.NOT_EVALUABLE, T0));
        service.record("t", evaluation(PerformanceTargetEvaluation.Status.NOT_EVALUABLE, T0.plusSeconds(60)));
        assertThat(service.current("t")).contains(new State(T0.plusSeconds(60), 2));

        service.record("t", evaluation(PerformanceTargetEvaluation.Status.PASS, T0.plusSeconds(120)));

        assertThat(service.current("t")).contains(new State(T0.plusSeconds(120), 0));
    }

    @Test
    void should_record_an_evaluation_of_a_target_it_has_not_seen_yet() {
        service.record("t", evaluation(PerformanceTargetEvaluation.Status.BREACH, T0));

        assertThat(service.current("t")).contains(new State(T0, 0));
    }

    @Test
    void should_only_move_the_last_attempt_of_a_target_it_knows() {
        service.attempted("unknown", T0);
        service.stateOf("t", () -> new State(T0, 3));

        service.attempted("t", T0.plusSeconds(60));

        assertThat(service.current("unknown")).isEmpty();
        assertThat(service.current("t")).contains(new State(T0.plusSeconds(60), 3));
    }

    @Test
    void should_start_a_fresh_schedule_on_reset() {
        service.stateOf("t", () -> new State(T0, 5));

        service.reset("t");

        assertThat(service.current("t")).contains(State.FRESH);
    }

    @Test
    void should_forget_the_targets_it_is_not_told_to_retain() {
        service.stateOf("kept", () -> State.FRESH);
        service.stateOf("gone", () -> State.FRESH);

        service.retain(Set.of("kept"));

        assertThat(service.current("kept")).isPresent();
        assertThat(service.current("gone")).isEmpty();
    }

    private static PerformanceTargetEvaluation evaluation(PerformanceTargetEvaluation.Status status, Instant evaluatedAt) {
        return PerformanceTargetFixtures.anEvaluation("e-" + evaluatedAt.getEpochSecond(), "t", status, evaluatedAt);
    }
}
