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
package io.gravitee.apim.integration.tests.http.flows;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders.headerValue;
import static io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders.requestFlowHeader;
import static io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders.responseFlowHeader;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowPhaseExecutionV4EmulationIntegrationTest extends FlowPhaseExecutionV3IntegrationTest {

    @Nested
    @GatewayTest
    @DisplayName("Flows without condition and operator 'STARTS_WITH'")
    class NoConditionOperatorStartsWith extends FlowPhaseExecutionV3IntegrationTest.NoConditionOperatorStartsWith {}

    @Nested
    @GatewayTest
    @DisplayName("Flows without condition and operator 'EQUALS'")
    class NoConditionOperatorEquals extends FlowPhaseExecutionV3IntegrationTest.NoConditionOperatorEquals {}

    @Nested
    @GatewayTest
    @DisplayName("Flows without condition and mixed operators")
    class NoConditionOperatorMixed extends FlowPhaseExecutionV3IntegrationTest.NoConditionOperatorMixed {}

    @Nested
    @GatewayTest
    @DisplayName("Flows without condition and mixed operators")
    class ConditionalFlows extends FlowPhaseExecutionV3IntegrationTest.ConditionalFlows {

        /**
         * This test case remove the header used for evaluating the condition from the request.
         * In V4 Emulation mode, it's not a problem.
         * In V3, condition is evaluated before request and before response.
         * If the header is removed, with the current condition it will end with a NullPointerException and so a 500 response.
         */
        @Test
        @Override
        void should_pass_through_correct_flow_condition_remove_header_on_request(HttpClient client) {
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

            client
                .rxRequest(HttpMethod.GET, "/test-double-evaluation")
                .flatMap(request -> request.putHeader("X-Condition-Flow-Selection", "root-condition").rxSend())
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response)).contains(Map.entry(responseFlowHeader(0), headerValue("/")));
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });

            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(requestFlowHeader(0), equalTo(headerValue("/"))));
        }
    }
}
