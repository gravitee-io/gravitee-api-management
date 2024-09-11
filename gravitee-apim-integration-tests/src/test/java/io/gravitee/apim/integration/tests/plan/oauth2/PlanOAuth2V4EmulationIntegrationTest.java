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
package io.gravitee.apim.integration.tests.plan.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_SUCCESS_TOKEN;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_UNAUTHORIZED_TOKEN;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_UNAUTHORIZED_TOKEN_WITHOUT_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_UNAUTHORIZED_WITH_INVALID_PAYLOAD;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_OAUTH2_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.gravitee.apim.integration.tests.plan.oauth2.MockOAuth2Resource.RESOURCE_ID;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.oauth2.Oauth2Policy;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Collections;
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
public class PlanOAuth2V4EmulationIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureResources(Map<String, ResourcePlugin> resources) {
        resources.put("mock-oauth2-resource", ResourceBuilder.build("mock-oauth2-resource", MockOAuth2Resource.class));
    }

    /**
     * Override api plans to have a published KEY_LESS one.
     * @param api is the api to apply this function code
     */
    @Override
    public void configureApi(Api api) {
        configurePlans(api, Set.of("OAUTH2"));
    }

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("oauth2", PolicyBuilder.build("oauth2", Oauth2Policy.class, OAuth2PolicyConfiguration.class));
    }

    protected Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v2-api", true));
    }

    protected Stream<Arguments> provideWrongSecurityHeaders() {
        return provideApis()
            .flatMap(arguments -> {
                String apiId = (String) arguments.get()[0];
                return Stream.of(
                    Arguments.of(apiId, null, null),
                    Arguments.of(apiId, "X-Gravitee-Api-Key", "an-api-key"),
                    Arguments.of(apiId, "Authorization", ""),
                    Arguments.of(apiId, "Authorization", "Bearer"),
                    Arguments.of(apiId, "Authorization", "Bearer "),
                    Arguments.of(apiId, "Authorization", "Bearer a-jwt-token"),
                    Arguments.of(apiId, "Authorization", "Bearer " + OAUTH2_UNAUTHORIZED_TOKEN_WITHOUT_CLIENT_ID),
                    Arguments.of(apiId, "Authorization", "Bearer " + OAUTH2_UNAUTHORIZED_WITH_INVALID_PAYLOAD),
                    Arguments.of(apiId, "Authorization", "Bearer " + OAUTH2_UNAUTHORIZED_TOKEN)
                );
            });
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_200_success_with_valid_oauth2_token_and_subscription_on_the_api(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) throws Exception {
        whenSearchingSubscription(apiId, OAUTH2_CLIENT_ID, PLAN_OAUTH2_ID)
            .thenReturn(Optional.of(createSubscription(apiId, PLAN_OAUTH2_ID, false)));

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + OAUTH2_SUCCESS_TOKEN);
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
    @MethodSource("provideWrongSecurityHeaders")
    protected void should_return_401_unauthorized_with_wrong_security(
        final String apiId,
        final String headerName,
        final String headerValue,
        final HttpClient client
    ) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                if (headerName != null && headerValue != null) {
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
    protected void should_return_401_unauthorized_with_valid_oauth2_token_but_no_subscription_on_the_api(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) throws Exception {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("data")));
        }

        whenSearchingSubscription(apiId, OAUTH2_CLIENT_ID, PLAN_OAUTH2_ID).thenReturn(Optional.empty());

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + OAUTH2_SUCCESS_TOKEN);
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
        if (requireWiremock) {
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    protected OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(String api, String clientId, String plan) {
        return when(
            getBean(SubscriptionService.class)
                .getByApiAndSecurityToken(
                    eq(api),
                    argThat(securityToken ->
                        securityToken.getTokenType().equals(SecurityToken.TokenType.CLIENT_ID.name()) &&
                        securityToken.getTokenValue().equals(clientId)
                    ),
                    eq(plan)
                )
        );
    }
}
