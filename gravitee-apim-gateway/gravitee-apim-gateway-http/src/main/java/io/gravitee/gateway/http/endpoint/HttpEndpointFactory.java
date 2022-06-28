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
package io.gravitee.gateway.http.endpoint;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.ProtocolVersion;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.http.connector.http.Http2Connector;
import io.gravitee.gateway.http.connector.http.HttpConnector;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointFactory extends AbstractEndpointFactory {

    @Override
    public boolean support(Endpoint endpoint) {
        return EndpointType.HTTP == endpoint.getType();
    }

    @Override
    protected Connector create(io.gravitee.definition.model.Endpoint endpoint) {
        HttpEndpoint httpEndpoint = (HttpEndpoint) endpoint;
        HttpClientOptions httpClientOptions = httpEndpoint.getHttpClientOptions();
        if (httpClientOptions != null && ProtocolVersion.HTTP_2.equals(httpClientOptions.getVersion())) {
            return new Http2Connector<>(httpEndpoint);
        }
        return new HttpConnector<>(httpEndpoint);
    }
}
