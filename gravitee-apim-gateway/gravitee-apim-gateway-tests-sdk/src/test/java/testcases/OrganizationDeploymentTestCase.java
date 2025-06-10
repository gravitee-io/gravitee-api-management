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
package testcases;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header2Policy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployOrganization(organization = "/organizations/organization-add-header-1.json", apis = { "/apis/nothing.json" })
@EnableForGatewayTestingExtensionTesting
public class OrganizationDeploymentTestCase extends AbstractGatewayTest {

    @Test
    void should_execute_organization_flow(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/team").willReturn(ok()));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .await()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("X-Gravitee-Policy")).isEqualTo("response-header1");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Policy", equalTo("request-header1")));
    }

    @Test
    @DeployOrganization(organization = "/organizations/organization-add-header-2.json", apis = "/apis/nothing-bis.json")
    void should_execute_organization_flow_deployed_at_test_level(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/team").willReturn(ok()));

        httpClient
            .rxRequest(HttpMethod.GET, "/test2")
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .await()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("X-Gravitee-Policy")).isEqualTo("response-header2");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Policy", equalTo("request-header2")));
    }

    @Test
    @DeployOrganization(organization = "/organizations/organization-add-header-1.json", apis = "/apis/nothing.json")
    void should_fail_deploying_a_already_deployed_organization() {}

    @Test
    void should_execute_organization_flow_deployed_at_test_level_with_update(HttpClient httpClient) throws InterruptedException {
        super.updateOrganization(
            "ORGA-1",
            organization -> {
                organization.getFlows().getFirst().getPre().getFirst().setPolicy("header-policy2");
                organization.getFlows().getFirst().getPost().getFirst().setPolicy("header-policy2");
            }
        );

        wiremock.stubFor(get("/team").willReturn(ok()));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .await()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("X-Gravitee-Policy")).isEqualTo("response-header2");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Policy", equalTo("request-header2")));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));
        policies.put("header-policy2", PolicyBuilder.build("header-policy2", Header2Policy.class));
    }
}
