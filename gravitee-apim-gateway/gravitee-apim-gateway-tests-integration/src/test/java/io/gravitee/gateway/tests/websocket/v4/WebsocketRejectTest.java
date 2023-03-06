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

import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@GatewayTest
@DeployApi({ "/apis/v4/http/api.json" })
public class WebsocketRejectTest extends AbstractWebsocketV4GatewayTest {

    @Test
    public void websocket_rejected_request(VertxTestContext testContext) throws Throwable {
        httpServer
            .webSocketHandler(webSocket -> webSocket.reject(UNAUTHORIZED_401))
            .listen(websocketPort)
            .map(
                httpServer ->
                    httpClient
                        .webSocket("/test")
                        .subscribe(
                            webSocket -> {
                                testContext.failNow("Websocket connection should not succeed");
                            },
                            error -> {
                                testContext.verify(() -> assertThat(error.getClass()).isEqualTo(UpgradeRejectedException.class));
                                testContext.verify(
                                    () -> assertThat(((UpgradeRejectedException) error).getStatus()).isEqualTo(UNAUTHORIZED_401)
                                );
                                testContext.completeNow();
                            }
                        )
            )
            .subscribe();

        testContext.awaitCompletion(10, TimeUnit.SECONDS);
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }
}
