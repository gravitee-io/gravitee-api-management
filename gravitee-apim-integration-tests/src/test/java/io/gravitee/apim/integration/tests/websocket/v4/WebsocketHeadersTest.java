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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.WebSocketClient;
import org.junit.jupiter.api.Test;

@GatewayTest
public class WebsocketHeadersTest extends AbstractWebsocketV4GatewayTest {

    @Test
    @DeployApi({ "/apis/v4/http/api.json" })
    public void websocket_header_request(VertxTestContext testContext, WebSocketClient webSocketClient) throws Throwable {
        var serverConnected = testContext.checkpoint();
        var serverMessageSent = testContext.checkpoint();
        var serverMessageChecked = testContext.checkpoint();

        final String customHeaderName = "Custom-Header";
        final String customHeaderValue = "My-Custom-Header-Value";
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setURI("/test").setHeaders(MultiMap.caseInsensitiveMultiMap().add(customHeaderName, customHeaderValue));

        Promise<Void> clientReady = Promise.promise();

        websocketServerHandler = serverWebSocket ->
            Completable.fromRunnable(() -> {
                serverConnected.flag();

                String customHeader = serverWebSocket.headers().get(customHeaderName);
                testContext.verify(() -> assertThat(customHeader).isNotNull());
                testContext.verify(() -> assertThat(customHeaderValue).isEqualTo(customHeader));

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

        webSocketClient
            .connect(options)
            .doOnSuccess(webSocket -> {
                webSocket.exceptionHandler(testContext::failNow);
                webSocket.frameHandler(frame -> {
                    testContext.verify(() -> assertThat(frame.isText()).isTrue());
                    testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                    serverMessageChecked.flag();
                });
                clientReady.complete();
            })
            .doOnError(testContext::failNow)
            .test()
            .await();
    }

    @Test
    @DeployApi({ "/apis/v4/http/api.json" })
    public void websocket_should_forward_origin_header(VertxTestContext testContext, WebSocketClient webSocketClient) throws Throwable {
        // Origin is special-cased by Vert.x, unlike arbitrary headers, so the custom-header test does not cover it.
        var serverConnected = testContext.checkpoint();
        var serverMessageSent = testContext.checkpoint();
        var serverMessageChecked = testContext.checkpoint();

        final String originValue = "http://my-origin-host";
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setURI("/test").setHeaders(MultiMap.caseInsensitiveMultiMap().add("Origin", originValue));

        Promise<Void> clientReady = Promise.promise();

        websocketServerHandler = serverWebSocket ->
            Completable.fromRunnable(() -> {
                serverConnected.flag();

                String origin = serverWebSocket.headers().get("Origin");
                testContext.verify(() -> assertThat(origin).isEqualTo(originValue));

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

        webSocketClient
            .connect(options)
            .doOnSuccess(webSocket -> {
                webSocket.exceptionHandler(testContext::failNow);
                webSocket.frameHandler(frame -> {
                    testContext.verify(() -> assertThat(frame.isText()).isTrue());
                    testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                    serverMessageChecked.flag();
                });
                clientReady.complete();
            })
            .doOnError(testContext::failNow)
            .test()
            .await();
    }
}
