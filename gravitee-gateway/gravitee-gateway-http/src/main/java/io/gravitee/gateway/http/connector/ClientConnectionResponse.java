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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.core.processor.ProcessorFailure;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientConnectionResponse implements ProxyResponse, ProcessorFailure {

    static final String GATEWAY_CLIENT_CONNECTION_ERROR = "GATEWAY_CLIENT_CONNECTION_ERROR";

    private Handler<Buffer> bodyHandler;
    private Handler<Void> endHandler;

    private final HttpHeaders httpHeaders = new HttpHeaders();

    public ClientConnectionResponse() {
        httpHeaders.set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
    }

    @Override
    public int status() {
        return HttpStatusCode.BAD_GATEWAY_502;
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

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public int statusCode() {
        return HttpStatusCode.BAD_GATEWAY_502;
    }

    @Override
    public String message() {
        return null;
    }

    @Override
    public String key() {
        return GATEWAY_CLIENT_CONNECTION_ERROR;
    }

    @Override
    public Map<String, Object> parameters() {
        return null;
    }

    @Override
    public String contentType() {
        return null;
    }
}
