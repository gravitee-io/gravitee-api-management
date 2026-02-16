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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductPlanDefinitionCache;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.Date;
import java.util.List;
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
class ApiProductDeployerTest {

    @Mock
    private io.gravitee.gateway.handlers.api.manager.ApiProductManager apiProductManager;

    @Mock
    private PlanService planService;

    @Mock
    private DistributedSyncService distributedSyncService;

    @Mock
    private ApiProductPlanDefinitionCache apiProductPlanDefinitionCache;

    @Mock
    private EventManager eventManager;

    @Mock
    private ApiProductSubscriptionRefresher subscriptionRefresher;

    private ApiProductDeployer cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductDeployer(
            apiProductManager,
            planService,
            distributedSyncService,
            apiProductPlanDefinitionCache,
            subscriptionRefresher
        );
        lenient()
            .when(distributedSyncService.distributeIfNeeded(any(ApiProductReactorDeployable.class)))
            .thenReturn(Completable.complete());
        lenient().when(subscriptionRefresher.refresh(anySet(), anySet())).thenReturn(Completable.complete());
        lenient().when(subscriptionRefresher.unregisterRemovedApis(anySet(), anySet(), anySet())).thenReturn(Completable.complete());
        lenient()
            .when(apiProductManager.register(any(ReactableApiProduct.class), any(Completable.class)))
            .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_api_product_successfully() {
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .version("1.0")
                .apiIds(Set.of("api-1", "api-2"))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .subscribablePlans(Set.of())
                .definitionPlans(List.of())
                .build();

            cut.deploy(deployable).test().assertComplete();
            verify(apiProductManager).register(eq(reactableApiProduct), any(Completable.class));
        }

        @Test
        void should_deploy_api_product_with_minimal_data() {
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-min")
                .name("Minimal Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-min")
                .reactableApiProduct(reactableApiProduct)
                .subscribablePlans(Set.of())
                .definitionPlans(List.of())
                .build();

            cut.deploy(deployable).test().assertComplete();
            verify(apiProductManager).register(eq(reactableApiProduct), any(Completable.class));
        }

        @Test
        void should_complete_after_deployment_and_distribute() {
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .subscribablePlans(Set.of())
                .definitionPlans(List.of())
                .build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            verify(distributedSyncService).distributeIfNeeded(deployable);
        }

        @Test
        void should_unregister_removed_apis_when_deploying_product_update() {
            ReactableApiProduct oldProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .apiIds(Set.of("api-1", "api-2"))
                .environmentId("env-1")
                .build();

            ReactableApiProduct newProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Updated Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .environmentId("env-1")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(newProduct)
                .subscribablePlans(Set.of("plan-1"))
                .definitionPlans(List.of())
                .build();

            when(apiProductManager.get("api-product-123")).thenReturn(oldProduct);
            when(apiProductManager.register(any(ReactableApiProduct.class), any(Completable.class))).thenAnswer(invocation ->
                invocation.getArgument(1)
            );

            cut.deploy(deployable).test().assertComplete();

            verify(subscriptionRefresher).unregisterRemovedApis(eq(Set.of("api-2")), eq(Set.of("plan-1")), eq(Set.of("env-1")));
            verify(subscriptionRefresher).refresh(eq(Set.of("plan-1")), eq(Set.of("env-1")));
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_api_product_successfully() {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.undeploy(deployable).test().assertComplete();
            verify(apiProductManager).unregister("api-product-123");
            verify(planService).unregister(deployable);
            verify(apiProductPlanDefinitionCache).unregister("api-product-123");
        }

        @Test
        void should_undeploy_api_product_without_reactable() {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-456")
                .reactableApiProduct(null)
                .build();

            cut.undeploy(deployable).test().assertComplete();
            verify(apiProductManager).unregister("api-product-456");
            verify(planService).unregister(deployable);
        }

        @Test
        void should_unregister_subscriptions_and_api_keys_when_undeploying_deployed_product() {
            ReactableApiProduct deployedProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .apiIds(Set.of("api-1", "api-2"))
                .environmentId("env-1")
                .build();

            when(apiProductManager.get("api-product-123")).thenReturn(deployedProduct);
            when(planService.getSubscribablePlansForApiProduct("api-product-123")).thenReturn(Set.of("plan-1"));

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.undeploy(deployable).test().assertComplete();

            verify(subscriptionRefresher).unregisterRemovedApis(eq(Set.of("api-1", "api-2")), eq(Set.of("plan-1")), eq(Set.of("env-1")));
            verify(apiProductManager).unregister("api-product-123");
            verify(planService).unregister(deployable);
        }

        @Test
        void should_complete_after_undeployment_and_distribute() {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.doAfterUndeployment(deployable).test().assertComplete();
            verify(distributedSyncService).distributeIfNeeded(deployable);
        }
    }

