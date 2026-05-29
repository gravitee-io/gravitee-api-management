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

import io.gravitee.common.http.HttpStatusCode;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.vertx.circuitbreaker.TimeoutException;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.impl.NoStackTraceTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/**
 * Single source of truth for classifying a {@link Throwable} raised while contacting an upstream (gateway→backend)
 * into a (statusCode, errorKey, message) triple. Shared by every connector in the HTTP proxy plugin
 * (HTTP, WebSocket, gRPC) so they report the same taxonomy for the same failure (APIM-12769).
 * <p>
 * Classification is type-first (walking the whole cause chain, since Vert.x wraps low-level causes) and falls back
 * to message matching, finally to the generic {@link #GATEWAY_CLIENT_CONNECTION_ERROR}. The two pre-existing umbrella
 * keys ({@code REQUEST_TIMEOUT}, {@code GATEWAY_CLIENT_CONNECTION_ERROR}) are preserved so existing dashboards keep
 * working; the {@code UPSTREAM_*} keys carve out the actionable subcases. A {@code null} message means "derive it
 * from the cause".
 */
public final class ConnectionFailureClassifier {

    // Pre-existing umbrella keys (kept for dashboard backward-compatibility).
    public static final String GATEWAY_CLIENT_CONNECTION_ERROR = "GATEWAY_CLIENT_CONNECTION_ERROR";
    public static final String REQUEST_TIMEOUT = "REQUEST_TIMEOUT";

    // Carved-out upstream subcases (APIM-12769).
    public static final String UPSTREAM_CONNECT_TIMEOUT = "UPSTREAM_CONNECT_TIMEOUT";
    public static final String UPSTREAM_IDLE_TIMEOUT = "UPSTREAM_IDLE_TIMEOUT";
    public static final String UPSTREAM_DNS_FAILURE = "UPSTREAM_DNS_FAILURE";
    public static final String UPSTREAM_CONNECTION_REFUSED = "UPSTREAM_CONNECTION_REFUSED";
    public static final String UPSTREAM_TLS_FAILURE = "UPSTREAM_TLS_FAILURE";
    public static final String UPSTREAM_CONNECTION_RESET = "UPSTREAM_CONNECTION_RESET";
    public static final String UPSTREAM_BROKEN_PIPE = "UPSTREAM_BROKEN_PIPE";
    public static final String UPSTREAM_CONNECTION_CLOSED = "UPSTREAM_CONNECTION_CLOSED";

    public static final String UPSTREAM_CONNECT_TIMEOUT_MESSAGE = "Timed out while establishing a connection to the upstream";
    public static final String UPSTREAM_IDLE_TIMEOUT_MESSAGE = "The connection to the upstream was closed after the idle timeout elapsed";
    public static final String UPSTREAM_DNS_FAILURE_MESSAGE = "Unable to resolve the upstream host";
    public static final String UPSTREAM_CONNECTION_REFUSED_MESSAGE = "The upstream refused the connection";
    public static final String UPSTREAM_TLS_FAILURE_MESSAGE = "TLS handshake with the upstream failed";
    public static final String UPSTREAM_CONNECTION_RESET_MESSAGE = "The upstream reset the connection";
    public static final String UPSTREAM_BROKEN_PIPE_MESSAGE = "The connection to the upstream was broken while sending the request";
    public static final String UPSTREAM_CONNECTION_CLOSED_MESSAGE = "The upstream closed the connection unexpectedly";

    private ConnectionFailureClassifier() {}

    /** Result of classification. {@code message == null} means the caller should derive it from the cause. */
    public record Classification(int statusCode, String key, String message) {}

    /**
     * Classify an upstream failure. Order matters: more specific causes win, {@link ConnectTimeoutException} (which
     * extends {@link ConnectException}) is checked before connection-refused, and the typed checks run before the
     * message-based fallback.
     */
    public static Classification classify(final Throwable t) {
        if (
            hasCause(t, TimeoutException.class) ||
            hasCause(t, NoStackTraceTimeoutException.class) ||
            hasCause(t, ReadTimeoutException.class)
        ) {
            return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, REQUEST_TIMEOUT, null);
        }
        if (hasCause(t, ConnectTimeoutException.class)) {
            return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, UPSTREAM_CONNECT_TIMEOUT, UPSTREAM_CONNECT_TIMEOUT_MESSAGE);
        }
        if (hasCause(t, UpstreamIdleTimeoutException.class)) {
            return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, UPSTREAM_IDLE_TIMEOUT, UPSTREAM_IDLE_TIMEOUT_MESSAGE);
        }
        if (hasCause(t, UnknownHostException.class)) {
            return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_DNS_FAILURE, UPSTREAM_DNS_FAILURE_MESSAGE);
        }
        if (hasCause(t, ConnectException.class)) {
            return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_CONNECTION_REFUSED, UPSTREAM_CONNECTION_REFUSED_MESSAGE);
        }
        if (hasCause(t, SSLException.class)) {
            return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_TLS_FAILURE, UPSTREAM_TLS_FAILURE_MESSAGE);
        }
        // HTTP/2 RST_STREAM from the upstream (common gRPC failure mode).
        if (hasCause(t, StreamResetException.class)) {
            return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_CONNECTION_RESET, UPSTREAM_CONNECTION_RESET_MESSAGE);
        }
        // Pooled connection closed by the backend / reused-then-stale (typed, more robust than message matching).
        if (hasCause(t, HttpClosedException.class)) {
            return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_CONNECTION_CLOSED, UPSTREAM_CONNECTION_CLOSED_MESSAGE);
        }
        return classifyByMessage(t);
    }

    /**
     * Fallback when no specific type matched. Vert.x wraps low-level IO failures in {@code VertxException} (not an
     * {@link java.io.IOException}) and the meaningful text is often on a nested cause, so the whole cause chain is
     * scanned.
     */
    private static Classification classifyByMessage(final Throwable t) {
        final String message = collectChainMessages(t);
        if (message != null) {
            if (message.contains("Connection reset")) {
                return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_CONNECTION_RESET, UPSTREAM_CONNECTION_RESET_MESSAGE);
            }
            if (message.contains("Broken pipe")) {
                return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_BROKEN_PIPE, UPSTREAM_BROKEN_PIPE_MESSAGE);
            }
            if (message.contains("Connection was closed") || message.contains("Connection closed")) {
                return new Classification(HttpStatusCode.BAD_GATEWAY_502, UPSTREAM_CONNECTION_CLOSED, UPSTREAM_CONNECTION_CLOSED_MESSAGE);
            }
        }
        return new Classification(HttpStatusCode.BAD_GATEWAY_502, GATEWAY_CLIENT_CONNECTION_ERROR, null);
    }

    /** Walk the cause chain looking for a given exception type (Vert.x frequently wraps the real cause). */
    public static boolean hasCause(final Throwable t, final Class<? extends Throwable> type) {
        Throwable current = t;
        while (current != null) {
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
    private static String collectChainMessages(final Throwable t) {
        final StringBuilder sb = new StringBuilder();
        Throwable current = t;
        while (current != null) {
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
