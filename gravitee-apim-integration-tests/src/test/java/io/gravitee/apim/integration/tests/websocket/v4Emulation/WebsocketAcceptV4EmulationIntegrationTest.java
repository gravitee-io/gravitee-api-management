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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Promise;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@GatewayTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebsocketAcceptV4EmulationIntegrationTest extends AbstractWebsocketGatewayTest {

    @Test
    @DeployApi({ "/apis/http/api.json" })
    public void websocket_accepted_request(VertxTestContext testContext, HttpClient httpClient) throws Throwable {
        var serverConnected = testContext.checkpoint();
        var serverMessageSent = testContext.checkpoint();
        var serverMessageChecked = testContext.checkpoint();

        Promise<Void> clientReady = Promise.promise();

        websocketServerHandler = serverWebSocket ->
            Completable.fromRunnable(() -> {
                serverConnected.flag();
                serverWebSocket.exceptionHandler(testContext::failNow);
                serverWebSocket.accept();

                clientReady
                    .future()
                    .onSuccess(__ ->
                        serverWebSocket
                            .writeTextMessage("PING")
                            .doOnComplete(serverMessageSent::flag)
                            .doOnError(testContext::failNow)
                            .subscribe()
                    );
            })
                .subscribeOn(Schedulers.io())
                .subscribe();

        httpClient
            .webSocket("/test")
            .doOnSuccess(webSocket -> {
                webSocket.exceptionHandler(testContext::failNow);
                webSocket.frameHandler(frame -> {
                    if (frame.isText()) {
                        testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                        serverMessageChecked.flag();
                    } else {
                        testContext.failNow("The frame is not a text frame");
                    }
                });
                clientReady.complete();
            })
            .doOnError(testContext::failNow)
            .test()
            .await();
    }
}
