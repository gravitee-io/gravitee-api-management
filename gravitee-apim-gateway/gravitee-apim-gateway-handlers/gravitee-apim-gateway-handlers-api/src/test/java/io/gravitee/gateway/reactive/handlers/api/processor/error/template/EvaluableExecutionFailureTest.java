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
package io.gravitee.gateway.reactive.handlers.api.processor.error.template;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class EvaluableExecutionFailureTest {

    @Test
    void should_expose_cause_from_diagnostic_message() {
        ExecutionFailure failure = new ExecutionFailure(HttpStatusCode.BAD_GATEWAY_502).key("GATEWAY_CLIENT_TLS_HANDSHAKE_ERROR");

        EvaluableExecutionFailure evaluable = new EvaluableExecutionFailure(failure, "Received fatal alert: handshake_failure");

        assertThat(evaluable.getCause()).isEqualTo("Received fatal alert: handshake_failure");
    }

    @Test
    void should_fallback_cause_to_message_when_diagnostic_message_is_null() {
        ExecutionFailure failure = new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).key("POLICY_ERROR_KEY").message("Policy failed");

        EvaluableExecutionFailure evaluable = new EvaluableExecutionFailure(failure, null);

        assertThat(evaluable.getCause()).isEqualTo("Policy failed");
    }

    @Test
    void should_return_null_cause_when_no_diagnostic_and_no_message() {
        ExecutionFailure failure = new ExecutionFailure(HttpStatusCode.BAD_GATEWAY_502).key("GATEWAY_CLIENT_TLS_HANDSHAKE_ERROR");

        EvaluableExecutionFailure evaluable = new EvaluableExecutionFailure(failure, null);

        assertThat(evaluable.getCause()).isNull();
    }

    @Test
    void should_leave_message_unchanged() {
        ExecutionFailure failure = new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).key("POLICY_ERROR_KEY").message("Policy failed");

        // Even with a diagnostic cause, getMessage() keeps returning the raw ExecutionFailure message (no breaking change).
        EvaluableExecutionFailure evaluable = new EvaluableExecutionFailure(failure, "Policy failed (some cause)");

        assertThat(evaluable.getMessage()).isEqualTo("Policy failed");
    }
}
