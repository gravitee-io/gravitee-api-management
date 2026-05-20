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
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class LoggingContextTest {

    @Test
    void should_return_true_for_all_direction_methods_when_otelLogs_enabled() {
        var ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);

        assertThat(ctx.entrypointRequest()).isTrue();
        assertThat(ctx.entrypointResponse()).isTrue();
        assertThat(ctx.endpointRequest()).isTrue();
        assertThat(ctx.endpointResponse()).isTrue();
    }

    @Test
    void should_return_true_for_all_payload_methods_when_otelLogs_enabled() {
        var ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);

        assertThat(ctx.entrypointRequestPayload()).isTrue();
        assertThat(ctx.entrypointResponsePayload()).isTrue();
        assertThat(ctx.endpointRequestPayload()).isTrue();
        assertThat(ctx.endpointResponsePayload()).isTrue();
    }

    @Test
    void should_not_widen_headers_methods_when_otelLogs_enabled() {
        // otelLogs only widens direction + payload — headers capture is ES-logging-only
        var ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(true);

        assertThat(ctx.entrypointRequestHeaders()).isFalse();
        assertThat(ctx.entrypointResponseHeaders()).isFalse();
        assertThat(ctx.endpointRequestHeaders()).isFalse();
        assertThat(ctx.endpointResponseHeaders()).isFalse();
    }

    @Test
    void should_return_false_for_all_direction_methods_when_otelLogs_disabled_and_no_logging() {
        var ctx = new LoggingContext(null);
        ctx.setOtelLogsEnabled(false);

        assertThat(ctx.entrypointRequest()).isFalse();
        assertThat(ctx.entrypointResponse()).isFalse();
        assertThat(ctx.endpointRequest()).isFalse();
        assertThat(ctx.endpointResponse()).isFalse();
        assertThat(ctx.entrypointRequestPayload()).isFalse();
        assertThat(ctx.entrypointResponsePayload()).isFalse();
        assertThat(ctx.endpointRequestPayload()).isFalse();
        assertThat(ctx.endpointResponsePayload()).isFalse();
    }

    @Test
    void should_fall_back_to_logging_config_when_otelLogs_disabled() {
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setContent(LoggingContent.builder().headers(true).payload(true).build());
        logging.setPhase(LoggingPhase.builder().request(true).response(true).build());

        var ctx = new LoggingContext(logging);
        ctx.setOtelLogsEnabled(false);

        assertThat(ctx.entrypointRequest()).isTrue();
        assertThat(ctx.entrypointResponse()).isTrue();
        assertThat(ctx.endpointRequest()).isTrue();
        assertThat(ctx.endpointResponse()).isTrue();
        assertThat(ctx.entrypointRequestPayload()).isTrue();
        assertThat(ctx.entrypointResponsePayload()).isTrue();
        assertThat(ctx.endpointRequestPayload()).isTrue();
        assertThat(ctx.endpointResponsePayload()).isTrue();
        assertThat(ctx.entrypointRequestHeaders()).isTrue();
        assertThat(ctx.entrypointResponseHeaders()).isTrue();
        assertThat(ctx.endpointRequestHeaders()).isTrue();
        assertThat(ctx.endpointResponseHeaders()).isTrue();
    }

    @Test
    void should_return_empty_condition_when_logging_is_null() {
        var ctx = new LoggingContext(null);

        assertThat(ctx.getCondition()).isEmpty();
    }

    @Test
    void should_return_condition_from_logging_when_provided() {
        final Logging logging = new Logging();
        logging.setCondition("{#response.status == 200}");
        logging.setMode(LoggingMode.builder().build());
        logging.setContent(LoggingContent.builder().build());
        logging.setPhase(LoggingPhase.builder().build());

        var ctx = new LoggingContext(logging);

        assertThat(ctx.getCondition()).isEqualTo("{#response.status == 200}");
    }
}
