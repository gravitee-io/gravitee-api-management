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
package io.gravitee.gateway.jupiter.http.vertx.ws;

import static io.vertx.rxjava3.core.http.WebSocketFrame.binaryFrame;
import static io.vertx.rxjava3.core.http.WebSocketFrame.continuationFrame;
import static io.vertx.rxjava3.core.http.WebSocketFrame.pingFrame;
import static io.vertx.rxjava3.core.http.WebSocketFrame.pongFrame;
import static io.vertx.rxjava3.core.http.WebSocketFrame.textFrame;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpClosedException;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.core.http.WebSocketFrame;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxWebSocket implements WebSocket {

    private final HttpServerRequest httpServerRequest;

    private boolean upgraded;
    private ServerWebSocket webSocket;

    public VertxWebSocket(final HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
    }

    public ServerWebSocket getDelegate() {
        return webSocket;
    }

    @Override
    public Single<WebSocket> upgrade() {
        if (!upgraded) {
            return Single.defer(
                () ->
                    httpServerRequest
                        .rxToWebSocket()
                        .doOnSuccess(
                            serverWebSocket -> {
                                webSocket = serverWebSocket;
                                upgraded = true;
                            }
                        )
                        .map(serverWebSocket -> this)
            );
        }

        return Single.just(this);
    }

    @Override
    public Completable write(Buffer buffer) {
        if (isValid()) {
            return webSocket.rxWrite(io.vertx.rxjava3.core.buffer.Buffer.buffer(buffer.getNativeBuffer()));
        }

        return Completable.complete();
    }

    public Completable writeFrame(io.gravitee.gateway.api.ws.WebSocketFrame frame) {
        if (isValid()) {
            final WebSocketFrame webSocketFrame = convert(frame);

            if (webSocketFrame == null) {
                return this.close();
            }
            return webSocket.rxWriteFrame(webSocketFrame);
        }

        return Completable.complete();
    }

    public void frameHandler(Handler<io.gravitee.gateway.api.ws.WebSocketFrame> frameHandler) {
        if (isValid()) {
            webSocket.frameHandler(frame -> frameHandler.handle(new VertxWebSocketFrame(frame)));
        }
    }

    public void closeHandler(Handler<Void> closeHandler) {
        if (isValid()) {
            webSocket.closeHandler(closeHandler::handle);
        }
    }

    @Override
    public Flowable<Buffer> read() {
        if (isValid()) {
            return webSocket
                .toFlowable()
                .map(Buffer::buffer)
                .onErrorResumeNext(
                    t -> {
                        if (t instanceof HttpClosedException) {
                            // Ends the flow properly if connection is closed by the client.
                            return Flowable.empty();
                        }

                        // Propagate in case of any other error.
                        return Flowable.error(t);
                    }
                );
        }

        return Flowable.empty();
    }

    @Override
    public Completable close() {
        if (isValid()) {
            return webSocket.rxClose();
        }

        return Completable.complete();
    }

    @Override
    public Completable close(int status) {
        if (isValid()) {
            return webSocket.rxClose((short) status);
        }

        return Completable.complete();
    }

    @Override
    public Completable close(int status, String reason) {
        if (isValid()) {
            return webSocket.rxClose((short) status, reason);
        }

        return Completable.complete();
    }

    @Override
    public boolean upgraded() {
        return upgraded;
    }

    @Override
    public boolean closed() {
        return webSocket.isClosed();
    }

    private boolean isValid() {
        return upgraded && !webSocket.isClosed();
    }

    private io.vertx.rxjava3.core.http.WebSocketFrame convert(io.gravitee.gateway.api.ws.WebSocketFrame frame) {
        switch (frame.type()) {
            case BINARY:
                return binaryFrame(io.vertx.rxjava3.core.buffer.Buffer.buffer(frame.data().getNativeBuffer()), frame.isFinal());
            case TEXT:
                return textFrame(frame.data().toString(), frame.isFinal());
            case CONTINUATION:
                return continuationFrame(io.vertx.rxjava3.core.buffer.Buffer.buffer(frame.data().toString()), frame.isFinal());
            case PING:
                return pingFrame(io.vertx.rxjava3.core.buffer.Buffer.buffer(frame.data().toString()));
            case PONG:
                return pongFrame(io.vertx.rxjava3.core.buffer.Buffer.buffer(frame.data().toString()));
            default:
                return null;
        }
    }
}
