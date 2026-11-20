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
package io.gravitee.gateway.handlers.api.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiKeyCacheServiceTest {

    private static final String PRODUCT_ID = "my-api-product";
    private static final String MEMBER_API_ID = "my-member-api";

    private ApiKeyCacheService apiKeyService;
    private ApiProductRegistry apiProductRegistry;
    private Map<ApiKeyCacheService.CacheKey, ApiKey> cacheApiKeys;
    private Map<ApiKeyCacheService.CacheKey, ApiKey> cacheMd5ApiKeys;
    private Map<String, Set<String>> cacheApiKeysByScope;

    @BeforeEach
    public void beforeEach() throws Exception {
        apiProductRegistry = mock(ApiProductRegistry.class);
        apiKeyService = new ApiKeyCacheService(apiProductRegistry);
        cacheApiKeys = (Map<ApiKeyCacheService.CacheKey, ApiKey>) ReflectionTestUtils.getField(apiKeyService, "cacheApiKeys");
        cacheMd5ApiKeys = (Map<ApiKeyCacheService.CacheKey, ApiKey>) ReflectionTestUtils.getField(apiKeyService, "cacheMd5ApiKeys");
        cacheApiKeysByScope = (Map<String, Set<String>>) ReflectionTestUtils.getField(apiKeyService, "cacheApiKeysByScope");
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register_apiKey_when_apiKey_is_active() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            var cacheKey = apiKeyService.buildCacheKey(apiKey);
            ApiKey actual = cacheApiKeys.get(cacheKey);
            assertThat(actual).isNotNull();
            assertThat(actual).isEqualTo(apiKey);

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            ApiKey md5Actual = cacheMd5ApiKeys.get(md5CacheKey);
            assertThat(md5Actual).isNotNull();
            assertThat(md5Actual).isEqualTo(apiKey);

            Set<String> actuals = cacheApiKeysByScope.get("my-api");
            assertThat(actuals.contains("my-key")).isTrue();
        }

        @Test
        void should_not_register_apiKey_when_apiKey_is_inactive() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", false);

            apiKeyService.register(apiKey);

            var cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByScope.get("my-api")).isNull();
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister_apiKey_when_registering_inactive_apiKey() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            ApiKey apiKeyInactive = buildApiKey("my-api", "my-key", false);

            apiKeyService.register(apiKeyInactive);

            var cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByScope.get("my-api")).isNull();
        }

        @Test
        void should_do_nothing_when_unregistering_already_unregistered_apiKey() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            ApiKey apiKeyToUnregister = buildApiKey("my-api", "my-key", true);

            apiKeyService.unregister(apiKeyToUnregister);

            var cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByScope.get("my-api")).isNull();
        }

        @Test
        void should_unregister_apiKey() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.unregister(apiKey);

            var cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByScope.get("my-api")).isNull();
        }

        @Test
        void should_unregister_one_apiKey_when_many_registered_by_apis() {
            for (int i = 0; i < 5; i++) {
                ApiKey apiKey = buildApiKey("my-api", "my-key-" + i, true);
                apiKeyService.register(apiKey);
            }
            ApiKey apiKey1 = buildApiKey("my-api", "my-key-1", true);
            apiKeyService.unregister(apiKey1);

            var cacheKey = apiKeyService.buildCacheKey(apiKey1);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey1);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            Set<String> apiKeysByApi = cacheApiKeysByScope.get("my-api");
            assertThat(apiKeysByApi).isNotNull();
            assertThat(apiKeysByApi.size()).isEqualTo(4);
        }

        @Test
        void should_unregister_all_apiKeys_by_api() {
            for (int i = 0; i < 5; i++) {
                ApiKey apiKey = buildApiKey("my-api", "my-key-" + i, true);
                apiKeyService.register(apiKey);
            }
            Set<String> apiKeysByApi = cacheApiKeysByScope.get("my-api");
            assertThat(apiKeysByApi.size()).isEqualTo(5);

            apiKeyService.unregisterByApiId("my-api");

            for (int i = 0; i < 5; i++) {
                ApiKey apiKeyToUnregister = buildApiKey("my-api", "my-key-" + i, true);

                var cacheKey = apiKeyService.buildCacheKey(apiKeyToUnregister);
                assertThat(cacheApiKeys.get(cacheKey)).isNull();

                var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKeyToUnregister);
                assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();
            }
            assertThat(cacheApiKeysByScope.get("my-api")).isNull();
        }

        @Test
        void should_keep_the_active_key_when_a_revoked_key_with_another_id_shares_its_value_and_subscription() {
            ApiKey active = buildApiKey("orders", "my-key", true, "active-key", "orders-subscription");
            ApiKey revoked = buildRevokedApiKey("orders", "my-key", "revoked-key", "orders-subscription");

            apiKeyService.register(active);
            apiKeyService.register(revoked);

            assertCached(active);
        }

        @Test
        void should_keep_the_active_key_when_unregister_is_called_for_another_key_id_with_the_same_value() {
            ApiKey active = buildApiKey("orders", "my-key", true, "active-key", "orders-subscription");
            ApiKey revoked = buildRevokedApiKey("orders", "my-key", "revoked-key", "orders-subscription");

            apiKeyService.register(active);
            apiKeyService.unregister(revoked);

            assertCached(active);
        }

        @Test
        void should_keep_the_active_key_when_the_revoked_duplicate_is_loaded_first() {
            ApiKey revoked = buildRevokedApiKey("orders", "my-key", "revoked-key", "orders-subscription");
            ApiKey active = buildApiKey("orders", "my-key", true, "active-key", "orders-subscription");

            apiKeyService.register(revoked);
            apiKeyService.register(active);

            assertCached(active);
        }

        @Test
        void should_remove_the_key_when_its_record_is_inactive_through_another_subscription() {
            ApiKey accepted = buildApiKey("api-1", "shared-key", true, "key-1", "accepted-subscription");
            ApiKey closed = buildApiKey("api-1", "shared-key", false, "key-1", "closed-subscription");

            apiKeyService.register(accepted);
            apiKeyService.register(closed);

            assertAbsent(accepted);
        }

        @Test
        void should_remove_the_key_when_unregister_is_called_for_its_record_on_another_subscription() {
            ApiKey accepted = buildApiKey("api-1", "shared-key", true, "key-1", "accepted-subscription");
            ApiKey closed = buildApiKey("api-1", "shared-key", false, "key-1", "closed-subscription");

            apiKeyService.register(accepted);
            apiKeyService.unregister(closed);

            assertAbsent(accepted);
        }

        @Test
        void should_remove_the_key_when_its_record_is_revoked_through_another_subscription() {
            ApiKey accepted = buildApiKey("api-1", "shared-key", true, "key-1", "accepted-subscription");
            ApiKey revokedThroughClosed = buildRevokedApiKey("api-1", "shared-key", "key-1", "closed-subscription");

            apiKeyService.register(accepted);
            apiKeyService.register(revokedThroughClosed);

            assertAbsent(accepted);
        }

        @Test
        void should_remove_the_key_when_unregister_is_called_for_its_revoked_record_on_another_subscription() {
            ApiKey accepted = buildApiKey("api-1", "shared-key", true, "key-1", "accepted-subscription");
            ApiKey revokedThroughClosed = buildRevokedApiKey("api-1", "shared-key", "key-1", "closed-subscription");

            apiKeyService.register(accepted);
            apiKeyService.unregister(revokedThroughClosed);

            assertAbsent(accepted);
        }

        @Test
        void should_remove_the_key_when_its_record_is_paused_through_another_subscription() {
            ApiKey accepted = buildApiKey("api-1", "shared-key", true, "key-1", "accepted-subscription");
            ApiKey pausedThroughOther = buildPausedApiKey("api-1", "shared-key", "key-1", "other-subscription");

            apiKeyService.register(accepted);
            apiKeyService.register(pausedThroughOther);

            assertAbsent(accepted);
        }

        @Test
        void should_remove_the_key_when_unregister_is_called_for_its_paused_record_on_another_subscription() {
            ApiKey accepted = buildApiKey("api-1", "shared-key", true, "key-1", "accepted-subscription");
            ApiKey pausedThroughOther = buildPausedApiKey("api-1", "shared-key", "key-1", "other-subscription");

            apiKeyService.register(accepted);
            apiKeyService.unregister(pausedThroughOther);

            assertAbsent(accepted);
        }

        @Test
        void should_keep_the_active_key_when_a_paused_key_with_another_id_shares_its_value() {
            ApiKey active = buildApiKey("orders", "my-key", true, "active-key", "orders-subscription");
            ApiKey paused = buildPausedApiKey("orders", "my-key", "paused-key", "orders-subscription");

            apiKeyService.register(active);
            apiKeyService.unregister(paused);

            assertCached(active);
        }

        @Test
        void should_remove_the_key_when_the_same_record_and_subscription_are_revoked() {
            ApiKey active = buildApiKey("orders", "my-key", true, "key-1", "orders-subscription");
            ApiKey revoked = buildRevokedApiKey("orders", "my-key", "key-1", "orders-subscription");

            apiKeyService.register(active);
            apiKeyService.register(revoked);

            assertAbsent(active);
        }

        @Test
        void should_remove_the_key_when_unregister_is_called_for_the_same_record_and_subscription() {
            ApiKey active = buildApiKey("orders", "my-key", true, "key-1", "orders-subscription");
            ApiKey revoked = buildRevokedApiKey("orders", "my-key", "key-1", "orders-subscription");

            apiKeyService.register(active);
            apiKeyService.unregister(revoked);

            assertAbsent(active);
        }

        @Test
        void should_keep_a_key_on_another_api_when_the_duplicate_is_revoked() {
            ApiKey customers = buildApiKey("customers", "my-key", true, "active-key", "customers-subscription");
            ApiKey revokedOnOrders = buildRevokedApiKey("orders", "my-key", "revoked-key", "orders-subscription");

            apiKeyService.register(customers);
            apiKeyService.register(revokedOnOrders);

            assertCached(customers);
            assertAbsent(revokedOnOrders);
        }

        @Test
        void should_keep_the_active_key_on_every_api_the_revoked_duplicate_does_not_own() {
            ApiKey activeOrders = buildApiKey("orders", "my-key", true, "active-key", "orders-subscription");
            ApiKey activeCustomers = buildApiKey("customers", "my-key", true, "active-key", "customers-subscription");
            ApiKey activeProducts = buildApiKey("products", "my-key", true, "active-key", "products-subscription");
            ApiKey revokedOrders = buildRevokedApiKey("orders", "my-key", "revoked-key", "orders-subscription");
            ApiKey revokedProducts = buildRevokedApiKey("products", "my-key", "revoked-key", "products-subscription");

            apiKeyService.register(activeOrders);
            apiKeyService.register(activeCustomers);
            apiKeyService.register(activeProducts);
            apiKeyService.register(revokedOrders);
            apiKeyService.register(revokedProducts);

            assertCached(activeOrders);
            assertCached(activeCustomers);
            assertCached(activeProducts);
        }

        @Test
        void should_keep_a_product_key_when_a_revoked_duplicate_is_loaded_for_the_same_product() {
            when(apiProductRegistry.getApiProductIdsForApi(MEMBER_API_ID)).thenReturn(Set.of(PRODUCT_ID));
            ApiKey active = buildApiKey(PRODUCT_ID, "my-key", true, "active-key", "product-subscription");
            ApiKey revoked = buildRevokedApiKey(PRODUCT_ID, "my-key", "revoked-key", "product-subscription");

            apiKeyService.register(active);
            apiKeyService.register(revoked);

            assertCached(active);
            Optional<ApiKey> fromMember = apiKeyService.getByApiAndKey(MEMBER_API_ID, "my-key");
            assertThat(fromMember).isPresent();
            assertThat(fromMember.get()).isEqualTo(active);
        }

        @Test
        void should_keep_the_key_that_replaced_another_active_key_with_the_same_value() {
            ApiKey first = buildApiKey("orders", "my-key", true, "first-key", "orders-subscription");
            ApiKey second = buildApiKey("orders", "my-key", true, "second-key", "orders-subscription");

            apiKeyService.register(first);
            apiKeyService.register(second);
            apiKeyService.unregister(first);

            assertCached(second);
        }

        @Test
        void should_remove_the_replacement_when_that_record_is_revoked() {
            ApiKey first = buildApiKey("orders", "my-key", true, "first-key", "orders-subscription");
            ApiKey second = buildApiKey("orders", "my-key", true, "second-key", "orders-subscription");

            apiKeyService.register(first);
            apiKeyService.register(second);
            apiKeyService.unregister(second);

            assertAbsent(second);
        }

        @Test
        void should_keep_the_other_values_of_the_api_when_a_duplicate_is_rejected() {
            ApiKey kept = buildApiKey("orders", "other-key", true, "other-key", "other-subscription");
            ApiKey active = buildApiKey("orders", "my-key", true, "active-key", "orders-subscription");
            ApiKey revoked = buildRevokedApiKey("orders", "my-key", "revoked-key", "orders-subscription");

            apiKeyService.register(kept);
            apiKeyService.register(active);
            apiKeyService.register(revoked);

            assertCached(kept);
            assertCached(active);
            assertThat(cacheApiKeysByScope.get("orders").size()).isEqualTo(2);
        }

        @Test
        void should_remove_the_key_when_its_record_is_inactive_with_no_subscription() {
            ApiKey active = buildApiKey("orders", "my-key", true, "key-1", "orders-subscription");
            ApiKey withoutSubscription = buildApiKey("orders", "my-key", false, "key-1", null);

            apiKeyService.register(active);
            apiKeyService.unregister(withoutSubscription);

            assertAbsent(active);
        }
    }

    @Nested
    class getTest {

        @Test
        void should_get_apiKey_when_registered() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            var cacheKey = apiKeyService.buildCacheKey(apiKey);
            ApiKey actual = cacheApiKeys.get(cacheKey);
            assertThat(actual).isNotNull();
            assertThat(actual).isEqualTo(apiKey);

            var md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            ApiKey md5Actual = cacheMd5ApiKeys.get(md5CacheKey);
            assertThat(md5Actual).isNotNull();
            assertThat(md5Actual).isEqualTo(apiKey);

            Set<String> actuals = cacheApiKeysByScope.get("my-api");
            assertThat(actuals.contains("my-key")).isTrue();

            Optional<ApiKey> keyFoundOpt = apiKeyService.getByApiAndKey("my-api", "my-key");
            assertThat(keyFoundOpt).isPresent();
            ApiKey apiKeyFound = keyFoundOpt.get();
            assertThat(apiKeyFound).isEqualTo(apiKey);

            Optional<ApiKey> md5KeyFoundOpt = apiKeyService.getByApiAndMd5Key("my-api", DigestUtils.md5DigestAsHex("my-key".getBytes()));
            assertThat(md5KeyFoundOpt).isPresent();
            ApiKey md5ApiKeyFound = md5KeyFoundOpt.get();
            assertThat(md5ApiKeyFound).isEqualTo(apiKey);
        }

        @Test
        void should_not_get_apiKey_when_not_registered() {
            assertThat(cacheApiKeys.size()).isEqualTo(0);
            assertThat(cacheMd5ApiKeys.size()).isEqualTo(0);
            assertThat(cacheApiKeysByScope.size()).isEqualTo(0);

            Optional<ApiKey> keyFoundOpt = apiKeyService.getByApiAndKey("my-api", "my-key");
            assertThat(keyFoundOpt).isEmpty();

            Optional<ApiKey> md5KeyFoundOpt = apiKeyService.getByApiAndMd5Key("my-api", DigestUtils.md5DigestAsHex("my-key".getBytes()));
            assertThat(md5KeyFoundOpt).isEmpty();
        }
    }

    @Nested
    class ConcurrentUpdateTest {

        @Test
        void should_unregister_every_api_key_registered_concurrently_for_the_same_api() throws Exception {
            // Sync appenders register api keys of the same API from several threads
            // (services.sync.appender.parallelism). Every cache key must survive the concurrent
            // maintenance of cacheApiKeysByScope so unregisterByApiId() can evict them all.
            int writerCount = 8;
            int keysPerWriter = 200;
            CyclicBarrier barrier = new CyclicBarrier(writerCount);
            List<Future<?>> futures = new ArrayList<>();
            try (ExecutorService writers = Executors.newFixedThreadPool(writerCount)) {
                for (int w = 0; w < writerCount; w++) {
                    int writerId = w;
                    futures.add(
                        writers.submit(() -> {
                            barrier.await();
                            for (int i = 0; i < keysPerWriter; i++) {
                                apiKeyService.register(buildApiKey("my-api", "key-" + writerId + "-" + i, true));
                            }
                            return null;
                        })
                    );
                }
                for (Future<?> future : futures) {
                    future.get(10, TimeUnit.SECONDS);
                }
            }

            apiKeyService.unregisterByApiId("my-api");

            assertThat(cacheApiKeys.isEmpty()).isTrue();
            assertThat(cacheMd5ApiKeys.isEmpty()).isTrue();
            assertThat(cacheApiKeysByScope.isEmpty()).isTrue();
        }
    }

    @Nested
    class ApiProductScopeTest {

        @Test
        void should_reach_product_api_key_from_a_member_api() {
            when(apiProductRegistry.getApiProductIdsForApi(MEMBER_API_ID)).thenReturn(Set.of(PRODUCT_ID));
            // The sync mapper scopes an API Product subscription's key to the product
            ApiKey apiKey = buildApiKey(PRODUCT_ID, "my-key", true);
            apiKeyService.register(apiKey);

            assertThat(apiKeyService.getByApiAndKey(MEMBER_API_ID, "my-key")).contains(apiKey);
            assertThat(apiKeyService.getByApiAndMd5Key(MEMBER_API_ID, DigestUtils.md5DigestAsHex("my-key".getBytes()))).contains(apiKey);
            // Cached once, under the product
            assertThat(cacheApiKeysByScope.get(PRODUCT_ID)).isEqualTo(Set.of("my-key"));
            assertThat(cacheApiKeysByScope.get(MEMBER_API_ID)).isNull();
        }

        @Test
        void should_not_reach_product_api_key_from_an_api_outside_the_product() {
            apiKeyService.register(buildApiKey(PRODUCT_ID, "my-key", true));

            assertThat(apiKeyService.getByApiAndKey("api-outside", "my-key")).isEmpty();
        }

        @Test
        void should_keep_product_api_key_when_a_member_api_is_undeployed() {
            when(apiProductRegistry.getApiProductIdsForApi(MEMBER_API_ID)).thenReturn(Set.of(PRODUCT_ID));
            ApiKey apiKey = buildApiKey(PRODUCT_ID, "my-key", true);
            apiKeyService.register(apiKey);

            apiKeyService.unregisterByApiId(MEMBER_API_ID);

            assertThat(apiKeyService.getByApiAndKey(MEMBER_API_ID, "my-key")).contains(apiKey);
        }

        @Test
        void should_evict_product_api_keys_when_the_product_is_undeployed() {
            when(apiProductRegistry.getApiProductIdsForApi(MEMBER_API_ID)).thenReturn(Set.of(PRODUCT_ID));
            apiKeyService.register(buildApiKey(PRODUCT_ID, "my-key", true));

            apiKeyService.unregisterByApiProductId(PRODUCT_ID);

            assertThat(apiKeyService.getByApiAndKey(MEMBER_API_ID, "my-key")).isEmpty();
            assertThat(cacheApiKeys.isEmpty()).isTrue();
            assertThat(cacheMd5ApiKeys.isEmpty()).isTrue();
            assertThat(cacheApiKeysByScope.isEmpty()).isTrue();
        }

        @Test
        void should_evict_only_the_api_keys_of_the_removed_plans() {
            when(apiProductRegistry.getApiProductIdsForApi(MEMBER_API_ID)).thenReturn(Set.of(PRODUCT_ID));
            ApiKey removedPlanKey = buildApiKey(PRODUCT_ID, "removed-plan-key", true);
            removedPlanKey.setPlan("removed-plan");
            ApiKey keptPlanKey = buildApiKey(PRODUCT_ID, "kept-plan-key", true);
            keptPlanKey.setPlan("kept-plan");
            ApiKey otherProductKey = buildApiKey("other-product", "other-product-key", true);
            otherProductKey.setPlan("removed-plan");
            apiKeyService.register(removedPlanKey);
            apiKeyService.register(keptPlanKey);
            apiKeyService.register(otherProductKey);

            apiKeyService.unregisterByApiProductPlans(PRODUCT_ID, Set.of("removed-plan"));

            assertThat(apiKeyService.getByApiAndKey(MEMBER_API_ID, "removed-plan-key")).isEmpty();
            assertThat(apiKeyService.getByApiAndMd5Key(MEMBER_API_ID, DigestUtils.md5DigestAsHex("removed-plan-key".getBytes()))).isEmpty();
            assertThat(apiKeyService.getByApiAndKey(MEMBER_API_ID, "kept-plan-key")).contains(keptPlanKey);
            assertThat(cacheApiKeysByScope.get(PRODUCT_ID)).isEqualTo(Set.of("kept-plan-key"));
            assertThat(cacheApiKeysByScope.get("other-product")).isEqualTo(Set.of("other-product-key"));
        }

        @Test
        void should_drop_the_product_scope_once_its_last_plan_is_evicted() {
            ApiKey apiKey = buildApiKey(PRODUCT_ID, "my-key", true);
            apiKey.setPlan("removed-plan");
            apiKeyService.register(apiKey);

            apiKeyService.unregisterByApiProductPlans(PRODUCT_ID, Set.of("removed-plan"));

            assertThat(cacheApiKeys.isEmpty()).isTrue();
            assertThat(cacheMd5ApiKeys.isEmpty()).isTrue();
            assertThat(cacheApiKeysByScope.isEmpty()).isTrue();
        }
    }

    private ApiKey buildApiKey(String api, String key, boolean active) {
        return buildApiKey(api, key, active, null, "subscription");
    }

    private ApiKey buildApiKey(String api, String key, boolean active, String id, String subscription) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setApi(api);
        apiKey.setKey(key);
        apiKey.setActive(active);
        apiKey.setSubscription(subscription);
        apiKey.setApplication("application");
        return apiKey;
    }

    private ApiKey buildRevokedApiKey(String api, String key, String id, String subscription) {
        ApiKey apiKey = buildApiKey(api, key, false, id, subscription);
        apiKey.setRevoked(true);
        return apiKey;
    }

    private ApiKey buildPausedApiKey(String api, String key, String id, String subscription) {
        ApiKey apiKey = buildApiKey(api, key, false, id, subscription);
        apiKey.setPaused(true);
        return apiKey;
    }

    private void assertCached(ApiKey apiKey) {
        assertThat(cacheApiKeys.get(apiKeyService.buildCacheKey(apiKey))).isEqualTo(apiKey);
        assertThat(cacheMd5ApiKeys.get(apiKeyService.buildMd5CacheKey(apiKey))).isEqualTo(apiKey);
        Set<String> keys = cacheApiKeysByScope.get(apiKey.getApi());
        assertThat(keys != null && keys.contains(apiKey.getKey())).isTrue();
        assertThat(apiKeyService.getByApiAndKey(apiKey.getApi(), apiKey.getKey()).orElse(null)).isEqualTo(apiKey);
        assertThat(
            apiKeyService.getByApiAndMd5Key(apiKey.getApi(), DigestUtils.md5DigestAsHex(apiKey.getKey().getBytes())).orElse(null)
        ).isEqualTo(apiKey);
    }

    private void assertAbsent(ApiKey apiKey) {
        assertThat(cacheApiKeys.get(apiKeyService.buildCacheKey(apiKey))).isNull();
        assertThat(cacheMd5ApiKeys.get(apiKeyService.buildMd5CacheKey(apiKey))).isNull();
        Set<String> keys = cacheApiKeysByScope.get(apiKey.getApi());
        assertThat(keys == null || !keys.contains(apiKey.getKey())).isTrue();
        assertThat(apiKeyService.getByApiAndKey(apiKey.getApi(), apiKey.getKey())).isEmpty();
        assertThat(apiKeyService.getByApiAndMd5Key(apiKey.getApi(), DigestUtils.md5DigestAsHex(apiKey.getKey().getBytes()))).isEmpty();
    }
}
