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

import io.gravitee.apim.common.mapper.HttpClientOptionsMapper;
import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.ProtocolVersion;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;

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
        HttpClientOptions httpOptions = sharedConfiguration.getHttpOptions();
        httpOptions.setVersion(ProtocolVersion.HTTP_2);
        httpOptions.setClearTextUpgrade(false);

        return super
            .buildHttpClient(ctx, configuration, sharedConfiguration)
            .httpOptions(HttpClientOptionsMapper.INSTANCE.map(httpOptions));
    }
}
