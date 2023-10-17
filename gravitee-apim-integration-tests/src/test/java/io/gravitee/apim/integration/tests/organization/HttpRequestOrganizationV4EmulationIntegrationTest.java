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
package io.gravitee.apim.integration.tests.organization;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganizations;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.AddHeader2Policy;
import io.gravitee.apim.integration.tests.fake.AddHeaderPolicy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployOrganizations(
    {
        @DeployOrganization(organization = "/organizations/organization-1-add-header-1.json", apis = { "/organizations/apis/api-1.json" }),
        @DeployOrganization(organization = "/organizations/organization-2-add-header-2.json", apis = { "/organizations/apis/api-2.json" }),
        @DeployOrganization(organization = "/organizations/organization-3.json"),
    }
)
class HttpRequestOrganizationV4EmulationIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("add-header-1", PolicyBuilder.build("add-header-1", AddHeaderPolicy.class));
        policies.put("add-header-2", PolicyBuilder.build("add-header-2", AddHeader2Policy.class));
    }

    @Test
    void should_apply_organization_1_flow_on_api_1(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test-1")
            .flatMap(request -> request.rxSend())
            .flatMapPublisher(response -> {
                assertOrganization1ResponseHeaders(response);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertNoErrors();

        assertOrganization1RequestHeaders();
    }

    // With V4 Emulation engine, if an exception is thrown during api flows, then platform response flow is executed
    protected void assertOrganization1ResponseHeaders(HttpClientResponse response) {
        assertThat(response.headers().get(AddHeaderPolicy.HEADER_NAME)).isEqualTo(AddHeaderPolicy.RESPONSE_HEADER);
    }

    private void assertOrganization1RequestHeaders() {
        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(AddHeaderPolicy.HEADER_NAME, equalTo(AddHeaderPolicy.REQUEST_HEADER))
        );
    }

    @Test
    void should_apply_organization_2_flow_on_api_2(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.POST, "/test-2")
            .flatMap(request -> request.rxSend("request body"))
            .flatMapPublisher(response -> {
                assertOrganization2ResponseHeaders(response);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertNoErrors();

        assertOrganization2RequestHeaders();
    }

    // With V4 Emulation engine, if an exception is thrown during api flows, then platform response flow is executed
    protected void assertOrganization2ResponseHeaders(HttpClientResponse response) {
        assertThat(response.headers().get(AddHeader2Policy.HEADER_NAME)).isEqualTo(AddHeader2Policy.RESPONSE_HEADER);
    }

    protected void assertOrganization2RequestHeaders() {
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/endpoint")).withHeader(AddHeader2Policy.HEADER_NAME, equalTo(AddHeader2Policy.REQUEST_HEADER))
        );
    }
}
