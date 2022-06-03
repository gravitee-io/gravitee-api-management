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

import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.utils.Hex;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

public class DebugReactorVerticle extends AbstractVerticle {

    private static final String HEX_FORMAT = "hex";

    private final Logger logger = LoggerFactory.getLogger(DebugReactorVerticle.class);

    @Autowired
    @Qualifier("debugGatewayHttpServer")
    private HttpServer httpServer;

    @Autowired
    @Qualifier("debugReactor")
    private Reactor reactor;

    @Autowired
    @Qualifier("debugHttpServerConfiguration")
    private HttpServerConfiguration debugHttpServerConfiguration;

    @Value("${handlers.request.format:uuid}")
    private String requestFormat;

    @Override
    public void start(Promise<Void> startPromise) {
        VertxDebugReactorHandler handler;

        final IdGenerator idGenerator;
        if (HEX_FORMAT.equals(requestFormat)) {
            idGenerator = new Hex();
        } else {
            idGenerator = new UUID();
        }

        handler = new VertxDebugReactorHandler(reactor, idGenerator);

        httpServer.requestHandler(handler);

        httpServer.listen(
            res -> {
                if (res.succeeded()) {
                    logger.info("Debug HTTP listener ready to accept requests on port {}", debugHttpServerConfiguration.getPort());
                    startPromise.complete();
                } else {
                    logger.error("Unable to start Debug HTTP Server", res.cause());
                    startPromise.fail(res.cause());
                }
            }
        );
    }

    @Override
    public void stop() {
        logger.info("Stopping Debug HTTP Server...");
        httpServer.close(voidAsyncResult -> logger.info("Debug HTTP Server has been correctly stopped"));
    }
}
