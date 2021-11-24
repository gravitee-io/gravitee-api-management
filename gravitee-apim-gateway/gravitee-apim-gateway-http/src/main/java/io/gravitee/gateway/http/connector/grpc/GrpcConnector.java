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

import io.gravitee.definition.model.endpoint.GrpcEndpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.http.connector.AbstractHttpProxyConnection;
import io.gravitee.gateway.http.connector.http.HttpConnector;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcConnector extends HttpConnector<GrpcEndpoint> {

    @Autowired
    public GrpcConnector(GrpcEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    protected HttpClientOptions getOptions() throws EndpointException {
        HttpClientOptions options = super.getOptions();

        // For gRPC, force HTTP/2 protocol
        options.setProtocolVersion(HttpVersion.HTTP_2).setHttp2ClearTextUpgrade(false);

        return options;
    }

    @Override
    protected AbstractHttpProxyConnection create(ProxyRequest proxyRequest) {
        return new GrpcProxyConnection(endpoint, proxyRequest);
    }
}
