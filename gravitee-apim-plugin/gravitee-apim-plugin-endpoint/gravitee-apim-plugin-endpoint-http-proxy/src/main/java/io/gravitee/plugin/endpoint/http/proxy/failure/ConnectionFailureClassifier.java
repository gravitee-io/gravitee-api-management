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

import io.gravitee.common.http.HttpStatusCode;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.StreamResetException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;

/**
 * Classifies a low-level connection {@link Throwable} raised while the gateway acts as an HTTP client towards the
 * backend into a stable error key + HTTP status code, so that connection failures can be told apart in metrics and
 * targeted by response templates.
 *
 * <p>Classification walks the cause chain and, for each link, matches by exception type first (most-specific subtype
 * before its supertypes). The type-matched path is drift-proof. A small message-substring fallback then covers the
 * cases only distinguishable by text (a bare {@code IOException}/{@code VertxException}); those substrings are
 * anchored phrases ("connection reset", not "reset") to limit false positives from arbitrary error text, and anything
 * unmatched degrades to the {@link #GATEWAY_CLIENT_CONNECTION_ERROR} umbrella rather than a wrong specific key.
 */
public final class ConnectionFailureClassifier {

    // Umbrellas (kept stable for backward compatibility with existing dashboards and response templates).
    public static final String GATEWAY_CLIENT_CONNECTION_ERROR = "GATEWAY_CLIENT_CONNECTION_ERROR";
    public static final String REQUEST_TIMEOUT = "REQUEST_TIMEOUT";

    // Fine-grained keys.
    public static final String CONNECTION_REFUSED = "GATEWAY_CLIENT_CONNECTION_REFUSED";
    public static final String DNS_RESOLUTION_ERROR = "GATEWAY_CLIENT_DNS_RESOLUTION_ERROR";
    public static final String UNREACHABLE = "GATEWAY_CLIENT_UNREACHABLE";
    public static final String TLS_HANDSHAKE_ERROR = "GATEWAY_CLIENT_TLS_HANDSHAKE_ERROR";
    public static final String CONNECTION_RESET = "GATEWAY_CLIENT_CONNECTION_RESET";
    public static final String CONNECTION_CLOSED = "GATEWAY_CLIENT_CONNECTION_CLOSED";
    public static final String CONNECT_TIMEOUT = "GATEWAY_CLIENT_CONNECT_TIMEOUT";
    public static final String READ_TIMEOUT = "GATEWAY_CLIENT_READ_TIMEOUT";

    /**
     * Name of the {@link io.gravitee.gateway.reactive.api.ExecutionFailure} parameter carrying the umbrella/parent
     * error key, so that a response template configured against the umbrella still fires for a fine-grained key. The
     * response-template failure processor reads the same literal (it cannot depend on this plugin).
     */
    public static final String PARENT_ERROR_KEY_PARAMETER = "parentErrorKey";

    private static final int MAX_CAUSE_DEPTH = 20;

    private ConnectionFailureClassifier() {}

    /**
     * Result of a classification: the HTTP status to expose, the fine-grained error key, and the umbrella/parent key a
     * response template should fall back to when no template targets the fine key ({@code null} when the key is itself
     * an umbrella).
     */
    public record Classification(int statusCode, String key, String parentKey) {}

    /** Classify a failure that occurred once the upstream connection was already established (e.g. response phase). */
    public static Classification classify(final Throwable throwable) {
        return classify(throwable, true);
    }

