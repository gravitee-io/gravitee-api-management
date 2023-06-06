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
package io.gravitee.apim.integration.tests.http.bestmatch;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;

import java.util.Map;
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

    @Nested
    @GatewayTest
    @DeployApi("/apis/http/bestmatch/api.json")
    class StartsWithOperator extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.putIfAbsent(
                    "transform-headers",
                    PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

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
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            client
                    .rxRequest(HttpMethod.GET, "/test" + path)
                    .flatMap(HttpClientRequest::rxSend)
                    .flatMap(response -> {
                        assertThat(response.statusCode()).isEqualTo(200);
                        return response.body();
                    })
                    .test()
                    .awaitDone(10, TimeUnit.SECONDS)
                    .assertValue(Buffer.buffer("response from backend"))
                    .assertComplete();

            final RequestPatternBuilder requestedFor = getRequestedFor(urlPathEqualTo("/endpoint" + path));
            if (planFlowSelected != null) {
                requestedFor.withHeader(PLAN_FLOW_SELECTED, equalTo(planFlowSelected));
            }
            if (apiFlowSelected != null) {
                requestedFor.withHeader(API_FLOW_SELECTED, equalTo(apiFlowSelected));
            }
            wiremock.verify(requestedFor);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/http/bestmatch/api.json")
    class EqualsOperator extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.putIfAbsent(
                    "transform-headers",
                    PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isLegacyApi(definitionClass)) {
                final Api definition = (Api) api.getDefinition();
                definition.getFlows().forEach(flow -> flow.setPathOperator(new PathOperator(flow.getPath(), Operator.EQUALS)));
                definition
                        .getPlans()
                        .stream()
                        .flatMap(plan -> plan.getFlows().stream())
                        .forEach(flow -> flow.setPathOperator(new PathOperator(flow.getPath(), Operator.EQUALS)));
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
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            client
                    .rxRequest(HttpMethod.GET, "/test" + path)
                    .flatMap(HttpClientRequest::rxSend)
                    .flatMap(response -> {
                        assertThat(response.statusCode()).isEqualTo(200);
                        return response.body();
                    })
                    .test()
                    .awaitDone(10, TimeUnit.SECONDS)
                    .assertValue(Buffer.buffer("response from backend"))
                    .assertComplete();

            final RequestPatternBuilder requestedFor = getRequestedFor(urlPathEqualTo("/endpoint" + path));
            if (planFlowSelected != null) {
                requestedFor.withHeader(PLAN_FLOW_SELECTED, equalTo(planFlowSelected));
            }
            if (apiFlowSelected != null) {
                requestedFor.withHeader(API_FLOW_SELECTED, equalTo(apiFlowSelected));
            }
            wiremock.verify(requestedFor);
        }
    }
}
