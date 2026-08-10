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

import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.services.ApiKeyCacheService;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Scenarios running against the real subscription and api-key caches, so the assertions come from
 * the gateway's own resolution rather than from a stub. {@link ApiProductV4IntegrationTest} drives
 * its outcomes with {@code when(...)}, which cannot show that a product subscription registered
 * once is reachable from every member API.
 *
 * <p>Kept in its own file rather than as another nested class: the SDK starts one gateway per
 * annotated class, and these scenarios need their own.</p>
 *
 * <p>Covers the acceptance criteria of APIM-12491 — a product subscription governs access to every
 * member API, and APIs outside a product are untouched.</p>
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" })
class ApiProductV4RealCacheIntegrationTest extends ApiProductV4IntegrationTest.TestPreparer {

    private static final String ENV_ID = "DEFAULT";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_2_ID = "my-api-v4-2";
    private static final String API_3_ID = "my-api-v4-3";
    private static final String API_1_PATH = "/test-1";
    private static final String API_2_PATH = "/test-2";
    private static final String API_3_PATH = "/test-3";

    private static final String REAL_PRODUCT_ID = "real-cache-product";
    private static final String REAL_PLAN_ID = "real-cache-product-plan";
    private static final String REAL_KEY = "real-cache-key";
    private static final String REAL_SUBSCRIPTION_ID = "real-cache-subscription";

    @BeforeEach
    void useRealCaches() {
        useRealSubscriptionCaches();
    }

    /**
     * One subscription, taken on the product, reachable from every member API — and from none
     * of the APIs outside it.
     */
    @Test
    void should_resolve_one_product_subscription_from_every_member_api(HttpClient client) {
        deployProductWithApiKeyPlan(Set.of(API_1_ID, API_2_ID));
        registerProductSubscriptionAndKey();

        assertStatus(client, API_1_PATH, REAL_KEY, 200);
        assertStatus(client, API_2_PATH, REAL_KEY, 200);
        // API 3 is deployed but not part of the product
        assertStatus(client, API_3_PATH, REAL_KEY, 401);
    }

    /** Detaching an API stops resolution for it while the others keep working. */
    @Test
    void should_stop_resolving_for_an_api_detached_from_the_product(HttpClient client) {
        deployProductWithApiKeyPlan(Set.of(API_1_ID, API_2_ID));
        registerProductSubscriptionAndKey();
        assertStatus(client, API_1_PATH, REAL_KEY, 200);

        redeployProductWithApiKeyPlan(Set.of(API_2_ID));

        assertStatus(client, API_1_PATH, REAL_KEY, 401);
        assertStatus(client, API_2_PATH, REAL_KEY, 200);
    }

    /** Attaching an API makes the existing subscription resolve for it, with no new key. */
    @Test
    void should_start_resolving_for_an_api_attached_to_the_product(HttpClient client) {
        deployProductWithApiKeyPlan(Set.of(API_1_ID));
        registerProductSubscriptionAndKey();
        assertStatus(client, API_2_PATH, REAL_KEY, 401);

        redeployProductWithApiKeyPlan(Set.of(API_1_ID, API_2_ID));

        assertStatus(client, API_1_PATH, REAL_KEY, 200);
        assertStatus(client, API_2_PATH, REAL_KEY, 200);
    }

    /** Undeploying the product evicts the subscription for all of its APIs at once. */
    @Test
    void should_stop_resolving_everywhere_once_the_product_is_undeployed(HttpClient client) {
        deployProductWithApiKeyPlan(Set.of(API_1_ID, API_2_ID));
        registerProductSubscriptionAndKey();
        assertStatus(client, API_1_PATH, REAL_KEY, 200);

        undeployApiProduct(REAL_PRODUCT_ID);

        assertStatus(client, API_1_PATH, REAL_KEY, 401);
        assertStatus(client, API_2_PATH, REAL_KEY, 401);
    }

    /**
     * APIM-12491: an API outside any product keeps behaving exactly as before — its own plan
     * still governs access, and a product key grants nothing on it.
     */
    @Test
    void should_leave_an_api_outside_any_product_untouched(HttpClient client) {
        deployProductWithApiKeyPlan(Set.of(API_1_ID));
        registerProductSubscriptionAndKey();

        // API 3 has its own api-key plan; the product key must not open it
        assertStatus(client, API_3_PATH, REAL_KEY, 401);

        registerApiSubscriptionAndKey(API_3_ID, "own-key", "own-subscription");
        assertStatus(client, API_3_PATH, "own-key", 200);
    }

