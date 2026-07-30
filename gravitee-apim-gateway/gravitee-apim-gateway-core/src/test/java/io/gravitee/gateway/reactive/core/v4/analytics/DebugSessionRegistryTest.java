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
package io.gravitee.gateway.reactive.core.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DebugSessionRegistryTest {

    private static final String API_ID = "api-1";

    private final AtomicLong now = new AtomicLong(1_000L);
    private DebugSessionRegistry cut;

    @BeforeEach
    void setUp() {
        cut = new DebugSessionRegistry(now::get);
    }

    @Test
    void should_not_report_a_session_for_an_api_that_has_none() {
        assertThat(cut.activeFor(API_ID)).isEmpty();
    }

    @Test
    void should_report_an_open_session() {
        cut.open(API_ID, 2_000L, 100);

        assertThat(cut.activeFor(API_ID)).hasValueSatisfying(session -> {
            assertThat(session.apiId()).isEqualTo(API_ID);
            assertThat(session.expiresAt()).isEqualTo(2_000L);
        });
    }

    @Test
    void should_isolate_sessions_by_api() {
        cut.open(API_ID, 2_000L, 100);

        assertThat(cut.activeFor("api-2")).isEmpty();
    }

    @Test
    void should_drop_a_session_once_it_expires() {
        cut.open(API_ID, 2_000L, 100);
        now.set(2_000L);

        assertThat(cut.activeFor(API_ID)).isEmpty();
        // A node that stops being told about the session must stop capturing.
        assertThat(cut.isEmpty()).isTrue();
    }

    @Test
    void should_close_a_session_on_demand() {
        cut.open(API_ID, 2_000L, 100);
        cut.close(API_ID);

        assertThat(cut.activeFor(API_ID)).isEmpty();
    }

    @Test
    void should_replace_the_session_of_an_api_that_already_has_one() {
        cut.open(API_ID, 2_000L, 100);
        cut.open(API_ID, 5_000L, 100);

        assertThat(cut.activeFor(API_ID)).hasValueSatisfying(session -> assertThat(session.expiresAt()).isEqualTo(5_000L));
    }

    @Test
    void should_capture_every_request_at_full_sampling() {
        cut.open(API_ID, 2_000L, 100);
        var session = cut.activeFor(API_ID).orElseThrow();

        assertThat(
            IntStream.range(0, 10)
                .filter(i -> session.shouldCapture())
                .count()
        ).isEqualTo(10);
    }

    @Test
    void should_capture_one_request_in_ten_at_ten_percent() {
        cut.open(API_ID, 2_000L, 10);
        var session = cut.activeFor(API_ID).orElseThrow();

        assertThat(
            IntStream.range(0, 100)
                .filter(i -> session.shouldCapture())
                .count()
        ).isEqualTo(10);
    }

    @Test
    void should_capture_the_first_request_of_the_sample() {
        cut.open(API_ID, 2_000L, 10);
        var session = cut.activeFor(API_ID).orElseThrow();

        // Waiting ten requests before the first capture would make a short
        // session on low traffic look broken.
        assertThat(session.shouldCapture()).isTrue();
    }

    @Test
    void should_keep_sampling_state_across_lookups() {
        cut.open(API_ID, 2_000L, 50);

        assertThat(cut.activeFor(API_ID).orElseThrow().shouldCapture()).isTrue();
        assertThat(cut.activeFor(API_ID).orElseThrow().shouldCapture()).isFalse();
    }

    @Test
    void should_capture_everything_when_the_sampling_percentage_is_out_of_range() {
        cut.open(API_ID, 2_000L, 0);
        var session = cut.activeFor(API_ID).orElseThrow();

        assertThat(
            IntStream.range(0, 5)
                .filter(i -> session.shouldCapture())
                .count()
        ).isEqualTo(5);
    }
}
