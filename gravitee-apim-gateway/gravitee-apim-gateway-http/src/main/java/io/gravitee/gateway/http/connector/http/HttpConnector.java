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
package io.gravitee.gateway.http.connector.http;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.http.connector.AbstractConnector;
import io.gravitee.gateway.http.connector.AbstractHttpProxyConnection;
import io.gravitee.gateway.http.connector.http.ws.WebSocketProxyConnection;
import org.springframework.beans.factory.annotation.Autowired;

import static io.gravitee.gateway.http.connector.http.ws.WebSocketUtils.isWebSocket;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpConnector<T extends HttpEndpoint> extends AbstractConnector<T> {

    @Autowired
    public HttpConnector(T endpoint) {
        super(endpoint);
    }

    @Override
    protected AbstractHttpProxyConnection create(ProxyRequest proxyRequest) {
        String connectionHeader = proxyRequest.headers().getFirst(HttpHeaders.CONNECTION);
        String upgradeHeader = proxyRequest.headers().getFirst(HttpHeaders.UPGRADE);

        boolean websocket = isWebSocket(proxyRequest.method().name(), connectionHeader, upgradeHeader);

        return websocket ?
                new WebSocketProxyConnection(endpoint, proxyRequest) :
                new HttpProxyConnection<>(endpoint, proxyRequest);
    }
}
