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

import static io.vertx.reactivex.core.http.WebSocketFrame.*;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.api.ws.WebSocketFrame;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.ServerWebSocket;
import java.util.concurrent.CompletableFuture;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxWebSocket implements WebSocket {

    private final HttpServerRequest httpServerRequest;

    private boolean closed;
    private boolean upgraded;
    private ServerWebSocket webSocket;

    public VertxWebSocket(final HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
    }

    @Override
    public CompletableFuture<WebSocket> upgrade() {
        if (upgraded) {
            return CompletableFuture.completedFuture(this);
        }

        final CompletableFuture<WebSocket> future = new CompletableFuture<>();

        httpServerRequest.toWebSocket(
            result -> {
                if (result.failed()) {
                    future.completeExceptionally(result.cause());
                } else {
                    webSocket = result.result();
                    upgraded = true;
                    future.complete(this);
                }
            }
        );

        return future;
    }

    @Override
    public WebSocket reject(int statusCode) {
        if (upgraded) {
            webSocket.close((short) statusCode);
        }
        return this;
    }

    @Override
    public WebSocket write(WebSocketFrame frame) {
        if (upgraded) {
            final io.vertx.reactivex.core.http.WebSocketFrame webSocketFrame = convert(frame);

            if (webSocketFrame == null) {
                this.close();
            } else {
                webSocket.writeFrame(webSocketFrame);
            }
        }
        return this;
    }

    @Override
    public WebSocket close() {
        if (upgraded && !closed) {
            webSocket.close();
        }
        return this;
    }

    @Override
    public WebSocket frameHandler(Handler<WebSocketFrame> frameHandler) {
        if (upgraded) {
            webSocket.frameHandler(frame -> frameHandler.handle(new VertxWebSocketFrame(frame)));
        }
        return this;
    }

    @Override
    public WebSocket closeHandler(Handler<Void> closeHandler) {
        if (upgraded) {
            webSocket.closeHandler(
                event -> {
                    closed = true;
                    closeHandler.handle(event);
                }
            );
        }
        return this;
    }

    @Override
    public boolean upgraded() {
        return upgraded;
    }

    private io.vertx.reactivex.core.http.WebSocketFrame convert(io.gravitee.gateway.api.ws.WebSocketFrame frame) {
        switch (frame.type()) {
            case BINARY:
                return binaryFrame(Buffer.buffer(frame.data().getNativeBuffer()), frame.isFinal());
            case TEXT:
                return textFrame(frame.data().toString(), frame.isFinal());
            case CONTINUATION:
                return continuationFrame(Buffer.buffer(frame.data().toString()), frame.isFinal());
            case PING:
                return pingFrame(Buffer.buffer(frame.data().toString()));
            case PONG:
                return pongFrame(Buffer.buffer(frame.data().toString()));
            default:
                return null;
        }
    }
}
