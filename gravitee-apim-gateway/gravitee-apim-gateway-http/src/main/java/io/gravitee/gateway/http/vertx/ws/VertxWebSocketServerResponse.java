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
package io.gravitee.gateway.http.vertx.ws;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.http.vertx.VertxHttpServerResponse;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxWebSocketServerResponse extends VertxHttpServerResponse {

    private final Request request;

    VertxWebSocketServerResponse(final VertxHttpServerRequest request) {
        super(request);
        this.request = request;
    }

    @Override
    public void end() {
        WebSocket websocket = request.websocket();

        // HTTP request has not been upgraded to WS connection, so it is a normal / plain HTTP response
        if (!websocket.upgraded()) {
            super.end();
        }
    }
}
