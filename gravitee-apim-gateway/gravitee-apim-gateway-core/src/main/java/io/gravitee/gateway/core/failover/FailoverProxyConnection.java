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
package io.gravitee.gateway.core.failover;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.WriteStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class FailoverProxyConnection implements ProxyConnection {

    private final ProxyConnection proxyConnection;
    private final ProxyResponse proxyResponse;
    private Handler<ProxyResponse> responseHandler;

    FailoverProxyConnection(ProxyConnection proxyConnection, ProxyResponse proxyResponse) {
        this.proxyConnection = proxyConnection;
        this.proxyResponse = proxyResponse;
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        return proxyConnection.write(content);
    }

    @Override
    public void end() {
        proxyConnection.end();
    }

    @Override
    public void end(Buffer buffer) {
        proxyConnection.end(buffer);
    }

    @Override
    public ProxyConnection cancel() {
        return proxyConnection.cancel();
    }

    @Override
    public ProxyConnection exceptionHandler(Handler<Throwable> exceptionHandler) {
        return proxyConnection.exceptionHandler(exceptionHandler);
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return proxyConnection.responseHandler(responseHandler);
    }

    void sendResponse() {
        this.responseHandler.handle(proxyResponse);
    }
}
