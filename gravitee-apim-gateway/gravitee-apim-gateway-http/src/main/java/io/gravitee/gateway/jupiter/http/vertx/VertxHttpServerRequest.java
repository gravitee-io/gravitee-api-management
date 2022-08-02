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

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.http.utils.WebSocketUtils;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.http.vertx.ws.VertxWebSocket;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;
import io.vertx.reactivex.core.http.HttpServerRequest;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerRequest extends AbstractVertxServerRequest implements MutableRequest {

    private final BodyChunksFlowable bodyChunksFlowable;
    protected Boolean isWebSocket = null;
    protected WebSocket webSocket;

    public VertxHttpServerRequest(final HttpServerRequest nativeRequest, IdGenerator idGenerator) {
        super(nativeRequest, idGenerator);
        bodyChunksFlowable = new BodyChunksFlowable();
        bodyChunksFlowable.chunks =
            nativeRequest
                .toFlowable()
                .doOnNext(buffer -> metrics().setRequestContentLength(metrics().getRequestContentLength() + buffer.length()))
                .map(Buffer::buffer);
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
        return bodyChunksFlowable.body();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return bodyChunksFlowable.bodyOrEmpty();
    }

    @Override
    public void body(final Buffer buffer) {
        bodyChunksFlowable.body(buffer);
    }

    @Override
    public Completable onBody(final MaybeTransformer<Buffer, Buffer> onBody) {
        return bodyChunksFlowable.onBody(onBody);
    }

    @Override
    public Flowable<Buffer> chunks() {
        return bodyChunksFlowable.chunks();
    }

    @Override
    public void chunks(final Flowable<Buffer> chunks) {
        bodyChunksFlowable.chunks(chunks);
    }

    @Override
    public Completable onChunks(final FlowableTransformer<Buffer, Buffer> onChunks) {
        return bodyChunksFlowable.onChunks(onChunks);
    }
}
