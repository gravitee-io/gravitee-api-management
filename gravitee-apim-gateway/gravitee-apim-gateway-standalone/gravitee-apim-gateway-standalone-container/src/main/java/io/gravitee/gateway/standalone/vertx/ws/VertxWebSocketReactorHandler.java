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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.http.utils.WebSocketUtils;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.http.vertx.ws.VertxWebSocketServerRequest;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.standalone.vertx.VertxReactorHandler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;

/**
 * HTTP 1.x support for WebSockets.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxWebSocketReactorHandler extends VertxReactorHandler {

    private final IdGenerator idGenerator;

    public VertxWebSocketReactorHandler(final Reactor reactor, IdGenerator idGenerator) {
        super(reactor, idGenerator);
        this.idGenerator = idGenerator;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        if (isWebSocket(httpServerRequest)) {
            VertxHttpServerRequest request = new VertxWebSocketServerRequest(httpServerRequest, idGenerator);
            route(request, request.create());
        } else {
            super.handle(httpServerRequest);
        }
    }

    /**
     * We are only considering HTTP_1.x requests for now.
     * There is a dedicated RFC to support WebSockets over HTTP2: https://tools.ietf.org/html/rfc8441
     *
     * @param httpServerRequest
     * @return
     */
    private boolean isWebSocket(HttpServerRequest httpServerRequest) {
        String connectionHeader = httpServerRequest.getHeader(HttpHeaders.CONNECTION);
        String upgradeHeader = httpServerRequest.getHeader(HttpHeaders.UPGRADE);
        return (
            (httpServerRequest.version() == HttpVersion.HTTP_1_0 || httpServerRequest.version() == HttpVersion.HTTP_1_1) &&
            WebSocketUtils.isWebSocket(httpServerRequest.method().name(), connectionHeader, upgradeHeader)
        );
    }
}
