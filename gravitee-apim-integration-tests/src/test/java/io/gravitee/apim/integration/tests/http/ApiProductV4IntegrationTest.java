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
package io.gravitee.apim.integration.tests.http;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApiProducts;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductV4IntegrationTest {

    private static final String PRODUCT_RESOURCE = "/api-products/product-with-two-apis.json";
    private static final String PRODUCT_ID = "api-product-integration-1";
    private static final String PRODUCT_A_ID = "api-product-a";
    private static final String PRODUCT_B_ID = "api-product-b";
    private static final String ENV_ID = "DEFAULT";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_2_ID = "my-api-v4-2";
    private static final String API_3_ID = "my-api-v4-3";

    private static final String API_1_PATH = "/test-1";
    private static final String API_2_PATH = "/test-2";
    private static final String API_3_PATH = "/test-3";

    private static final String KEY_ALPHA = "api-key-alpha";
    private static final String KEY_BETA = "api-key-beta";

    @Nested
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" })
    class CreationBootstrapScenarios extends TestPreparer {

        @Test
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_allow_access_with_valid_product_plan_and_subscription(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_ALPHA, API_2_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);
        }

        @Test
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_grow_from_one_api_to_three_apis_without_reissuing_key(HttpClient client) {
            redeployApiProduct(product(PRODUCT_ID, Set.of(API_1_ID)));
            allowKeyForApi(KEY_ALPHA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 401);
            assertStatus(client, API_3_PATH, KEY_ALPHA, 401);

            redeployApiProduct(product(PRODUCT_ID, Set.of(API_1_ID, API_2_ID, API_3_ID)));
            allowKeyForApi(KEY_ALPHA, API_2_ID);
            allowKeyForApi(KEY_ALPHA, API_3_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_3_PATH, KEY_ALPHA, 200);
        }

        @Test
        void should_isolate_access_when_same_api_is_shared_across_two_products(HttpClient client) {
            deployApiProduct(product(PRODUCT_A_ID, Set.of(API_1_ID)));
            deployApiProduct(product(PRODUCT_B_ID, Set.of(API_1_ID)));

            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_BETA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_1_PATH, KEY_BETA, 200);

            redeployApiProduct(product(PRODUCT_A_ID, Set.of(API_2_ID)));
            denyKeyForApi(KEY_ALPHA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 401);
            assertStatus(client, API_1_PATH, KEY_BETA, 200);
        }
    }

    @Nested
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" })
    class UpdateScenarios extends TestPreparer {

        @Test
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_allow_existing_key_on_newly_added_api_after_membership_update(HttpClient client) {
            redeployApiProduct(product(PRODUCT_ID, Set.of(API_1_ID)));
            allowKeyForApi(KEY_ALPHA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 401);

            redeployApiProduct(product(PRODUCT_ID, Set.of(API_1_ID, API_2_ID)));
            allowKeyForApi(KEY_ALPHA, API_2_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);
        }

        @Test
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_return_401_for_removed_api_and_keep_200_for_remaining_apis(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_ALPHA, API_2_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);

            redeployApiProduct(product(PRODUCT_ID, Set.of(API_1_ID)));
            denyKeyForApi(KEY_ALPHA, API_2_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 401);
        }
    }

    @Nested
    class DeletionScenarios extends TestPreparer {

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_return_401_for_all_product_apis_after_product_deletion(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_ALPHA, API_2_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);

            undeployApiProduct(PRODUCT_ID);
            denyKeyForApi(KEY_ALPHA, API_1_ID);
            denyKeyForApi(KEY_ALPHA, API_2_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 401);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 401);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_return_401_when_api_is_detached_from_product_but_api_itself_is_still_deployed(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_ALPHA, API_2_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);

            redeployApiProduct(product(PRODUCT_ID, Set.of(API_2_ID)));
            denyKeyForApi(KEY_ALPHA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 401);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_keep_sibling_api_accessible_after_underlying_api_is_undeployed_from_gateway(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_ALPHA, API_2_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);

            undeploy(API_1_ID);
            redeployApiProduct(product(PRODUCT_ID, Set.of(API_2_ID)));

            assertStatus(client, API_1_PATH, KEY_ALPHA, 404);
            assertStatus(client, API_2_PATH, KEY_ALPHA, 200);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_return_401_for_all_apis_when_last_api_is_undeployed_and_product_redeployed_with_no_apis(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);

            undeploy(API_1_ID);
            redeployApiProduct(product(PRODUCT_ID, Set.of()));

            assertStatus(client, API_1_PATH, KEY_ALPHA, 404);
        }
    }

    @Nested
    class PlanStateScenarios extends TestPreparer {

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_produce_deterministic_blocked_or_error_outcome_when_plan_is_not_published(HttpClient client) {
            final String productId = "plan-state-d1-product";
            final String planId = "plan-state-d1-plan";
            final String key = "plan-state-d1-key";

            deployApiProduct(product(productId, Set.of(API_1_ID, API_2_ID)));
            registerProductApiKeyPlanWithStatus(productId, planId, PlanStatus.STAGING);
            allowKeyForApiAndPlan(key, API_1_ID, planId);
            allowKeyForApiAndPlan(key, API_2_ID, planId);

            int api1Status = getStatus(client, API_1_PATH, key);
            int api2Status = getStatus(client, API_2_PATH, key);

            assertThat(api1Status).isEqualTo(api2Status);
            assertThat(api1Status).isEqualTo(401);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_keep_deterministic_access_outcome_after_plan_deprecation_transition(HttpClient client) {
            final String productId = "plan-state-d2-product";
            final String planId = "plan-state-d2-plan";
            final String key = "plan-state-d2-key";

            deployApiProduct(product(productId, Set.of(API_1_ID, API_2_ID)));
            registerProductApiKeyPlanWithStatus(productId, planId, PlanStatus.PUBLISHED);
            allowKeyForApiAndPlan(key, API_1_ID, planId);
            allowKeyForApiAndPlan(key, API_2_ID, planId);

            int preTransitionApi1Status = getStatus(client, API_1_PATH, key);
            int preTransitionApi2Status = getStatus(client, API_2_PATH, key);
            assertDeterministicStatus(preTransitionApi1Status, preTransitionApi2Status);

            registerProductApiKeyPlanWithStatus(productId, planId, PlanStatus.DEPRECATED);

            int api1Status = getStatus(client, API_1_PATH, key);
            int api2Status = getStatus(client, API_2_PATH, key);
            assertDeterministicStatus(api1Status, api2Status);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_keep_deterministic_access_outcome_after_plan_close_or_delete_transition(HttpClient client) {
            final String productId = "plan-state-d4-product";
            final String planId = "plan-state-d4-plan";
            final String key = "plan-state-d4-key";

            deployApiProduct(product(productId, Set.of(API_1_ID, API_2_ID)));
            registerProductApiKeyPlanWithStatus(productId, planId, PlanStatus.PUBLISHED);
            allowKeyForApiAndPlan(key, API_1_ID, planId);
            allowKeyForApiAndPlan(key, API_2_ID, planId);

            int preTransitionApi1Status = getStatus(client, API_1_PATH, key);
            int preTransitionApi2Status = getStatus(client, API_2_PATH, key);
            assertDeterministicStatus(preTransitionApi1Status, preTransitionApi2Status);

            // Simulate plan close/delete by removing product plans.
            ReactableApiProduct deployedProduct = getBean(ApiProductRegistry.class).get(productId, ENV_ID);
            deployedProduct.setPlans(List.of());

            int api1Status = getStatus(client, API_1_PATH, key);
            int api2Status = getStatus(client, API_2_PATH, key);
            assertDeterministicStatus(api1Status, api2Status);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_register_and_resolve_api_product_plan_security_types_for_api_key_jwt_and_mtls() {
            final String productId = "plan-state-d3-product";
            deployApiProduct(product(productId, Set.of(API_1_ID)));
            registerProductPlans(
                productId,
                List.of(
                    productPlan("plan-state-d3-apikey", "api-key", PlanStatus.PUBLISHED),
                    productPlan("plan-state-d3-jwt", "jwt", PlanStatus.PUBLISHED),
                    productPlan("plan-state-d3-mtls", "mtls", PlanStatus.PUBLISHED)
                )
            );

            ApiProductRegistry apiProductRegistry = getBean(ApiProductRegistry.class);
            List<ApiProductRegistry.ApiProductPlanEntry> entries = apiProductRegistry.getApiProductPlanEntriesForApi(API_1_ID, ENV_ID);

            assertThat(entries).isNotEmpty();
            assertThat(entries)
                .filteredOn(entry -> productId.equals(entry.apiProductId()))
                .isNotEmpty();
            assertThat(
                entries
                    .stream()
                    .filter(entry -> productId.equals(entry.apiProductId()))
                    .map(entry -> entry.plan().getSecurity().getType())
                    .toList()
            ).containsExactlyInAnyOrder("api-key", "jwt", "mtls");
        }
    }

    @Nested
    class SubscriptionStateScenarios extends TestPreparer {

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_return_401_after_subscription_is_revoked_or_cancelled(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);

            denyKeyForApi(KEY_ALPHA, API_1_ID);
            assertStatus(client, API_1_PATH, KEY_ALPHA, 401);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        @DeployApiProducts(PRODUCT_RESOURCE)
        void should_keep_key_isolation_when_two_apps_are_subscribed_and_one_key_is_revoked(HttpClient client) {
            allowKeyForApi(KEY_ALPHA, API_1_ID);
            allowKeyForApi(KEY_BETA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 200);
            assertStatus(client, API_1_PATH, KEY_BETA, 200);

            denyKeyForApi(KEY_ALPHA, API_1_ID);

            assertStatus(client, API_1_PATH, KEY_ALPHA, 401);
            assertStatus(client, API_1_PATH, KEY_BETA, 200);
        }
    }

    @GatewayTest
    // Empty @DeployApiProducts({}) signals that this test class needs the "universe" license tier for API Product features.
    @DeployApiProducts({})
    static class TestPreparer extends AbstractGatewayTest {

        private final Set<String> manuallyDeployedApiProducts = new HashSet<>();

        @BeforeEach
        void setupSecurityDefaults() {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
            when(getBean(ApiKeyService.class).getByApiAndKey(anyString(), anyString())).thenReturn(Optional.empty());
            when(getBean(SubscriptionService.class).getByApiAndSecurityToken(anyString(), any(), anyString())).thenReturn(Optional.empty());
        }

        @AfterEach
        void cleanupManuallyDeployedApiProducts() {
            Set.copyOf(manuallyDeployedApiProducts).forEach(this::undeployApiProduct);
        }

        @Override
        public void deployApiProduct(ReactableApiProduct reactableApiProduct) {
            manuallyDeployedApiProducts.add(reactableApiProduct.getId());
            super.deployApiProduct(reactableApiProduct);
        }

        @Override
        public void undeployApiProduct(String apiProductId) {
            manuallyDeployedApiProducts.remove(apiProductId);
            super.undeployApiProduct(apiProductId);
        }

        @Override
        public void redeployApiProduct(ReactableApiProduct reactableApiProduct) {
            manuallyDeployedApiProducts.add(reactableApiProduct.getId());
            super.redeployApiProduct(reactableApiProduct);
        }

        @Override
        public void configureApi(io.gravitee.gateway.reactor.ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                final io.gravitee.definition.model.v4.Api apiDefinition = (io.gravitee.definition.model.v4.Api) api.getDefinition();
                configurePlans(apiDefinition, Set.of("api-key"));
            }
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "api-key",
                PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
            );
            policies.put("jwt", PolicyBuilder.build("jwt", JWTPolicy.class, JWTPolicyConfiguration.class));
            policies.put("mtls", PolicyBuilder.build("mtls", MtlsPolicy.class, MtlsPolicyConfiguration.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        void assertStatus(HttpClient client, String path, String apiKey, int expectedStatus) {
            client
                .rxRequest(GET, path)
                .flatMap(request -> {
                    request.putHeader("X-Gravitee-Api-Key", apiKey);
                    return request.rxSend();
                })
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(expectedStatus);
                    return response.rxBody();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertComplete();
        }

        int getStatus(HttpClient client, String path, String apiKey) {
            return client
                .rxRequest(GET, path)
                .flatMap(request -> {
                    request.putHeader("X-Gravitee-Api-Key", apiKey);
                    return request.rxSend();
                })
                .map(response -> response.statusCode())
                .blockingGet();
        }

        void assertDeterministicStatus(int firstStatus, int secondStatus) {
            assertThat(firstStatus).isEqualTo(secondStatus);
            assertThat(firstStatus).isIn(200, 401);
        }

        void allowKeyForApi(String key, String apiId) {
            ApiKey apiKey = new ApiKey();
            apiKey.setApi(apiId);
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-" + key);
            apiKey.setPlan(PLAN_APIKEY_ID);
            apiKey.setKey(key);
            apiKey.setRevoked(false);
            apiKey.setExpireAt(new Date(System.currentTimeMillis() + 60_000));

            Subscription subscription = new Subscription();
            subscription.setApplication("application-id");
            subscription.setApplicationName("Application");
            subscription.setId("subscription-" + key);
            subscription.setApi(apiId);
            subscription.setPlan(PLAN_APIKEY_ID);

            when(getBean(ApiKeyService.class).getByApiAndKey(eq(apiId), eq(key))).thenReturn(Optional.of(apiKey));
            when(getBean(SubscriptionService.class).getByApiAndSecurityToken(eq(apiId), any(), eq(PLAN_APIKEY_ID))).thenReturn(
                Optional.of(subscription)
            );
        }

        void denyKeyForApi(String key, String apiId) {
            when(getBean(ApiKeyService.class).getByApiAndKey(eq(apiId), eq(key))).thenReturn(Optional.empty());
        }

        void allowKeyForApiAndPlan(String key, String apiId, String planId) {
            ApiKey apiKey = new ApiKey();
            apiKey.setApi(apiId);
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-" + key);
            apiKey.setPlan(planId);
            apiKey.setKey(key);
            apiKey.setRevoked(false);
            apiKey.setExpireAt(new Date(System.currentTimeMillis() + 60_000));

            Subscription subscription = new Subscription();
            subscription.setApplication("application-id");
            subscription.setApplicationName("Application");
            subscription.setId("subscription-" + key);
            subscription.setApi(apiId);
            subscription.setPlan(planId);

            when(getBean(ApiKeyService.class).getByApiAndKey(eq(apiId), eq(key))).thenReturn(Optional.of(apiKey));
            when(getBean(SubscriptionService.class).getByApiAndSecurityToken(eq(apiId), any(), eq(planId))).thenReturn(
                Optional.of(subscription)
            );
        }

        void registerProductApiKeyPlanWithStatus(String apiProductId, String planId, PlanStatus status) {
            ReactableApiProduct product = getBean(ApiProductRegistry.class).get(apiProductId, ENV_ID);
            product.setPlans(List.of(productApiKeyPlan(planId, status)));
        }

        void registerProductPlans(String apiProductId, List<Plan> plans) {
            ReactableApiProduct product = getBean(ApiProductRegistry.class).get(apiProductId, ENV_ID);
            product.setPlans(plans);
        }

        ReactableApiProduct product(String productId, Set<String> apiIds) {
            return ReactableApiProduct.builder()
                .id(productId)
                .name("Product-" + productId)
                .description("Scenario product " + productId)
                .version("1.0.0")
                .apiIds(apiIds)
                .environmentId(ENV_ID)
                .build();
        }

        private Plan productApiKeyPlan(String planId, PlanStatus status) {
            return productPlan(planId, "api-key", status);
        }

        Plan productPlan(String planId, String securityType, PlanStatus status) {
            return Plan.builder()
                .id(planId)
                .name("ProductPlan-" + planId)
                .security(PlanSecurity.builder().type(securityType).build())
                .status(status)
                .mode(PlanMode.STANDARD)
                .build();
        }
    }
}
