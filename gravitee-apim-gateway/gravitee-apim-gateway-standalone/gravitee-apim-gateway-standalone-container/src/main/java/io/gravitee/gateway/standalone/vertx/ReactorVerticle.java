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
import io.gravitee.common.utils.Hex;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.standalone.vertx.ws.VertxWebSocketReactorHandler;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorVerticle extends AbstractVerticle {

    private static final String HEX_FORMAT = "hex";

    private final Logger logger = LoggerFactory.getLogger(ReactorVerticle.class);

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private Reactor reactor;

    @Autowired
    private Vertx vertx;

    @Value("${http.requestTimeout:0}")
    private long requestTimeout;

    @Value("${http.websocket.enabled:false}")
    private boolean websocketEnabled;

    @Value("${handlers.request.format:uuid}")
    private String requestFormat;

    private Map<VertxHttpServer, HttpServer> httpServerMap;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        this.httpServerMap = new HashMap<>();

        logger.info("Starting HTTP Servers...");

        final IdGenerator idGenerator;
        if (HEX_FORMAT.equals(requestFormat)) {
            idGenerator = new Hex();
        } else {
            idGenerator = new UUID();
        }

        final List<VertxHttpServer> servers = this.serverManager.servers(VertxHttpServer.class);

        final List<Future> serverPromises = servers
            .stream()
            .map(gioServer -> {
                final HttpServer httpServer = gioServer.newInstance().getDelegate();
                httpServerMap.put(gioServer, httpServer);

                VertxReactorHandler handler;

                if (websocketEnabled) {
                    handler = new VertxWebSocketReactorHandler(reactor, idGenerator, vertx, requestTimeout, gioServer.id());
                } else {
                    handler = new VertxReactorHandler(reactor, idGenerator, vertx, requestTimeout, gioServer.id());
                }

                httpServer.requestHandler(handler);

                final Promise<Void> promise = Promise.promise();

                httpServer.listen(res -> {
                    if (res.succeeded()) {
                        logger.info("HTTP server [{}] ready to accept requests on port {}", gioServer.id(), httpServer.actualPort());
                        promise.complete();
                    } else {
                        logger.error("Unable to start HTTP server [{}]", gioServer.id(), res.cause());
                        promise.fail(res.cause());
                    }
                });

                return promise.future();
            })
            .collect(Collectors.toList());

        CompositeFuture
            .all(serverPromises)
            .onComplete(result -> {
                if (result.failed()) {
                    startPromise.fail(result.cause());
                } else {
                    startPromise.complete();
                }
            });
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping HTTP servers...");

        httpServerMap.forEach((gioServer, httpServer) ->
            httpServer.close(voidAsyncResult -> logger.info("HTTP server [{}] has been correctly stopped", gioServer.id()))
        );
    }
}
