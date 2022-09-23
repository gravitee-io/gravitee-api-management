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
package io.gravitee.gateway.jupiter.reactor.v4.subscription.context;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.context.HttpResponse;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.http.vertx.MessageFlow;
import io.reactivex.rxjava3.core.*;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResponse implements MutableResponse {

    private HttpHeaders headers;
    private int statusCode;
    private String reason;
    private final MessageFlow messageFlow;
    private Flowable<Buffer> chunks;
    private boolean ended;

    public SubscriptionResponse() {
        this.statusCode = HttpStatusCode.OK_200;
        this.headers = HttpHeaders.create();
        this.messageFlow = new MessageFlow();
    }

    @Override
    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public HttpResponse status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @Override
    public int status() {
        return this.statusCode;
    }

    @Override
    public String reason() {
        return this.reason;
    }

    @Override
    public HttpResponse reason(String message) {
        this.reason = message;
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public HttpHeaders trailers() {
        return null;
    }

    @Override
    public boolean ended() {
        return ended;
    }

    @Override
    public Flowable<Message> messages() {
        return this.messageFlow.messages();
    }

    @Override
    public void messages(Flowable<Message> messages) {
        this.messageFlow.messages(messages);
    }

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        return Completable.fromRunnable(() -> this.messageFlow.onMessages(onMessages));
    }

    @Override
    public Maybe<Buffer> body() {
        // Subscription does not allow buffer body access.
        return Maybe.empty();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        // Subscription does not allow buffer body access.
        return Single.just(Buffer.buffer());
    }

    @Override
    public void body(Buffer buffer) {
        // Subscription does not allow buffer body access.
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        // Subscription does not allow buffer body access.
        return Completable.complete();
    }

    @Override
    public void chunks(Flowable<Buffer> chunks) {
        this.chunks = chunks;
    }

    @Override
    public Flowable<Buffer> chunks() {
        return this.chunks;
    }

    @Override
    public Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks) {
        if (chunks == null) {
            return Completable.error(new IllegalStateException("The is no chunks to apply the transformation on"));
        }
        return Completable.fromRunnable(() -> chunks = chunks.compose(onChunks));
    }

    @Override
    public Completable end() {
        return Completable.defer(() -> chunks.ignoreElements().andThen(Completable.fromRunnable(() -> ended = true)));
    }

    @Override
    public void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor) {
        this.messageFlow.setOnMessagesInterceptor(interceptor);
    }

    @Override
    public void unsetMessagesInterceptor() {
        this.messageFlow.unsetOnMessagesInterceptor();
    }
}
