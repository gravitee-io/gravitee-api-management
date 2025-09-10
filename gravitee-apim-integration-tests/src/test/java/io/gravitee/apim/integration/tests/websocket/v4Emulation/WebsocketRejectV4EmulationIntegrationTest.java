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

import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.Test;

@GatewayTest
public class WebsocketRejectV4EmulationIntegrationTest extends AbstractWebsocketGatewayTest {

    @Test
    @DeployApi({ "/apis/http/api.json" })
    public void websocket_rejected_request(VertxTestContext testContext, HttpClient httpClient) throws Throwable {
        websocketServerHandler = (webSocket -> webSocket.reject(UNAUTHORIZED_401));

        httpClient
            .webSocket("/test")
            .doOnSuccess(webSocket -> testContext.failNow("Websocket connection should not succeed"))
            .doOnError(error -> {
                testContext.verify(() ->
                    assertThat(error).isInstanceOf(UpgradeRejectedException.class).extracting("status").isEqualTo(UNAUTHORIZED_401)
                );
                testContext.completeNow();
            })
            .test()
            .await();
    }
}
