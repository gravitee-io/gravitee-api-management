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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.Response;
import io.reactivex.*;
import io.vertx.reactivex.core.http.HttpServerResponse;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse implements Response {

    protected final VertxHttpServerRequest serverRequest;
    protected final HttpHeaders headers;
    protected final HttpHeaders trailers;
    protected final HttpServerResponse nativeResponse;
    protected Flowable<Buffer> chunks;

    public VertxHttpServerResponse(VertxHttpServerRequest serverRequest) {
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
    public VertxHttpServerResponse reason(String reason) {
        if (reason != null) {
            nativeResponse.setStatusMessage(reason);
        }
        return this;
    }

    @Override
    public VertxHttpServerResponse status(int statusCode) {
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

    @Override
    public Flowable<Buffer> chunks() {
        if (this.chunks == null) {
            this.chunks = Flowable.empty();
        }

        return this.chunks;
    }

    @Override
    public Completable onChunk(FlowableTransformer<Buffer, Buffer> chunkTransformer) {
        chunks = chunks.compose(chunkTransformer);

        return Completable.complete();
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> bodyTransformer) {
        return body(body().compose(bodyTransformer));
    }

    @Override
    public synchronized Maybe<Buffer> body() {
        // Reduce all the chunks to create a unique buffer containing all the content.
        final Maybe<Buffer> buffer = chunks().reduce(Buffer::appendBuffer);
        this.chunks = buffer.toFlowable();
        return buffer;
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return body().switchIfEmpty(Single.just(Buffer.buffer()));
    }

    @Override
    public Completable body(Maybe<Buffer> buffer) {
        return setChunks(buffer.toFlowable());
    }

    @Override
    public Completable body(Buffer buffer) {
        return setChunks(Flowable.just(buffer));
    }

    @Override
    public Completable chunks(final Flowable<Buffer> chunks) {
        return setChunks(chunks);
    }

    @Override
    public Completable end() {
        if (!valid()) {
            return Completable.error(new IllegalStateException("The response is already ended"));
        }

        if (!nativeResponse.headWritten()) {
            writeHeaders();
        }

        if (chunks != null) {
            return nativeResponse.rxSend(
                chunks
                    .map(buffer -> io.vertx.reactivex.core.buffer.Buffer.buffer(buffer.getNativeBuffer()))
                    .doOnNext(
                        buffer ->
                            serverRequest
                                .metrics()
                                .setResponseContentLength(serverRequest.metrics().getResponseContentLength() + buffer.length())
                    )
            );
        }

        return nativeResponse.rxEnd();
    }

    private synchronized Completable setChunks(Flowable<Buffer> chunks) {
        if (chunks != null) {
            // Current chunks need to be drained before being replaced.
            this.chunks = chunks().ignoreElements().andThen(chunks);
        }

        return Completable.complete();
    }
}
