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
package io.gravitee.apim.integration.tests.http;

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
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.AddHeaderPolicy;
import io.gravitee.apim.integration.tests.fake.LatencyPolicy;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/http/api.json", "/apis/http/api-latency.json" })
@DeployOrganization("/organizations/organization-add-header.json")
class HttpRequestTimeoutIntegrationTest extends AbstractGatewayTest {

    protected int REQUEST_TIMEOUT = 500;

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "true");
        gatewayConfigurationBuilder.set("http.requestTimeout", REQUEST_TIMEOUT);
    }

    @Override
    public void configureApi(Api api) {
        api.setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("latency", PolicyBuilder.build("latency", LatencyPolicy.class, LatencyPolicy.LatencyConfiguration.class));
        policies.put("add-header", PolicyBuilder.build("add-header", AddHeaderPolicy.class));
    }

    @Test
    @DisplayName(
        "Should receive 504 - GATEWAY_TIMEOUT and a plain text response when backend answers slower than the timeout and without Accept header"
    )
    void shouldGet504TextPlainWhenTimeoutFromBackend(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend").withFixedDelay(REQUEST_TIMEOUT + 250)));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.rxSend())
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(504);
                assertPlatformHeaders(response);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("Request timeout");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(AddHeaderPolicy.HEADER_NAME, equalTo(AddHeaderPolicy.REQUEST_HEADER))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON, MediaType.WILDCARD })
    @DisplayName(
        "Should receive 504 - GATEWAY_TIMEOUT and a json response when backend answers slower than the timeout and with Accept header"
    )
    void shouldGet504WhenTimeoutFromBackend(String acceptHeader, HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend").withFixedDelay(REQUEST_TIMEOUT + 10)));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), acceptHeader).rxSend())
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(504);
                assertPlatformHeaders(response);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("{\"message\":\"Request timeout\",\"http_status_code\":504}");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(AddHeaderPolicy.HEADER_NAME, equalTo(AddHeaderPolicy.REQUEST_HEADER))
        );
    }

    @Test
    @DisplayName(
        "Should receive 504 - GATEWAY_TIMEOUT and a plain text response when policy executes slower than the timeout and without Accept header"
    )
    void shouldGet504TextPlainWhenTimeoutFromPolicy(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test-latency")
            .flatMap(request -> request.rxSend())
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(504);
                assertPlatformHeaders(response);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("Request timeout");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            0,
            getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(AddHeaderPolicy.HEADER_NAME, equalTo(AddHeaderPolicy.REQUEST_HEADER))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON, MediaType.WILDCARD })
    @DisplayName(
        "Should receive 504 - GATEWAY_TIMEOUT and a json response when policy executes slower than the timeout and with Accept header"
    )
    void shouldGet504WhenTimeoutFromPolicy(String acceptHeader, HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test-latency")
            .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), acceptHeader).rxSend())
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(504);
                assertPlatformHeaders(response);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("{\"message\":\"Request timeout\",\"http_status_code\":504}");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    @DisplayName(
        "Should receive 504 - GATEWAY_TIMEOUT when policy on platform response executes slower than the timeout and interrupt chain"
    )
    void shouldGet504WhenTimeoutFromPlatformResponse(HttpClient httpClient) throws InterruptedException {
        addLatencyOnPlatformResponse();

        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.rxSend())
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(504);
                assertThat(response.headers().contains(AddHeaderPolicy.HEADER_NAME)).isFalse();
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("Request timeout");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(AddHeaderPolicy.HEADER_NAME, equalTo(AddHeaderPolicy.REQUEST_HEADER))
        );
    }

    // With V4 Emulation engine, if an exception is thrown during api flows, then platform response flow is executed
    protected void assertPlatformHeaders(HttpClientResponse response) {
        assertThat(response.headers().get(AddHeaderPolicy.HEADER_NAME)).isEqualTo(AddHeaderPolicy.RESPONSE_HEADER);
    }

    private void addLatencyOnPlatformResponse() {
        super.updateAndDeployOrganization(organization -> {
            final Step responseLatencyStep = new Step();
            responseLatencyStep.setConfiguration("{\"delay\": 1000}");
            responseLatencyStep.setName("latency");
            responseLatencyStep.setPolicy("latency");
            responseLatencyStep.setEnabled(true);
            organization.getFlows().get(0).getPost().add(0, responseLatencyStep);
        });
    }
}
