package testcases;/**
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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header2Policy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/nothing.json" })
@DeployOrganization("/organizations/organization-add-header-1.json")
public class OrganizationDeploymentTestCase extends AbstractGatewayTest {

    public static final String ON_REQUEST_POLICY = "on-request-policy";

    @Test
    @DisplayName("Should test organization flow")
    void shouldTestOrganizationFlow(WebClient webClient) {
        wiremock.stubFor(get("/team").willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.headers().get("X-Gravitee-Policy")).isEqualTo("response-header1");

                    return true;
                }
            )
            .assertNoErrors();
        wiremock.verify(getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Policy", equalTo("request-header1")));
    }

    @Test
    @DeployOrganization("/organizations/organization-add-header-2.json")
    @DisplayName("Should test organization flow at test level using annotation")
    void shouldTestOrganizationFlowAtTestLevel(WebClient webClient) {
        wiremock.stubFor(get("/team").willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.headers().get("X-Gravitee-Policy")).isEqualTo("response-header2");

                    return true;
                }
            )
            .assertNoErrors();
        wiremock.verify(getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Policy", equalTo("request-header2")));
    }

    @Test
    @DisplayName("Should test organization flow at test level using updateAndDeployOrganizationMethod")
    void shouldTestOrganizationFlowAtTestLevelWithMethodCall(WebClient webClient) {
        super.updateAndDeployOrganization(
            organization -> {
                organization.getFlows().get(0).getPre().get(0).setPolicy("header-policy2");
                organization.getFlows().get(0).getPost().get(0).setPolicy("header-policy2");
            }
        );

        wiremock.stubFor(get("/team").willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.headers().get("X-Gravitee-Policy")).isEqualTo("response-header2");

                    return true;
                }
            )
            .assertNoErrors();
        wiremock.verify(getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Policy", equalTo("request-header2")));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));
        policies.put("header-policy2", PolicyBuilder.build("header-policy2", Header2Policy.class));
    }
}
