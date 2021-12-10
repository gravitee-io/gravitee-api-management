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
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRuleHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import java.net.URI;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcEndpointRuleHandler extends HttpEndpointRuleHandler<HttpEndpoint> {

    private static final String GRPC_TRAILERS_TE = "trailers";

    GrpcEndpointRuleHandler(Vertx vertx, EndpointRule<HttpEndpoint> rule, TemplateEngine templateEngine, Environment environment)
        throws Exception {
        super(vertx, rule, templateEngine, environment);
    }

    @Override
    protected Future<HttpClientRequest> createHttpClientRequest(
        final HttpClient httpClient,
        URI request,
        io.gravitee.definition.model.services.healthcheck.Step step
    ) {
        RequestOptions options = prepareHttpClientRequest(request, step);

        return httpClient
            .request(options)
            .map(
                httpClientRequest -> {
                    // Always set chunked mode for gRPC transport
                    httpClientRequest.setChunked(true);
                    return httpClientRequest;
                }
            );
    }

    @Override
    protected RequestOptions prepareHttpClientRequest(URI request, io.gravitee.definition.model.services.healthcheck.Step step) {
        RequestOptions options = super.prepareHttpClientRequest(request, step);

        // Ensure required grpc headers
        options.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_GRPC);
        options.putHeader(io.gravitee.common.http.HttpHeaders.TE, GRPC_TRAILERS_TE);

        return options;
    }

    @Override
    protected HttpClientOptions createHttpClientOptions(final HttpEndpoint endpoint, final URI requestUri) throws Exception {
        HttpClientOptions httpClientOptions = super.createHttpClientOptions((HttpEndpoint) endpoint, requestUri);

        // Force HTTP_2 and disable Upgrade
        httpClientOptions.setProtocolVersion(HttpVersion.HTTP_2).setHttp2ClearTextUpgrade(false);

        return httpClientOptions;
    }
}