    /**
     * @param connectionEstablished whether the connector had acquired a usable upstream connection/stream before the
     *   failure. A request-level timeout that fires while this is still {@code false} is a connection (acquisition)
     *   timeout — pool saturation, slow DNS, slow connect — and the request never reached the backend, so it is keyed
     *   {@link #CONNECT_TIMEOUT} rather than {@link #READ_TIMEOUT}. This is decided from the connector's own
     *   observation of the reactive pipeline, never from the timeout's message text (which Vert.x may reword between
     *   versions).
     */
    public static Classification classify(final Throwable throwable, final boolean connectionEstablished) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < MAX_CAUSE_DEPTH) {
            Classification classification = classifyOne(current, connectionEstablished);
            if (classification != null) {
                return classification;
            }
            current = current.getCause();
        }
        return new Classification(HttpStatusCode.BAD_GATEWAY_502, GATEWAY_CLIENT_CONNECTION_ERROR, null);
    }

    private static Classification classifyOne(final Throwable t, final boolean connectionEstablished) {
        // Timeouts (504). Most-specific subtype first: ConnectTimeoutException extends ConnectException, so it must be
        // matched before the refused branch below or connect-timeouts would be misfiled as "refused".
        if (t instanceof ConnectTimeoutException) {
            return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
        }
        // io.vertx.circuitbreaker.TimeoutException extends RuntimeException (not j.u.c.TimeoutException), so it is
        // matched explicitly. Read/idle timeouts get their own key (parent REQUEST_TIMEOUT) so they are distinguishable
        // from the global request timeout, which keeps the bare REQUEST_TIMEOUT key.
        if (t instanceof ReadTimeoutException || t instanceof TimeoutException || t instanceof io.vertx.circuitbreaker.TimeoutException) {
            // The same request-level timeout type fires whether we were waiting for a connection (pool saturation,
            // DNS, connect) or reading the backend response. Tell them apart by whether the connection was acquired —
            // a structural signal we own — instead of parsing the timeout message.
            if (!connectionEstablished) {
                return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
            }
            return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, READ_TIMEOUT, REQUEST_TIMEOUT);
        }
        // Connection-level (502). TLS before the generic IOException family it extends.
        if (t instanceof SSLException) {
            return badGateway(TLS_HANDSHAKE_ERROR);
        }
        if (t instanceof UnknownHostException) {
            return badGateway(DNS_RESOLUTION_ERROR);
        }
        if (t instanceof NoRouteToHostException) {
            return badGateway(UNREACHABLE);
        }
        if (t instanceof ConnectException) {
            // Netty native transports (epoll/kqueue) surface "No route to host", "Network is unreachable" and OS-level
            // connect timeouts as a plain ConnectException: disambiguate by message before defaulting to "refused".
            final String connectMessage = t.getMessage() != null ? t.getMessage().toLowerCase(Locale.ROOT) : "";
            if (connectMessage.contains("no route") || connectMessage.contains("unreachable")) {
                return badGateway(UNREACHABLE);
            }
            if (connectMessage.contains("timed out")) {
                return new Classification(HttpStatusCode.GATEWAY_TIMEOUT_504, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
            }
            return badGateway(CONNECTION_REFUSED);
        }
        // An HTTP/2 RST_STREAM from the backend is semantically a connection reset; matched by type so the key does
        // not depend on the "Stream reset: <code>" message wording.
        if (t instanceof StreamResetException) {
            return badGateway(CONNECTION_RESET);
        }
        // The backend (or an intermediary) closed the connection prematurely — typically a keep-alive race when the
        // backend's keep-alive timeout is shorter than the gateway pool's, or a backend restart.
        if (t instanceof HttpClosedException || t instanceof ClosedChannelException) {
            return badGateway(CONNECTION_CLOSED);
        }
        // Message fallback for cases only distinguishable by text (bare IOException / VertxException). Unmatched here
        // returns null so the cause walk continues and ultimately degrades to the umbrella.
        final String message = t.getMessage();
        if (message != null) {
            // Anchored phrases only: a bare substring like "reset" or "unreachable" would also match arbitrary backend
            // error text (e.g. a response body "failed to reset cache") and return a confident, wrong 502 key.
            final String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("connection reset")) {
                return badGateway(CONNECTION_RESET);
            }
            if (normalized.contains("connection was closed")) {
                return badGateway(CONNECTION_CLOSED);
            }
            if (normalized.contains("is unreachable") || normalized.contains("no route to host")) {
                return badGateway(UNREACHABLE);
            }
            if (normalized.contains("failed to resolve") || normalized.contains("name or service not known")) {
                return badGateway(DNS_RESOLUTION_ERROR);
            }
            if (normalized.contains("connection refused")) {
                return badGateway(CONNECTION_REFUSED);
            }
        }
        return null;
    }

    private static Classification badGateway(final String key) {
        return new Classification(HttpStatusCode.BAD_GATEWAY_502, key, GATEWAY_CLIENT_CONNECTION_ERROR);
    }
}
