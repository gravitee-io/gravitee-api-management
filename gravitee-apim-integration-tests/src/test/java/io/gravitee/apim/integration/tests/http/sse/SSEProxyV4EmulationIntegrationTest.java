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
package io.gravitee.apim.integration.tests.http.sse;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class SSEProxyV4EmulationIntegrationTest extends SSEProxyV3IntegrationTest {

    @Test
    @Override
    @DeployApi({ "/apis/http/sse/api-sse-proxy-v2.json", "/apis/v4/http/sse/api-mock-sse-backend.json" })
    void should_proxy_sse_and_close_backend_connection_when_client_closes_the_connection(HttpClient httpClient) {
        super.should_proxy_sse_and_close_backend_connection_when_client_closes_the_connection(httpClient);
    }
}
