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
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowPhaseExecutionV3IntegrationTest {

    public static final String RESPONSE_FROM_BACKEND = "response from backend";
    public static final String PARAMETERS_PROVIDERS_CLASS =
        "io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders#";

    /**
     * Inherit from this class to have the required configuration to run each tests of this class
     */
    class TestPreparer extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.putIfAbsent(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/flows/api.json")
    @DisplayName("Flows without condition and operator 'STARTS_WITH'")
    class NoConditionOperatorStartsWith extends TestPreparer {

        @ParameterizedTest
        @MethodSource(PARAMETERS_PROVIDERS_CLASS + "parametersStartsWithOperatorCase")
        void should_pass_through_correct_flows(
            String path,
            Map<String, String> expectedRequestHeaders,
            Map<String, String> expectedResponseHeaders,
            HttpClient client
        ) {
            wiremock.stubFor(get("/endpoint" + path).willReturn(ok(RESPONSE_FROM_BACKEND)));

            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    expectedResponseHeaders.forEach((name, value) -> {
                        assertThat(extractHeaders(response)).contains(Map.entry(name, value));
                    });
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });

            final RequestPatternBuilder requestedFor = getRequestedFor(urlPathEqualTo("/endpoint" + path));
            expectedRequestHeaders.forEach((name, value) -> requestedFor.withHeader(name, equalTo(value)));
            wiremock.verify(requestedFor);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/flows/api.json")
    @DisplayName("Flows without condition and operator 'EQUALS'")
    class NoConditionOperatorEquals extends TestPreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isLegacyApi(definitionClass)) {
                final Api definition = (Api) api.getDefinition();
                definition
                    .getFlows()
                    .forEach(flow -> {
                        flow.setPathOperator(new PathOperator(flow.getPath(), Operator.EQUALS));
                    });
            }
        }

        @ParameterizedTest
        @MethodSource(PARAMETERS_PROVIDERS_CLASS + "parametersEqualsOperatorCase")
        void should_pass_through_correct_flows(
            String path,
            Map<String, String> expectedRequestHeaders,
            Map<String, String> expectedResponseHeaders,
            HttpClient client
        ) {
            wiremock.stubFor(get("/endpoint" + path).willReturn(ok(RESPONSE_FROM_BACKEND)));

            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    expectedResponseHeaders.forEach((name, value) -> {
                        assertThat(extractHeaders(response)).contains(Map.entry(name, value));
                    });
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });

            final RequestPatternBuilder requestedFor = getRequestedFor(urlPathEqualTo("/endpoint" + path));
            expectedRequestHeaders.forEach((name, value) -> requestedFor.withHeader(name, equalTo(value)));
            wiremock.verify(requestedFor);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/flows/api-flows-equals-and-starts-with.json")
    @DisplayName("Flows without condition and mixed operators")
    class NoConditionOperatorMixed extends TestPreparer {

        @ParameterizedTest
        @MethodSource(PARAMETERS_PROVIDERS_CLASS + "parametersMixedOperatorCase")
        void should_pass_through_correct_flows(
            String path,
            Map<String, String> expectedRequestHeaders,
            Map<String, String> expectedResponseHeaders,
            HttpClient client
        ) {
            wiremock.stubFor(get("/endpoint" + path).willReturn(ok(RESPONSE_FROM_BACKEND)));

            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    expectedResponseHeaders.forEach((name, value) -> {
                        assertThat(extractHeaders(response)).contains(Map.entry(name, value));
                    });
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });

            final RequestPatternBuilder requestedFor = getRequestedFor(urlPathEqualTo("/endpoint" + path));
            expectedRequestHeaders.forEach((name, value) -> requestedFor.withHeader(name, equalTo(value)));
            wiremock.verify(requestedFor);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi({ "/apis/http/flows/api-conditional-flows.json", "/apis/http/flows/api-conditional-flows-double-evaluation-case.json" })
    @DisplayName("Flows with condition")
    class ConditionalFlows extends TestPreparer {

        @ParameterizedTest
        @MethodSource(PARAMETERS_PROVIDERS_CLASS + "parametersConditionalFlowsCase")
        void should_pass_through_correct_flows(
            String path,
            String conditionalHeader,
            Map<String, String> expectedRequestHeaders,
            Map<String, String> expectedResponseHeaders,
            HttpClient client
        ) {
            wiremock.stubFor(get("/endpoint" + path).willReturn(ok(RESPONSE_FROM_BACKEND)));

            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request -> request.putHeader("X-Condition-Flow-Selection", conditionalHeader).rxSend())
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    expectedResponseHeaders.forEach((name, value) -> {
                        assertThat(extractHeaders(response)).contains(Map.entry(name, value));
                    });
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });

            final RequestPatternBuilder requestedFor = getRequestedFor(urlPathEqualTo("/endpoint" + path));
            expectedRequestHeaders.forEach((name, value) -> requestedFor.withHeader(name, equalTo(value)));
            wiremock.verify(requestedFor);
        }

        /**
         * This test case remove the header used for evaluating the condition from the request.
         * In V4 Emulation mode, it's not a problem.
         * In V3, condition is evaluated before request and before response.
         * If the header is removed, with the current condition it will end with a NullPointerException and so a 500 response.
         */
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
                    assertThat(response).hasToString(
                        "The template evaluation returns an error. Expression:\n" +
                            "{##request.headers['X-Condition-Flow-Selection'][0] == 'root-condition'} "
                    );
                    return true;
                });
        }
    }
}
