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
package io.gravitee.apim.integration.tests.http.cors;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayMode;
import org.junit.jupiter.api.Nested;

public class CorsV3CompatibilityIntegrationTest extends CorsV3IntegrationTest {

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingResponseStatus extends CorsV3IntegrationTest.CheckingResponseStatus {}

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingPoliciesExecution extends CorsV3IntegrationTest.CheckingPoliciesExecution {}

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingRejection extends CorsV3IntegrationTest.CheckingRejection {}
}
