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

import io.gravitee.gateway.core.Reactor;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxReactorHandler implements Handler<HttpServerRequest> {

    private final Reactor reactor;

    VertxReactorHandler(Reactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        handleRequest(httpServerRequest);
    }

    private void handleRequest(HttpServerRequest httpServerRequest) {
        reactor.process(
                new VertxHttpServerRequest(httpServerRequest),
                new VertxHttpServerResponse(httpServerRequest.response())).thenAccept(response -> {});
    }
}
