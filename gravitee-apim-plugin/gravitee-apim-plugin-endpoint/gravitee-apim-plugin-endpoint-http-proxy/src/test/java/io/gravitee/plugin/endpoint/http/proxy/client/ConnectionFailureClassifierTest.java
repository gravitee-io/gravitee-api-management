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
package io.gravitee.plugin.endpoint.http.proxy.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.StreamResetException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the shared {@link ConnectionFailureClassifier}, focused on the cases the connector test does
 * not exercise directly: HTTP/2 stream resets, typed pooled-connection close, and cause-chain unwrapping.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionFailureClassifierTest {

    @Test
    void should_classify_http2_stream_reset_as_upstream_connection_reset() {
        ConnectionFailureClassifier.Classification c = ConnectionFailureClassifier.classify(new StreamResetException(8L));
        assertThat(c.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(c.key()).isEqualTo(ConnectionFailureClassifier.UPSTREAM_CONNECTION_RESET);
    }

    @Test
    void should_classify_http_closed_exception_as_upstream_connection_closed_by_type() {
        // Pool-reuse stale connection: typed match, independent of the message wording.
        ConnectionFailureClassifier.Classification c = ConnectionFailureClassifier.classify(new HttpClosedException("anything"));
        assertThat(c.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(c.key()).isEqualTo(ConnectionFailureClassifier.UPSTREAM_CONNECTION_CLOSED);
    }

    @Test
    void should_classify_idle_timeout() {
        ConnectionFailureClassifier.Classification c = ConnectionFailureClassifier.classify(new UpstreamIdleTimeoutException("idle"));
        assertThat(c.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(c.key()).isEqualTo(ConnectionFailureClassifier.UPSTREAM_IDLE_TIMEOUT);
    }

    @Test
    void should_unwrap_dns_failure_from_the_cause_chain() {
        ConnectionFailureClassifier.Classification c = ConnectionFailureClassifier.classify(
            new RuntimeException("wrapper", new UnknownHostException("api.invalid"))
        );
        assertThat(c.key()).isEqualTo(ConnectionFailureClassifier.UPSTREAM_DNS_FAILURE);
    }

    @Test
    void should_classify_connection_refused_before_treating_it_as_a_message() {
        ConnectionFailureClassifier.Classification c = ConnectionFailureClassifier.classify(new ConnectException("Connection refused"));
        assertThat(c.key()).isEqualTo(ConnectionFailureClassifier.UPSTREAM_CONNECTION_REFUSED);
    }

    @Test
    void should_fall_back_to_gateway_client_connection_error_for_unrecognised_failure() {
        ConnectionFailureClassifier.Classification c = ConnectionFailureClassifier.classify(new RuntimeException("something unexpected"));
        assertThat(c.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(c.key()).isEqualTo(ConnectionFailureClassifier.GATEWAY_CLIENT_CONNECTION_ERROR);
    }
}
