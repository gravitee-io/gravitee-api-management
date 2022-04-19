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
package io.gravitee.gateway.reactive.http.vertx;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.http.HttpServerResponse;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractVertxHttpServerResponse<T> implements Response<T> {

    protected final AbstractVertxHttpServerRequest<T> serverRequest;
    protected final HttpHeaders headers;
    protected final HttpHeaders trailers;
    protected final HttpServerResponse nativeResponse;
    protected Flowable<T> content;

    public AbstractVertxHttpServerResponse(AbstractVertxHttpServerRequest<T> serverRequest) {
        this.serverRequest = serverRequest;
        this.nativeResponse = serverRequest.nativeRequest.response();
        this.headers = new VertxHttpHeaders(nativeResponse.headers().getDelegate());
        this.trailers = new VertxHttpHeaders(nativeResponse.trailers().getDelegate());
    }

    protected boolean valid() {
        return !nativeResponse.closed() && !nativeResponse.ended();
    }

    @Override
    public int status() {
        return nativeResponse.getStatusCode();
    }

    @Override
    public String reason() {
        return nativeResponse.getStatusMessage();
    }

    @Override
    public AbstractVertxHttpServerResponse<T> reason(String reason) {
        if (reason != null) {
            nativeResponse.setStatusMessage(reason);
        }
        return this;
    }

    @Override
    public AbstractVertxHttpServerResponse<T> status(int statusCode) {
        nativeResponse.setStatusCode(statusCode);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public boolean ended() {
        return nativeResponse.ended();
    }

    @Override
    public HttpHeaders trailers() {
        return trailers;
    }

    protected void writeHeaders() {
        if (HttpVersion.HTTP_2 == serverRequest.version()) {
            if (
                headers.contains(io.vertx.core.http.HttpHeaders.CONNECTION) &&
                headers.getAll(io.vertx.core.http.HttpHeaders.CONNECTION).contains(HttpHeadersValues.CONNECTION_GO_AWAY)
            ) {
                // 'Connection: goAway' is a special header indicating the native connection should be shutdown because of the node itself will shutdown.
                serverRequest.nativeRequest.connection().shutdown();
            }

            // As per https://tools.ietf.org/html/rfc7540#section-8.1.2.2
            // connection-specific header fields must be remove from response headers
            headers
                .remove(io.vertx.core.http.HttpHeaders.CONNECTION)
                .remove(io.vertx.core.http.HttpHeaders.KEEP_ALIVE)
                .remove(io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING);
        }
    }
}
