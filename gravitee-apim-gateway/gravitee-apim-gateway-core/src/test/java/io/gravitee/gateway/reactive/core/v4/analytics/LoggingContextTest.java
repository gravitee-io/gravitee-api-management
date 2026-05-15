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

import io.gravitee.definition.model.v4.analytics.logging.Logging;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingContextTest {

    @Nested
    class TracingVerbose {

        @Test
        void entrypointRequest_returns_true_when_tracingVerbose_set() {
            var ctx = new LoggingContext(Logging.builder().build());
            ctx.setTracingVerbose(true);
            assertThat(ctx.entrypointRequest()).isTrue();
        }

        @Test
        void entrypointResponse_returns_true_when_tracingVerbose_set() {
            var ctx = new LoggingContext(Logging.builder().build());
            ctx.setTracingVerbose(true);
            assertThat(ctx.entrypointResponse()).isTrue();
        }

        @Test
        void entrypointRequestPayload_returns_true_when_tracingVerbose_set() {
            var ctx = new LoggingContext(Logging.builder().build());
            ctx.setTracingVerbose(true);
            assertThat(ctx.entrypointRequestPayload()).isTrue();
        }

        @Test
        void entrypointResponsePayload_returns_true_when_tracingVerbose_set() {
            var ctx = new LoggingContext(Logging.builder().build());
            ctx.setTracingVerbose(true);
            assertThat(ctx.entrypointResponsePayload()).isTrue();
        }

        @Test
        void entrypointRequest_returns_false_when_tracingVerbose_false_and_logging_disabled() {
            var ctx = new LoggingContext(Logging.builder().build());
            assertThat(ctx.entrypointRequest()).isFalse();
        }

        @Test
        void entrypointRequestPayload_returns_false_when_tracingVerbose_false_and_logging_disabled() {
            var ctx = new LoggingContext(Logging.builder().build());
            assertThat(ctx.entrypointRequestPayload()).isFalse();
        }
    }
}
