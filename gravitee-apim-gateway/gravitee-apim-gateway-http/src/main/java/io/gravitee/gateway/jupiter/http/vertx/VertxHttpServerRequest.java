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

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.http.utils.WebSocketUtils;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.http.vertx.ws.VertxWebSocket;
import io.reactivex.*;
import io.vertx.reactivex.core.http.HttpServerRequest;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerRequest extends AbstractVertxServerRequest {

    private final BufferFlow bufferFlow;
    private MessageFlow messageFlow;
    protected Boolean isWebSocket = null;
    protected WebSocket webSocket;

    public VertxHttpServerRequest(final HttpServerRequest nativeRequest, IdGenerator idGenerator) {
        super(nativeRequest, idGenerator);
        bufferFlow = new BufferFlow();
        bufferFlow.chunks =
            nativeRequest
                .toFlowable()
                .doOnNext(buffer -> metrics().setRequestContentLength(metrics().getRequestContentLength() + buffer.length()))
                .map(Buffer::buffer);
        messageFlow = null;
    }

    public VertxHttpServerResponse response() {
        return new VertxHttpServerResponse(this);
    }

    @Override
    public boolean isWebSocket() {
        if (isWebSocket == null) {
            String connectionHeader = nativeRequest.getHeader(io.vertx.reactivex.core.http.HttpHeaders.CONNECTION);
            String upgradeHeader = nativeRequest.getHeader(io.vertx.reactivex.core.http.HttpHeaders.UPGRADE);
            final io.vertx.core.http.HttpVersion httpVersion = nativeRequest.version();
            isWebSocket =
                (httpVersion == io.vertx.core.http.HttpVersion.HTTP_1_0 || httpVersion == io.vertx.core.http.HttpVersion.HTTP_1_1) &&
                WebSocketUtils.isWebSocket(method().name(), connectionHeader, upgradeHeader);
        }
        return isWebSocket;
    }

    @Override
    public WebSocket webSocket() {
        if (isWebSocket() && webSocket == null) {
            webSocket = new VertxWebSocket(nativeRequest);
        }
        return webSocket;
    }

    /**
     * Indicates if the request is a websocket request and the connection has been upgraded (meaning, a websocket connection has been created).
     *
     * @return <code>true</code> if the connection has been upgraded to websocket, <code>false</code> else.
     * @see #webSocket()
     */
    public boolean isWebSocketUpgraded() {
        return webSocket != null && webSocket.upgraded();
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

    private MessageFlow getMessageFlow() {
        if (messageFlow == null) {
            messageFlow = new MessageFlow();
        }

        return this.messageFlow;
    }
}
