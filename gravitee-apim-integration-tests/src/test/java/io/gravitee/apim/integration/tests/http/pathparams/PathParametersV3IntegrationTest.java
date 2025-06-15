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
package io.gravitee.apim.integration.tests.http.pathparams;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.tests.fakes.policies.PathParamToHeaderPolicy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest(v2ExecutionMode = ExecutionMode.V3)
public class PathParametersV3IntegrationTest extends AbstractGatewayTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set("http.instances", 0);
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("path-param-to-header", PolicyBuilder.build("path-param-to-header", PathParamToHeaderPolicy.class));
    }

    @Test
    @DeployApi("/apis/http/pathparams/api-no-path-param.json")
    void should_not_add_path_param_to_headers_when_no_param(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        callUrl(httpClient, HttpMethod.GET, "/test")
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue(body -> {
                assertThat(body).hasToString("response from backend");
                return true;
            });

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @ParameterizedTest
    @DeployApi("/apis/http/pathparams/api-path-param.json")
    @MethodSource("provideParameters")
    void should_add_path_param_to_headers_when_no_param(
        String method,
        String path,
        Map<String, String> expectedHeaders,
        Set<String> excludedHeaders,
        HttpClient httpClient
    ) throws InterruptedException {
        wiremock.stubFor(request(method, urlEqualTo("/endpoint" + path)).willReturn(ok("response from backend")));

        callUrl(httpClient, HttpMethod.valueOf(method), "/test" + path)
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue(body -> {
                assertThat(body).hasToString("response from backend");
                return true;
            });

        final RequestPatternBuilder requestedFor = requestedFor(method, urlPathEqualTo("/endpoint" + path));
        expectedHeaders.forEach((key, value) -> requestedFor.withHeader(PathParamToHeaderPolicy.X_PATH_PARAM + key, equalTo(value)));
        excludedHeaders.forEach(key -> requestedFor.withoutHeader(PathParamToHeaderPolicy.X_PATH_PARAM + key));

        wiremock.verify(1, requestedFor);
    }

    private static Single<Buffer> callUrl(HttpClient httpClient, HttpMethod method, String path) {
        return httpClient
            .rxRequest(method, path)
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            });
    }

    @Test
    @DeployApi("/apis/http/pathparams/api-path-param.json")
    void should_handle_mulitple_parallel_execution_when_path_param(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(request("GET", urlEqualTo("/endpoint/products")).willReturn(ok("response from backend")));

        int nbCall = 1_000;

        Flowable
            .range(1, nbCall)
            .subscribeOn(Schedulers.io()) // Change scheduler to avoid blocking the main thread
            .flatMap(i -> callUrl(httpClient, HttpMethod.GET, "/test/products").toFlowable())
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValueCount(nbCall)
            .assertValueSequence(
                IntStream.range(0, nbCall).mapToObj(i -> Buffer.buffer("response from backend")).collect(Collectors.toList())
            );

        final RequestPatternBuilder requestedFor = requestedFor("GET", urlPathEqualTo("/endpoint/products"));
        wiremock.verify(nbCall, requestedFor);
    }

    private RequestPatternBuilder requestedFor(String method, UrlPattern urlPattern) {
        return new RequestPatternBuilder(RequestMethod.fromString(method), urlPattern);
    }

    public Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("GET", "/products", Map.of(), Set.of()),
            Arguments.of("TRACE", "/products", Map.of(), Set.of()),
            Arguments.of("GET", "/products/my-product", Map.of("productId", "my-product"), Set.of("disabledProductId")),
            Arguments.of("GET", "/products/my-product/hello", Map.of("productId", "my-product", "id", "my-product"), Set.of()),
            Arguments.of("DELETE", "/products/my-product/hello", Map.of("productId", "my-product"), Set.of("id")),
            Arguments.of("PUT", "/products/my-product/hello", Map.of("id", "my-product"), Set.of("productId")),
            Arguments.of("GET", "/products/my-product/hello/something", Map.of("productId", "my-product"), Set.of("id")),
            Arguments.of("GET", "/products/my-product/items/my-item", Map.of("productId", "my-product", "itemId", "my-item"), Set.of())
        );
    }
}
