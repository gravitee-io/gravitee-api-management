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
package io.gravitee.management.services.http;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.service.AbstractService;
import io.gravitee.management.services.http.handler.NodeHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpServer extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    private final static String PATH = "/_node";

    @Autowired
    @Qualifier("vertxNodeHttpServer")
    private io.vertx.core.http.HttpServer httpServer;

    @Autowired
    private Router nodeRouter;

    @Autowired
    private Vertx vertx;

    @Autowired
    private AuthProvider authProvider;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        LOGGER.info("Start HTTP server for node management");

        // Start HTTP server
        Router mainRouter = Router.router(vertx).mountSubRouter(PATH, nodeRouter);

        // Set security handler
        AuthHandler authHandler = BasicAuthHandler.create(authProvider, "gravitee.io");
        mainRouter.route().handler(authHandler);
        nodeRouter.route().handler(authHandler);

        // Set default handler
        mainRouter.route().handler(ctx -> ctx.fail(HttpStatusCode.NOT_FOUND_404));

        // Add request handler
        httpServer.requestHandler(mainRouter::accept).listen(event ->
                LOGGER.info("HTTP server for node management listening on port {}", event.result().actualPort()));

        // Set node handler
        NodeHandler nodeHandler = new NodeHandler();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(nodeHandler);
        nodeRouter.get("/").handler(nodeHandler);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        httpServer.close(event -> LOGGER.info("HTTP server for node management has been stopped"));
    }

    @Override
    protected String name() {
        return "Gateway Services Manager";
    }
}
