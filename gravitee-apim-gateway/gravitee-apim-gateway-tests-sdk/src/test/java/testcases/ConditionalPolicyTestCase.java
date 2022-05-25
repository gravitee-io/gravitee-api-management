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
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.OnRequestPolicy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Stream1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Stream2Policy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/conditional-policy-flow.json" })
public class ConditionalPolicyTestCase extends AbstractGatewayTest {

    public static final String ENDPOINT = "/test/my_team";
    public static final String API_ENTRYPOINT = "/team/my_team";
    public static final String CONDITION_HEADER = "conditionHeader";
    public static final String X_GRAVITEE_POLICY = "X-Gravitee-Policy";
    public static final String ON_REQUEST_POLICY = "on-request-policy";
    public static final String INVOKED = "invoked";

    @Test
    @DisplayName("Should test policy if condition is met")
    void testConditional(WebClient webClient) {
        wiremock.stubFor(get(API_ENTRYPOINT).willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get(ENDPOINT).putHeader(CONDITION_HEADER, "condition-ok").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    final String content = response.bodyAsString();

                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.headers().contains(X_GRAVITEE_POLICY)).isFalse();
                    assertThat(content).isEqualTo("OnResponseContent2Policy");

                    return true;
                }
            );
        obs.assertNoErrors();
        wiremock.verify(
            getRequestedFor(urlPathEqualTo(API_ENTRYPOINT))
                .withHeader(X_GRAVITEE_POLICY, equalTo("request-header1"))
                .withHeader(ON_REQUEST_POLICY, equalTo(INVOKED))
        );
    }

    @Test
    @DisplayName("Should not execute policy if condition is not met")
    void testConditionalNotMet(WebClient webClient) {
        wiremock.stubFor(get(API_ENTRYPOINT).willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get(ENDPOINT).putHeader(CONDITION_HEADER, "condition-no").rxSend().test();

        obs.awaitTerminalEvent(10000, TimeUnit.MILLISECONDS);
        obs.assertComplete();
        obs.assertValue(
            response -> {
                final String content = response.bodyAsString();

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().contains(X_GRAVITEE_POLICY)).isFalse();
                assertThat(content).isNull();

                return true;
            }
        );
        obs.assertNoErrors();
        wiremock.verify(
            getRequestedFor(urlPathEqualTo(API_ENTRYPOINT)).withoutHeader(X_GRAVITEE_POLICY).withHeader(ON_REQUEST_POLICY, equalTo(INVOKED))
        );
    }

    @Test
    @DeployApi("/nonExisting.json")
    @DisplayName("Should not execute policy if condition is not met")
    void shouldFailBecauseOfNonExistingApi(WebClient webClient) {
        wiremock.stubFor(get(API_ENTRYPOINT).willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get(ENDPOINT).putHeader(CONDITION_HEADER, "condition-no").rxSend().test();

        obs.awaitTerminalEvent(10000, TimeUnit.MILLISECONDS);
        obs.assertComplete();
        obs.assertValue(
            response -> {
                final String content = response.bodyAsString();

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().contains(X_GRAVITEE_POLICY)).isFalse();
                assertThat(content).isNull();

                return true;
            }
        );
        obs.assertNoErrors();
        wiremock.verify(
            getRequestedFor(urlPathEqualTo(API_ENTRYPOINT)).withoutHeader(X_GRAVITEE_POLICY).withHeader(ON_REQUEST_POLICY, equalTo(INVOKED))
        );
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));
        policies.put(ON_REQUEST_POLICY, PolicyBuilder.build(ON_REQUEST_POLICY, OnRequestPolicy.class));
        policies.put("stream-policy", PolicyBuilder.build("stream-policy", Stream1Policy.class));
        policies.put("stream-policy2", PolicyBuilder.build("stream-policy2", Stream2Policy.class));
    }
}
