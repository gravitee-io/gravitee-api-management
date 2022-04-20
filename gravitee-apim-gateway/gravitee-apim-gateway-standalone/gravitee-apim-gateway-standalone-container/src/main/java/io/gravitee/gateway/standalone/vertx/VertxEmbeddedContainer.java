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

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.reactive.standalone.vertx.HttpProtocolVerticle;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxEmbeddedContainer extends AbstractLifecycleComponent<VertxEmbeddedContainer> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(VertxEmbeddedContainer.class);

    @Value("${http.instances:0}")
    private int instances;

    @Autowired
    private Vertx vertx;

    private String deploymentId;

    @Override
    public VertxEmbeddedContainer start() throws Exception {
        doStart();
        return this;
    }

    @Override
    public VertxEmbeddedContainer stop() throws Exception {
        doStop();
        return this;
    }

    @Override
    protected void doStart() throws Exception {
        instances = (instances < 1) ? VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE : instances;
        logger.info("Starting Vertx container and deploy Gateway Verticles [{} instance(s)]", instances);

        DeploymentOptions options = new DeploymentOptions().setInstances(instances);
        vertx.deployVerticle(
            SpringVerticleFactory.VERTICLE_PREFIX + ':' + HttpProtocolVerticle.class.getName(),
            options,
            event -> {
                if (event.failed()) {
                    logger.error("Unable to start HTTP server", event.cause());

                    // HTTP Server is a required component. Shutdown if not available
                    Runtime.getRuntime().exit(1);
                }

                deploymentId = event.result();
                lifecycle.moveToStarted();
            }
        );
    }

    @Override
    protected void doStop() throws Exception {
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
