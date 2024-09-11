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
package io.gravitee.apim.integration.tests.plan.apikey;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.gravitee.gateway.reactive.api.policy.SecurityToken.TokenType.API_KEY;
import static io.vertx.core.http.HttpMethod.GET;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
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
public class PlanApiKeyV4EmulationIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureApi(Api api) {
        configurePlans(api, Set.of("API_KEY"));
    }

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put(
            "api-key",
            PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
        );
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
                    Arguments.of(apiId, "X-Gravitee-Api-Key", ""),
                    Arguments.of(apiId, "Authorization", ""),
                    Arguments.of(apiId, "Authorization", "Bearer"),
                    Arguments.of(apiId, "Authorization", "Bearer "),
                    Arguments.of(apiId, "Authorization", "Bearer a-jwt-token")
                );
            });
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_200_success_with_api_key_and_subscription_on_the_api(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        final ApiKey apiKey = createApiKey(apiId, false, false);
        when(getBean(ApiKeyService.class).getByApiAndKey(any(), any())).thenReturn(Optional.of(apiKey));
        whenSearchingSubscription(apiKey).thenReturn(Optional.of(createSubscription(apiKey.getApi(), PLAN_APIKEY_ID, false)));

        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("X-Gravitee-Api-Key", apiKey.getKey());
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
    protected void should_return_401_unauthorized_with_valid_api_key_but_no_subscription_on_the_api(
        final String path,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        final ApiKey apiKey = createApiKey("another-api", false, false);
        assertUnauthorizedWithKey(path, client, apiKey, false);
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_401_unauthorized_with_expired_api_key_and_subscription_on_the_api(
        final String path,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        final ApiKey apiKey = createApiKey("my-v2-api", true, false);
        assertUnauthorizedWithKey(path, client, apiKey, true);
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    protected void should_return_401_unauthorized_with_revoked_api_key_and_subscription_on_the_api(
        final String path,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        final ApiKey apiKey = createApiKey("my-v2-api", false, true);
        assertUnauthorizedWithKey(path, client, apiKey, true);
    }

    private void assertUnauthorizedWithKey(
        final String apiId,
        final HttpClient client,
        final ApiKey apiKey,
        final boolean withSubscription
    ) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("data")));

        when(getBean(ApiKeyService.class).getByApiAndKey(any(), any())).thenReturn(Optional.of(apiKey));
        if (withSubscription) {
            whenSearchingSubscription(apiKey).thenReturn(Optional.of(createSubscription(apiKey.getApi(), PLAN_APIKEY_ID, false)));
        } else {
            whenSearchingSubscription(apiKey).thenReturn(Optional.empty());
        }

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                request.putHeader("X-Gravitee-Api-Key", apiKey.getKey());
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
     * Generate the ApiKey object that would be returned by the ApiKeyService
     * @return the ApiKey object
     */
    private ApiKey createApiKey(final String apiId, final boolean isExpired, final boolean isRevoked) {
        final ApiKey apiKey = new ApiKey();
        apiKey.setApi(apiId);
        apiKey.setApplication("application-id");
        apiKey.setSubscription("subscription-id");
        apiKey.setPlan(PLAN_APIKEY_ID);
        apiKey.setKey("apiKeyValue");
        if (isExpired) {
            apiKey.setExpireAt(new Date(Instant.now().minus(1, HOURS).toEpochMilli()));
        }
        apiKey.setRevoked(isRevoked);
        return apiKey;
    }

    private OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(ApiKey apiKey) {
        return when(
            getBean(SubscriptionService.class)
                .getByApiAndSecurityToken(
                    eq(apiKey.getApi()),
                    argThat(securityToken ->
                        securityToken.getTokenType().equals(API_KEY.name()) && securityToken.getTokenValue().equals(apiKey.getKey())
                    ),
                    eq(apiKey.getPlan())
                )
        );
    }
}
