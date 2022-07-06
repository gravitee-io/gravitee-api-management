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

import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocket;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/websocket/teams.json")
public class WebsocketPingFrameTest extends AbstractWebSocketGatewayTest {

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    @Test
    public void websocket_ping_request() throws InterruptedException {
        VertxTestContext testContext = new VertxTestContext();

        httpServer
            .webSocketHandler(
                serverWebSocket -> {
                    serverWebSocket.exceptionHandler(testContext::failNow);
                    serverWebSocket.accept();
                    serverWebSocket.frameHandler(
                        frame -> {
                            if (frame.isPing()) {
                                Assert.assertEquals("PING", frame.textData());
                                testContext.completeNow();
                            } else {
                                testContext.failNow("The frame is not a text frame");
                            }
                        }
                    );
                }
            )
            .listen(16664);

        httpClient.webSocket(
            "/test",
            event -> {
                if (event.failed()) {
                    testContext.failNow(event.cause());
                } else {
                    final WebSocket webSocket = event.result();
                    webSocket.exceptionHandler(testContext::failNow);
                    webSocket.writePing(Buffer.buffer("PING"));
                }
            }
        );

        testContext.awaitCompletion(10, TimeUnit.SECONDS);
        Assert.assertTrue(testContext.completed());
    }
}
