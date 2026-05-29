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
package io.gravitee.plugin.endpoint.http.proxy;

import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.connector.endpoint.sync.HttpEndpointSyncConnector;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.plugin.endpoint.http.proxy.client.ConnectionFailureClassifier;
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.connector.GrpcConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.HttpConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.ProxyConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.WebSocketConnector;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.impl.NoStackTraceTimeoutException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HttpProxyEndpointConnector extends HttpEndpointSyncConnector {

    private static final String ENDPOINT_ID = "http-proxy";
    private final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.REQUEST_RESPONSE);
    // Connection-failure keys/messages are owned by ConnectionFailureClassifier (single source of truth, shared
    // with the WebSocket and gRPC connectors). Re-exported here for callers/tests that reference the connector.
    static final String GATEWAY_CLIENT_CONNECTION_ERROR = ConnectionFailureClassifier.GATEWAY_CLIENT_CONNECTION_ERROR;
    static final String REQUEST_TIMEOUT = ConnectionFailureClassifier.REQUEST_TIMEOUT;
    static final String UPSTREAM_CONNECT_TIMEOUT = ConnectionFailureClassifier.UPSTREAM_CONNECT_TIMEOUT;
    static final String UPSTREAM_IDLE_TIMEOUT = ConnectionFailureClassifier.UPSTREAM_IDLE_TIMEOUT;
    static final String UPSTREAM_DNS_FAILURE = ConnectionFailureClassifier.UPSTREAM_DNS_FAILURE;
    static final String UPSTREAM_CONNECTION_REFUSED = ConnectionFailureClassifier.UPSTREAM_CONNECTION_REFUSED;
    static final String UPSTREAM_TLS_FAILURE = ConnectionFailureClassifier.UPSTREAM_TLS_FAILURE;
    static final String UPSTREAM_CONNECTION_RESET = ConnectionFailureClassifier.UPSTREAM_CONNECTION_RESET;
    static final String UPSTREAM_BROKEN_PIPE = ConnectionFailureClassifier.UPSTREAM_BROKEN_PIPE;
    static final String UPSTREAM_CONNECTION_CLOSED = ConnectionFailureClassifier.UPSTREAM_CONNECTION_CLOSED;
    static final String UPSTREAM_CONNECT_TIMEOUT_MESSAGE = ConnectionFailureClassifier.UPSTREAM_CONNECT_TIMEOUT_MESSAGE;
    static final String UPSTREAM_IDLE_TIMEOUT_MESSAGE = ConnectionFailureClassifier.UPSTREAM_IDLE_TIMEOUT_MESSAGE;
    static final String UPSTREAM_DNS_FAILURE_MESSAGE = ConnectionFailureClassifier.UPSTREAM_DNS_FAILURE_MESSAGE;
    static final String UPSTREAM_CONNECTION_REFUSED_MESSAGE = ConnectionFailureClassifier.UPSTREAM_CONNECTION_REFUSED_MESSAGE;
    static final String UPSTREAM_TLS_FAILURE_MESSAGE = ConnectionFailureClassifier.UPSTREAM_TLS_FAILURE_MESSAGE;
    static final String UPSTREAM_CONNECTION_RESET_MESSAGE = ConnectionFailureClassifier.UPSTREAM_CONNECTION_RESET_MESSAGE;
    static final String UPSTREAM_BROKEN_PIPE_MESSAGE = ConnectionFailureClassifier.UPSTREAM_BROKEN_PIPE_MESSAGE;
    static final String UPSTREAM_CONNECTION_CLOSED_MESSAGE = ConnectionFailureClassifier.UPSTREAM_CONNECTION_CLOSED_MESSAGE;
    private static final String SERVER_NULL_PATTERN = " for server null";
    static final String CLIENT_ABORTED_DURING_RESPONSE_ERROR = "CLIENT_ABORTED_DURING_RESPONSE_ERROR";
    static final String CLIENT_ABORTED_DURING_RESPONSE_MESSAGE = "The response cannot be sent to the client because the client has aborted";

    protected final HttpProxyEndpointConnectorConfiguration configuration;
    protected final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private final HttpClientFactory httpClientFactory;
    private final GrpcHttpClientFactory grpcHttpClientFactory;

    private final Map<String, ProxyConnector> connectors = new ConcurrentHashMap<>(3);
    private final boolean targetStartWithGrpc;

    public HttpProxyEndpointConnector(
        HttpProxyEndpointConnectorConfiguration configuration,
        HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        this.configuration = configuration;
        this.sharedConfiguration = sharedConfiguration;
        if (this.configuration.getTarget() == null || this.configuration.getTarget().isBlank()) {
            throw new IllegalArgumentException("target cannot be null or empty");
        }
        this.httpClientFactory = new HttpClientFactory();
        this.grpcHttpClientFactory = new GrpcHttpClientFactory();
        this.targetStartWithGrpc = configuration.getTarget().startsWith("grpc://");
    }

    @Override
    public String id() {
        return ENDPOINT_ID;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(HttpExecutionContext ctx) {
        return Completable.defer(() -> {
            // Http status set to 0 to detect client abort before backend response
            ctx.response().status(0);
            HttpRequest request = ctx.request();
            return getConnector(request).connect(ctx);
        })
            .doOnDispose(() -> handleClientAbort(ctx))
            .onErrorResumeNext(throwable -> handleException(throwable, ctx));
    }

    /**
     * Handle client abort by setting appropriate error status and metrics.
     * This is called when the client disconnects before the response is fully delivered.
     * <p>
     * If the Vert.x connection-level exception handler already classified the close reason
     * (TCP reset, broken pipe, idle timeout, …), keep that more specific information instead
     * of overwriting it with the generic fallback (APIM-12769).
     */
    private void handleClientAbort(final HttpExecutionContext ctx) {
        if (ctx.response().status() == 0) {
            ctx.response().status(499);
            if (ctx.metrics().getFailure() == null && ctx.metrics().getErrorKey() == null) {
                ctx.metrics().setErrorKey(CLIENT_ABORTED_DURING_RESPONSE_ERROR);
                ctx.metrics().setErrorMessage(CLIENT_ABORTED_DURING_RESPONSE_MESSAGE);
            }
        }
    }

    @Override
    protected void doStop() {
        if (httpClientFactory != null) {
            httpClientFactory.close();
        }
        if (grpcHttpClientFactory != null) {
            grpcHttpClientFactory.close();
        }
    }

    private ProxyConnector getConnector(HttpRequest request) {
        if (request.isWebSocket()) {
            return this.connectors.computeIfAbsent("ws", type ->
                new WebSocketConnector(configuration, sharedConfiguration, httpClientFactory)
            );
        } else if (isGrpc(request)) {
            return this.connectors.computeIfAbsent("grpc", type ->
                new GrpcConnector(configuration, sharedConfiguration, grpcHttpClientFactory)
            );
        } else {
            return this.connectors.computeIfAbsent("http", type ->
                new HttpConnector(configuration, sharedConfiguration, httpClientFactory)
            );
        }
    }

    private boolean isGrpc(final HttpRequest httpRequest) {
        String contentType = httpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            return this.targetStartWithGrpc;
        } else {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.MEDIA_APPLICATION_GRPC.equals(mediaType) || this.targetStartWithGrpc;
        }
    }

    private Completable handleException(Throwable throwable, HttpExecutionContext ctx) {
        if (throwable instanceof InterruptionFailureException) {
            return Completable.error(throwable);
        }
        ConnectionFailureClassifier.Classification classification = ConnectionFailureClassifier.classify(throwable);
        // Preserve the rewritten "for endpoint …" message for the NoStackTrace request-timeout case.
        Throwable cause = throwable instanceof NoStackTraceTimeoutException nst ? rewriteServerNull(nst, ctx) : throwable;
        ExecutionFailure executionFailure = new ExecutionFailure(classification.statusCode()).key(classification.key()).cause(cause);
        if (classification.message() != null) {
            executionFailure.message(classification.message());
        }
        return ctx.interruptWith(executionFailure);
    }

    private Throwable rewriteServerNull(NoStackTraceTimeoutException e, HttpExecutionContext ctx) {
        String msg = e.getMessage();
        String endpoint = ctx.metrics().getEndpoint();
        return (msg != null && msg.endsWith(SERVER_NULL_PATTERN) && endpoint != null)
            ? new NoStackTraceTimeoutException(msg.replace(SERVER_NULL_PATTERN, " for endpoint " + endpoint))
            : e;
    }
}
