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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocket;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junitpioneer.jupiter.RetryingTest;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/websocket/teams.json")
public class WebsocketPingFrameTest extends AbstractWebSocketGatewayTest {

    private static final Integer WEBSOCKET_PORT = 16669;

    @Override
    protected String getApiEndpointTarget() {
        return "http://localhost:" + WEBSOCKET_PORT;
    }

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    @RetryingTest(maxAttempts = 3)
    public void websocket_bidirectional_request() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        VertxTestContext testContext = new VertxTestContext();

        HttpServer httpServer = vertx.createHttpServer();
        httpServer
            .webSocketHandler(event -> {
                event.accept();
                event.frameHandler(frame -> {
                    if (frame.isPing()) {
                        testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                        testContext.completeNow();
                    } else {
                        testContext.failNow("The frame is not a text frame");
                    }
                });
            })
            .listen(WEBSOCKET_PORT);

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8082).setDefaultHost("localhost"));

        httpClient.webSocket(
            "/test",
            event -> {
                if (event.failed()) {
                    logger.error("An error occurred during websocket call", event.cause());
                    testContext.failNow("An error occurred during websocket call");
                } else {
                    final WebSocket webSocket = event.result();
                    webSocket.writePing(Buffer.buffer("PING"));
                }
            }
        );

        testContext.awaitCompletion(10, TimeUnit.SECONDS);
        httpServer.close();

        String failureMessage = testContext.causeOfFailure() != null ? testContext.causeOfFailure().getMessage() : null;
        assertTrue(failureMessage, testContext.completed());
    }
}
