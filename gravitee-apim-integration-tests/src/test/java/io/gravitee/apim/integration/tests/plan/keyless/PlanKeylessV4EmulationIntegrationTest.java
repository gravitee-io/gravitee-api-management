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
package io.gravitee.apim.integration.tests.plan.keyless;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/plan/v2-api.json")
public class PlanKeylessV4EmulationIntegrationTest extends AbstractGatewayTest {

    /**
     * Override api plans to have a published KEY_LESS one.
     * @param api is the api to apply this function code
     */
    @Override
    public void configureApi(Api api) {
        configurePlans(api, Set.of("KEY_LESS"));
    }

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_200_success_without_any_security(String apiId, boolean requireWiremock, HttpClient client) {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains("endpoint response");
                return true;
            });

        if (requireWiremock) {
            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    protected Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v2-api", true));
    }

    @ParameterizedTest
    @MethodSource("provideSecurityHeaders")
    protected void should_access_api_and_ignore_security(
        final String apiId,
        boolean requireWiremock,
        final String headerName,
        final String headerValue,
        HttpClient client
    ) {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader(headerName, headerValue);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.rxBody();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains("endpoint response");
                return true;
            });

        if (requireWiremock) {
            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    protected Stream<Arguments> provideSecurityHeaders() {
        return provideApis()
            .flatMap(arguments -> {
                String path = (String) arguments.get()[0];
                boolean requireWiremock = (boolean) arguments.get()[1];
                return Stream.of(
                    Arguments.of(path, requireWiremock, "X-Gravitee-Api-Key", "an-api-key"),
                    Arguments.of(path, requireWiremock, "X-Gravitee-Api-Key", ""),
                    Arguments.of(path, requireWiremock, "Authorization", ""),
                    Arguments.of(path, requireWiremock, "Authorization", "Basic 1231456789"),
                    Arguments.of(path, requireWiremock, "Authorization", "Bearer"),
                    Arguments.of(path, requireWiremock, "Authorization", "Bearer "),
                    Arguments.of(path, requireWiremock, "Authorization", "Bearer a-jwt-token")
                );
            });
    }
}
