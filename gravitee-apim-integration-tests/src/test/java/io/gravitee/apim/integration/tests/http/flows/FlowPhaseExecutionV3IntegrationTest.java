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
package io.gravitee.apim.integration.tests.http.flows;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayMode;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;

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
class FlowPhaseExecutionV3IntegrationTest extends FlowPhaseExecutionIntegrationTest {

    @Nested
    @GatewayTest(mode = GatewayMode.V3)
    @DisplayName("Flows without condition and operator 'STARTS_WITH'")
    class NoConditionOperatorStartsWith extends FlowPhaseExecutionIntegrationTest.NoConditionOperatorStartsWith {}

    @Nested
    @GatewayTest(mode = GatewayMode.V3)
    @DisplayName("Flows without condition and operator 'EQUALS'")
    class NoConditionOperatorEquals extends FlowPhaseExecutionIntegrationTest.NoConditionOperatorEquals {}

    @Nested
    @GatewayTest(mode = GatewayMode.V3)
    @DisplayName("Flows without condition and mixed operators")
    class NoConditionOperatorMixed extends FlowPhaseExecutionIntegrationTest.NoConditionOperatorMixed {}

    @Nested
    @GatewayTest(mode = GatewayMode.V3)
    @DisplayName("Flows without condition and mixed operators")
    class ConditionalFlows extends FlowPhaseExecutionIntegrationTest.ConditionalFlows {

        /**
         * This test case remove the header used for evaluating the condition from the request.
         * In V4 Emulation mode, it's not a problem.
         * In V3, condition is evaluated before request and before response.
         * If the header is removed, with the current condition it will end with a NullPointerException and so a 500 response.
         */
        @Override
        @Test
        void should_pass_through_correct_flow_condition_remove_header_on_request(HttpClient client) {
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

            client
                    .rxRequest(HttpMethod.GET, "/test-double-evaluation")
                    .flatMap(request -> request.putHeader("X-Condition-Flow-Selection", "root-condition").rxSend())
                    .flatMap(response -> {
                        assertThat(response.statusCode()).isEqualTo(500);
                        return response.body();
                    })
                    .test()
                    .awaitDone(2, TimeUnit.SECONDS)
                    .assertComplete()
                    .assertValue(response -> {
                        assertThat(response)
                                .hasToString(
                                        "The template evaluation returns an error. Expression:\n" +
                                                "{##request.headers['X-Condition-Flow-Selection'][0] == 'root-condition'} "
                                );
                        return true;
                    });
        }
    }
}
