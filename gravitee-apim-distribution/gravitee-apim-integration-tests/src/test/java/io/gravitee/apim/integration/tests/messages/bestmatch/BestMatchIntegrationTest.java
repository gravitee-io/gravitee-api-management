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
package io.gravitee.apim.integration.tests.messages.bestmatch;

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
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class BestMatchIntegrationTest {

    public static final String PLAN_FLOW_SELECTED = "X-Plan-Flow-Selected";
    public static final String API_FLOW_SELECTED = "X-Api-Flow-Selected";

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
    @DeployApi("/apis/v4/messages/bestmatch/api.json")
    class StartsWithOperator extends TestPreparer {

        /**
         * @returns { path used for the request, expected plan flow, expected api flow }
         */
        Stream<Arguments> provideParameters() {
            return Stream.of(
                Arguments.of("/books", "/books", "/books"),
                Arguments.of("/books", "/books", "/books"),
                Arguments.of("/books/145/chapters/12", "/books/:bookId", "/books/:bookId/chapters/:chapterId"),
                Arguments.of("/books/9999/chapters", "/books/:bookId", "/books/9999/chapters"),
                Arguments.of("/books/9999/chapters/random", "/books/:bookId", "/books/9999/chapters"),
                Arguments.of("/random", "/", null),
                Arguments.of("/books/145", "/books/:bookId", null)
            );
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        void should_use_best_matching_flow(String path, String planFlowSelected, String apiFlowSelected, HttpClient client) {
            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON).rxSend())
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    if (planFlowSelected != null) {
                        assertThat(response.getHeader(PLAN_FLOW_SELECTED)).isEqualTo(planFlowSelected);
                    }
                    if (apiFlowSelected != null) {
                        assertThat(response.getHeader(API_FLOW_SELECTED)).isEqualTo(apiFlowSelected);
                    }
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValue(response -> {
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                })
                .assertComplete();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/messages/bestmatch/api.json")
    class EqualsOperator extends TestPreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                final io.gravitee.definition.model.v4.Api definition = (Api) api.getDefinition();
                definition
                    .getFlows()
                    .stream()
                    .flatMap(flow -> flow.selectorByType(SelectorType.CHANNEL).stream())
                    .forEach(selector -> {
                        ChannelSelector channelSelector = (ChannelSelector) selector;
                        channelSelector.setChannelOperator(Operator.EQUALS);
                    });
                definition
                    .getPlans()
                    .stream()
                    .flatMap(plan -> plan.getFlows().stream())
                    .flatMap(flow -> flow.selectorByType(SelectorType.CHANNEL).stream())
                    .forEach(selector -> {
                        ChannelSelector channelSelector = (ChannelSelector) selector;
                        channelSelector.setChannelOperator(Operator.EQUALS);
                    });
            }
        }

        /**
         * @returns { path used for the request, expected plan flow, expected api flow }
         */
        Stream<Arguments> provideParameters() {
            return Stream.of(
                Arguments.of("/books", "/books", "/books"),
                Arguments.of("/books", "/books", "/books"),
                Arguments.of("/books/145/chapters/12", null, "/books/:bookId/chapters/:chapterId"),
                Arguments.of("/books/9999/chapters", null, "/books/9999/chapters"),
                Arguments.of("/books/9999/chapters/random", null, "/books/:bookId/chapters/:chapterId"),
                Arguments.of("/random", null, null),
                Arguments.of("/", "/", null),
                Arguments.of("/books/145", "/books/:bookId", null)
            );
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        void should_use_best_matching_flow(String path, String planFlowSelected, String apiFlowSelected, HttpClient client) {
            client
                .rxRequest(HttpMethod.GET, "/test" + path)
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON).rxSend())
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    if (planFlowSelected != null) {
                        assertThat(response.getHeader(PLAN_FLOW_SELECTED)).isEqualTo(planFlowSelected);
                    }
                    if (apiFlowSelected != null) {
                        assertThat(response.getHeader(API_FLOW_SELECTED)).isEqualTo(apiFlowSelected);
                    }
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValue(response -> {
                    final JsonObject jsonResponse = new JsonObject(response.toString());
                    final JsonArray items = jsonResponse.getJsonArray("items");
                    assertThat(items).hasSize(1);
                    final JsonObject message = items.getJsonObject(0);
                    assertThat(message.getString("content")).isEqualTo("Mock data");
                    return true;
                })
                .assertComplete();
        }
    }
}
