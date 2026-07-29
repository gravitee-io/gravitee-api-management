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
package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.node.vertx.client.http.VertxHttpClientOptions;
import io.gravitee.node.vertx.client.http.VertxHttpProtocolVersion;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.mappers.HttpClientOptionsMapper;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcHttpClientFactory extends HttpClientFactory {

    @Override
    protected VertxHttpClientFactory.VertxHttpClientFactoryBuilder buildHttpClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        // The shared configuration is reused by every request and by the plain HTTP client factory of the same
        // endpoint group, so it must never be mutated here: forcing HTTP/2 on it would permanently switch the
        // endpoint to HTTP/2 for all subsequent traffic on this node. Force the version on a fresh copy instead.
        VertxHttpClientOptions httpOptions = HttpClientOptionsMapper.INSTANCE.map(sharedConfiguration.getHttpOptions());
        httpOptions.setVersion(VertxHttpProtocolVersion.HTTP_2);
        httpOptions.setClearTextUpgrade(false);

        return super.buildHttpClient(ctx, configuration, sharedConfiguration).httpOptions(httpOptions);
    }
}
