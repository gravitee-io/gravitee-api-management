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

import static io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductChange;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private EventManager eventManager;

    @Mock
    private ApiProductSubscriptionRefresher subscriptionRefresher;

    private ApiProductDeployer cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductDeployer(apiProductManager, planService, distributedSyncService, subscriptionRefresher);
        lenient()
            .when(distributedSyncService.distributeIfNeeded(any(ApiProductReactorDeployable.class)))
            .thenReturn(Completable.complete());
        lenient().when(subscriptionRefresher.refresh(anySet(), anySet())).thenReturn(Completable.complete());
        lenient().when(subscriptionRefresher.unregisterByApiProduct(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionRefresher.unregisterByApiProductPlans(any(), anySet())).thenReturn(Completable.complete());
        givenDeployedVersion(null);
    }

    /** Makes the manager hand the deployer the change of the incoming product against the given deployed version. */
    private void givenDeployedVersion(ReactableApiProduct deployed) {
        lenient()
            .when(apiProductManager.register(any(ReactableApiProduct.class), any()))
            .thenAnswer(invocation -> {
                Function<ApiProductChange, Completable> doBeforeEmit = invocation.getArgument(1);
                return doBeforeEmit.apply(ApiProductChange.between(deployed, invocation.getArgument(0), plan -> true));
            });
    }

    private static Plan plan(String id) {
        return Plan.builder().id(id).security(PlanSecurity.builder().type("api-key").build()).status(PlanStatus.PUBLISHED).build();
    }

    private static ApiProductReactorDeployable deployableOf(ReactableApiProduct product) {
        return ApiProductReactorDeployable.builder()
            .syncAction(SyncAction.DEPLOY)
            .apiProductId(product.getId())
            .reactableApiProduct(product)
            .subscribablePlans(product.getPlans().stream().map(Plan::getId).collect(Collectors.toSet()))
            .build();
    }

    private static ReactableApiProduct productWithPlans(Date deployedAt, Plan... plans) {
        return ReactableApiProduct.builder()
            .id("api-product-123")
            .name("Product")
            .apiIds(Set.of("api-1"))
            .environmentId("env-1")
            .deployedAt(deployedAt)
            .plans(List.of(plans))
            .build();
    }

    @Nested
    class ChangeAwareDeployTest {

        private final ReactableApiProduct deployed = productWithPlans(new Date(1_000), plan("plan-1"), plan("plan-2"));

        @BeforeEach
        void givenDeployed() {
            givenDeployedVersion(deployed);
        }

        @Test
        void should_reload_no_subscription_when_only_a_plan_flow_changes() {
            Plan withFlow = plan("plan-1");
            withFlow.setFlows(List.of(Flow.builder().name("flow").build()));
            ReactableApiProduct incoming = productWithPlans(new Date(2_000), withFlow, plan("plan-2"));

            cut.deploy(deployableOf(incoming)).test().assertComplete();

            verify(subscriptionRefresher, never()).refresh(argThat(plans -> !plans.isEmpty()), anySet());
            verify(subscriptionRefresher, never()).unregisterByApiProductPlans(any(), argThat(plans -> !plans.isEmpty()));
        }

        @Test
        void should_load_only_the_added_plan_then_evict_the_removed_one() {
            ReactableApiProduct incoming = productWithPlans(new Date(2_000), plan("plan-1"), plan("plan-3"));
            ApiProductReactorDeployable deployable = deployableOf(incoming);
            List<String> executed = new ArrayList<>();
            doAnswer(invocation -> {
                executed.add("register plans");
                return null;
            })
                .when(planService)
                .register(deployable);
            when(subscriptionRefresher.unregisterByApiProductPlans("api-product-123", Set.of("plan-2"))).thenReturn(
                Completable.fromRunnable(() -> executed.add("evict plan-2"))
            );
            when(subscriptionRefresher.refresh(Set.of("plan-3"), Set.of("env-1"))).thenReturn(
                Completable.fromRunnable(() -> executed.add("load plan-3"))
            );

            cut.deploy(deployable).test().assertComplete();

            assertThat(executed).containsExactly("register plans", "load plan-3", "evict plan-2");
        }

        @Test
        void should_keep_the_previous_plans_registered_and_evict_only_the_plan_it_was_loading_when_the_load_fails() {
            PlanService realPlanService = new PlanService();
            realPlanService.register(deployableOf(deployed));
            ApiProductDeployer deployer = new ApiProductDeployer(
                apiProductManager,
                realPlanService,
                distributedSyncService,
                subscriptionRefresher
            );
            when(subscriptionRefresher.refresh(Set.of("plan-3"), Set.of("env-1"))).thenReturn(
                Completable.error(new IllegalStateException("repository down"))
            );
            when(subscriptionRefresher.unregisterByApiProductPlans("api-product-123", Set.of("plan-3"))).thenReturn(Completable.complete());

            deployer
                .deploy(deployableOf(productWithPlans(new Date(2_000), plan("plan-1"), plan("plan-3"))))
                .test()
                .assertError(SyncException.class);

            assertThat(realPlanService.isDeployed("api-product-123", "plan-2", API_PRODUCT)).isTrue();
            assertThat(realPlanService.isDeployed("api-product-123", "plan-3", API_PRODUCT)).isFalse();
            verify(subscriptionRefresher).unregisterByApiProductPlans("api-product-123", Set.of("plan-3"));
            verify(subscriptionRefresher, never()).unregisterByApiProductPlans(any(), argThat(plans -> plans.contains("plan-2")));
        }

        @Test
        void should_fail_with_the_load_error_when_evicting_the_partial_load_fails_too() {
            when(subscriptionRefresher.refresh(Set.of("plan-3"), Set.of("env-1"))).thenReturn(
                Completable.error(new IllegalStateException("repository down"))
            );
            when(subscriptionRefresher.unregisterByApiProductPlans("api-product-123", Set.of("plan-3"))).thenReturn(
                Completable.error(new IllegalStateException("eviction failed"))
            );

            cut
                .deploy(deployableOf(productWithPlans(new Date(2_000), plan("plan-1"), plan("plan-3"))))
                .test()
                .assertError(e -> e instanceof SyncException && e.getCause().getMessage().equals("repository down"));
        }
    }

    @Test
    void should_unregister_the_plans_of_a_first_deployment_whose_load_fails() {
        PlanService realPlanService = new PlanService();
        ApiProductDeployer deployer = new ApiProductDeployer(
            apiProductManager,
            realPlanService,
            distributedSyncService,
            subscriptionRefresher
        );
        when(subscriptionRefresher.refresh(anySet(), anySet())).thenReturn(Completable.error(new IllegalStateException("repository down")));

        deployer.deploy(deployableOf(productWithPlans(new Date(1_000), plan("plan-1")))).test().assertError(SyncException.class);

        assertThat(realPlanService.isDeployed("api-product-123", "plan-1", API_PRODUCT)).isFalse();
        verify(subscriptionRefresher).unregisterByApiProductPlans("api-product-123", Set.of("plan-1"));
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
                .build();

            cut.deploy(deployable).test().assertComplete();
            verify(apiProductManager).register(eq(reactableApiProduct), any());
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
                .build();

            cut.deploy(deployable).test().assertComplete();
            verify(apiProductManager).register(eq(reactableApiProduct), any());
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
                .build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            verify(distributedSyncService).distributeIfNeeded(deployable);
        }

        @Test
        void should_only_refresh_subscriptions_when_deploying_a_product_update() {
            // A member API removed from the product needs no eviction: the subscription stays cached
            // under the product and the registry simply stops resolving that API to it.
            ReactableApiProduct newProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Updated Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .plans(List.of(plan("plan-1")))
                .environmentId("env-1")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(newProduct)
                .subscribablePlans(Set.of("plan-1"))
                .build();

            cut.deploy(deployable).test().assertComplete();

            verify(subscriptionRefresher).refresh(eq(Set.of("plan-1")), eq(Set.of("env-1")));
            verify(subscriptionRefresher, never()).unregisterByApiProduct(any());
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
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.undeploy(deployable).test().assertComplete();

            verify(subscriptionRefresher).unregisterByApiProduct("api-product-123");
            verify(apiProductManager).unregister("api-product-123");
            verify(planService).unregister(deployable);
        }

        /**
         * The refresher translates an Error from the trust store rebuild into a SyncException, which is a
         * RuntimeException, so {@code blockingAwait()} rethrows it as is and this method stops on it. The
         * product must not be marked gone, nor its plans unregistered, while its subscription certificates
         * are still loaded. The SyncException is rethrown untouched rather than wrapped a second time.
         */
        @Test
        void should_stop_undeploying_when_evicting_the_subscriptions_fails() {
            var failure = new SyncException("Failed to unregister subscriptions and API keys of API Product [api-product-123]");
            when(subscriptionRefresher.unregisterByApiProduct("api-product-123")).thenReturn(Completable.error(failure));
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.UNDEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.undeploy(deployable).test().assertError(failure);

            verify(apiProductManager, never()).unregister(any());
            verify(planService, never()).unregister(any(ApiProductReactorDeployable.class));
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
    class DistributionTest {

        @Test
        void should_distribute_deploy_when_deploy_tags_do_not_match_gateway() {
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .tags(Set.of("internal"))
                .environmentId("env-id")
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            verify(distributedSyncService).distributeIfNeeded(deployable);
        }

        @Test
        void should_distribute_deploy_when_product_tags_match_gateway() {
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .tags(Set.of("internal"))
                .environmentId("env-id")
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            verify(distributedSyncService).distributeIfNeeded(deployable);
        }

        @Test
        void should_distribute_deploy_without_reactable_api_product() {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            verify(distributedSyncService).distributeIfNeeded(deployable);
        }

        @Test
        void should_distribute_undeploy_even_when_product_tags_do_not_match_gateway() {
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
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("api-product-123")
                .name("Test Product")
                .version("1.0")
                .apiIds(Set.of("api-1"))
                .plans(List.of(plan("plan-1")))
                .environmentId("env-id")
                .deployedAt(new Date())
                .build();

            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .syncAction(SyncAction.DEPLOY)
                .apiProductId("api-product-123")
                .reactableApiProduct(reactableApiProduct)
                .subscribablePlans(Set.of("plan-1"))
                .build();

            cut.deploy(deployable).test().assertComplete();

            verify(planService).register(deployable);
            verify(subscriptionRefresher).refresh(Set.of("plan-1"), Set.of("env-id"));
        }
    }
}
