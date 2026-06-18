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

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.context.diagnostic.DiagnosticReportHelper;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.vertx.core.http.HttpClosedException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import lombok.CustomLog;

/**
 * Classifies a client→gateway connection close (TCP reset, broken pipe, plain channel close) into a stable error
 * key and decorates the in-flight request {@link Metrics} with it, so a client abort is observable in access logs
 * and analytics with its actual reason instead of the single generic {@code CLIENT_ABORTED_DURING_RESPONSE_ERROR}
 * (APIM-12769).
 * <p>
 * The decoration always targets the {@link HttpBaseExecutionContext} of the request that is closing — it is invoked
 * from points where that context is already in scope (the response write-failure path in
 * {@link VertxHttpServerResponse}, and the per-request stream handlers registered by the dispatcher). It deliberately
 * does <b>not</b> rely on any connection-scoped state (e.g. a Netty channel attribute): a connection is reused across
 * keep-alive requests and multiplexes concurrent HTTP/2 streams, so connection-scoped request state cannot be
 * correlated back to a single request without races.
 */
@CustomLog
public final class ClientCloseClassifier {

    public static final String CLIENT_ABORTED_TCP_RESET = "CLIENT_ABORTED_TCP_RESET";
    public static final String CLIENT_ABORTED_BROKEN_PIPE = "CLIENT_ABORTED_BROKEN_PIPE";
    public static final String CLIENT_ABORTED_CHANNEL_CLOSED = "CLIENT_ABORTED_CHANNEL_CLOSED";
    public static final String CLIENT_ABORTED_TCP_RESET_MESSAGE = "The client reset the connection";
    public static final String CLIENT_ABORTED_BROKEN_PIPE_MESSAGE = "The client closed the connection while the response was being written";
    public static final String CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE = "The client closed the connection";

    private static final int CLIENT_CLOSED_REQUEST_499 = 499;
    private static final int MAX_CAUSE_DEPTH = 20;

    private ClientCloseClassifier() {}

    public record ClientCloseReason(String key, String message) {}

    /**
     * Map a client-close {@link Throwable} to a stable (key, message) pair. The whole cause chain is scanned because
     * the close cause is frequently wrapped — native transports wrap the kernel error (e.g.
     * {@code "writevAddresses(..) failed: Connection reset by peer"} on epoll), so matching only the top-level message
     * misses them. Matching is case-insensitive so it stays robust across OS/JVM/native-transport wordings.
     * Consistent with the upstream connector's classifier.
     */
    public static ClientCloseReason classify(Throwable cause) {
        if (hasCause(cause, ClosedChannelException.class) || hasCause(cause, HttpClosedException.class)) {
            return new ClientCloseReason(CLIENT_ABORTED_CHANNEL_CLOSED, CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
        }
        String message = collectChainMessages(cause);
        if (message != null) {
            String lowerMessage = message.toLowerCase(Locale.ENGLISH);
            if (lowerMessage.contains("connection reset")) {
                return new ClientCloseReason(CLIENT_ABORTED_TCP_RESET, CLIENT_ABORTED_TCP_RESET_MESSAGE);
            }
            if (lowerMessage.contains("broken pipe")) {
                return new ClientCloseReason(CLIENT_ABORTED_BROKEN_PIPE, CLIENT_ABORTED_BROKEN_PIPE_MESSAGE);
            }
        }
        // Any other IO/connection error reaching a close handler is still a client-initiated close.
        return new ClientCloseReason(CLIENT_ABORTED_CHANNEL_CLOSED, CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
    }

    /**
     * Whether the given write/stream failure denotes the client closing the connection, scanning the cause chain
     * (case-insensitively) so that wrapped kernel errors are matched too.
     */
    public static boolean isClientConnectionClose(Throwable t) {
        // Vert.x signals a connection close on an in-flight request with HttpClosedException ("Connection was closed");
        // a fully-closed NIO channel surfaces as ClosedChannelException. Both are genuine client-side closes, not
        // gateway logic faults — recognizing them keeps a mid-stream client abort from being reported as a clean 2xx.
        if (hasCause(t, ClosedChannelException.class) || hasCause(t, HttpClosedException.class)) {
            return true;
        }
        String messages = collectChainMessages(t);
        if (messages == null) {
            return false;
        }
        String lowerMessages = messages.toLowerCase(Locale.ENGLISH);
        return (
            lowerMessages.contains("connection reset") ||
            lowerMessages.contains("broken pipe") ||
            lowerMessages.contains("connection was closed")
        );
    }

    /** Classify the given close cause and decorate the in-flight request metrics with it. */
    public static void decorate(HttpBaseExecutionContext ctx, Throwable cause) {
        ClientCloseReason reason = classify(cause);
        decorate(ctx, reason.key(), reason.message(), cause);
    }

    /**
     * Decorate the in-flight request {@link Metrics} with a client-close failure, going through
     * {@link DiagnosticReportHelper} — the same path {@code AbstractExecutionContext.interruptWith} uses — so the
     * {@link Diagnostic} written here is <b>component-attributed</b> (the closing {@code ENDPOINT}/{@code SYSTEM}
     * component, via {@link ComponentScope}). No-op when there is no metrics yet, when the request already completed,
     * or when a more specific failure was already recorded (first-tagger wins).
     * <p>
     * The coarse phase-based fallbacks in {@code HttpProxyEndpointConnector} / {@code InvokerAdapter} deliberately set
     * only the flat {@code errorKey}/{@code errorMessage} (they run on the dispose path, where the plugin module has no
     * access to this helper and the component scope is already unwinding); the {@code ReporterProcessor} backfills a
     * Diagnostic from those flat fields at report time, so every key still ends up as a Diagnostic — just with an
     * "Unknown component" rather than the precise attribution this method gives.
     */
    public static void decorate(HttpBaseExecutionContext ctx, String key, String message, Throwable cause) {
        Metrics metrics = ctx.metrics();
        if (metrics == null) {
            // MetricsProcessor hasn't run yet (extremely early abort) — nothing to decorate.
            return;
        }
        if (metrics.isRequestEnded()) {
            // The request already completed and was reported: the close concerns a kept-alive connection dying
            // between requests, not an abort of this request — never rewrite the metrics of a finished request.
            return;
        }
        if (metrics.getFailure() != null || metrics.getErrorKey() != null) {
            // A more specific classifier (e.g. the upstream connector) or another close handler already set it.
            return;
        }
        ExecutionFailure failure = new ExecutionFailure(CLIENT_CLOSED_REQUEST_499).key(key).message(message);
        if (cause != null) {
            failure.cause(cause);
        }
        ComponentScope.ComponentEntry component = ComponentScope.peek((BaseExecutionContext) ctx);
        Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
            component,
            metrics.getErrorKey(),
            metrics.getErrorMessage(),
            failure
        );
        metrics.setFailure(diagnostic);
        // Legacy flat fields kept in sync so dashboards reading error-key/error-message still work.
        metrics.setErrorKey(diagnostic.getKey());
        metrics.setErrorMessage(diagnostic.getMessage());
        ctx.withLogger(log).debug("Classified client close reason as {}", key);
    }

    /** Walk the cause chain looking for a given exception type (the close cause may be wrapped). */
    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        Throwable current = t;
        // Bounded walk: the cap also breaks any cause cycle that would otherwise pin this event-loop thread forever.
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    /** Concatenate the messages along the cause chain so message-based detection survives wrapping. */
    private static String collectChainMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append('\n');
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
