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
package io.gravitee.apim.integration.tests.multiservers;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest(v2ExecutionMode = ExecutionMode.V3)
public class MultiServersApiDeployV3IntegrationTest extends AbstractMultiServersApiDeployIntegrationTest {

    @Test
    @DeployApi("/apis/http/api.json")
    @Override
    void should_get_200_when_calling_on_both_first_and_second_servers(HttpClient httpClient) throws InterruptedException {
        super.should_get_200_when_calling_on_both_first_and_second_servers(httpClient);
    }

    @Test
    @DeployApi("/apis/http/api-single-server.json")
    @Override
    void should_get_200_when_calling_on_second_server_and_404_on_first_server(HttpClient httpClient) throws InterruptedException {
        super.should_get_200_when_calling_on_second_server_and_404_on_first_server(httpClient);
    }
}
