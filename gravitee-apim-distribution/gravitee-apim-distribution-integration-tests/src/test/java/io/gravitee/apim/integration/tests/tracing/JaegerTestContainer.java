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
package io.gravitee.apim.integration.tests.tracing;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class JaegerTestContainer extends GenericContainer<JaegerTestContainer> {

    private static final String JAEGER_DOCKER_IMAGE = "jaegertracing/all-in-one:1.69.0";
    public static final int JAEGER_COLLECTOR_GRPC_PORT = 4317;
    public static final int JAEGER_COLLECTOR_HTTP_PORT = 4318;
    public static final int JAEGER_ADMIN_PORT = 14269;
    public static final int JAEGER_FRONTEND_PORT = 16686;

    public JaegerTestContainer() {
        super(DockerImageName.parse(JAEGER_DOCKER_IMAGE));
        setWaitStrategy(Wait.forHttp("/").forPort(JAEGER_ADMIN_PORT).forStatusCode(200));
        withExposedPorts(JAEGER_ADMIN_PORT, JAEGER_COLLECTOR_GRPC_PORT, JAEGER_COLLECTOR_HTTP_PORT, JAEGER_FRONTEND_PORT);
    }

    public WebClient client(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions().setDefaultHost(getHost()).setDefaultPort(getJaegerFrontendPort()));
    }

    public int getCollectorGrpcPort() {
        return getMappedPort(JAEGER_COLLECTOR_GRPC_PORT);
    }

    public int getCollectorHttpPort() {
        return getMappedPort(JAEGER_COLLECTOR_HTTP_PORT);
    }

    public int getJaegerFrontendPort() {
        return getMappedPort(JAEGER_FRONTEND_PORT);
    }
}
