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
package io.gravitee.gateway.http.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.gravitee.common.http.IdGenerator;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * What the dispatcher hands the wrapper at construction, and what the wrapper does with it.
 *
 * <p>Two things are pinned here, and both are behaviours a reader would otherwise have to infer
 * from the dispatcher: the wrapper reports the path it was given rather than the native one, and it
 * dates itself from the instant the dispatcher started handling the request rather than from its
 * own construction.
 *
 * <p>That second point cannot be asserted end to end. The gateway dates requests in milliseconds,
 * and everything the dispatcher does before building the wrapper takes tens of nanoseconds, so a
 * reported metric looks identical either way. What is testable — and what actually matters — is
 * that the wrapper honours the instant it is handed instead of taking its own reading.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxHttpServerRequestOptionsTest {

    private static final String NATIVE_PATH = "/alpha/api/public/../admin";
    private static final String RESOLVED_PATH = "/alpha/api/admin";

    @Mock
    HttpServerRequest serverRequest;

    @Mock
    IdGenerator idGenerator;

    @BeforeEach
    void init() {
        lenient().when(serverRequest.path()).thenReturn(NATIVE_PATH);
        lenient().when(serverRequest.uri()).thenReturn(NATIVE_PATH);
        lenient().when(serverRequest.method()).thenReturn(HttpMethod.GET);
        lenient().when(serverRequest.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(idGenerator.randomString()).thenReturn("request-id");
    }

    @Nested
    class The_path {

        @Test
        void should_be_the_one_the_acceptor_matched_when_the_dispatcher_supplies_it() {
            var request = new VertxHttpServerRequest(
                serverRequest,
                idGenerator,
                VertxHttpServerRequestOptions.builder().path(RESOLVED_PATH).build()
            );

            assertThat(request.path()).isEqualTo(RESOLVED_PATH);
        }

        @Test
        void should_keep_reporting_the_untouched_uri_whatever_the_path_becomes() {
            var request = new VertxHttpServerRequest(
                serverRequest,
                idGenerator,
                VertxHttpServerRequestOptions.builder().path(RESOLVED_PATH).build()
            );

            // The received bytes stay available, which is what an operator correlates with the
            // logs of whatever sits in front of the gateway.
            assertThat(request.uri()).isEqualTo(NATIVE_PATH);
        }

        @Test
        void should_fall_back_to_the_native_path_when_nothing_is_supplied() {
            var request = new VertxHttpServerRequest(serverRequest, idGenerator, VertxHttpServerRequestOptions.builder().build());

            assertThat(request.path()).isEqualTo(NATIVE_PATH);
        }
    }

    @Nested
    class The_timestamp {

        @Test
        void should_be_the_instant_the_dispatcher_started_handling_the_request() {
            // A value far enough in the past that no clock reading could produce it by accident,
            // which is the whole point: the wrapper must not take its own.
            final long receivedAt = System.currentTimeMillis() - 60_000;

            var request = new VertxHttpServerRequest(
                serverRequest,
                idGenerator,
                VertxHttpServerRequestOptions.builder().timestamp(receivedAt).build()
            );

            assertThat(request.timestamp()).isEqualTo(receivedAt);
        }

        @Test
        void should_reach_the_metrics_so_that_every_latency_derives_from_it() {
            final long receivedAt = System.currentTimeMillis() - 60_000;

            var request = new VertxHttpServerRequest(
                serverRequest,
                idGenerator,
                VertxHttpServerRequestOptions.builder().timestamp(receivedAt).build()
            );

            // This is the link that makes the decision worth anything: whatever the dispatcher did
            // before the wrapper existed now falls inside the window the metrics measure.
            assertThat(request.metrics().timestamp()).isEqualTo(Instant.ofEpochMilli(receivedAt));
        }

        @Test
        void should_stamp_the_clock_itself_when_nothing_is_supplied() {
            final long before = System.currentTimeMillis();

            var request = new VertxHttpServerRequest(serverRequest, idGenerator, VertxHttpServerRequestOptions.builder().build());

            // The historical behaviour, kept so that every existing caller of the deprecated
            // constructors keeps working exactly as it did.
            assertThat(request.timestamp()).isBetween(before, System.currentTimeMillis());
        }
    }

    @Nested
    class The_deprecated_constructor {

        /**
         * The two-argument form is the only one this branch inherited, so it is the only one a
         * plugin can have been compiled against and the only one worth keeping alive.
         */
        @Test
        void should_still_report_the_native_path() {
            var request = new VertxHttpServerRequest(serverRequest, idGenerator);

            assertThat(request.path()).isEqualTo(NATIVE_PATH);
        }
    }
}
