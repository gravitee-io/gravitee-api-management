/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.endpoint.discovery;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.services.endpoint.discovery.verticle.EndpointDiscoveryVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointDiscoveryService.class);

    @Autowired
    private Vertx vertx;

    private String deploymentId;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        EndpointDiscoveryVerticle discoveryVerticle = new EndpointDiscoveryVerticle();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(discoveryVerticle);

        vertx.deployVerticle(discoveryVerticle, event -> {
            if (event.failed()) {
                LOGGER.error("Endpoints Discovery service cannot be started", event.cause());
            }

            deploymentId = event.result();
        });
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (deploymentId != null) {
            vertx.undeploy(deploymentId);
        }
    }

    @Override
    protected String name() {
        return "Endpoints Discovery";
    }

    @Override
    public int getOrder() {
        // ensure service is started before SyncService, as it consumes its events
        return 900;
    }
}
