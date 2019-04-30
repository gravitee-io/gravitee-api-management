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
package io.gravitee.gateway.standalone.vertx.ws;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.ws.WebSocket;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxWebSocket implements WebSocket {

    private ServerWebSocket websocket;
    private final HttpServerRequest httpServerRequest;

    private boolean closed;
    private boolean upgraded;

    VertxWebSocket(final HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
    }

    @Override
    public WebSocket upgrade() {
        websocket = httpServerRequest.upgrade();
        upgraded = true;
        return this;
    }

    @Override
    public WebSocket reject(int statusCode) {
        if (upgraded) {
            websocket.close((short) statusCode);
        }
        return this;
    }

    @Override
    public WebSocket write(io.gravitee.gateway.api.ws.WebSocketFrame frame) {
        if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.BINARY) {
            websocket.writeBinaryMessage(io.vertx.core.buffer.Buffer.buffer(frame.data().getBytes()));
        } else if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.TEXT) {
            websocket.writeTextMessage(frame.data().toString());
        }

        return this;
    }

    @Override
    public WebSocket close() {
        if (upgraded && ! closed) {
            websocket.close();
        }
        return this;
    }

    @Override
    public WebSocket frameHandler(Handler<io.gravitee.gateway.api.ws.WebSocketFrame> frameHandler) {
        websocket.frameHandler(frame -> frameHandler.handle(new VertxWebSocketFrame(frame)));
        return this;
    }

    @Override
    public WebSocket closeHandler(Handler<Void> closeHandler) {
        websocket.closeHandler(event -> {
            closed = true;
            closeHandler.handle(event);
        });
        return this;
    }

    @Override
    public boolean upgraded() {
        return upgraded;
    }
}
