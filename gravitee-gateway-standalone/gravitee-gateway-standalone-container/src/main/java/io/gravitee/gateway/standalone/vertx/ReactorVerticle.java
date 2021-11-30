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
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorVerticle extends AbstractVerticle {

    private static final String HEX_FORMAT = "hex";

    private final Logger logger = LoggerFactory.getLogger(ReactorVerticle.class);

    @Autowired
    @Qualifier("gatewayHttpServer")
    private HttpServer httpServer;

    @Autowired
    private Reactor reactor;

    @Autowired
    @Qualifier("httpServerConfiguration")
    private HttpServerConfiguration httpServerConfiguration;

    @Autowired
    private Vertx vertx;

    @Value("${http.requestTimeout:0}")
    private long requestTimeout;

    @Value("${http.websocket.enabled:false}")
    private boolean websocketEnabled;

    @Value("${handlers.request.format:uuid}")
    private String requestFormat;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxReactorHandler handler;

        final IdGenerator idGenerator;
        if (HEX_FORMAT.equals(requestFormat)) {
            idGenerator = new Hex();
        } else {
            idGenerator = new UUID();
        }

        if (websocketEnabled) {
            handler = new VertxWebSocketReactorHandler(reactor, idGenerator);
        } else {
            handler = new VertxReactorHandler(reactor, idGenerator);
        }

        if (requestTimeout > 0) {
            handler = new VertxReactorTimeoutHandler(reactor, handler, vertx, requestTimeout, idGenerator);
        }

        httpServer.requestHandler(handler);

        httpServer.listen(
            res -> {
                if (res.succeeded()) {
                    logger.info("HTTP listener ready to accept requests on port {}", httpServerConfiguration.getPort());
                    startPromise.complete();
                } else {
                    logger.error("Unable to start HTTP Server", res.cause());
                    startPromise.fail(res.cause());
                }
            }
        );
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping HTTP Server...");
        httpServer.close(voidAsyncResult -> logger.info("HTTP Server has been correctly stopped"));
    }
}
