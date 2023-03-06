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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.ws.WebSocketFrame;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.gravitee.gateway.jupiter.http.vertx.ws.VertxWebSocket;
import io.gravitee.gateway.policy.impl.PolicyConfigurationFactoryImpl;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class WebSocketAdapter implements io.gravitee.gateway.api.ws.WebSocket {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAdapter.class);

    private final WebSocket webSocket;

    public WebSocketAdapter(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public CompletableFuture<io.gravitee.gateway.api.ws.WebSocket> upgrade() {
        final CompletableFuture<io.gravitee.gateway.api.ws.WebSocket> future = new CompletableFuture<>();

        webSocket.upgrade().subscribe(ws -> future.complete(this), future::completeExceptionally);

        return future;
    }

    @Override
    public io.gravitee.gateway.api.ws.WebSocket reject(int statusCode) {
        webSocket.close((short) statusCode).subscribe();
        return this;
    }

    @Override
    public io.gravitee.gateway.api.ws.WebSocket write(WebSocketFrame frame) {
        ((VertxWebSocket) webSocket).writeFrame(frame)
            .subscribe(() -> {}, throwable -> log.warn("An error occurred when tyring to write to the websocket", throwable));
        return this;
    }

    @Override
    public io.gravitee.gateway.api.ws.WebSocket close() {
        webSocket.close().subscribe(() -> {}, throwable -> log.warn("An error occurred when tyring to close the websocket", throwable));
        return this;
    }

    @Override
    public io.gravitee.gateway.api.ws.WebSocket frameHandler(Handler<WebSocketFrame> frameHandler) {
        ((VertxWebSocket) webSocket).frameHandler(frameHandler);
        return this;
    }

    @Override
    public io.gravitee.gateway.api.ws.WebSocket closeHandler(Handler<Void> closeHandler) {
        ((VertxWebSocket) webSocket).closeHandler(closeHandler);
        return this;
    }

    @Override
    public boolean upgraded() {
        return webSocket.upgraded();
    }
}