    /**
     * Routes the subscription and api-key beans to real caches for this test.
     *
     * <p>The SDK exposes both as bare Mockito mocks: every lookup answers empty and every
     * registration is swallowed, so a test can only assert what it stubbed itself. That is enough to
     * drive a scenario, but it cannot show that a subscription registered on one side is reachable
     * from the request path — which is the whole point of API Product scoping, a product
     * subscription being stored under its product and resolved through the registry.</p>
     *
     * <p>Wired with {@code doAnswer}, never {@code when(...)}: on a partial mock the latter invokes
     * the delegate while stubbing, with the nulls that argument matchers produce.</p>
     *
     * <p>Kept here rather than in the SDK: one test needs it, and AbstractGatewayTest is public API
     * for every plugin that writes integration tests.</p>
     */
    private void useRealSubscriptionCaches() {
        final ApiProductRegistry apiProductRegistry = getBean(ApiProductRegistry.class);
        final ApiKeyService apiKeyBean = getBean(ApiKeyService.class);
        final SubscriptionService subscriptionBean = getBean(SubscriptionService.class);

        final ApiKeyCacheService realApiKeys = new ApiKeyCacheService(apiProductRegistry);
        doAnswer(inv -> realApiKeys.getByApiAndKey(inv.getArgument(0), inv.getArgument(1)))
            .when(apiKeyBean)
            .getByApiAndKey(any(), any());
        doAnswer(inv -> realApiKeys.getByApiAndMd5Key(inv.getArgument(0), inv.getArgument(1)))
            .when(apiKeyBean)
            .getByApiAndMd5Key(any(), any());
        doAnswer(inv -> {
            realApiKeys.register(inv.getArgument(0));
            return null;
        })
            .when(apiKeyBean)
            .register(any());
        doAnswer(inv -> {
            realApiKeys.unregister(inv.getArgument(0));
            return null;
        })
            .when(apiKeyBean)
            .unregister(any());

        // Built on the api-key bean, not on realApiKeys, so the api-key path still goes through
        // whatever the test has stubbed on it.
        final SubscriptionCacheService realSubscriptions = new SubscriptionCacheService(
            apiKeyBean,
            getBean(SubscriptionTrustStoreLoaderManager.class),
            getBean(ApiManager.class),
            apiProductRegistry
        );
        doAnswer(inv -> realSubscriptions.getByApiAndSecurityToken(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)))
            .when(subscriptionBean)
            .getByApiAndSecurityToken(any(), any(), any());
        doAnswer(inv -> realSubscriptions.getByApiAndClientIdAndPlan(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)))
            .when(subscriptionBean)
            .getByApiAndClientIdAndPlan(any(), any(), any());
        doAnswer(inv -> realSubscriptions.getById(inv.getArgument(0)))
            .when(subscriptionBean)
            .getById(any());
        doAnswer(inv -> {
            realSubscriptions.register(inv.getArgument(0));
            return null;
        })
            .when(subscriptionBean)
            .register(any());
        doAnswer(inv -> {
            realSubscriptions.unregister(inv.getArgument(0));
            return null;
        })
            .when(subscriptionBean)
            .unregister(any());
    }

    private void deployProductWithApiKeyPlan(Set<String> apiIds) {
        // The plan must be on the product before it is deployed: deployment is what emits the
        // event that rebuilds each member API's security chain. Adding plans afterwards writes
        // straight to the registry and notifies nobody.
        ReactableApiProduct p = product(REAL_PRODUCT_ID, apiIds);
        p.setPlans(List.of(productPlan(REAL_PLAN_ID, "api-key", PlanStatus.PUBLISHED)));
        deployApiProduct(p);
    }

    private void redeployProductWithApiKeyPlan(Set<String> apiIds) {
        ReactableApiProduct p = product(REAL_PRODUCT_ID, apiIds);
        p.setPlans(List.of(productPlan(REAL_PLAN_ID, "api-key", PlanStatus.PUBLISHED)));
        redeployApiProduct(p);
    }

    /** Registers the subscription on the product itself — the scope this PR introduces. */
    private void registerProductSubscriptionAndKey() {
        Subscription subscription = new Subscription();
        subscription.setId(REAL_SUBSCRIPTION_ID);
        subscription.setApi(null);
        subscription.setApiProductId(REAL_PRODUCT_ID);
        subscription.setPlan(REAL_PLAN_ID);
        subscription.setApplication("application-id");
        subscription.setStatus("ACCEPTED");
        subscription.setEnvironmentId(ENV_ID);
        getBean(SubscriptionService.class).register(subscription);

        ApiKey apiKey = new ApiKey();
        apiKey.setId("key-" + REAL_KEY);
        apiKey.setApi(REAL_PRODUCT_ID);
        apiKey.setKey(REAL_KEY);
        apiKey.setPlan(REAL_PLAN_ID);
        apiKey.setSubscription(REAL_SUBSCRIPTION_ID);
        apiKey.setApplication("application-id");
        apiKey.setActive(true);
        getBean(ApiKeyService.class).register(apiKey);
    }

    private void registerApiSubscriptionAndKey(String apiId, String key, String subscriptionId) {
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setApi(apiId);
        subscription.setPlan(PLAN_APIKEY_ID);
        subscription.setApplication("application-id");
        subscription.setStatus("ACCEPTED");
        subscription.setEnvironmentId(ENV_ID);
        getBean(SubscriptionService.class).register(subscription);

        ApiKey apiKey = new ApiKey();
        apiKey.setId("key-" + key);
        apiKey.setApi(apiId);
        apiKey.setKey(key);
        apiKey.setPlan(PLAN_APIKEY_ID);
        apiKey.setSubscription(subscriptionId);
        apiKey.setApplication("application-id");
        apiKey.setActive(true);
        getBean(ApiKeyService.class).register(apiKey);
    }
}
