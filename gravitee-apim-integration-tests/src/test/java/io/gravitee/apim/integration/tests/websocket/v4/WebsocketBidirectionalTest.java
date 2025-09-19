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
package io.gravitee.apim.integration.tests.websocket.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.Test;

@GatewayTest
public class WebsocketBidirectionalTest extends AbstractWebsocketV4GatewayTest {

    @Test
    @DeployApi({ "/apis/v4/http/api.json" })
    public void websocket_bidirectional_request(VertxTestContext testContext, HttpClient httpClient) throws Throwable {
        var serverConnected = testContext.checkpoint();
        var serverMessageSent = testContext.checkpoint();
        var serverMessageChecked = testContext.checkpoint();
        var clientMessageSent = testContext.checkpoint();
        var clientMessageChecked = testContext.checkpoint();

        websocketServerHandler = serverWebSocket -> {
            serverConnected.flag();
            serverWebSocket.exceptionHandler(testContext::failNow);
            serverWebSocket.accept();
            serverWebSocket.frameHandler(frame -> {
                if (frame.isText()) {
                    testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                    clientMessageChecked.flag();
                    serverWebSocket
                        .writeTextMessage("PONG")
                        .doOnComplete(serverMessageSent::flag)
                        .doOnError(testContext::failNow)
                        .subscribe();
                }
            });
        };

        httpClient
            .webSocket("/test")
            .doOnSuccess(webSocket -> {
                webSocket.exceptionHandler(testContext::failNow);
                webSocket.frameHandler(frame -> {
                    if (frame.isText()) {
                        testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PONG"));
                        serverMessageChecked.flag();
                    }
                });
                webSocket.writeTextMessage("PING").doOnComplete(clientMessageSent::flag).doOnError(testContext::failNow).subscribe();
            })
            .doOnError(testContext::failNow)
            .test()
            .await();
    }
}
