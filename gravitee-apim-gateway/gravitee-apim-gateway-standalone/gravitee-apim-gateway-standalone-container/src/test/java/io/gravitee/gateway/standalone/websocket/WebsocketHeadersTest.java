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
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/websocket/teams.json")
public class WebsocketHeadersTest extends AbstractWebSocketGatewayTest {

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    @Test
    public void websocket_header_request() throws InterruptedException {
        final String customHeaderName = "Custom-Header";
        final String customHeaderValue = "My-Custom-Header-Value";

        VertxTestContext testContext = new VertxTestContext();

        httpServer
            .webSocketHandler(
                event -> {
                    event.accept();
                    String customHeader = event.headers().get(customHeaderName);

                    Assert.assertNotNull(customHeader);
                    Assert.assertEquals(customHeaderValue, customHeader);
                    event.writeTextMessage("PING");
                }
            )
            .listen(16664);

        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setURI("/test").setHeaders(MultiMap.caseInsensitiveMultiMap().add(customHeaderName, customHeaderValue));

        httpClient.webSocket(
            options,
            event -> {
                if (event.failed()) {
                    testContext.failNow(event.cause());
                } else {
                    final WebSocket webSocket = event.result();
                    webSocket.exceptionHandler(testContext::failNow);
                    webSocket.frameHandler(
                        frame -> {
                            if (frame.isText()) {
                                Assert.assertEquals("PING", frame.textData());
                                testContext.completeNow();
                            } else {
                                testContext.failNow("The frame is not a text frame");
                            }
                        }
                    );
                }
            }
        );

        testContext.awaitCompletion(10, TimeUnit.SECONDS);
        Assert.assertTrue(testContext.completed());
    }
}
