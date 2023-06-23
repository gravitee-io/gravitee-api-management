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
package io.gravitee.apim.integration.tests.messages.flows;

import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders.headerValue;
import static io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders.responseFlowHeader;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
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
public class FlowPhaseExecutionIntegrationTest {

    public static final String PARAMETERS_PROVIDERS_CLASS =
        "io.gravitee.apim.integration.tests.http.flows.FlowPhaseExecutionParameterProviders#";

    /**
     * Inherit from this class to have the required configuration to run each tests of this class
     */
    class TestPreparer extends AbstractGatewayTest {

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.putIfAbsent(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/messages/flows/api.json")
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
            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON).rxSend())
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
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/messages/flows/api.json")
    @DisplayName("Flows without condition and operator 'EQUALS'")
    class NoConditionOperatorEquals extends TestPreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                final Api definition = (Api) api.getDefinition();
                definition
                    .getFlows()
                    .forEach(flow -> {
                        flow
                            .selectorByType(SelectorType.CHANNEL)
                            .ifPresent(selector -> ((ChannelSelector) selector).setChannelOperator(Operator.EQUALS));
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
            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON).rxSend())
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
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/messages/flows/api-flows-equals-and-starts-with.json")
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
            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON).rxSend())
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
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi(
        {
            "/apis/v4/messages/flows/api-conditional-flows.json",
            "/apis/v4/messages/flows/api-conditional-flows-double-evaluation-case.json",
        }
    )
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
            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request ->
                    request
                        .putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON)
                        .putHeader("X-Condition-Flow-Selection", conditionalHeader)
                        .rxSend()
                )
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
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                });
        }

        @Test
        void should_pass_through_correct_flow_condition_remove_header_on_request(HttpClient client) {
            client
                .rxRequest(HttpMethod.GET, "/test-double-evaluation")
                .flatMap(request ->
                    request
                        .putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON)
                        .putHeader("X-Condition-Flow-Selection", "root-condition")
                        .rxSend()
                )
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response)).contains(Map.entry(responseFlowHeader(0), headerValue("/")));
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                });
        }
    }
}
