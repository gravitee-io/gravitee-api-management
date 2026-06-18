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
package io.gravitee.gateway.reactive.http.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.http.vertx.ClientCloseClassifier.ClientCloseReason;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.vertx.core.http.HttpClosedException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

/**
 * Pins the (key, message) mapping for client-close reasons (APIM-12769) and the decoration lifecycle guards: a
 * completed or already-classified request is never rewritten, and the cause-chain walk always terminates.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCloseClassifierTest {

    @Test
    void should_classify_connection_reset_as_tcp_reset() {
        ClientCloseReason reason = ClientCloseClassifier.classify(new IOException("Connection reset by peer"));
        assertThat(reason.key()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET);
        assertThat(reason.message()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET_MESSAGE);
    }

    @Test
    void should_classify_wrapped_connection_reset_via_cause_chain() {
        ClientCloseReason reason = ClientCloseClassifier.classify(
            new RuntimeException("wrapper", new IOException("Connection reset by peer"))
        );
        assertThat(reason.key()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET);
    }

    @Test
    void should_classify_case_insensitively() {
        // Defensive against platform/JVM wording differences (e.g. a lowercased message).
        assertThat(ClientCloseClassifier.classify(new IOException("connection reset by peer")).key()).isEqualTo(
            ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET
        );
        assertThat(ClientCloseClassifier.classify(new IOException("BROKEN PIPE")).key()).isEqualTo(
            ClientCloseClassifier.CLIENT_ABORTED_BROKEN_PIPE
        );
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("syscall: Connection RESET by peer"))).isTrue();
    }

    @Test
    void should_classify_broken_pipe() {
        ClientCloseReason reason = ClientCloseClassifier.classify(new IOException("Broken pipe"));
        assertThat(reason.key()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_BROKEN_PIPE);
        assertThat(reason.message()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_BROKEN_PIPE_MESSAGE);
    }

    @Test
    void should_classify_closed_channel_exception_as_channel_closed() {
        ClientCloseReason reason = ClientCloseClassifier.classify(new ClosedChannelException());
        assertThat(reason.key()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED);
        assertThat(reason.message()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
    }

    @Test
    void should_recognise_vertx_http_closed_exception_as_a_client_close() {
        // Vert.x's HttpClosedException ("Connection was closed") is the common signal for a client closing an
        // in-flight request on this stack — it must be treated as a client close, not a generic gateway error.
        HttpClosedException closed = new HttpClosedException("Connection was closed");
        assertThat(ClientCloseClassifier.classify(closed).key()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED);
        assertThat(ClientCloseClassifier.isClientConnectionClose(closed)).isTrue();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("Connection was closed"))).isTrue();
    }

    @Test
    void should_not_treat_a_genuine_gateway_error_as_a_client_close() {
        // C1 guard: arbitrary throwables on the response stream must NOT be classified as client aborts, otherwise
        // a real gateway fault gets masked as a 499.
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IllegalStateException("policy blew up"))).isFalse();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("No space left on device"))).isFalse();
    }

    @Test
    void should_default_unrecognised_cause_to_channel_closed() {
        ClientCloseReason reason = ClientCloseClassifier.classify(new RuntimeException("something else"));
        assertThat(reason.key()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED);
        assertThat(reason.message()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
    }

    @Test
    void should_terminate_on_a_cyclic_cause_chain() {
        // A 2-node cycle (a -> b -> a) is not caught by the self-loop check; without the depth cap the chain walk
        // would pin the event-loop thread forever. Runs on the close hot path, so this must always terminate.
        Throwable a = new IOException("a");
        Throwable b = new IOException("b", a);
        a.initCause(b);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            ClientCloseClassifier.classify(a);
            ClientCloseClassifier.isClientConnectionClose(a);
        });
    }

    @Test
    void should_recognise_client_close_from_native_transport_messages() {
        // Netty native transports (epoll/kqueue) wrap the kernel error: exact message matching misses these,
        // which made client aborts mid-response silently reported as clean 200s on Linux.
        assertThat(
            ClientCloseClassifier.isClientConnectionClose(new IOException("writevAddresses(..) failed: Connection reset by peer"))
        ).isTrue();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("sendmsg(..) failed: Broken pipe"))).isTrue();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("Connection reset by peer"))).isTrue();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("Broken pipe"))).isTrue();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new ClosedChannelException())).isTrue();
        assertThat(
            ClientCloseClassifier.isClientConnectionClose(new RuntimeException("wrap", new IOException("Connection reset")))
        ).isTrue();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException("No space left on device"))).isFalse();
        assertThat(ClientCloseClassifier.isClientConnectionClose(new IOException((String) null))).isFalse();
    }

    @Test
    void should_decorate_in_flight_request_metrics_with_classified_close() {
        HttpBaseExecutionContext ctx = mockContext();
        Metrics metrics = Metrics.builder().build();
        when(ctx.metrics()).thenReturn(metrics);

        ClientCloseClassifier.decorate(ctx, new IOException("Connection reset by peer"));

        assertThat(metrics.getErrorKey()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET);
        assertThat(metrics.getFailure()).isNotNull();
        assertThat(metrics.getFailure().getKey()).isEqualTo(ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET);
    }

    @Test
    void should_not_decorate_when_metrics_are_absent() {
        HttpBaseExecutionContext ctx = mockContext();
        when(ctx.metrics()).thenReturn(null);

        // Must not throw when the metrics processor has not run yet (very early abort).
        ClientCloseClassifier.decorate(ctx, new IOException("Connection reset by peer"));
    }

    @Test
    void should_not_decorate_metrics_of_an_already_completed_request() {
        // A close on a kept-alive connection between requests (client RST, server idle reap) must never rewrite the
        // metrics of the request that already completed on that connection.
        HttpBaseExecutionContext ctx = mockContext();
        Metrics metrics = Metrics.builder().build();
        metrics.setRequestEnded(true);
        when(ctx.metrics()).thenReturn(metrics);

        ClientCloseClassifier.decorate(
            ctx,
            ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET,
            ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET_MESSAGE,
            new IOException("Connection reset by peer")
        );

        assertThat(metrics.getErrorKey()).isNull();
        assertThat(metrics.getErrorMessage()).isNull();
        assertThat(metrics.getFailure()).isNull();
    }

    @Test
    void should_not_overwrite_an_already_classified_failure() {
        HttpBaseExecutionContext ctx = mockContext();
        Metrics metrics = Metrics.builder().build();
        metrics.setErrorKey("GATEWAY_CLIENT_CONNECTION_RESET");
        when(ctx.metrics()).thenReturn(metrics);

        ClientCloseClassifier.decorate(
            ctx,
            ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED,
            ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE,
            null
        );

        assertThat(metrics.getErrorKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_RESET");
        assertThat(metrics.getFailure()).isNull();
    }

    @Test
    void should_not_overwrite_when_only_a_failure_is_set_and_error_key_is_null() {
        // interruptWith sets failure but leaves errorKey null; that record must not be overwritten by a client close.
        HttpBaseExecutionContext ctx = mockContext();
        Metrics metrics = Metrics.builder().build();
        metrics.setFailure(new Diagnostic("GATEWAY_OAUTH2_ACCESS_DENIED", "denied", "SECURITY", "oauth2"));
        when(ctx.metrics()).thenReturn(metrics);

        ClientCloseClassifier.decorate(ctx, new IOException("Connection reset by peer"));

        assertThat(metrics.getErrorKey()).isNull();
        assertThat(metrics.getFailure().getKey()).isEqualTo("GATEWAY_OAUTH2_ACCESS_DENIED");
    }

    private static HttpBaseExecutionContext mockContext() {
        HttpBaseExecutionContext ctx = mock(HttpBaseExecutionContext.class);
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        return ctx;
    }
}
