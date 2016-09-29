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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.reporter.api.http.RequestMetrics;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
class VertxHttpServerRequest implements Request {

    private final String id;
    private final Instant instant;

    private final HttpServerRequest httpServerRequest;

    private Map<String, String> queryParameters = null;

    private HttpHeaders headers = null;

    private final RequestMetrics metrics;

    VertxHttpServerRequest(HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
        this.instant = Instant.now();
        this.id = UUID.toString(UUID.random());
        this.metrics = RequestMetrics.on(instant.toEpochMilli()).build();
        this.init();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String uri() {
        return httpServerRequest.uri();
    }

    @Override
    public String path() {
        return httpServerRequest.path();
    }

    @Override
    public Map<String, String> parameters() {
        if (queryParameters == null) {
            queryParameters = new HashMap<>(httpServerRequest.params().size());

            for(Map.Entry<String, String> param : httpServerRequest.params()) {
                queryParameters.put(param.getKey(), param.getValue());
            }
        }

        return queryParameters;
    }

    @Override
    public HttpHeaders headers() {
        if (headers == null) {
            MultiMap vertxHeaders = httpServerRequest.headers();
            headers = new HttpHeaders(vertxHeaders.size());
            for(Map.Entry<String, String> header : vertxHeaders) {
                headers.add(header.getKey(), header.getValue());
            }
        }

        return headers;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.valueOf(httpServerRequest.method().name());
    }

    @Override
    public HttpVersion version() {
        return HttpVersion.valueOf(httpServerRequest.version().name());
    }

    @Override
    public Instant timestamp() {
        return instant;
    }

    @Override
    public String remoteAddress() {
        SocketAddress address = httpServerRequest.remoteAddress();
        return (address != null) ? address.host() : null;
    }

    @Override
    public String localAddress() {
        SocketAddress address = httpServerRequest.localAddress();
        return (address != null) ? address.host() : null;
    }

    @Override
    public Request bodyHandler(Handler<Buffer> bodyHandler) {
        httpServerRequest.handler(buffer -> bodyHandler.handle(Buffer.buffer(buffer.getBytes())));
        return this;
    }

    @Override
    public Request endHandler(Handler<Void> endHandler) {
        httpServerRequest.endHandler(endHandler::handle);
        return this;
    }

    @Override
    public Request pause() {
        httpServerRequest.pause();
        return this;
    }

    @Override
    public Request resume() {
        httpServerRequest.resume();
        return this;
    }

    @Override
    public RequestMetrics metrics() {
        return metrics;
    }

    private void init() {
        this.metrics.setRequestId(id());
        this.metrics.setRequestHttpMethod(method());
        this.metrics.setRequestLocalAddress(localAddress());
        this.metrics.setRequestRemoteAddress(remoteAddress());
        this.metrics.setRequestPath(path());
        this.metrics.setRequestUri(uri());
        this.metrics.setRequestContentType(headers().contentType());
        this.metrics.setRequestContentLength(headers().contentLength());
    }
}
