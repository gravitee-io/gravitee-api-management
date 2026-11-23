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

package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.vertx.core.http.WebSocketClientOptions;

/**
 * The backend WebSocket client is shared by every gateway server, so it takes the largest limits of the servers accepting
 * WebSockets. It never goes below the Vert.x client defaults, so lowering the server limits to protect the gateway from
 * callers does not shrink what backends can send. The message size only applies to aggregated messages: frames relayed
 * one by one are bound by the frame size alone.
 */
record WebSocketSizeLimits(int maxFrameSize, int maxMessageSize) {
    static WebSocketSizeLimits from(ServerManager serverManager) {
        int maxFrameSize = WebSocketClientOptions.DEFAULT_MAX_FRAME_SIZE;
        int maxMessageSize = WebSocketClientOptions.DEFAULT_MAX_MESSAGE_SIZE;
        for (VertxHttpServer server : serverManager.servers(VertxHttpServer.class)) {
            final VertxHttpServerOptions options = server.options();
            if (options.isWebsocketEnabled()) {
                maxFrameSize = Math.max(maxFrameSize, options.getMaxWebSocketFrameSize());
                maxMessageSize = Math.max(maxMessageSize, options.getMaxWebSocketMessageSize());
            }
        }
        return new WebSocketSizeLimits(maxFrameSize, maxMessageSize);
    }
}
