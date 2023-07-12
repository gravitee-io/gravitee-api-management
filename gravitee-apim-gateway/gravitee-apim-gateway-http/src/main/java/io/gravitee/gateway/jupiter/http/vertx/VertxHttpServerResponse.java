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
package io.gravitee.gateway.jupiter.http.vertx;

import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.INTERNAL_SERVER_ERROR;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.reactivex.rxjava3.core.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.reactivestreams.Subscription;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse extends AbstractVertxServerResponse {

    private final BufferFlow bufferFlow;
    private MessageFlow messageFlow;

    public VertxHttpServerResponse(final VertxHttpServerRequest vertxHttpServerRequest) {
        super(vertxHttpServerRequest);
        bufferFlow = new BufferFlow();
        messageFlow = null;
    }

    @Override
    public Maybe<Buffer> body() {
        return bufferFlow.body();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return bufferFlow.bodyOrEmpty();
    }

    @Override
    public void body(final Buffer buffer) {
        bufferFlow.body(buffer);
    }

    @Override
    public Completable onBody(final MaybeTransformer<Buffer, Buffer> onBody) {
        return bufferFlow.onBody(onBody);
    }

    @Override
    public Flowable<Buffer> chunks() {
        return bufferFlow.chunks();
    }

    @Override
    public void chunks(final Flowable<Buffer> chunks) {
        bufferFlow.chunks(chunks);
    }

    @Override
    public Completable onChunks(final FlowableTransformer<Buffer, Buffer> onChunks) {
        return bufferFlow.onChunks(onChunks);
    }

    @Override
    public Completable end() {
        return Completable.defer(() -> {
            if (((VertxHttpServerRequest) serverRequest).isWebSocketUpgraded()) {
                return chunks().ignoreElements().andThen(ensureWebSocketClosed());
            }

            if (!opened()) {
                return Completable.error(new IllegalStateException("The response is already ended"));
            }
            prepareHeaders();

            final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

            if (bufferFlow.chunks != null) {
                return nativeResponse
                    .rxSend(
                        chunks()
                            .doOnSubscribe(subscriptionRef::set)
                            .map(buffer -> io.vertx.rxjava3.core.buffer.Buffer.buffer(buffer.getNativeBuffer()))
                            .doOnNext(buffer ->
                                serverRequest
                                    .metrics()
                                    .setResponseContentLength(serverRequest.metrics().getResponseContentLength() + buffer.length())
                            )
                    )
                    .doOnDispose(() -> subscriptionRef.get().cancel());
            }

            return nativeResponse.rxEnd();
        });
    }

    private Completable ensureWebSocketClosed() {
        return Completable.defer(() -> {
            final WebSocket webSocket = serverRequest.webSocket();
            if (!webSocket.closed()) {
                return webSocket.close(INTERNAL_SERVER_ERROR.code(), INTERNAL_SERVER_ERROR.reasonText());
            }

            return Completable.complete();
        });
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        getMessageFlow().messages(messages);

        // If message flow is set up, make sure any access to chunk buffers will not be possible anymore and returns empty.
        chunks(Flowable.empty());
    }

    @Override
    public Flowable<Message> messages() {
        return getMessageFlow().messages();
    }

    @Override
    public Completable onMessages(final FlowableTransformer<Message, Message> onMessages) {
        return Completable.fromRunnable(() -> getMessageFlow().onMessages(onMessages));
    }

    @Override
    public void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor) {
        getMessageFlow().setOnMessagesInterceptor(interceptor);
    }

    @Override
    public void unsetMessagesInterceptor() {
        getMessageFlow().unsetOnMessagesInterceptor();
    }

    private MessageFlow getMessageFlow() {
        if (messageFlow == null) {
            messageFlow = new MessageFlow();
        }

        return this.messageFlow;
    }
}
