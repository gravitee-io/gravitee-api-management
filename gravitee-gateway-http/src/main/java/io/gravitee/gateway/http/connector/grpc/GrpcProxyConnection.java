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
package io.gravitee.gateway.http.connector.grpc;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.endpoint.GrpcEndpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.http.connector.http.HttpProxyConnection;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcProxyConnection extends HttpProxyConnection {

    private static final String GRPC_TRAILERS_TE = "trailers";

    GrpcProxyConnection(GrpcEndpoint endpoint, ProxyRequest proxyRequest) {
        super(endpoint, proxyRequest);
    }

    @Override
    protected HttpClientRequest prepareUpstreamRequest(HttpClient httpClient, int port, String host, String uri) {
        HttpClientRequest clientRequest = httpClient.request(HttpMethod.POST, port, host, uri);

        clientRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());

        // Always set chunked mode for gRPC transport
        clientRequest.setChunked(true);

        // Ensure required gRPC headers
        clientRequest.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_GRPC);
        clientRequest.headers().set(io.gravitee.common.http.HttpHeaders.TE, GRPC_TRAILERS_TE);

        return clientRequest;
    }
}
