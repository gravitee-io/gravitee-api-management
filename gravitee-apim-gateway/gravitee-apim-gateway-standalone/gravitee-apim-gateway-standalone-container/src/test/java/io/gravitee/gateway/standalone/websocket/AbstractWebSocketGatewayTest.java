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

import static org.junit.Assert.assertTrue;

import io.gravitee.gateway.standalone.AbstractGatewayTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
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

    static {
        System.setProperty("vertx.disableWebsockets", Boolean.FALSE.toString());
    }

    protected Vertx vertx;
    protected HttpServer httpServer;
    protected HttpClient httpClient;

    @Before
    public void initHttpClientAndServer() {
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

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
