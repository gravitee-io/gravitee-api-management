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
package io.gravitee.apim.integration.tests.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.integration.tests.fake.AddHeaderPolicy;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import org.junit.jupiter.api.Nested;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class HttpRequestTimeoutV3IntegrationTest extends HttpRequestTimeoutV4EmulationIntegrationTest {

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployOrganization(
        organization = "/organizations/organization-add-header.json",
        apis = { "/apis/http/api.json", "/apis/http/api-latency.json" }
    )
    class OrganizationWithAddHeader extends HttpRequestTimeoutV4EmulationIntegrationTest.OrganizationWithAddHeader {

        /**
         * Assert that platform headers are not present in the response because in V3 mode,
         * if an exception is thrown during api flows, then platform response flow is not executed.
         */
        @Override
        protected void assertPlatformHeaders(HttpClientResponse response) {
            assertThat(response.headers().contains(AddHeaderPolicy.HEADER_NAME)).isFalse();
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployOrganization(organization = "/organizations/organization-add-header-and-latency.json", apis = { "/apis/http/api.json" })
    class OrganizationWithAddHeaderAndLatency extends HttpRequestTimeoutV4EmulationIntegrationTest.OrganizationWithAddHeaderAndLatency {}
}
