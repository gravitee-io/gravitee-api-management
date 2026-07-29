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
package io.gravitee.apim.integration.tests.plan.jwt;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.JWT_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_JWT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_JWT_NESTED_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.generateJWT;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/plan/v2-api.json")
public class PlanJwtNestedV4EmulationIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureApi(Api api) {
        // Deploy both the nested-claim plan (jwt-nested) and a standard JWT plan (JWT) so that
        // the flat-claim regression test can exercise the existing behavior alongside the nested path.
        configurePlans(api, Set.of("JWT", "jwt-nested"));
    }

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("jwt", PolicyBuilder.build("jwt", JWTPolicy.class, JWTPolicyConfiguration.class));
    }

    protected Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v2-api", true));
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_200_success_with_nested_jwt_and_subscription(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) throws Exception {
        String jwtToken = generateJWT(5000, Map.of("act", Map.of("repository", JWT_CLIENT_ID)));
        whenSearchingSubscription(apiId, JWT_CLIENT_ID, PLAN_JWT_NESTED_ID).thenReturn(
            Optional.of(createSubscription(apiId, PLAN_JWT_NESTED_ID, false))
        );

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + jwtToken);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.rxBody();
            })
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
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
    @MethodSource("provideApis")
    protected void should_return_401_unauthorized_with_wrong_nested_jwt(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) throws Exception {
        String jwtToken = generateJWT(5000, Map.of("act", Map.of("repository", "wrong-client-id")));
        whenSearchingSubscription(apiId, "wrong-client-id", PLAN_JWT_NESTED_ID).thenReturn(Optional.empty());

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + jwtToken);
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

    /**
     * Partial-path guard: the nested claim key ({@code act}) is present in the JWT but the expected
     * leaf ({@code repository}) is absent. The ClaimResolver must return {@code null} cleanly and the
     * gateway must respond 401 — not fall through to a less-restrictive plan.
     */
    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_401_when_nested_claim_parent_present_but_leaf_absent(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) throws Exception {
        // JWT has "act" object but no "repository" leaf — clientIdClaim "act.repository" resolves to null.
        String jwtToken = generateJWT(5000, Map.of("act", Map.of("other_key", "some-value")));

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + jwtToken);
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

    /**
     * Flat-claim regression guard: verifies that existing behavior for a standard (non-nested)
     * {@code client_id} claim is byte-identical after introducing nested-claim support.
     * The flat plan uses the default {@code client_id} claim; no dot-notation is involved.
     *
     * <p>Note: V3 path regression is covered by unit tests in the {@code gravitee-policy-jwt}
     * plugin (APIM-13713); this test covers the V4 reactive path.
     */
    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_200_with_flat_claim_jwt_regression(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) throws Exception {
        // Standard JWT with a flat client_id — no nested path needed.
        String jwtToken = generateJWT(5000);
        whenSearchingSubscription(apiId, JWT_CLIENT_ID, PLAN_JWT_ID).thenReturn(Optional.of(createSubscription(apiId, PLAN_JWT_ID, false)));

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + jwtToken);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.rxBody();
            })
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains("endpoint response");
                return true;
            });

        if (requireWiremock) {
            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    protected OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(String api, String clientId, String plan) {
        return when(
            getBean(SubscriptionService.class).getByApiAndSecurityToken(
                eq(api),
                argThat(
                    securityToken ->
                        securityToken.getTokenType().equals(SecurityToken.TokenType.CLIENT_ID.name()) &&
                        securityToken.getTokenValue().equals(clientId)
                ),
                eq(plan)
            )
        );
    }
}
