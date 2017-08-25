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
package io.gravitee.gateway.http.vertx;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.vertx.core.http.HttpClientRequest;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxProxyConnection implements ProxyConnection {

    private final HttpClientRequest httpClientRequest;
    private ProxyResponse proxyResponse;
    private Handler<Throwable> timeoutHandler;
    private boolean canceled = false;

    VertxProxyConnection(final HttpClientRequest httpClientRequest) {
        this.httpClientRequest = httpClientRequest;
    }

    public void setProxyResponse(ProxyResponse proxyResponse) {
        this.proxyResponse = proxyResponse;
    }

    @Override
    public ProxyConnection cancel() {
        this.canceled = true;
        this.httpClientRequest.reset();
        if (proxyResponse != null) {
            proxyResponse.bodyHandler(null);
        }
        return this;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    @Override
    public VertxProxyConnection connectTimeoutHandler(Handler<Throwable> timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        return this;
    }

    public Handler<Throwable> connectTimeoutHandler() {
        return this.timeoutHandler;
    }

    @Override
    public VertxProxyConnection write(Buffer chunk) {
        httpClientRequest.write(io.vertx.core.buffer.Buffer.buffer(chunk.getBytes()));

        return this;
    }

    @Override
    public void end() {
        httpClientRequest.end();
    }
}
