/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.sync.EndpointSyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpRequest;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.connector.GrpcConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.HttpConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.ProxyConnector;
import io.gravitee.plugin.endpoint.http.proxy.connector.WebSocketConnector;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpProxyEndpointConnector extends EndpointSyncConnector {

    private static final String ENDPOINT_ID = "http-proxy";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.REQUEST_RESPONSE);

    protected final HttpProxyEndpointConnectorConfiguration configuration;
    private final HttpClientFactory httpClientFactory;
    private final GrpcHttpClientFactory grpcHttpClientFactory;

    public HttpProxyEndpointConnector(HttpProxyEndpointConnectorConfiguration configuration) {
        this.configuration = configuration;
        if (this.configuration.getTarget() == null || this.configuration.getTarget().isBlank()) {
            throw new IllegalArgumentException("target cannot be null or empty");
        }
        httpClientFactory = new HttpClientFactory();
        grpcHttpClientFactory = new GrpcHttpClientFactory();
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
    public Completable connect(ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                Request request = ctx.request();
                ProxyConnector proxyConnector;
                if (request.isWebSocket()) {
                    proxyConnector = new WebSocketConnector(configuration, httpClientFactory);
                } else if (isGrpc(request, configuration.getTarget())) {
                    proxyConnector = new GrpcConnector(configuration, grpcHttpClientFactory);
                } else {
                    proxyConnector = new HttpConnector(configuration, httpClientFactory);
                }
                return proxyConnector.connect(ctx);
            }
        );
    }

    private static boolean isGrpc(final HttpRequest httpRequest, final String target) {
        String contentType = httpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE);
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return MediaType.MEDIA_APPLICATION_GRPC.equals(mediaType) || target.startsWith("grpc://");
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
