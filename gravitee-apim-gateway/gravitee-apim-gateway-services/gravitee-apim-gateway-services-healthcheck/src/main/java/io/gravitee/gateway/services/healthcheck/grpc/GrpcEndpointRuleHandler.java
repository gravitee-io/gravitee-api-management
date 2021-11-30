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
package io.gravitee.gateway.services.healthcheck.grpc;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.endpoint.GrpcEndpoint;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRuleHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpVersion;
import org.springframework.core.env.Environment;

import java.net.URI;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcEndpointRuleHandler extends HttpEndpointRuleHandler<GrpcEndpoint> {

    private static final String GRPC_TRAILERS_TE = "trailers";

    GrpcEndpointRuleHandler(Vertx vertx, EndpointRule<GrpcEndpoint> rule, Environment environment) throws Exception {
        super(vertx, rule, environment);
    }

    @Override
    protected HttpClientRequest createHttpClientRequest(final HttpClient httpClient, URI request, io.gravitee.definition.model.services.healthcheck.Step step) throws Exception {
        HttpClientRequest httpClientRequest = super.createHttpClientRequest(httpClient, request, step);

        // Always set chunked mode for gRPC transport
        httpClientRequest.setChunked(true);

        // Ensure required grpc headers
        httpClientRequest.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_GRPC);
        httpClientRequest.headers().set(io.gravitee.common.http.HttpHeaders.TE, GRPC_TRAILERS_TE);

        return httpClientRequest;
    }

    @Override
    protected HttpClientOptions createHttpClientOptions(final GrpcEndpoint endpoint, final URI requestUri) throws Exception {
        HttpClientOptions httpClientOptions = super.createHttpClientOptions((HttpEndpoint) endpoint, requestUri);

        // Force HTTP_2 and disable Upgrade
        httpClientOptions.setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false);

        return httpClientOptions;
    }
}
