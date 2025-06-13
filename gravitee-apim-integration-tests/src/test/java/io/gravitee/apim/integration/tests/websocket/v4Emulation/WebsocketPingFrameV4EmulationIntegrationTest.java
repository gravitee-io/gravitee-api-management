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
package io.gravitee.apim.integration.tests.websocket.v4Emulation;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

@GatewayTest
public class WebsocketPingFrameV4EmulationIntegrationTest extends AbstractWebsocketGatewayTest {

    @Test
    @DeployApi({ "/apis/http/api.json" })
    public void websocket_ping_request(VertxTestContext testContext) throws Throwable {
        var serverConnected = testContext.checkpoint();
        var pingReceived = testContext.checkpoint();
        var pingSent = testContext.checkpoint();
        // use a lax checkpoint because Pong frames may be received unsolicited https://vertx.io/docs/apidocs/io/vertx/core/http/WebSocketBase.html#pongHandler-io.vertx.core.Handler-
        var pongReceived = testContext.laxCheckpoint();

        websocketServerHandler =
            (
                serverWebSocket -> {
                    serverConnected.flag();
                    serverWebSocket.exceptionHandler(testContext::failNow);
                    serverWebSocket.accept();
                    serverWebSocket.frameHandler(frame -> {
                        if (frame.isPing()) {
                            testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                            pingReceived.flag();
                        }
                    });
                }
            );

        httpClient
            .webSocket("/test")
            .doOnSuccess(webSocket -> {
                webSocket.exceptionHandler(testContext::failNow);
                webSocket.pongHandler(buffer -> pongReceived.flag());
                webSocket.writePing(Buffer.buffer("PING")).doOnComplete(pingSent::flag).doOnError(testContext::failNow).subscribe();
            })
            .doOnError(testContext::failNow)
            .test()
            .await();
    }
}
