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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductManagerImplTest {

    @Mock
    private ApiProductRegistry apiProductRegistry;

    @Mock
    private io.gravitee.node.api.license.LicenseManager licenseManager;

    private ApiProductManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new ApiProductManagerImpl(apiProductRegistry, null);
    }

    @Nested
    class RegistrationTest {

        @Test
        void should_register_new_api_product() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1", new Date());

            manager.register(apiProduct);

            assertThat(manager.get("product-1")).isNotNull();
            verify(apiProductRegistry).register(apiProduct);
        }

        @Test
        void should_register_multiple_api_products() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1", new Date());
            ReactableApiProduct apiProduct2 = createApiProduct("product-2", "env-1", new Date());

            manager.register(apiProduct1);
            manager.register(apiProduct2);

            assertThat(manager.get("product-1")).isNotNull();
            assertThat(manager.get("product-2")).isNotNull();
            assertThat(manager.getApiProducts()).hasSize(2);
        }

        @Test
        void should_update_existing_api_product_with_newer_deployed_date() {
            Date oldDate = new Date(System.currentTimeMillis() - 10000);
            Date newDate = new Date();

            ReactableApiProduct oldProduct = createApiProduct("product-1", "env-1", oldDate);
            ReactableApiProduct newProduct = createApiProduct("product-1", "env-1", newDate);
            newProduct.setName("Updated Product");

            manager.register(oldProduct);
            manager.register(newProduct);

            ReactableApiProduct retrieved = manager.get("product-1");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Updated Product");
            verify(apiProductRegistry, times(2)).register(any(ReactableApiProduct.class));
        }

        @Test
        void should_not_update_existing_api_product_with_older_deployed_date() {
            Date newDate = new Date();
            Date oldDate = new Date(System.currentTimeMillis() - 10000);

            ReactableApiProduct newProduct = createApiProduct("product-1", "env-1", newDate);
            newProduct.setName("Newer Product");

            ReactableApiProduct oldProduct = createApiProduct("product-1", "env-1", oldDate);
            oldProduct.setName("Older Product");

            manager.register(newProduct);
            manager.register(oldProduct);

            ReactableApiProduct retrieved = manager.get("product-1");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Newer Product");
            verify(apiProductRegistry, times(1)).register(any(ReactableApiProduct.class));
        }

        @Test
        void should_get_all_api_products() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1", new Date());
            ReactableApiProduct apiProduct2 = createApiProduct("product-2", "env-1", new Date());
            ReactableApiProduct apiProduct3 = createApiProduct("product-3", "env-2", new Date());

            manager.register(apiProduct1);
            manager.register(apiProduct2);
            manager.register(apiProduct3);

            Collection<ReactableApiProduct> allProducts = manager.getApiProducts();
            assertThat(allProducts).hasSize(3);
        }
    }

    @Nested
    class UnregistrationTest {

        @Test
        void should_unregister_existing_api_product() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1", new Date());
            manager.register(apiProduct);

            assertThat(manager.get("product-1")).isNotNull();

            manager.unregister("product-1");

            assertThat(manager.get("product-1")).isNull();
            verify(apiProductRegistry).remove("product-1", "env-1");
        }

        @Test
        void should_not_fail_when_unregistering_non_existent_product() {
            manager.unregister("non-existent");

            verify(apiProductRegistry, never()).remove(any(), any());
        }

        @Test
        void should_unregister_only_specified_product() {
            ReactableApiProduct apiProduct1 = createApiProduct("product-1", "env-1", new Date());
            ReactableApiProduct apiProduct2 = createApiProduct("product-2", "env-1", new Date());

            manager.register(apiProduct1);
            manager.register(apiProduct2);

            manager.unregister("product-1");

            assertThat(manager.get("product-1")).isNull();
            assertThat(manager.get("product-2")).isNotNull();
        }
    }

    @Nested
    class GetTest {

        @Test
        void should_return_null_for_non_existent_product() {
            ReactableApiProduct retrieved = manager.get("non-existent");
            assertThat(retrieved).isNull();
        }

        @Test
        void should_return_registered_product() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1", new Date());
            manager.register(apiProduct);

            ReactableApiProduct retrieved = manager.get("product-1");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo("product-1");
        }
    }

    @Nested
    class DeploymentLifecycleTest {

        @Test
        void should_deploy_then_update_then_undeploy() {
            Date deployDate = new Date();
            ReactableApiProduct initialProduct = createApiProduct("product-1", "env-1", deployDate);
            initialProduct.setVersion("1.0");

            // Deploy
            manager.register(initialProduct);
            assertThat(manager.get("product-1")).isNotNull();
            assertThat(manager.get("product-1").getVersion()).isEqualTo("1.0");

            // Update
            Date updateDate = new Date(deployDate.getTime() + 5000);
            ReactableApiProduct updatedProduct = createApiProduct("product-1", "env-1", updateDate);
            updatedProduct.setVersion("2.0");
            manager.register(updatedProduct);
            assertThat(manager.get("product-1").getVersion()).isEqualTo("2.0");

            // Undeploy
            manager.unregister("product-1");
            assertThat(manager.get("product-1")).isNull();

            verify(apiProductRegistry, times(2)).register(any(ReactableApiProduct.class));
            verify(apiProductRegistry).remove("product-1", "env-1");
        }

        @Test
        void should_handle_multiple_updates_with_same_deployment_date() {
            Date deployDate = new Date();
            ReactableApiProduct product1 = createApiProduct("product-1", "env-1", deployDate);
            product1.setVersion("1.0");

            ReactableApiProduct product2 = createApiProduct("product-1", "env-1", deployDate);
            product2.setVersion("1.1");

            manager.register(product1);
            manager.register(product2);

            // Since dates are the same, no update should occur (not before)
            assertThat(manager.get("product-1").getVersion()).isEqualTo("1.0");
            verify(apiProductRegistry, times(1)).register(any(ReactableApiProduct.class));
        }
    }

    @Nested
    class RegistryIntegrationTest {

        @Test
        void should_register_product_in_registry_on_deploy() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1", new Date());

            manager.register(apiProduct);

            verify(apiProductRegistry).register(apiProduct);
        }

        @Test
        void should_remove_product_from_registry_on_undeploy() {
            ReactableApiProduct apiProduct = createApiProduct("product-1", "env-1", new Date());

            manager.register(apiProduct);
            manager.unregister("product-1");

            verify(apiProductRegistry).remove("product-1", "env-1");
        }

        @Test
        void should_update_product_in_registry_on_update() {
            Date oldDate = new Date(System.currentTimeMillis() - 10000);
            Date newDate = new Date();

            ReactableApiProduct oldProduct = createApiProduct("product-1", "env-1", oldDate);
            ReactableApiProduct newProduct = createApiProduct("product-1", "env-1", newDate);

            manager.register(oldProduct);
            manager.register(newProduct);

            verify(apiProductRegistry, times(2)).register(any(ReactableApiProduct.class));
        }
    }

    @Nested
    class EdgeCasesTest {

        @Test
        void should_handle_null_deployed_date_gracefully() {
            ReactableApiProduct productWithoutDate = createApiProduct("product-1", "env-1", null);

            manager.register(productWithoutDate);

            assertThat(manager.get("product-1")).isNotNull();
        }

        @Test
        void should_handle_product_with_empty_api_ids() {
            ReactableApiProduct apiProduct = ReactableApiProduct.builder()
                .id("product-1")
                .name("Empty Product")
                .version("1.0")
                .apiIds(Set.of())
                .environmentId("env-1")
                .deployedAt(new Date())
                .build();

            manager.register(apiProduct);

            assertThat(manager.get("product-1")).isNotNull();
            assertThat(manager.get("product-1").getApiIds()).isEmpty();
        }

        @Test
        void should_handle_concurrent_registrations() throws InterruptedException {
            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    manager.register(createApiProduct("product-" + i, "env-1", new Date()));
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 50; i < 100; i++) {
                    manager.register(createApiProduct("product-" + i, "env-1", new Date()));
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            assertThat(manager.getApiProducts()).hasSize(100);
        }
    }

    private ReactableApiProduct createApiProduct(String id, String environmentId, Date deployedAt) {
        return ReactableApiProduct.builder()
            .id(id)
            .name("Product " + id)
            .version("1.0")
            .description("Test product")
            .apiIds(Set.of("api-1", "api-2"))
            .environmentId(environmentId)
            .environmentHrid("env-hrid")
            .organizationId("org-1")
            .organizationHrid("org-hrid")
            .deployedAt(deployedAt)
            .build();
    }
}
