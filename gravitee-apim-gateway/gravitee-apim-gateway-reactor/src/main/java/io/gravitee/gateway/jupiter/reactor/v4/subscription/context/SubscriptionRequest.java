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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.AbstractRequest;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.function.Function;

/**
 * This is a sort of fake {@link MutableRequest} to deal with subscription.
 * Some methods are returning a {@link IllegalStateException} because the methods doesn't make sense
 * in the case of subscription where the request is initiated by an incoming {@link Subscription}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRequest extends AbstractRequest implements MutableRequest {

    public SubscriptionRequest(Subscription subscription, final IdGenerator idGenerator) {
        this.id = idGenerator.randomString();
        this.clientIdentifier = subscription.getId();
        this.timestamp = System.currentTimeMillis();
        this.headers = HttpHeaders.create();
        this.parameters = new LinkedMultiValueMap<>(Collections.emptyMap());
        this.pathParameters = new LinkedMultiValueMap<>(Collections.emptyMap());
        this.uri = "";
        this.host = "";
        this.originalHost = "";
        this.path = "";
        this.pathInfo = "";
        this.contextPath = "";
        this.method = HttpMethod.OTHER;
        this.scheme = "";
        this.remoteAddress = "";
        this.localAddress = "";
        this.ended = true;
    }

    @Override
    public Maybe<Buffer> body() {
        // Subscription request has not body because it is issued by the gateway itself.
        return Maybe.empty();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        // Subscription request has not body. Just return an empty buffer.
        return Single.just(Buffer.buffer());
    }

    @Override
    public void body(Buffer buffer) {
        // It is not possible to assign a body to a subscription request.
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        // No transformation on body can be performed on a subscription request.
        return Completable.complete();
    }

    @Override
    public Flowable<Buffer> chunks() {
        // There is no chunks on a subscription request.
        return Flowable.empty();
    }

    @Override
    public void chunks(Flowable<Buffer> chunks) {
        // It is not possible to assign chunks to a subscription request.
    }

    @Override
    public Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks) {
        // No transformation on chunks can be performed on a subscription request.
        return Completable.complete();
    }

    @Override
    public Flowable<Message> messages() {
        // A subscription request is not able to deal with messages.
        return Flowable.empty();
    }

    @Override
    public void messages(Flowable<Message> messages) {
        // It is not possible to assign messages to a subscription request.
    }

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        // No transformation on messages can be performed on a subscription request.
        return Completable.complete();
    }

    @Override
    public void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor) {
        // No message so no interceptor.
    }

    @Override
    public void unsetMessagesInterceptor() {
        // No message so no interceptor.
    }

    @Override
    public void method(HttpMethod method) {
        // It is not possible to override a subscription request's method.
    }
}
