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
package io.gravitee.gateway.core.proxy;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DirectProxyConnection implements ProxyConnection {

    private Handler<ProxyResponse> responseHandler;

    private final DirectResponse response;

    public DirectProxyConnection(int statusCode) {
        this.response = new DirectResponse(statusCode);
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        return null;
    }

    @Override
    public void end() {
        // Nothing to do here...
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    public void sendResponse() {
        this.responseHandler.handle(response);
    }

    public static class DirectResponse implements ProxyResponse {

        private Handler<Buffer> bodyHandler;
        private Handler<Void> endHandler;

        private final HttpHeaders httpHeaders = new HttpHeaders();

        private final int statusCode;

        DirectResponse(int statusCode) {
            this.statusCode = statusCode;
            httpHeaders.set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        }

        @Override
        public int status() {
            return statusCode;
        }

        @Override
        public HttpHeaders headers() {
            return httpHeaders;
        }

        @Override
        public ProxyResponse bodyHandler(Handler<Buffer> bodyHandler) {
            this.bodyHandler = bodyHandler;
            return this;
        }

        Handler<Buffer> bodyHandler() {
            return this.bodyHandler;
        }

        @Override
        public ProxyResponse endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }

        Handler<Void> endHandler() {
            return this.endHandler;
        }

        @Override
        public ReadStream<Buffer> resume() {
            endHandler.handle(null);
            return this;
        }
    }
}