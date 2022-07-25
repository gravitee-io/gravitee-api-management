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

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.http.vertx.TimeoutServerResponse;
import io.gravitee.gateway.reactor.Reactor;
import io.vertx.core.Vertx;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxReactorTimeoutHandler extends VertxReactorHandler {

    private final VertxReactorHandler handler;

    private final Vertx vertx;

    private final long timeout;

    VertxReactorTimeoutHandler(
        final Reactor reactor,
        final VertxReactorHandler handler,
        final Vertx vertx,
        final long timeout,
        IdGenerator idGenerator
    ) {
        super(reactor, idGenerator);
        this.handler = handler;
        this.vertx = vertx;
        this.timeout = timeout;
    }

    protected void route(final Request request, final Response response) {
        if (!request.isWebSocket()) {
            long timeoutId = vertx.setTimer(
                timeout,
                event -> {
                    if (!response.ended()) {
                        Handler<Long> handler = request.timeoutHandler();
                        handler.handle(event);
                    }
                }
            );

            // Release timeout when response ends
            handler.route(request, new TimeoutServerResponse(vertx, response, timeoutId));
        } else {
            handler.route(request, response);
        }
    }
}
