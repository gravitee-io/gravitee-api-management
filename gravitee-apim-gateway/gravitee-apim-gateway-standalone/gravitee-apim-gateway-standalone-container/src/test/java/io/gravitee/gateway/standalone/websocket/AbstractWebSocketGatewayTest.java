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
package io.gravitee.gateway.standalone.websocket;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.gateway.standalone.AbstractGatewayTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractWebSocketGatewayTest extends AbstractGatewayTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected int websocetPort;

    static {
        System.setProperty("vertx.disableWebsockets", Boolean.FALSE.toString());
    }

    protected Vertx vertx;
    protected HttpServer httpServer;
    protected HttpClient httpClient;

    @Before
    public void initHttpClientAndServer() {
        websocetPort = getFreePort();
        vertx = Vertx.vertx();
        httpServer = vertx.createHttpServer();
        httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8082).setDefaultHost("localhost"));
    }

    @After
    public void closeHttpClientAndServer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        httpClient.close(
            result -> {
                if (result.failed()) {
                    logger.warn("Unable to close http client", result.cause());
                }
                latch.countDown();
            }
        );

        httpServer.close(
            result -> {
                if (result.failed()) {
                    logger.warn("Unable to close http server", result.cause());
                }
                latch.countDown();
            }
        );

        if (!latch.await(5, TimeUnit.SECONDS)) {
            logger.warn("Unable to close http client and server");
        }

        vertx.close();
    }

    /**
     * Override this method to set dynamically the API endpoint target.
     *
     * @return Override API endpoint target
     */
    protected String getApiEndpointTarget() {
        return null;
    }

    @Override
    public void before(Api api) {
        super.before(api);

        if (getApiEndpointTarget() != null) {
            Set<EndpointGroup> groups = api.getProxy().getGroups();
            if (!groups.isEmpty()) {
                Set<Endpoint> endpoints = groups.iterator().next().getEndpoints();
                if (!endpoints.isEmpty()) {
                    endpoints.iterator().next().setTarget(getApiEndpointTarget());
                }
            }
        }
    }

    static int getFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }
}
