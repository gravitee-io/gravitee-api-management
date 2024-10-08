/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.AbstractResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscription;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse extends AbstractResponse {

    protected final HttpServerResponse nativeResponse;
    private final VertxHttpServerRequest vertxHttpServerRequest;
    private Boolean isStreaming = null;

    public VertxHttpServerResponse(final VertxHttpServerRequest vertxHttpServerRequest) {
        this.nativeResponse = vertxHttpServerRequest.nativeRequest.response();
        this.vertxHttpServerRequest = vertxHttpServerRequest;
        this.headers = new VertxHttpHeaders(nativeResponse.headers().getDelegate());
        this.trailers = new VertxHttpHeaders(nativeResponse.trailers().getDelegate());
    }

    public HttpServerResponse getNativeResponse() {
        return nativeResponse;
    }

    public boolean opened() {
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
    public boolean ended() {
        return nativeResponse.ended();
    }

    @Override
    public Completable end(final HttpBaseExecutionContext ctx) {
        return Completable.defer(() -> {
            if (vertxHttpServerRequest.isWebSocketUpgraded()) {
                return chunks().ignoreElements();
            }

            if (!opened()) {
                return Completable.error(new IllegalStateException("The response is already ended"));
            }
            prepareHeaders();
            // this atomic reference maybe useless due to  https://github.com/vert-x3/vertx-rx/pull/285
            // how to confirm ?
            final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

            if (lazyBufferFlow().hasChunks()) {
                return nativeResponse
                    .rxSend(
                        chunks()
                            .doOnSubscribe(subscriptionRef::set)
                            .map(buffer -> io.vertx.rxjava3.core.buffer.Buffer.buffer(buffer.getNativeBuffer()))
                            .doOnNext(buffer ->
                                ctx.metrics().setResponseContentLength(ctx.metrics().getResponseContentLength() + buffer.length())
                            )
                    )
                    .doOnDispose(() -> subscriptionRef.get().cancel());
            }

            return nativeResponse.rxEnd();
        });
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        super.messages(messages);

        // If message flow is set up, make sure any access to chunk buffers will not be possible anymore and returns empty.
        chunks(Flowable.empty());
    }

    protected void prepareHeaders() {
        if (!nativeResponse.headWritten()) {
            if (HttpVersion.HTTP_2 == vertxHttpServerRequest.version()) {
                if (
                    headers.contains(io.vertx.core.http.HttpHeaders.CONNECTION) &&
                    headers.getAll(io.vertx.core.http.HttpHeaders.CONNECTION).contains(HttpHeadersValues.CONNECTION_GO_AWAY)
                ) {
                    // 'Connection: goAway' is a special header indicating the native connection should be shutdown because of the node itself will shutdown.
                    vertxHttpServerRequest.nativeRequest.connection().shutdown();
                }

                // As per https://tools.ietf.org/html/rfc7540#section-8.1.2.2
                // connection-specific header fields must be removed from response headers
                headers
                    .remove(io.vertx.core.http.HttpHeaders.CONNECTION)
                    .remove(io.vertx.core.http.HttpHeaders.KEEP_ALIVE)
                    .remove(io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING);
            }

            if (headers.contains(HttpHeaders.CONTENT_LENGTH)) {
                headers.remove(HttpHeaders.TRANSFER_ENCODING);
            }
        }
    }

    @Override
    public boolean isStreaming() {
        if (isStreaming == null) {
            isStreaming = RequestUtils.isStreaming(this.vertxHttpServerRequest, this);
        }
        return isStreaming;
    }
}
