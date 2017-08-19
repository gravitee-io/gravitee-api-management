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
package io.gravitee.gateway.reactor.handler.transaction;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.reporter.api.http.RequestMetrics;

import java.time.Instant;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionRequest implements Request {

    private final Request request;
    private final String transactionId;

    public TransactionRequest(final String transactionId, final Request request) {
        this.transactionId = transactionId;
        this.request = request;
    }

    @Override
    public String id() {
        return request.id();
    }

    @Override
    public String transactionId() {
        return transactionId;
    }

    @Override
    public String uri() {
        return request.uri();
    }

    @Override
    public String path() {
        return request.path();
    }

    @Override
    public String pathInfo() {
        return request.pathInfo();
    }

    @Override
    public String contextPath() {
        return request.contextPath();
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        return request.parameters();
    }

    @Override
    public HttpHeaders headers() {
        return request.headers();
    }

    @Override
    public HttpMethod method() {
        return request.method();
    }

    @Override
    public HttpVersion version() {
        return request.version();
    }

    @Override
    public Instant timestamp() {
        return request.timestamp();
    }

    @Override
    public String remoteAddress() {
        return request.remoteAddress();
    }

    @Override
    public String localAddress() {
        return request.localAddress();
    }

    @Override
    public RequestMetrics metrics() {
        return request.metrics();
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        return request.bodyHandler(bodyHandler);
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        return request.endHandler(endHandler);
    }

    @Override
    public ReadStream<Buffer> pause() {
        return request.pause();
    }

    @Override
    public ReadStream<Buffer> resume() {
        return request.resume();
    }
}
