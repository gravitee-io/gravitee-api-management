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
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxEmbeddedContainer extends AbstractLifecycleComponent<VertxEmbeddedContainer> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(VertxEmbeddedContainer.class);

    @Autowired
    private Vertx vertx;

    private String deploymentId;

    @Override
    protected void doStart() throws Exception {
        logger.info("Starting Vertx container and deploy Gravitee Verticles");
        // TODO: Providing a simple way to configure number of Gravitee instances
        DeploymentOptions options = new DeploymentOptions().setInstances(1);
        vertx.deployVerticle(GraviteeVerticleFactory.GRAVITEE_VERTICLE_PREFIX + ':' + GraviteeVerticle.class.getName(), options, event -> {
            deploymentId = event.result();
        });
    }

    @Override
    protected void doStop() throws Exception {
        if (deploymentId != null) {
            vertx.undeploy(deploymentId);
        }
    }
}
