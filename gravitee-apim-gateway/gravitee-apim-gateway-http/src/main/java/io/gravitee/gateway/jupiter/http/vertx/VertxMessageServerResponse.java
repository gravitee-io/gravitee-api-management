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
package io.gravitee.gateway.jupiter.http.vertx;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableMessageResponse;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxMessageServerResponse extends AbstractVertxServerResponse implements MutableMessageResponse {

    private final MessageFlowable httpMessages;

    public VertxMessageServerResponse(final AbstractVertxServerRequest vertxServerRequest) {
        super(vertxServerRequest);
        httpMessages = new MessageFlowable();
        nativeResponse.setChunked(true);
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        httpMessages.messages(messages);
    }

    @Override
    public Flowable<Message> messages() {
        return httpMessages.messages();
    }

    @Override
    public Completable onMessages(final FlowableTransformer<Message, Message> onMessages) {
        return httpMessages.onMessages(onMessages);
    }

    @Override
    public Completable end() {
        return Completable.defer(
            () -> {
                if (!opened()) {
                    return Completable.error(new IllegalStateException("The response is already ended"));
                }
                prepareHeaders();

                return nativeResponse.rxEnd();
            }
        );
    }

    @Override
    public Completable end(final Buffer buffer) {
        return Completable.defer(
            () -> {
                if (((VertxHttpServerRequest) serverRequest).isWebSocketUpgraded()) {
                    return Completable.complete();
                }

                if (!opened()) {
                    return Completable.error(new IllegalStateException("The response is already ended"));
                }
                prepareHeaders();

                return nativeResponse.rxEnd(io.vertx.reactivex.core.buffer.Buffer.buffer(buffer.getNativeBuffer()));
            }
        );
    }

    @Override
    public Completable write(final Buffer buffer) {
        return Completable.defer(() -> nativeResponse.rxWrite(io.vertx.reactivex.core.buffer.Buffer.buffer(buffer.getNativeBuffer())));
    }

    public Completable writeHeaders() {
        super.prepareHeaders();
        return Completable.defer(() -> nativeResponse.rxWrite(io.vertx.reactivex.core.buffer.Buffer.buffer()));
    }
}
