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
package io.gravitee.gateway.services.sync.process.common.synchronizer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResyncWindowTest {

    @Nested
    class EffectiveFromTest {

        @Test
        void should_return_the_callers_from_when_nothing_is_pending() {
            ResyncWindow window = new ResyncWindow();

            assertThat(window.effectiveFrom(100L)).isEqualTo(100L);
        }

        @Test
        void should_return_the_callers_from_unchanged_when_nothing_is_pending_even_an_initial_sync() {
            ResyncWindow window = new ResyncWindow();

            assertThat(window.effectiveFrom(-1L)).isEqualTo(-1L);
            assertThat(window.effectiveFrom(null)).isNull();
        }

        @Test
        void should_widen_to_the_pending_window_when_it_is_older_than_the_callers() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);

            assertThat(window.effectiveFrom(100L)).isEqualTo(50L);
        }

        @Test
        void should_keep_the_callers_from_when_it_is_older_than_the_pending_window() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(100L);

            assertThat(window.effectiveFrom(50L)).isEqualTo(50L);
        }

        @Test
        void should_force_an_initial_sync_when_the_pending_window_is_itself_an_initial_sync() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(-1L);

            assertThat(window.effectiveFrom(100L)).isEqualTo(-1L);
        }

        @Test
        void should_force_an_initial_sync_when_a_failed_initial_sync_is_recorded_as_null() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(null);

            assertThat(window.effectiveFrom(100L)).isEqualTo(-1L);
        }

        @Test
        void should_force_an_initial_sync_when_the_callers_from_is_itself_an_initial_sync() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);

            assertThat(window.effectiveFrom(-1L)).isEqualTo(-1L);
            assertThat(window.effectiveFrom(null)).isEqualTo(-1L);
        }

        @Test
        void should_return_the_callers_from_once_recovered() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);
            window.markSucceeded();

            assertThat(window.effectiveFrom(100L)).isEqualTo(100L);
        }
    }

    @Nested
    class MarkFailedTest {

        @Test
        void should_report_the_first_failure_after_a_success() {
            ResyncWindow window = new ResyncWindow();

            assertThat(window.markFailed(50L)).isTrue();
        }

        @Test
        void should_not_report_a_failure_still_pending_from_a_previous_one() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);

            assertThat(window.markFailed(50L)).isFalse();
        }

        @Test
        void should_report_the_first_failure_again_once_recovered() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);
            window.markSucceeded();

            assertThat(window.markFailed(70L)).isTrue();
        }

        @Test
        void should_keep_the_oldest_pending_window_across_repeated_failures() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);
            window.markFailed(70L);

            assertThat(window.effectiveFrom(100L)).isEqualTo(70L);
        }
    }

    @Nested
    class MarkSucceededTest {

        @Test
        void should_report_no_recovery_when_nothing_was_pending() {
            ResyncWindow window = new ResyncWindow();

            assertThat(window.markSucceeded()).isFalse();
        }

        @Test
        void should_report_a_recovery_when_a_failure_was_pending() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);

            assertThat(window.markSucceeded()).isTrue();
        }

        @Test
        void should_clear_the_pending_window_so_a_later_success_reports_no_recovery() {
            ResyncWindow window = new ResyncWindow();
            window.markFailed(50L);
            window.markSucceeded();

            assertThat(window.markSucceeded()).isFalse();
        }
    }
}
