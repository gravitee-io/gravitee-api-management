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
package io.gravitee.plugin.endpoint.http.proxy.failure;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.plugin.endpoint.http.proxy.failure.ConnectionFailureClassifier.Classification;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.StreamResetException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;

class ConnectionFailureClassifierTest {

    @Test
    void should_classify_connection_refused_as_bad_gateway() {
        Classification classification = ConnectionFailureClassifier.classify(new ConnectException("Connection refused"));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_REFUSED");
        assertThat(classification.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
    }

    @Test
    void should_classify_unknown_host_as_dns_resolution_error() {
        Classification classification = ConnectionFailureClassifier.classify(new UnknownHostException("api.example.invalid"));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_DNS_RESOLUTION_ERROR");
        assertThat(classification.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
    }

    @Test
    void should_classify_connect_timeout_distinctly_from_its_connect_exception_supertype() {
        // ConnectTimeoutException extends ConnectException: ordering must keep it out of the "refused" bucket.
        Classification classification = ConnectionFailureClassifier.classify(new ConnectTimeoutException("connection timed out"));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_CONNECT_TIMEOUT");
        assertThat(classification.parentKey()).isEqualTo("REQUEST_TIMEOUT");
    }

    @Test
    void should_classify_read_timeout_distinctly_from_the_global_request_timeout() {
        Classification readTimeout = ConnectionFailureClassifier.classify(ReadTimeoutException.INSTANCE);
        Classification genericTimeout = ConnectionFailureClassifier.classify(new TimeoutException("idle"));

        // The backend read timeout gets its own key so it is distinguishable from the global REQUEST_TIMEOUT, while
        // keeping REQUEST_TIMEOUT as its parent so existing templates/dashboards keep matching.
        assertThat(readTimeout.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(readTimeout.key()).isEqualTo("GATEWAY_CLIENT_READ_TIMEOUT");
        assertThat(readTimeout.parentKey()).isEqualTo("REQUEST_TIMEOUT");
        assertThat(genericTimeout.key()).isEqualTo("GATEWAY_CLIENT_READ_TIMEOUT");
    }

    @Test
    void should_classify_circuit_breaker_timeout_as_read_timeout() {
        Classification classification = ConnectionFailureClassifier.classify(io.vertx.circuitbreaker.TimeoutException.INSTANCE);

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_READ_TIMEOUT");
        assertThat(classification.parentKey()).isEqualTo("REQUEST_TIMEOUT");
    }

    @Test
    void should_classify_tls_handshake_failure_before_generic_io() {
        Classification classification = ConnectionFailureClassifier.classify(new SSLHandshakeException("PKIX path building failed"));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_TLS_HANDSHAKE_ERROR");
        assertThat(classification.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
    }

    @Test
    void should_classify_connection_reset_via_message_fallback() {
        Classification classification = ConnectionFailureClassifier.classify(new IOException("Connection reset by peer"));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_RESET");
        assertThat(classification.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
    }

    @Test
    void should_classify_host_unreachable_by_type_and_by_message() {
        Classification byType = ConnectionFailureClassifier.classify(new NoRouteToHostException("No route to host"));
        Classification byMessage = ConnectionFailureClassifier.classify(new IOException("Network is unreachable"));

        assertThat(byType.key()).isEqualTo("GATEWAY_CLIENT_UNREACHABLE");
        assertThat(byType.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
        assertThat(byMessage.key()).isEqualTo("GATEWAY_CLIENT_UNREACHABLE");
    }

    @Test
    void should_disambiguate_native_transport_connect_exceptions_by_message() {
        // Netty epoll/kqueue surface these as plain ConnectException: they must not be filed under "refused".
        Classification noRoute = ConnectionFailureClassifier.classify(new ConnectException("connect(..) failed: No route to host"));
        Classification osTimeout = ConnectionFailureClassifier.classify(new ConnectException("Connection timed out"));

        assertThat(noRoute.key()).isEqualTo("GATEWAY_CLIENT_UNREACHABLE");
        assertThat(osTimeout.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(osTimeout.key()).isEqualTo("GATEWAY_CLIENT_CONNECT_TIMEOUT");
        assertThat(osTimeout.parentKey()).isEqualTo("REQUEST_TIMEOUT");
    }

    @Test
    void should_classify_http2_stream_reset_as_connection_reset_by_type() {
        Classification classification = ConnectionFailureClassifier.classify(new StreamResetException(8));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_RESET");
        assertThat(classification.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
    }

    @Test
    void should_classify_premature_backend_close_by_type_and_by_message() {
        Classification byHttpClosed = ConnectionFailureClassifier.classify(new HttpClosedException("Connection was closed"));
        Classification byChannel = ConnectionFailureClassifier.classify(new ClosedChannelException());
        Classification byMessage = ConnectionFailureClassifier.classify(new IOException("Connection was closed"));

        assertThat(byHttpClosed.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(byHttpClosed.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
        assertThat(byHttpClosed.parentKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
        assertThat(byChannel.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
        assertThat(byMessage.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
    }

    @Test
    void should_classify_a_timeout_before_connection_acquisition_as_connect_timeout() {
        // A request-level timeout fires with the same type whether we were waiting for a connection (pool saturation,
        // slow DNS, slow connect) or reading the backend response. The connector signals which by whether a connection
        // was acquired — a structural fact we own — so the classifier never has to parse the Vert.x timeout message.
        TimeoutException sameTimeout = new TimeoutException("The timeout of 4000 ms has been exceeded");

        Classification beforeConnection = ConnectionFailureClassifier.classify(sameTimeout, false);
        Classification afterConnection = ConnectionFailureClassifier.classify(sameTimeout, true);

        assertThat(beforeConnection.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(beforeConnection.key()).isEqualTo("GATEWAY_CLIENT_CONNECT_TIMEOUT");
        assertThat(beforeConnection.parentKey()).isEqualTo("REQUEST_TIMEOUT");
        assertThat(afterConnection.key()).isEqualTo("GATEWAY_CLIENT_READ_TIMEOUT");
    }

    @Test
    void should_unwrap_a_wrapped_cause() {
        Throwable wrapped = new RuntimeException("upstream failure", new ConnectException("Connection refused"));

        Classification classification = ConnectionFailureClassifier.classify(wrapped);

        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_REFUSED");
    }

    @Test
    void should_degrade_unknown_failure_to_umbrella() {
        Classification classification = ConnectionFailureClassifier.classify(new IOException("Bad Gateway"));

        assertThat(classification.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(classification.key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
        assertThat(classification.parentKey()).isNull();
    }

    @Test
    void should_not_classify_arbitrary_message_text_as_a_specific_key() {
        // The message fallback matches anchored phrases only: a bare word like "reset" or "unreachable" embedded in
        // unrelated text (e.g. a backend error body) must degrade to the umbrella, not a confident wrong 502 key.
        assertThat(ConnectionFailureClassifier.classify(new IOException("failed to reset cache entry")).key()).isEqualTo(
            "GATEWAY_CLIENT_CONNECTION_ERROR"
        );
        assertThat(
            ConnectionFailureClassifier.classify(new IOException("upstream marked service unreachable in registry")).key()
        ).isEqualTo("GATEWAY_CLIENT_CONNECTION_ERROR");
    }
}
