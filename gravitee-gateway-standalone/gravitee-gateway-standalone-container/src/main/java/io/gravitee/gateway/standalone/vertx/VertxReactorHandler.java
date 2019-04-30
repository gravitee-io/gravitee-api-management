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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.standalone.vertx.ws.VertxWebSocketServerRequest;
import io.gravitee.gateway.standalone.vertx.ws.VertxWebSocketServerResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxReactorHandler implements Handler<HttpServerRequest> {

    private final Reactor reactor;

    VertxReactorHandler(final Reactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        Request request;
        Response response;

        if (isWebSocket(httpServerRequest)) {
            request = new VertxWebSocketServerRequest(httpServerRequest);
            response = new VertxWebSocketServerResponse(httpServerRequest, request);
        } else {
            request = new VertxHttpServerRequest(httpServerRequest);
            response = new VertxHttpServerResponse(httpServerRequest, request.metrics());
        }

        route(request, response);
    }

    protected void route(final Request request, final Response response) {
        reactor.route(request, response, __ -> {});
    }

    private boolean isWebSocket(HttpServerRequest httpServerRequest) {
        String connectionHeader = httpServerRequest.getHeader(HttpHeaders.CONNECTION);
        String upgradeHeader = httpServerRequest.getHeader(HttpHeaders.UPGRADE);

        return httpServerRequest.method() == HttpMethod.GET &&
                HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(connectionHeader) &&
                HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader);
    }
}
