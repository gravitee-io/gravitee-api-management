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
package io.gravitee.apim.integration.tests.plan.basicauth;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_BASIC_AUTH_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.gravitee.apim.integration.tests.plan.basicauth.BasicAuthTestPolicy.BASIC_AUTH_TOKEN_TYPE;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.vertx.rxjava3.core.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
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
public class PlanBasicAuthV4EmulationIntegrationTest extends AbstractGatewayTest {

    public static final String BASIC_AUTH_USERNAME = "testuser";
    public static final String BASIC_AUTH_PASSWORD = "testpassword";

    @Override
    public void configureApi(Api api) {
        configurePlans(api, Set.of("BASIC_AUTH"));
    }

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("basic-auth", PolicyBuilder.build("basic-auth", BasicAuthTestPolicy.class));
    }

    protected Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v2-api", true));
    }

    protected Stream<Arguments> provideWrongSecurityHeaders() {
        return provideApis().flatMap(arguments -> {
            String apiId = (String) arguments.get()[0];
            return Stream.of(
                Arguments.of(apiId, null, null),
                Arguments.of(apiId, "Authorization", "Bearer invalid-token"),
                Arguments.of(
                    apiId,
                    "Authorization",
                    "Basic " + Base64.getEncoder().encodeToString("invaliduser:invalidpass".getBytes(StandardCharsets.UTF_8))
                )
            );
        });
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_200_success_with_valid_basic_auth_credentials(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        when(getBean(SubscriptionService.class).getByApiAndSecurityToken(any(), any(), any())).thenAnswer(invocation -> {
            String api = invocation.getArgument(0, String.class);
            var securityToken = invocation.getArgument(1, io.gravitee.gateway.reactive.api.policy.SecurityToken.class);
            if (
                securityToken != null &&
                BASIC_AUTH_TOKEN_TYPE.equals(securityToken.getTokenType()) &&
                BASIC_AUTH_USERNAME.equals(securityToken.getTokenValue())
            ) {
                return Optional.of(createSubscription(api, PLAN_BASIC_AUTH_ID, false));
            }
            return Optional.empty();
        });

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        String authHeader =
            "Basic " +
            Base64.getEncoder().encodeToString((BASIC_AUTH_USERNAME + ":" + BASIC_AUTH_PASSWORD).getBytes(StandardCharsets.UTF_8));

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", authHeader);
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

    @ParameterizedTest
    @MethodSource("provideWrongSecurityHeaders")
    protected void should_return_401_unauthorized_without_authorization_header(
        final String apiId,
        final String headerName,
        final String headerValue,
        final HttpClient client
    ) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));

        when(getBean(SubscriptionService.class).getByApiAndSecurityToken(any(), any(), any())).thenReturn(Optional.empty());

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                if (headerName != null) {
                    request.putHeader(headerName, headerValue);
                }
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(401);
                return response.rxBody();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("Unauthorized");
                return true;
            });

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_401_unauthorized_with_invalid_basic_auth_credentials(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        when(getBean(SubscriptionService.class).getByApiAndSecurityToken(any(), any(), any())).thenAnswer(invocation -> {
            var securityToken = invocation.getArgument(1, io.gravitee.gateway.reactive.api.policy.SecurityToken.class);
            if (BASIC_AUTH_TOKEN_TYPE.equals(securityToken.getTokenType()) && BASIC_AUTH_USERNAME.equals(securityToken.getTokenValue())) {
                return Optional.of(createSubscription(apiId, PLAN_BASIC_AUTH_ID, false));
            }
            return Optional.empty();
        });

        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));

        String invalidAuthHeader = "Basic " + Base64.getEncoder().encodeToString("wronguser:wrongpass".getBytes(StandardCharsets.UTF_8));

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", invalidAuthHeader);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(401);
                return response.rxBody();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("Unauthorized");
                return true;
            });

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
