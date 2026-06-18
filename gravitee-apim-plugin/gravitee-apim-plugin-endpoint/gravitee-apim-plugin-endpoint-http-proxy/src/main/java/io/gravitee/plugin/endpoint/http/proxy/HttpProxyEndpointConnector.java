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
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.WebSocketClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.connector.GrpcConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.HttpConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.ProxyConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.WebSocketConnector;
import io.gravitee.plugin.endpoint.http.proxy.failure.ConnectionFailureClassifier;
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
    static final String GATEWAY_CLIENT_CONNECTION_ERROR = ConnectionFailureClassifier.GATEWAY_CLIENT_CONNECTION_ERROR;
    static final String REQUEST_TIMEOUT = ConnectionFailureClassifier.REQUEST_TIMEOUT;
    private static final String SERVER_NULL_PATTERN = " for server null";
    static final String CLIENT_ABORTED_DURING_RESPONSE_ERROR = "CLIENT_ABORTED_DURING_RESPONSE_ERROR";
    static final String CLIENT_ABORTED_DURING_RESPONSE_MESSAGE = "The response cannot be sent to the client because the client has aborted";
    static final String CLIENT_ABORTED_DURING_REQUEST_ERROR = "CLIENT_ABORTED_DURING_REQUEST_ERROR";
    static final String CLIENT_ABORTED_DURING_REQUEST_MESSAGE = "The request was aborted by the client before it was fully received";

    protected final HttpProxyEndpointConnectorConfiguration configuration;
    protected final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private final HttpClientFactory httpClientFactory;
    private final GrpcHttpClientFactory grpcHttpClientFactory;
    private final WebSocketClientFactory webSocketClientFactory;

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
        this.webSocketClientFactory = new WebSocketClientFactory();
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
            // Reset per-attempt state: FailoverInvoker re-subscribes this Completable on retry against the same
            // context, so a connection acquired on a previous attempt must not leak into this attempt's timeout
            // classification (else a connect-timeout on retry would be misread as a read-timeout). APIM-12769.
            ctx.removeInternalAttribute(HttpConnector.ATTR_INTERNAL_UPSTREAM_CONNECTION_ACQUIRED);
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
     * This is called when the client disconnects before the backend response is delivered (status still 0).
     * <p>
     * This is the coarse, phase-based fallback. When the close carries an actual cause (TCP reset, broken pipe), the
     * gateway HTTP layer ({@code ClientCloseClassifier}) classifies it more precisely while the same execution context
     * is in scope, and sets the metrics error key first — so we never overwrite a more specific key here.
     */
    private void handleClientAbort(final HttpExecutionContext ctx) {
        if (ctx.response().status() == 0) {
            ctx.response().status(499);
            final var metrics = ctx.metrics();
            // Skip when a more specific reason was already recorded — by the gateway HTTP layer (ClientCloseClassifier)
            // via errorKey, or by interruptWith via failure (which sets failure but not errorKey). Never overwrite it.
            final boolean metricsIsWritable = metrics != null && metrics.getErrorKey() == null && metrics.getFailure() == null;
            if (!metricsIsWritable) {
                return;
            }
            if (!ctx.request().ended()) {
                // The client aborted while still uploading the request body.
                metrics.setErrorKey(CLIENT_ABORTED_DURING_REQUEST_ERROR);
                metrics.setErrorMessage(CLIENT_ABORTED_DURING_REQUEST_MESSAGE);
            } else {
                // The full request was received; the client aborted while waiting for / receiving the response.
                metrics.setErrorKey(CLIENT_ABORTED_DURING_RESPONSE_ERROR);
                metrics.setErrorMessage(CLIENT_ABORTED_DURING_RESPONSE_MESSAGE);
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
        if (webSocketClientFactory != null) {
            webSocketClientFactory.close();
        }
    }

    private ProxyConnector getConnector(HttpRequest request) {
        if (request.isWebSocket()) {
            return this.connectors.computeIfAbsent("ws", type ->
                new WebSocketConnector(configuration, sharedConfiguration, httpClientFactory, webSocketClientFactory)
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
        if (throwable instanceof InterruptionFailureException interruption) {
            return Completable.error(interruption);
        }

        Throwable cause = throwable instanceof NoStackTraceTimeoutException timeout ? rewriteServerNull(timeout, ctx) : throwable;
        boolean connectionEstablished = Boolean.TRUE.equals(
            ctx.getInternalAttribute(HttpConnector.ATTR_INTERNAL_UPSTREAM_CONNECTION_ACQUIRED)
        );
        ConnectionFailureClassifier.Classification classification = ConnectionFailureClassifier.classify(cause, connectionEstablished);

        ExecutionFailure failure = new ExecutionFailure(classification.statusCode()).key(classification.key()).cause(cause);
        if (classification.parentKey() != null) {
            failure.parameters(Map.of(ConnectionFailureClassifier.PARENT_ERROR_KEY_PARAMETER, classification.parentKey()));
        }
        return ctx.interruptWith(failure);
    }

    private Throwable rewriteServerNull(NoStackTraceTimeoutException e, HttpExecutionContext ctx) {
        String msg = e.getMessage();
        String endpoint = ctx.metrics().getEndpoint();
        return (msg != null && msg.endsWith(SERVER_NULL_PATTERN) && endpoint != null)
            ? new NoStackTraceTimeoutException(msg.replace(SERVER_NULL_PATTERN, " for endpoint " + endpoint))
            : e;
    }
}
