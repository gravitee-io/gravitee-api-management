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

import io.gravitee.common.service.AbstractService;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxDebugService extends AbstractService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(VertxDebugService.class);

    @Autowired
    @Qualifier("debugVerticle")
    private Verticle debugVerticle;

    @Autowired
    private Vertx vertx;

    private String deploymentId;

    @Override
    public VertxDebugService start() {
        doStart();
        return this;
    }

    @Override
    public VertxDebugService stop() {
        doStop();
        return this;
    }

    @Override
    protected void doStart() {
        logger.info("Starting Vertx DEBUG container and deploy only 1 Verticle");

        vertx.deployVerticle(
            debugVerticle,
            event -> {
                if (event.failed()) {
                    logger.warn("Unable to start debug verticle", event.cause());
                } else {
                    deploymentId = event.result();
                }
            }
        );
    }

    @Override
    protected void doStop() {
        if (deploymentId != null) {
            vertx.undeploy(
                deploymentId,
                event -> {
                    lifecycle.moveToStopped();
                }
            );
        }
    }
}
