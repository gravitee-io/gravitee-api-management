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
package io.gravitee.gateway.handlers.api.registry.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductPlanDefinitionCache;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductRegistryImplTest {

    private ApiProductRegistryImpl registry;
    private ApiProductPlanDefinitionCache planCache;

    @BeforeEach
    void setUp() {
        planCache = mock(ApiProductPlanDefinitionCache.class);
        registry = new ApiProductRegistryImpl(planCache);
    }

    @Nested
    class RegistrationTest {

        @Test
        void should_register_api_product() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1");

            registry.register(apiProduct);

            ReactableApiProduct retrieved = registry.get("product-1", "env-1");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo("product-1");
            assertThat(retrieved.getName()).isEqualTo("Product 1");
        }

        @Test
        void should_replace_existing_api_product_on_register() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1");
            apiProduct1.setName("Original Name");

            ReactableApiProduct apiProduct2 = createApiProduct("product-1", "env-1");
            apiProduct2.setName("Updated Name");

            registry.register(apiProduct1);
            registry.register(apiProduct2);

            ReactableApiProduct retrieved = registry.get("product-1", "env-1");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Updated Name");
        }

        @Test
        void should_register_multiple_api_products_in_same_environment() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1");
            ReactableApiProduct apiProduct2 = createApiProduct("product-2", "env-1");
            ReactableApiProduct apiProduct3 = createApiProduct("product-3", "env-1");

            registry.register(apiProduct1);
            registry.register(apiProduct2);
            registry.register(apiProduct3);

            assertThat(registry.get("product-1", "env-1")).isNotNull();
            assertThat(registry.get("product-2", "env-1")).isNotNull();
            assertThat(registry.get("product-3", "env-1")).isNotNull();
        }

        @Test
        void should_register_same_product_in_different_environments() {
            ReactableApiProduct apiProductEnv1 = createApiProduct("product-1", "env-1");
            ReactableApiProduct apiProductEnv2 = createApiProduct("product-1", "env-2");

            registry.register(apiProductEnv1);
            registry.register(apiProductEnv2);

            assertThat(registry.get("product-1", "env-1")).isNotNull();
            assertThat(registry.get("product-1", "env-2")).isNotNull();
            assertThat(registry.get("product-1", "env-1")).isNotEqualTo(registry.get("product-1", "env-2"));
        }
    }

    @Nested
    class RetrievalTest {

        @Test
        void should_return_null_when_api_product_not_found() {
            ReactableApiProduct retrieved = registry.get("non-existent", "env-1");
            assertThat(retrieved).isNull();
        }

        @Test
        void should_return_null_when_environment_does_not_match() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1");
            registry.register(apiProduct);

            ReactableApiProduct retrieved = registry.get("product-1", "env-2");
            assertThat(retrieved).isNull();
        }

        @Test
        void should_get_all_api_products() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1");
            ReactableApiProduct apiProduct2 = createApiProduct("product-2", "env-1");
            ReactableApiProduct apiProduct3 = createApiProduct("product-3", "env-2");

            registry.register(apiProduct1);
            registry.register(apiProduct2);
            registry.register(apiProduct3);

            Collection<ReactableApiProduct> allProducts = registry.getAll();
            assertThat(allProducts).hasSize(3);
        }

        @Test
        void should_return_empty_collection_when_no_products() {
            Collection<ReactableApiProduct> allProducts = registry.getAll();
            assertThat(allProducts).isEmpty();
        }
    }

    @Nested
    class RemovalTest {

        @Test
        void should_remove_api_product() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1");
            registry.register(apiProduct);

            assertThat(registry.get("product-1", "env-1")).isNotNull();

            registry.remove("product-1", "env-1");

            assertThat(registry.get("product-1", "env-1")).isNull();
        }

        @Test
        void should_not_fail_when_removing_non_existent_product() {
            registry.remove("non-existent", "env-1");
            // Should not throw exception
        }

        @Test
        void should_only_remove_product_from_specified_environment() {
            ReactableApiProduct apiProductEnv1 = createApiProduct("product-1", "env-1");
            ReactableApiProduct apiProductEnv2 = createApiProduct("product-1", "env-2");

            registry.register(apiProductEnv1);
            registry.register(apiProductEnv2);

            registry.remove("product-1", "env-1");

            assertThat(registry.get("product-1", "env-1")).isNull();
            assertThat(registry.get("product-1", "env-2")).isNotNull();
        }

        @Test
        void should_remove_all_products_with_clear() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1");
            ReactableApiProduct apiProduct2 = createApiProduct("product-2", "env-1");

            registry.register(apiProduct1);
            registry.register(apiProduct2);

            registry.clear();

            assertThat(registry.getAll()).isEmpty();
        }
    }

    @Nested
    class GetProductPlanEntriesForApiTest {

        @Test
        void should_return_empty_when_api_id_null() {
            registry.register(createApiProduct("product-1", "env-1"));
            when(planCache.getByApiProductId(anyString())).thenReturn((List) List.of(createPlan("plan-1")));

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi(null, "env-1");

            assertThat(entries).isEmpty();
        }

        @Test
        void should_return_empty_when_environment_id_null() {
            registry.register(createApiProduct("product-1", "env-1"));
            when(planCache.getByApiProductId(anyString())).thenReturn((List) List.of(createPlan("plan-1")));

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", null);

            assertThat(entries).isEmpty();
        }

        @Test
        void should_return_plan_entries_for_api_in_product() {
            ReactableApiProduct product = createApiProduct("product-1", "env-1");
            product.setApiIds(Set.of("api-1", "api-2"));
            registry.register(product);
            Plan plan = createPlan("plan-1");
            when(planCache.getByApiProductId("product-1")).thenReturn((List) List.of(plan));

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", "env-1");

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).apiProductId()).isEqualTo("product-1");
            assertThat(entries.get(0).plan().getId()).isEqualTo("plan-1");
        }

        @Test
        void should_return_empty_when_product_environment_mismatch() {
            ReactableApiProduct product = createApiProduct("product-1", "env-1");
            product.setApiIds(Set.of("api-1"));
            registry.register(product);
            when(planCache.getByApiProductId(anyString())).thenReturn((List) List.of(createPlan("plan-1")));

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", "env-2");

            assertThat(entries).isEmpty();
        }

        @Test
        void should_return_empty_when_product_api_ids_null() {
            ReactableApiProduct product = createApiProduct("product-1", "env-1");
            product.setApiIds(null);
            registry.register(product);

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", "env-1");

            assertThat(entries).isEmpty();
        }

        @Test
        void should_return_empty_when_api_not_in_product() {
            ReactableApiProduct product = createApiProduct("product-1", "env-1");
            product.setApiIds(Set.of("other-api"));
            registry.register(product);

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", "env-1");

            assertThat(entries).isEmpty();
        }

        @Test
        void should_return_multiple_entries_when_multiple_plans() {
            ReactableApiProduct product = createApiProduct("product-1", "env-1");
            product.setApiIds(Set.of("api-1"));
            registry.register(product);
            when(planCache.getByApiProductId(anyString())).thenReturn((List) List.of(createPlan("plan-1"), createPlan("plan-2")));

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", "env-1");

            assertThat(entries).hasSize(2);
        }

        @Test
        void should_skip_product_when_plan_cache_returns_null() {
            ReactableApiProduct product = createApiProduct("product-1", "env-1");
            product.setApiIds(Set.of("api-1"));
            registry.register(product);
            when(planCache.getByApiProductId(anyString())).thenReturn(null);

            List<ApiProductRegistry.ApiProductPlanEntry> entries = registry.getApiProductPlanEntriesForApi("api-1", "env-1");

            assertThat(entries).isEmpty();
        }

        private Plan createPlan(String id) {
            Plan plan = new Plan();
            plan.setId(id);
            plan.setName("Plan " + id);
            return plan;
        }
    }

    @Nested
    class ConcurrencyTest {

        @Test
        void should_handle_concurrent_registrations() throws InterruptedException {
            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    registry.register(createApiProduct("product-" + i, "env-1"));
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 100; i < 200; i++) {
                    registry.register(createApiProduct("product-" + i, "env-1"));
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            assertThat(registry.getAll()).hasSize(200);
        }

        @Test
        void should_handle_concurrent_reads() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                registry.register(createApiProduct("product-" + i, "env-1"));
            }

            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    registry.get("product-5", "env-1");
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    registry.getAll();
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            assertThat(registry.getAll()).hasSize(10);
        }
    }

    private ReactableApiProduct createApiProduct(String id, String environmentId) {
        return ReactableApiProduct.builder()
            .id(id)
            .name("Product " + id.substring(id.lastIndexOf('-') + 1))
            .version("1.0")
            .apiIds(Set.of("api-1"))
            .environmentId(environmentId)
            .environmentHrid("env-hrid")
            .organizationId("org-1")
            .organizationHrid("org-hrid")
            .deployedAt(new Date())
            .build();
    }
}
