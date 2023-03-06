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

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@GatewayTest
@DeployApi({ "/apis/v4/http/api.json" })
public class WebsocketPingFrameTest extends AbstractWebsocketV4GatewayTest {

    @Test
    public void websocket_ping_request(VertxTestContext testContext) throws Throwable {
        httpServer
            .webSocketHandler(
                serverWebSocket -> {
                    serverWebSocket.exceptionHandler(testContext::failNow);
                    serverWebSocket.accept();
                    serverWebSocket.frameHandler(
                        frame -> {
                            if (frame.isPing()) {
                                testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                                testContext.completeNow();
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
                        .flatMapCompletable(
                            webSocket -> {
                                webSocket.exceptionHandler(testContext::failNow);
                                return webSocket.rxWritePing(Buffer.buffer("PING"));
                            }
                        )
                        .subscribe(() -> {}, testContext::failNow)
            )
            .subscribe();

        testContext.awaitCompletion(10, TimeUnit.SECONDS);
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }
}
