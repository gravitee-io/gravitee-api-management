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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.reactive.standalone.vertx.HttpProtocolVerticle;
import io.gravitee.gateway.reactive.standalone.vertx.TcpProtocolVerticle;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class VertxEmbeddedContainer extends AbstractLifecycleComponent<VertxEmbeddedContainer> {

    @Value("${http.instances:0}")
    private int httpInstances;

    @Value("${tcp.instances:0}")
    private int tcpInstances;

    private final Vertx vertx;

    private String httpDeploymentId;
    private String tcpDeploymentId;

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
    protected void doStart() {
        httpInstances = (httpInstances < 1) ? CpuCoreSensor.availableProcessors() : httpInstances;
        tcpInstances = (tcpInstances < 1) ? CpuCoreSensor.availableProcessors() : tcpInstances;
        startHttpInstances();
        startTcpInstances();
    }

    private void startHttpInstances() {
        log.info("Starting Vertx container and deploy Gateway HTTP Verticles [{} instance(s)]", httpInstances);

        final var options = new DeploymentOptions().setInstances(httpInstances);
        final String verticleName = SpringVerticleFactory.VERTICLE_PREFIX + ':' + HttpProtocolVerticle.class.getName();

        vertx.deployVerticle(verticleName, options, event -> {
            if (event.failed()) {
                log.error("Unable to start HTTP server", event.cause());

                // HTTP Server is a required component. Shutdown if not available
                Runtime.getRuntime().exit(1);
            }

            httpDeploymentId = event.result();
            moveToStarted();
        });
    }

    private void moveToStarted() {
        if (httpDeploymentId != null && tcpDeploymentId != null) {
            lifecycle.moveToStarted();
        }
    }

    private void startTcpInstances() {
        log.info("Starting Vertx container and deploy Gateway TCP Verticles [{} instance(s)]", tcpInstances);

        final DeploymentOptions options = new DeploymentOptions().setInstances(tcpInstances);
        final String verticleName = SpringVerticleFactory.VERTICLE_PREFIX + ':' + TcpProtocolVerticle.class.getName();

        vertx.deployVerticle(verticleName, options, event -> {
            if (event.failed()) {
                log.error("Unable to start TCP server", event.cause());
            }

            tcpDeploymentId = event.result();
            moveToStarted();
        });
    }

    @Override
    protected void doStop() {
        if (httpDeploymentId != null) {
            vertx.undeploy(httpDeploymentId, event -> lifecycle.moveToStopped());
        }
        if (tcpDeploymentId != null) {
            vertx.undeploy(tcpDeploymentId);
        }
    }
}