    @Nested
    class ErrorHandlingTest {

        @Test
        void should_handle_distribution_error_on_deploy() {
            when(distributedSyncService.distributeIfNeeded(any(ApiProductReactorDeployable.class))).thenReturn(
                Completable.error(new RuntimeException("Distribution failed"))
            );

            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .build();

            cut.doAfterDeployment(deployable).test().assertError(RuntimeException.class);
        }

        @Test
        void should_handle_distribution_error_on_undeploy() {
            when(distributedSyncService.distributeIfNeeded(any(ApiProductReactorDeployable.class))).thenReturn(
                Completable.error(new RuntimeException("Distribution failed"))
            );

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.doAfterUndeployment(deployable).test().assertError(RuntimeException.class);
        }
    }

    @Nested
    class MultipleDeploymentsTest {

        @Test
        void should_deploy_multiple_api_products_sequentially() {
            ReactableApiProduct product1 = createProduct("product-1", "Product 1");
            ReactableApiProduct product2 = createProduct("product-2", "Product 2");
            ReactableApiProduct product3 = createProduct("product-3", "Product 3");

            ApiProductReactorDeployable deployable1 = createDeployable(product1, SyncAction.DEPLOY);
            ApiProductReactorDeployable deployable2 = createDeployable(product2, SyncAction.DEPLOY);
            ApiProductReactorDeployable deployable3 = createDeployable(product3, SyncAction.DEPLOY);

            cut.deploy(deployable1).test().assertComplete();
            cut.deploy(deployable2).test().assertComplete();
            cut.deploy(deployable3).test().assertComplete();
        }

        @Test
        void should_undeploy_multiple_api_products_sequentially() {
            ApiProductReactorDeployable deployable1 = createUndeployable("product-1");
            ApiProductReactorDeployable deployable2 = createUndeployable("product-2");
            ApiProductReactorDeployable deployable3 = createUndeployable("product-3");

            cut.undeploy(deployable1).test().assertComplete();
            cut.undeploy(deployable2).test().assertComplete();
            cut.undeploy(deployable3).test().assertComplete();
        }

        private ReactableApiProduct createProduct(String id, String name) {
            return ReactableApiProduct.builder()
                .id(id)
                .name(name)
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();
        }

        private ApiProductReactorDeployable createDeployable(ReactableApiProduct product, SyncAction action) {
            return ApiProductReactorDeployable.builder()
                .syncAction(action)
                .apiProductId(product.getId())
                .reactableApiProduct(product)
                .subscribablePlans(Set.of())
                .definitionPlans(List.of())
                .build();
        }

        private ApiProductReactorDeployable createUndeployable(String apiProductId) {
            return ApiProductReactorDeployable.builder().syncAction(SyncAction.UNDEPLOY).apiProductId(apiProductId).build();
        }
    }

    @Nested
    class DeployCallbackBehaviorTest {

        @Test
        void should_invoke_registerApiProductPlans_and_subscriptionRefresher_when_callback_runs() {
            when(apiProductManager.register(any(ReactableApiProduct.class), any(Completable.class))).thenAnswer(invocation ->
                invocation.getArgument(1)
            );

            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .subscribablePlans(Set.of("plan-1"))
                .definitionPlans(List.of())
                .build();

            cut.deploy(deployable).test().assertComplete();

            verify(planService).register(deployable);
            verify(subscriptionRefresher).refresh(Set.of("plan-1"), Set.of("env-id"));
        }
    }
}
