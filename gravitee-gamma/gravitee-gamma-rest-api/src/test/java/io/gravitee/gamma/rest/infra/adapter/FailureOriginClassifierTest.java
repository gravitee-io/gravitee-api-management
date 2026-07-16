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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.logs.model.FailureOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FailureOriginClassifierTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void should_be_none_without_error_key_when_status_is_not_internal(String errorKey) {
        assertThat(FailureOriginClassifier.classify(errorKey, "CONNECTED")).isEqualTo(FailureOrigin.NONE);
    }

    @Test
    void should_be_gateway_internal_without_error_key_when_status_is_internal() {
        assertThat(FailureOriginClassifier.classify(null, "INTERNAL_ERROR")).isEqualTo(FailureOrigin.GATEWAY_INTERNAL);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SASL_AUTHENTICATION_FAILED", "ILLEGAL_SASL_STATE", "UNSUPPORTED_SASL_MECHANISM", "SECURITY_DISABLED" })
    void should_localize_authentication_failures_between_client_and_gateway(String errorKey) {
        assertThat(FailureOriginClassifier.classify(errorKey, "CONNECTION_ERROR")).isEqualTo(FailureOrigin.CLIENT_TO_GATEWAY);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "BROKER_NOT_AVAILABLE",
            "COORDINATOR_NOT_AVAILABLE",
            "TOPIC_AUTHORIZATION_FAILED",
            "GROUP_AUTHORIZATION_FAILED",
            "REBALANCE_IN_PROGRESS",
            "UNKNOWN_TOPIC_OR_PARTITION",
            "ILLEGAL_GENERATION",
            "MEMBER_ID_REQUIRED",
            "INVALID_TXN_STATE",
            "NOT_COORDINATOR",
            "LEADER_NOT_AVAILABLE",
            "OFFSET_OUT_OF_RANGE",
        }
    )
    void should_localize_broker_domain_failures_between_gateway_and_broker(String errorKey) {
        assertThat(FailureOriginClassifier.classify(errorKey, "SESSION_ERROR")).isEqualTo(FailureOrigin.GATEWAY_TO_BROKER);
    }

    @Test
    void should_localize_unknown_server_error_as_gateway_internal() {
        assertThat(FailureOriginClassifier.classify("UNKNOWN_SERVER_ERROR", "SESSION_ERROR")).isEqualTo(FailureOrigin.GATEWAY_INTERNAL);
    }

    @ParameterizedTest
    @CsvSource(
        {
            // Unclassified keys: the connection phase localizes the client side; outside it the
            // side is honestly undetermined rather than guessed.
            "SOME_FUTURE_ERROR, CONNECTION_ERROR, CLIENT_TO_GATEWAY",
            "SOME_FUTURE_ERROR, SESSION_ERROR, UNKNOWN",
            "SOME_FUTURE_ERROR, INTERNAL_ERROR, GATEWAY_INTERNAL",
            "SOME_FUTURE_ERROR, DISCONNECTED, UNKNOWN",
            // Genuinely side-ambiguous Kafka keys are undetermined too.
            "NETWORK_EXCEPTION, SESSION_ERROR, UNKNOWN",
            "REQUEST_TIMED_OUT, SESSION_ERROR, UNKNOWN",
        }
    )
    void should_fall_back_on_connection_status_for_unclassified_keys(String errorKey, String status, FailureOrigin expected) {
        assertThat(FailureOriginClassifier.classify(errorKey, status)).isEqualTo(expected);
    }

    @Test
    void should_classify_case_insensitively() {
        assertThat(FailureOriginClassifier.classify("sasl_authentication_failed", "CONNECTION_ERROR")).isEqualTo(
            FailureOrigin.CLIENT_TO_GATEWAY
        );
    }
}
