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
package io.gravitee.repository.bridge.server;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.service.AbstractService;
import io.gravitee.repository.bridge.server.handler.*;
import io.gravitee.repository.bridge.server.http.configuration.HttpServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BridgeService  extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeService.class);

    @Value("${services.bridge.enabled:true}")
    private boolean enabled;

    private final static String PATH = "/_bridge";

    private final static String AUTHENTICATION_TYPE_NONE = "none";
    private final static String AUTHENTICATION_TYPE_BASIC = "basic";
    private final static String AUTHENTICATION_BASIC_REALM = "gravitee.io";

    @Autowired
    @Qualifier("vertxBridgeHttpServer")
    private io.vertx.core.http.HttpServer httpServer;

    @Autowired
    @Qualifier("bridgeRouter")
    private Router bridgeRouter;

    @Autowired
    private Vertx vertx;

    @Autowired
    @Qualifier("bridgeAuthProvider")
    private AuthProvider authProvider;

    @Autowired
    private HttpServerConfiguration httpServerConfiguration;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            LOGGER.info("Start HTTP server for bridge");

            // Start HTTP server
            Router mainRouter = Router.router(vertx).mountSubRouter(PATH, bridgeRouter);

            AuthHandler authHandler = null;
            switch ( httpServerConfiguration.getAuthenticationType().toLowerCase() ) {
                case AUTHENTICATION_TYPE_NONE:
                    break;
                case AUTHENTICATION_TYPE_BASIC:
                    authHandler = BasicAuthHandler.create(authProvider, AUTHENTICATION_BASIC_REALM);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Authentication type " + httpServerConfiguration.getAuthenticationType() + " for HTTP bridge service");
            }

            // Set security handler is defined
            if ( authHandler != null ) {
                mainRouter.route().handler(authHandler);
                bridgeRouter.route().handler(authHandler);
            }


            // Set default handler
            mainRouter.route().handler(ctx -> ctx.fail(HttpStatusCode.NOT_FOUND_404));

            // Add request handler
            httpServer.requestHandler(mainRouter::accept).listen(event ->
                    LOGGER.info("HTTP server for bridge listening on port {}", event.result().actualPort()));

            bridgeRouter.route().handler(BodyHandler.create());

            // Create and associate handler
            // APIs handler
            ApisHandler apisHandler = new ApisHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(apisHandler);
            bridgeRouter.get("/apis").handler(apisHandler::search);
            bridgeRouter.get("/apis/:apiId").handler(apisHandler::findById);

            // API Plans handler
            ApiPlansHandler apiPlansHandler = new ApiPlansHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(apiPlansHandler);
            bridgeRouter.get("/apis/:apiId/plans").handler(apiPlansHandler);

            // API Keys handler
            ApiKeysHandler apiKeysHandler = new ApiKeysHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(apiKeysHandler);
            bridgeRouter.post("/keys/_search").handler(apiKeysHandler);

            // Subscriptions handler
            SubscriptionsHandler subscriptionsHandler = new SubscriptionsHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(subscriptionsHandler);
            bridgeRouter.post("/subscriptions/_search").handler(subscriptionsHandler::search);

            // Events handler
            EventsHandler eventsHandler = new EventsHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(eventsHandler);
            bridgeRouter.post("/events/_search").handler(eventsHandler::search);
            bridgeRouter.post("/events").handler(eventsHandler::create);
            bridgeRouter.put("/events/:eventId").handler(eventsHandler::update);

            // Dictionaries handler
            DictionariesHandler dictionariesHandler = new DictionariesHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(dictionariesHandler);
            bridgeRouter.get("/dictionaries").handler(dictionariesHandler::findAll);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        httpServer.close(event -> LOGGER.info("HTTP server for bridge has been stopped"));
    }

    @Override
    protected String name() {
        return "Gateway - Repository Bridge HTTP Server";
    }
}
