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
package io.gravitee.apim.integration.tests.plan.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.OngoingStubbing;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.gravitee.policy.jwt.alg.Signature.HMAC_HS256;
import static io.gravitee.policy.v3.jwt.resolver.KeyResolver.GIVEN_KEY;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/plan/v2-api.json")
public class PlanJwtV4EmulationIntegrationTest extends AbstractGatewayTest {

    protected static final String CLIENT_ID = "my-test-client-id";
    protected static final String JWT_SECRET;

    static {
        SecureRandom random = new SecureRandom();
        byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);
        JWT_SECRET = new String(sharedSecret);
    }

    /**
     * Override api plans to have a published KEY_LESS one.
     * @param api is the api to apply this function code
     */
    @Override
    public void configureApi(Api api) {
        Plan plan = new Plan();
        plan.setId(PLAN_ID);
        plan.setApi(api.getId());
        plan.setSecurity("JWT");
        plan.setStatus("PUBLISHED");

        JWTPolicyConfiguration configuration = new JWTPolicyConfiguration();
        configuration.setSignature(HMAC_HS256);
        configuration.setResolverParameter(JWT_SECRET);
        configuration.setPublicKeyResolver(GIVEN_KEY);
        try {
            plan.setSecurityDefinition(new ObjectMapper().writeValueAsString(configuration));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to set JWT policy configuration", e);
        }
        api.setPlans(Collections.singletonList(plan));
    }

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("jwt", PolicyBuilder.build("jwt", JWTPolicy.class, JWTPolicyConfiguration.class));
    }

    protected Stream<Arguments> provideApis() {
        return Stream.of(
                Arguments.of("v2-api", true)
        );
    }

    protected Stream<Arguments> provideWrongSecurityHeaders() {
        return provideApis().flatMap(arguments -> {
            String apiId = (String) arguments.get()[0];
            return Stream.of(
                    Arguments.of(apiId, null, null),
                    Arguments.of(apiId, "X-Gravitee-Api-Key", "an-api-key"),
                    Arguments.of(apiId, "Authorization", "Bearer a-jwt-token")
            );
        });
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    void should_return_200_success_with_jwt_and_subscription_on_the_api(final String apiId, final boolean requireWiremock, final HttpClient client) throws Exception {
        String jwtToken = getJsonWebToken(5000);
        whenSearchingSubscription(apiId, CLIENT_ID, PLAN_ID).thenReturn(Optional.of(createSubscription(apiId, false)));

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client.rxRequest(GET, getApiPath(apiId))
                .flatMap(request -> {
                    request.putHeader("Authorization", "Bearer " + jwtToken);
                    return request.rxSend();
                })
                .flatMap(
                        response -> {
                            assertThat(response.statusCode()).isEqualTo(200);
                            return response.rxBody();
                        }
                )
                .test()
                .awaitDone(60, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(
                        body -> {
                            assertThat(body.toString()).contains("endpoint response");
                            return true;
                        }
                );

        if (requireWiremock) {
            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @ParameterizedTest
    @MethodSource("provideWrongSecurityHeaders")
    void should_return_401_unauthorized_with_wrong_security(final String apiId, final String headerName, final String headerValue, final HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));

        client
                .rxRequest(GET, getApiPath(apiId))
                .flatMap(request -> {
                    if (headerName != null && headerValue != null) {
                        request.putHeader(headerName, headerValue);
                    }
                    return request.rxSend();
                })
                .flatMap(
                        response -> {
                            assertThat(response.statusCode()).isEqualTo(401);
                            return response.rxBody();
                        }
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(
                        body -> {
                            assertThat(body).hasToString("Unauthorized");
                            return true;
                        }
                );

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    void should_return_401_unauthorized_with_valid_jwt_but_no_subscription_on_the_api(final String path, final boolean requireWiremock, final HttpClient client) throws Exception {
        String jwtToken = getJsonWebToken(5000);
        assertUnauthorizedWithJwt(path, client, jwtToken, false);
    }


    @ParameterizedTest
    @MethodSource("provideApis")
    void should_return_401_unauthorized_with_expired_jwt_and_subscription_on_the_api(final String path, final boolean requireWiremock, final HttpClient client) throws Exception {
        String jwtToken = getJsonWebToken(-5000);
        assertUnauthorizedWithJwt(path, client, jwtToken, true);
    }

    private void assertUnauthorizedWithJwt(final String apiId, final HttpClient client, final String jwtToken, final boolean withSubscription) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("data")));

        if (withSubscription) {
            whenSearchingSubscription(apiId, CLIENT_ID, PLAN_ID).thenReturn(Optional.of(createSubscription(apiId, false)));
        } else {
            whenSearchingSubscription(apiId, CLIENT_ID, PLAN_ID).thenReturn(Optional.empty());
        }

        client
                .rxRequest(GET, getApiPath(apiId))
                .flatMap(request -> {
                    request.putHeader("Authorization", "Bearer " + jwtToken);
                    return request.rxSend();
                })
                .flatMap(
                        response -> {
                            assertThat(response.statusCode()).isEqualTo(401);
                            return response.rxBody();
                        }
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(
                        body -> {
                            assertThat(body).hasToString("Unauthorized");
                            return true;
                        }
                );

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }


    private String getJsonWebToken(long secondsToAdd) throws Exception {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .claim("client_id", CLIENT_ID)
                .expirationTime(Date.from(Instant.now().plusSeconds(secondsToAdd)))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(HMAC_HS256.getAlg()), jwtClaimsSet);
        signedJWT.sign(new MACSigner(JWT_SECRET));
        return signedJWT.serialize();
    }


    protected OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(String api, String clientId, String plan) {
        return when(getBean(SubscriptionService.class).getByApiAndSecurityToken(eq(api), argThat(securityToken ->
                securityToken.getTokenType().equals(SecurityToken.TokenType.CLIENT_ID.name()) && securityToken.getTokenValue().equals(clientId)
        ), eq(plan)));
    }
}
