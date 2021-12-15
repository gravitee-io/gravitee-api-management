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
package io.gravitee.gateway.http.connector;

import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.http.connector.http.HttpProxyConnection;
import io.vertx.core.http.HttpClient;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractHttpProxyConnection implements ProxyConnection {

    protected final HttpEndpoint endpoint;
    protected Handler<ProxyResponse> responseHandler;
    protected Handler<Void> cancelHandler;

    public AbstractHttpProxyConnection(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public abstract void connect(
        HttpClient httpClient,
        int port,
        String host,
        String uri,
        Handler<AbstractHttpProxyConnection> connectionHandler,
        Handler<Void> tracker
    );

    protected void sendToClient(ProxyResponse proxyResponse) {
        this.responseHandler.handle(proxyResponse);
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    @Override
    public ProxyConnection cancelHandler(Handler<Void> cancelHandler) {
        this.cancelHandler = cancelHandler;
        return null;
    }
}
