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

import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcConnector extends HttpConnector {

    private static final String GRPC_TRAILERS_TE = "trailers";

    public GrpcConnector(
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration,
        final GrpcHttpClientFactory grpcHttpClientFactory
    ) {
        super(configuration, sharedConfiguration, grpcHttpClientFactory);
    }

    @Override
    protected RequestOptions buildRequestOptions(final HttpExecutionContext ctx) {
        return super
            .buildRequestOptions(ctx)
            .setMethod(HttpMethod.POST)
            .putHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_GRPC)
            .putHeader(HttpHeaderNames.TE, GRPC_TRAILERS_TE);
    }
}
