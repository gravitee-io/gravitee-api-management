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
package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.ProtocolVersion;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.http.vertx.client.VertxHttpClient;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcHttpClientFactory extends HttpClientFactory {

    protected VertxHttpClient.VertxHttpClientBuilder buildHttpClient(
        final ExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration
    ) {
        HttpClientOptions httpOptions = configuration.getHttpOptions();
        httpOptions.setVersion(ProtocolVersion.HTTP_2);
        httpOptions.setClearTextUpgrade(false);

        return super.buildHttpClient(ctx, configuration).httpOptions(httpOptions);
    }
}
