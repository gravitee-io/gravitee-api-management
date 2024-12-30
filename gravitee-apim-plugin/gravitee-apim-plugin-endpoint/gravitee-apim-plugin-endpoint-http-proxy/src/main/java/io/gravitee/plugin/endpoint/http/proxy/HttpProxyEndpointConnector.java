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
import io.gravitee.gateway.reactive.api.connector.endpoint.sync.HttpEndpointSyncConnector;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.connector.GrpcConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.HttpConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.ProxyConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.WebSocketConnector;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpProxyEndpointConnector extends HttpEndpointSyncConnector {

    private static final String ENDPOINT_ID = "http-proxy";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.REQUEST_RESPONSE);

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
            HttpRequest request = ctx.request();
            return getConnector(request).connect(ctx);
        });
    }

    private ProxyConnector getConnector(HttpRequest request) {
        if (request.isWebSocket()) {
            return this.connectors.computeIfAbsent(
                    "ws",
                    type -> new WebSocketConnector(configuration, sharedConfiguration, httpClientFactory)
                );
        } else if (isGrpc(request)) {
            return this.connectors.computeIfAbsent(
                    "grpc",
                    type -> new GrpcConnector(configuration, sharedConfiguration, grpcHttpClientFactory)
                );
        } else {
            return this.connectors.computeIfAbsent(
                    "http",
                    type -> new HttpConnector(configuration, sharedConfiguration, httpClientFactory)
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

    @Override
    protected void doStop() {
        if (httpClientFactory != null) {
            httpClientFactory.close();
        }
        if (grpcHttpClientFactory != null) {
            grpcHttpClientFactory.close();
        }
    }
}
