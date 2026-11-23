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
package io.gravitee.apim.gateway.integration.tests.websocket.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.WebSocketClient;
import org.junit.jupiter.api.Test;

@GatewayTest
class WebsocketBackendFrameFloorTest extends AbstractWebsocketLargeFrameTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.set("http.websocket.maxWebSocketFrameSize", 16 * 1024);
        gatewayConfigurationBuilder.set("http.websocket.maxWebSocketMessageSize", 16 * 1024);
    }

    @Test
    @DeployApi({ "/apis/v4/http/api.json" })
    void should_still_relay_64_kib_backend_frame_when_server_limit_is_lower(VertxTestContext testContext, WebSocketClient webSocketClient)
        throws Throwable {
        backendSendsOneFrameOfRequestedSize(testContext);

        Outcome outcome = askBackendForOneFrame(webSocketClient, VERTX_DEFAULT_FRAME_SIZE);

        assertThat(outcome).isEqualTo(new Outcome(VERTX_DEFAULT_FRAME_SIZE, null, null));
        testContext.completeNow();
    }
}
