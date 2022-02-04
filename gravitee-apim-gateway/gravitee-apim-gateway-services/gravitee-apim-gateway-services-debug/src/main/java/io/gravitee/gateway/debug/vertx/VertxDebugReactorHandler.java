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
package io.gravitee.gateway.debug.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.http.vertx.VertxHttp2ServerRequest;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.http.vertx.grpc.VertxGrpcServerRequest;
import io.gravitee.gateway.reactor.Reactor;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;

public class VertxDebugReactorHandler implements Handler<HttpServerRequest> {

    private final Reactor reactor;
    private final IdGenerator idGenerator;

    public VertxDebugReactorHandler(final Reactor reactor, IdGenerator idGenerator) {
        this.reactor = reactor;
        this.idGenerator = idGenerator;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        VertxHttpServerRequest request;

        if (httpServerRequest.version() == HttpVersion.HTTP_2) {
            if (MediaType.APPLICATION_GRPC.equals(httpServerRequest.getHeader(HttpHeaders.CONTENT_TYPE))) {
                request = new VertxGrpcServerRequest(httpServerRequest, idGenerator);
            } else {
                request = new VertxHttp2ServerRequest(httpServerRequest, idGenerator);
            }
        } else {
            request = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        }

        request = new VertxHttpServerRequestDebugDecorator(request, idGenerator);

        route(request, request.create());
    }

    protected void route(final Request request, final Response response) {
        reactor.route(request, response, __ -> {});
    }
}
