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
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.AbstractResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.internal.buffer.BufferInternal;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.concurrent.atomic.AtomicReference;
import lombok.CustomLog;
import org.reactivestreams.Subscription;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class VertxHttpServerResponse extends AbstractResponse {

    static final String GATEWAY_RESPONSE_STREAM_FAILED = "GATEWAY_RESPONSE_STREAM_FAILED";
    static final String GATEWAY_RESPONSE_STREAM_FAILED_MESSAGE = "The gateway failed while streaming the response";
    private static final long HTTP_2_INTERNAL_ERROR = 0x2L;

    protected final HttpServerResponse nativeResponse;
    private final VertxHttpServerRequest vertxHttpServerRequest;
    private Boolean isStreaming = null;

    public VertxHttpServerResponse(final VertxHttpServerRequest vertxHttpServerRequest) {
        this.nativeResponse = vertxHttpServerRequest.nativeRequest.response();
        this.vertxHttpServerRequest = vertxHttpServerRequest;
        this.headers = new VertxHttpHeaders(nativeResponse.headers());
        this.trailers = new VertxHttpHeaders(nativeResponse.trailers());
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

            final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
            final AtomicReference<Throwable> sourceFailureRef = new AtomicReference<>();

            if (lazyBufferFlow().hasChunks()) {
                return nativeResponse
                    .rxSend(
                        chunks()
                            .doOnSubscribe(subscriptionRef::set)
                            .map(buffer -> BufferInternal.buffer(buffer.getNativeBuffer()))
                            .cast(Buffer.class)
                            .doOnNext(buffer ->
                                ctx.metrics().setResponseContentLength(ctx.metrics().getResponseContentLength() + buffer.length())
                            )
                            .onErrorResumeNext(failure -> handleResponseChunkSourceFailure(ctx, sourceFailureRef, failure))
                    )
                    .onErrorResumeNext(throwable -> handleStreamFailure(ctx, throwable, sourceFailureRef.get()))
                    .doOnDispose(() -> {
                        if (!nativeResponse.ended()) {
                            // If the response is disposed before being ended, we need to cancel the
                            // subscription so cancellation is propagated to the endpoint connector.
                            var subscription = subscriptionRef.get();
                            if (subscription != null) {
                                subscription.cancel();
                            }
                        }
                    });
            }

            return nativeResponse.rxEnd();
        });
    }

    private Flowable<Buffer> handleResponseChunkSourceFailure(
        HttpBaseExecutionContext ctx,
        AtomicReference<Throwable> sourceFailureRef,
        Throwable sourceFailure
    ) {
        sourceFailureRef.compareAndSet(null, sourceFailure);
        ResponseFailureDecorator.decorate(ctx, responseStreamFailure(sourceFailure));
        ctx.withLogger(log).error("The response stream failed; aborting the downstream transport", sourceFailure);

        return abortTransport()
            .onErrorComplete(abortFailure -> {
                ctx.withLogger(log).error("The downstream transport could not be aborted after a response stream failure", abortFailure);
                return true;
            })
            .andThen(Flowable.error(sourceFailure));
    }

    private Completable handleStreamFailure(HttpBaseExecutionContext ctx, Throwable failure, Throwable knownSourceFailure) {
        if (knownSourceFailure != null) {
            // Source failures are decorated and the transport is aborted before their error reaches rxSend. A
            // follow-on send failure is therefore cleanup completion, not a downstream client close or a second abort.
            return Completable.complete();
        }
        if (ClientCloseClassifier.isClientConnectionClose(failure)) {
            // The client closed the connection while the response was being streamed. The write failure surfaces
            // here BEFORE the connection-level handlers run, and completing below makes the dispatch end normally —
            // so record the abort now without attempting to abort an already-closed transport again (APIM-12769).
            ClientCloseClassifier.decorate(ctx, failure);
            ctx.withLogger(log).debug("Client has closed the connection: {}", failure.getMessage());
            return Completable.complete();
        }

        ResponseFailureDecorator.decorate(ctx, responseStreamFailure(failure));
        ctx.withLogger(log).error("The response stream failed; aborting the downstream transport", failure);

        return abortTransport().onErrorComplete(abortFailure -> {
            ctx.withLogger(log).error("The downstream transport could not be aborted after a response stream failure", abortFailure);
            return true;
        });
    }

    private Completable abortTransport() {
        if (HttpVersion.HTTP_2 == vertxHttpServerRequest.version()) {
            return Completable.defer(() -> nativeResponse.rxReset(HTTP_2_INTERNAL_ERROR));
        }
        // Closing the HTTP/1.x connection prevents Vert.x from writing a terminating chunk, so downstream observes
        // a truncated transfer rather than the clean EOF that previously masked a mid-stream gateway failure.
        return Completable.defer(() -> vertxHttpServerRequest.nativeRequest.connection().rxClose());
    }

    static ExecutionFailure responseStreamFailure(Throwable cause) {
        return new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
            .key(GATEWAY_RESPONSE_STREAM_FAILED)
            .message(GATEWAY_RESPONSE_STREAM_FAILED_MESSAGE)
            .cause(cause);
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        super.messages(messages);

        // If message flow is set up, make sure any access to chunk buffers will not be
        // possible anymore and returns empty.
        chunks(Flowable.empty());
    }

    protected void prepareHeaders() {
        if (!nativeResponse.headWritten()) {
            if (HttpVersion.HTTP_2 == vertxHttpServerRequest.version()) {
                if (
                    headers.contains(io.vertx.core.http.HttpHeaders.CONNECTION) &&
                    headers.getAll(io.vertx.core.http.HttpHeaders.CONNECTION).contains(HttpHeadersValues.CONNECTION_GO_AWAY)
                ) {
                    // 'Connection: goAway' is a special header indicating the native connection
                    // should be shutdown because of the node itself will shutdown.
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
