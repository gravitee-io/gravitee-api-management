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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.GenericResponse;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.BufferFlow;
import io.gravitee.gateway.reactive.core.MessageFlow;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Function;

/**
 * Base implementation of {@link Response} that does nothing in particular and <b>can be</b> used to avoid reimplementing all methods.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResponse implements MutableResponse, HttpResponseInternal {

    protected BufferFlow bufferFlow;
    protected MessageFlow messageFlow;
    protected int statusCode;
    protected String reason;
    protected HttpHeaders headers;
    protected HttpHeaders trailers;
    protected boolean ended;

    @Override
    public GenericResponse status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @Override
    public int status() {
        return statusCode;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public GenericResponse reason(String message) {
        this.reason = message;
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public HttpHeaders trailers() {
        return trailers;
    }

    @Override
    public boolean ended() {
        return ended;
    }

    @Override
    public Completable end(final HttpBaseExecutionContext ctx) {
        return Completable.defer(() -> {
            final BufferFlow bufferFlow = lazyBufferFlow();
            if (bufferFlow.hasChunks()) {
                return bufferFlow.chunks().ignoreElements().andThen(Completable.fromRunnable(() -> ended = true));
            }

            ended = true;
            return Completable.complete();
        });
    }

    @Override
    public Maybe<Buffer> body() {
        return lazyBufferFlow().body();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return lazyBufferFlow().bodyOrEmpty();
    }

    @Override
    public void body(final Buffer buffer) {
        lazyBufferFlow().body(buffer);
    }

    @Override
    public Completable onBody(final MaybeTransformer<Buffer, Buffer> onBody) {
        return lazyBufferFlow().onBody(onBody);
    }

    @Override
    public Flowable<Buffer> chunks() {
        return lazyBufferFlow().chunks();
    }

    @Override
    public void chunks(final Flowable<Buffer> chunks) {
        lazyBufferFlow().chunks(chunks);
    }

    @Override
    public Completable onChunks(final FlowableTransformer<Buffer, Buffer> onChunks) {
        return lazyBufferFlow().onChunks(onChunks);
    }

    @Override
    public void contentLength(long contentLength) {
        headers.set(io.vertx.core.http.HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
        headers.remove(io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING);
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        lazyMessageFlow().messages(messages);
    }

    @Override
    public Flowable<Message> messages() {
        return lazyMessageFlow().messages();
    }

    @Override
    public Completable onMessages(final FlowableTransformer<Message, Message> onMessages) {
        return Completable.fromRunnable(() -> lazyMessageFlow().onMessages(onMessages));
    }

    @Override
    public MutableResponse setHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor) {
        lazyMessageFlow().setOnMessagesInterceptor(interceptor);
    }

    @Override
    public void unsetMessagesInterceptor() {
        lazyMessageFlow().unsetOnMessagesInterceptor();
    }

    @Override
    public void pipeDownstream() {}

    protected final BufferFlow lazyBufferFlow() {
        if (bufferFlow == null) {
            bufferFlow = new BufferFlow(this::isStreaming);
        }

        return this.bufferFlow;
    }

    public boolean isStreaming() {
        return false;
    }

    protected final MessageFlow lazyMessageFlow() {
        if (messageFlow == null) {
            messageFlow = new MessageFlow();
        }

        return this.messageFlow;
    }
}
