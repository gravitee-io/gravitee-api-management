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
package io.gravitee.gateway.tests.websocket.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@GatewayTest
@DeployApi({ "/apis/http/api.json" })
public class WebsocketHeadersJupiterIntegrationTest extends AbstractWebsocketGatewayTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.jupiterModeEnabled(true).jupiterModeDefault("always");
    }

    @Test
    public void websocket_header_request(VertxTestContext testContext) throws Throwable {
        final String customHeaderName = "Custom-Header";
        final String customHeaderValue = "My-Custom-Header-Value";
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setURI("/test").setHeaders(MultiMap.caseInsensitiveMultiMap().add(customHeaderName, customHeaderValue));

        httpServer
            .webSocketHandler(
                event -> {
                    event.accept();
                    String customHeader = event.headers().get(customHeaderName);

                    testContext.verify(() -> assertThat(customHeader).isNotNull());
                    testContext.verify(() -> assertThat(customHeaderValue).isEqualTo(customHeader));
                    event.writeTextMessage("PING");
                }
            )
            .listen(websocketPort)
            .map(
                httpServer ->
                    httpClient
                        .webSocket(options)
                        .subscribe(
                            webSocket -> {
                                webSocket.exceptionHandler(testContext::failNow);
                                webSocket.frameHandler(
                                    frame -> {
                                        testContext.verify(() -> assertThat(frame.isText()).isTrue());
                                        testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                                        testContext.completeNow();
                                    }
                                );
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
