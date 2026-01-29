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
package io.gravitee.plugin.endpoint.http.proxy.connector;

import static io.gravitee.gateway.api.http.HttpHeaderNames.KEEP_ALIVE;
import static io.gravitee.gateway.api.http.HttpHeaderNames.PROXY_AUTHENTICATE;
import static io.gravitee.gateway.api.http.HttpHeaderNames.PROXY_AUTHORIZATION;
import static io.gravitee.gateway.api.http.HttpHeaderNames.PROXY_CONNECTION;
import static io.gravitee.gateway.api.http.HttpHeaderNames.TE;
import static io.gravitee.gateway.api.http.HttpHeaderNames.TRAILER;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.http.vertx.ws.VertxWebSocket;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientRequest;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.WebSocketClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebSocketConnector extends HttpConnector {

    static final Set<CharSequence> HOP_HEADERS = Set.of(KEEP_ALIVE, PROXY_AUTHORIZATION, PROXY_AUTHENTICATE, PROXY_CONNECTION, TE, TRAILER);
    private static final String HTTP_PROXY_WEBSOCKET_UPGRADE_FAILURE = "HTTP_PROXY_WEBSOCKET_UPGRADE_FAILURE";
    private static final String HTTP_PROXY_WEBSOCKET_FAILURE = "HTTP_PROXY_WEBSOCKET_FAILURE";

    protected final HttpProxyEndpointConnectorConfiguration configuration;
    protected final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    protected final WebSocketClientFactory webSocketClientFactory;

    public WebSocketConnector(
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration,
        final HttpClientFactory httpClientFactory,
        final WebSocketClientFactory webSocketClientFactory
    ) {
        super(configuration, sharedConfiguration, httpClientFactory);
        this.configuration = configuration;
        this.sharedConfiguration = sharedConfiguration;
        this.webSocketClientFactory = webSocketClientFactory;
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    // Use NOOP Subscriber on websocket close so return completable could be ignored
    @Override
    public Completable connect(final HttpExecutionContext ctx) {
        try {
            final HttpRequest request = ctx.request();
            final RequestOptions options = buildRequestOptions(ctx);
            ObservableHttpClientRequest observableHttpClientRequest = new ObservableHttpClientRequest(options);
            Span httpRequestSpan = ctx.getTracer().startSpanFrom(observableHttpClientRequest);

            ctx.metrics().setEndpoint(buildWebSocketUri(options));
            WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions(options.toJson());

            // Add subprotocols: handle comma-separated values, trim whitespace, filter empty strings
            if (request.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL)) {
                webSocketConnectOptions.setSubProtocols(parseSubProtocols(request));
            }

            return webSocketClientFactory
                .getOrBuildWebSocketClient(ctx, configuration, sharedConfiguration)
                .rxConnect(webSocketConnectOptions)
                .flatMap(endpointWebSocket -> {
                    endpointWebSocket.pause();

                    endpointWebSocket
                        .headers()
                        .forEach((name, value) -> {
                            ctx.response().headers().add(name, value);
                        });

                    for (CharSequence header : hopHeaders()) {
                        ctx.response().headers().remove(header.toString());
                    }

                    return request
                        .webSocket()
                        .upgrade()
                        .doOnSuccess(requestWebSocket -> {
                            ServerWebSocket serverWebSocket = ((VertxWebSocket) requestWebSocket).getDelegate();
                            // Entrypoint to Endpoint
                            serverWebSocket.frameHandler(endpointWebSocket::writeFrame);
                            serverWebSocket.closeHandler(v ->
                                // Use NOOP Subscriber to completable could be ignored
                                endpointWebSocket.close()
                            );

                            // Endpoint to Entrypoint
                            endpointWebSocket.frameHandler(serverWebSocket::writeFrame);
                            endpointWebSocket.closeHandler(v ->
                                // Use NOOP Subscriber to completable could be ignored
                                serverWebSocket.close()
                            );
                            endpointWebSocket.exceptionHandler(throwable -> serverWebSocket.close((short) HttpStatusCode.BAD_REQUEST_400));
                            endpointWebSocket.resume();
                        });
                })
                .ignoreElement()
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof UpgradeRejectedException) {
                        UpgradeRejectedException rejectedException = (UpgradeRejectedException) throwable;
                        return request
                            .webSocket()
                            .close(rejectedException.getStatus())
                            .andThen(
                                ctx.interruptWith(
                                    new ExecutionFailure(rejectedException.getStatus())
                                        .key(HTTP_PROXY_WEBSOCKET_UPGRADE_FAILURE)
                                        .cause(throwable.getCause())
                                        .message(rejectedException.getMessage())
                                )
                            );
                    }
                    return request
                        .webSocket()
                        .close(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                        .andThen(
                            ctx.interruptWith(
                                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                                    .key(HTTP_PROXY_WEBSOCKET_FAILURE)
                                    .cause(throwable)
                                    .message("Endpoint Websocket connection in error")
                            )
                        );
                })
                .doFinally(() -> ctx.getTracer().end(httpRequestSpan));
        } catch (Exception e) {
            return Completable.error(e);
        }
    }

    protected Set<CharSequence> hopHeaders() {
        return HOP_HEADERS;
    }

    private String buildWebSocketUri(RequestOptions options) {
        String protocol = options.isSsl() ? "wss" : "ws";
        return protocol + "://" + defaultHost + ":" + defaultPort + options.getURI();
    }

    private List<String> parseSubProtocols(HttpRequest request) {
        return request
            .headers()
            .getAll(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL)
            .stream()
            .flatMap(header -> Arrays.stream(header.split(",")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
