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
package io.gravitee.gateway.tests.websocket.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@GatewayTest
@DeployApi({ "/apis/v4/http/api.json" })
public class WebsocketBidirectionalTest extends AbstractWebsocketV4GatewayTest {

    @Test
    public void websocket_bidirectional_request(VertxTestContext testContext) throws Throwable {
        httpServer
            .webSocketHandler(
                serverWebSocket -> {
                    serverWebSocket.exceptionHandler(testContext::failNow);
                    serverWebSocket.accept();
                    serverWebSocket.frameHandler(
                        frame -> {
                            if (frame.isText()) {
                                testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                                serverWebSocket.writeTextMessage("PONG");
                            }
                        }
                    );
                }
            )
            .listen(websocketPort)
            .map(
                httpServer ->
                    httpClient
                        .webSocket("/test")
                        .subscribe(
                            webSocket -> {
                                webSocket.exceptionHandler(testContext::failNow);
                                webSocket.frameHandler(
                                    frame -> {
                                        if (frame.isText()) {
                                            testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PONG"));
                                            testContext.completeNow();
                                        }
                                    }
                                );
                                webSocket.writeTextMessage("PING");
                            },
                            testContext::failNow
                        )
            )
            .subscribe();

        testContext.awaitCompletion(10, TimeUnit.SECONDS);
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }
}
