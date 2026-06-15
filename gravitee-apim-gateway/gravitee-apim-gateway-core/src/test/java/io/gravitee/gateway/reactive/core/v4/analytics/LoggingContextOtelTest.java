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
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.reporter.api.ReportTarget;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests LoggingContext.otelLogsEnabled flag behaviour:
 * when otelLogsEnabled=true, all payload methods return true even with null Logging config.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingContextOtelTest {

    @Test
    void entrypointRequestPayload_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.entrypointRequestPayload()).isTrue();
    }

    @Test
    void endpointRequestPayload_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.endpointRequestPayload()).isTrue();
    }

    @Test
    void entrypointResponsePayload_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.entrypointResponsePayload()).isTrue();
    }

    @Test
    void endpointResponsePayload_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.endpointResponsePayload()).isTrue();
    }

    @Test
    void entrypointRequest_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.entrypointRequest()).isTrue();
    }

    @Test
    void entrypointResponse_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.entrypointResponse()).isTrue();
    }

    @Test
    void endpointRequest_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.endpointRequest()).isTrue();
    }

    @Test
    void endpointResponse_should_return_true_when_otelLogsEnabled_and_logging_is_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);
        assertThat(ctx.endpointResponse()).isTrue();
    }

    @Test
    void all_direction_methods_should_return_false_when_otelLogsEnabled_false_and_logging_null() {
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(false);

        assertThat(ctx.entrypointRequest()).isFalse();
        assertThat(ctx.entrypointResponse()).isFalse();
        assertThat(ctx.endpointRequest()).isFalse();
        assertThat(ctx.endpointResponse()).isFalse();
        assertThat(ctx.entrypointRequestPayload()).isFalse();
        assertThat(ctx.endpointRequestPayload()).isFalse();
        assertThat(ctx.entrypointResponsePayload()).isFalse();
        assertThat(ctx.endpointResponsePayload()).isFalse();
    }

    @Test
    void headers_methods_should_return_false_when_only_otelLogsEnabled_and_logging_null() {
        // Header capture is NOT widened by otelLogs — only payloads are
        LoggingContext ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);

        assertThat(ctx.entrypointRequestHeaders()).isFalse();
        assertThat(ctx.endpointRequestHeaders()).isFalse();
        assertThat(ctx.entrypointResponseHeaders()).isFalse();
        assertThat(ctx.endpointResponseHeaders()).isFalse();
    }

    @Nested
    class ComputeReportTargets {

        @Test
        void should_return_tracing_only_when_otel_logs_on_and_es_logging_off() {
            LoggingContext ctx = new LoggingContext(null);
            ctx.setOtelLogsEnabled(true);
            assertThat(ctx.computeReportTargets()).isEqualTo(EnumSet.of(ReportTarget.TRACING));
        }

        @Test
        void should_return_analytics_only_when_otel_logs_off_and_es_logging_on() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEntrypoint(true);
            logging.setMode(mode);

            LoggingContext ctx = new LoggingContext(logging);
            ctx.setOtelLogsEnabled(false);
            assertThat(ctx.computeReportTargets()).isEqualTo(EnumSet.of(ReportTarget.ANALYTICS));
        }

        @Test
        void should_return_both_when_otel_logs_on_and_es_logging_on() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEntrypoint(true);
            logging.setMode(mode);

            LoggingContext ctx = new LoggingContext(logging);
            ctx.setOtelLogsEnabled(true);
            assertThat(ctx.computeReportTargets()).isEqualTo(ReportTarget.ALL);
        }

        @Test
        void should_return_analytics_when_neither_otel_logs_nor_es_logging() {
            LoggingContext ctx = new LoggingContext(null);
            ctx.setOtelLogsEnabled(false);
            assertThat(ctx.computeReportTargets()).containsExactly(ReportTarget.ANALYTICS);
        }
    }
}
