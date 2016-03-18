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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ClientResponse;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
class VertxClientResponse implements ClientResponse {

    private Handler<Buffer> bodyHandler;
    private Handler<Void> endHandler;

    private final int status;
    private final HttpHeaders httpHeaders = new HttpHeaders();

    public VertxClientResponse(final int status) {
        this.status = status;
    }

    @Override
    public int status() {
        return this.status;
    }

    @Override
    public HttpHeaders headers() {
        return httpHeaders;
    }

    @Override
    public ClientResponse bodyHandler(Handler<Buffer> bodyHandler) {
        this.bodyHandler = bodyHandler;
        return this;
    }

    Handler<Buffer> bodyHandler() {
        return this.bodyHandler;
    }

    @Override
    public ClientResponse endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    Handler<Void> endHandler() {
        return this.endHandler;
    }
}
