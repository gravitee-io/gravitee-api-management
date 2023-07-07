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
package io.gravitee.plugin.apiservice.healthcheck.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class HealthCheckStatusTest {

    @Test
    void should_stay_up_when_success() {
        final HealthCheckStatus cut = new HealthCheckStatus(ManagedEndpoint.Status.UP, 2, 3);

        assertThat(cut.reportSuccess()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.UP);
    }

    @Test
    void should_move_to_transitionally_up_when_success_and_was_down() {
        final HealthCheckStatus cut = new HealthCheckStatus(ManagedEndpoint.Status.DOWN, 2, 3);

        assertThat(cut.reportSuccess()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_UP);
        assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_UP);
    }

    @Test
    void should_move_back_to_up_when_success_threshold_reached_and_was_transitionally_down() {
        final int successThreshold = 5;
        final HealthCheckStatus cut = new HealthCheckStatus(ManagedEndpoint.Status.TRANSITIONALLY_DOWN, successThreshold, 3);

        for (int i = 0; i < successThreshold - 1; i++) {
            assertThat(cut.reportSuccess()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_DOWN);
            assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_DOWN);
        }

        assertThat(cut.reportSuccess()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.UP);
    }

    @Test
    void should_move_to_up_when_success_threshold_reached_and_was_down() {
        final int successThreshold = 5;
        final HealthCheckStatus cut = new HealthCheckStatus(ManagedEndpoint.Status.DOWN, successThreshold, 3);

        for (int i = 0; i < successThreshold - 1; i++) {
            assertThat(cut.reportSuccess()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_UP);
            assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_UP);
        }

        assertThat(cut.reportSuccess()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.UP);
    }

    @Test
    void should_move_to_transitionally_down_when_failure() {
        final HealthCheckStatus cut = new HealthCheckStatus(ManagedEndpoint.Status.UP, 2, 3);

        assertThat(cut.reportFailure()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_DOWN);
        assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_DOWN);
    }

    @Test
    void should_move_to_down_when_failure_threshold_reached_and_was_up() {
        final int failureThreshold = 5;
        final HealthCheckStatus cut = new HealthCheckStatus(ManagedEndpoint.Status.UP, 2, failureThreshold);

        for (int i = 0; i < failureThreshold - 1; i++) {
            assertThat(cut.reportFailure()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_DOWN);
            assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.TRANSITIONALLY_DOWN);
        }

        assertThat(cut.reportFailure()).isEqualTo(ManagedEndpoint.Status.DOWN);
        assertThat(cut.getCurrentStatus()).isEqualTo(ManagedEndpoint.Status.DOWN);
    }
}
