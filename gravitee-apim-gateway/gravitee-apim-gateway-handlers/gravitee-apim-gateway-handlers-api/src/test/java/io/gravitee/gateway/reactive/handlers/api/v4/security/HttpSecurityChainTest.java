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
package io.gravitee.gateway.reactive.handlers.api.v4.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlanContext;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntime;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class HttpSecurityChainTest {

    private static final String API_ID = "api-1";
    private static final String ENV_ID = "env-1";

    private final List<String> tried = new CopyOnWriteArrayList<>();
    private final Map<String, Object> internalAttributes = new HashMap<>();
    private final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
    private final PolicyManager policyManager = mock(PolicyManager.class);
    private final Map<String, List<HttpSecurityPlan>> securityPlansByProduct = new HashMap<>();
    private final TestScheduler scheduler = new TestScheduler();
    private final Map<String, LifecycleComponent<?>> componentByProduct = new HashMap<>();
    private final ApiProductRuntimeFactory runtimeFactory = mock(ApiProductRuntimeFactory.class);
    private final ApiProductRuntimeRegistry registry = new ApiProductRuntimeRegistry(runtimeFactory, 10_000, scheduler);

    @BeforeEach
    void setUp() {
        lenient()
            .when(runtimeFactory.create(any(), any()))
            .thenAnswer(invocation -> {
                ReactableApiProduct product = invocation.getArgument(0);
                return new ApiProductRuntime(
                    product,
                    null,
                    null,
                    null,
                    List.of(componentByProduct.computeIfAbsent(product.getId(), id -> mock(LifecycleComponent.class))),
                    () -> securityPlansByProduct.getOrDefault(product.getId(), List.of())
                );
            });
        Configuration configuration = mock(Configuration.class);
        lenient().when(configuration.getProperty("api.security.verbose401", Boolean.class, false)).thenReturn(false);
        lenient().when(ctx.getComponent(Configuration.class)).thenReturn(configuration);
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        lenient().when(ctx.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.error(new RuntimeException("401")));
        lenient()
            .doAnswer(invocation -> internalAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(ctx)
            .setInternalAttribute(anyString(), any());
        lenient()
            .doAnswer(invocation -> internalAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(ctx)
            .putInternalAttribute(anyString(), any());
        lenient()
            .doAnswer(invocation -> internalAttributes.remove(invocation.<String>getArgument(0)))
            .when(ctx)
            .removeInternalAttribute(anyString());
        lenient()
            .when(ctx.getInternalAttribute(anyString()))
            .thenAnswer(invocation -> internalAttributes.get(invocation.<String>getArgument(0)));
    }

    @Test
    void should_try_product_plans_first_sorted_across_products_then_api_plans() {
        securityPlansByProduct.put("product-a", List.of(productPlan("product-a", "a-2", 2, false)));
        securityPlansByProduct.put(
            "product-b",
            List.of(productPlan("product-b", "b-3", 3, false), productPlan("product-b", "b-1", 1, false))
        );
        registry.deploy(product("product-a"));
        registry.deploy(product("product-b"));
        HttpSecurityChain cut = new HttpSecurityChain(apiWithPlan("api-0", 0), policyManager, ExecutionPhase.REQUEST, registry, ENV_ID);

        cut.execute(ctx).test().assertError(RuntimeException.class);

        assertThat(tried).containsExactly("b-1", "a-2", "b-3", "api-0");
    }

    @Test
    void should_keep_the_runtime_of_the_matched_product_plan_pinned_and_release_the_others() throws Exception {
        securityPlansByProduct.put("product-a", List.of(productPlan("product-a", "a-1", 1, true)));
        securityPlansByProduct.put("product-b", List.of(productPlan("product-b", "b-2", 2, false)));
        registry.deploy(product("product-a"));
        registry.deploy(product("product-b"));
        LifecycleComponent<?> firstA = componentByProduct.remove("product-a");
        LifecycleComponent<?> firstB = componentByProduct.remove("product-b");
        HttpSecurityChain cut = new HttpSecurityChain(apiWithPlan("api-0", 0), policyManager, ExecutionPhase.REQUEST, registry, ENV_ID);

        cut.execute(ctx).test().assertComplete();
        registry.deploy(product("product-a"));
        registry.deploy(product("product-b"));
        scheduler.triggerActions();

        ApiProductRuntime pinned = (ApiProductRuntime) internalAttributes.get(ApiProductRuntime.ATTR_INTERNAL_API_PRODUCT_RUNTIME);
        assertThat(pinned.product().getId()).isEqualTo("product-a");
        verify(firstB).stop();
        verify(firstA, never()).stop();
        pinned.release();
    }

    @Test
    void should_read_the_product_plans_of_the_current_runtime_on_each_request() {
        securityPlansByProduct.put("product-a", List.of(productPlan("product-a", "a-1", 1, false)));
        registry.deploy(product("product-a"));
        HttpSecurityChain cut = new HttpSecurityChain(apiWithPlan("api-0", 0), policyManager, ExecutionPhase.REQUEST, registry, ENV_ID);
        cut.execute(ctx).test();

        securityPlansByProduct.put("product-a", List.of(productPlan("product-a", "a-new", 1, false)));
        registry.deploy(product("product-a"));
        tried.clear();
        cut.execute(ctx).test();

        assertThat(tried).containsExactly("a-new", "api-0");
    }

    @Test
    void should_release_every_pinned_runtime_when_the_request_is_cancelled_during_the_chain() throws Exception {
        HttpSecurityPolicy waitingPolicy = securityPolicy("a-1", 1, false);
        when(waitingPolicy.extractSecurityToken(any())).thenReturn(Maybe.never());
        securityPlansByProduct.put(
            "product-a",
            List.of(new HttpSecurityPlan(new SecurityPlanContext("a-1", "a-1", null, "product-a"), waitingPolicy))
        );
        registry.deploy(product("product-a"));
        LifecycleComponent<?> firstA = componentByProduct.remove("product-a");
        HttpSecurityChain cut = new HttpSecurityChain(apiWithPlan("api-0", 0), policyManager, ExecutionPhase.REQUEST, registry, ENV_ID);

        cut.execute(ctx).test().dispose();
        registry.deploy(product("product-a"));
        scheduler.triggerActions();

        verify(firstA).stop();
        assertThat(internalAttributes).doesNotContainKey(ApiProductRuntime.ATTR_INTERNAL_API_PRODUCT_RUNTIME);
    }

    @Test
    void should_keep_the_accepting_runtime_held_when_the_completed_chain_is_disposed_late() throws Exception {
        securityPlansByProduct.put("product-a", List.of(productPlan("product-a", "a-1", 1, true)));
        registry.deploy(product("product-a"));
        LifecycleComponent<?> firstA = componentByProduct.remove("product-a");
        HttpSecurityChain cut = new HttpSecurityChain(apiWithPlan("api-0", 0), policyManager, ExecutionPhase.REQUEST, registry, ENV_ID);

        TestObserver<Void> observer = cut.execute(ctx).test().assertComplete();
        observer.dispose();
        registry.deploy(product("product-a"));
        scheduler.triggerActions();

        verify(firstA, never()).stop();
        ApiProductRuntime kept = (ApiProductRuntime) internalAttributes.get(ApiProductRuntime.ATTR_INTERNAL_API_PRODUCT_RUNTIME);
        kept.release();
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        verify(firstA).stop();
    }

    @Test
    void should_try_only_the_api_plans_without_product_support() {
        HttpSecurityChain cut = new HttpSecurityChain(apiWithPlan("api-0", 0), policyManager, ExecutionPhase.REQUEST, null, ENV_ID);

        cut.execute(ctx).test().assertError(RuntimeException.class);

        assertThat(tried).containsExactly("api-0");
    }

    private HttpSecurityPlan productPlan(String productId, String planId, int order, boolean accepts) {
        return new HttpSecurityPlan(new SecurityPlanContext(planId, planId, null, productId), securityPolicy(planId, order, accepts));
    }

    private HttpSecurityPolicy securityPolicy(String name, int order, boolean accepts) {
        HttpSecurityPolicy policy = mock(HttpSecurityPolicy.class);
        lenient().when(policy.id()).thenReturn(name);
        lenient().when(policy.order()).thenReturn(order);
        lenient().when(policy.requireSubscription(any())).thenReturn(false);
        lenient().when(policy.wwwAuthenticate(any())).thenReturn(Single.just(false));
        lenient().when(policy.onWellKnown(any())).thenReturn(Single.just(false));
        lenient().when(policy.onRequest(any())).thenReturn(Completable.complete());
        lenient()
            .when(policy.extractSecurityToken(any()))
            .thenAnswer(invocation -> {
                tried.add(name);
                return accepts ? Maybe.just(SecurityToken.forApiKey("key")) : Maybe.empty();
            });
        return policy;
    }

    private Api apiWithPlan(String planId, int order) {
        HttpSecurityPolicy policy = securityPolicy(planId, order, false);
        when(policyManager.create(any(), any())).thenReturn(policy);
        Api api = new Api();
        api.setId(API_ID);
        api.setType(ApiType.PROXY);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setPlans(
            List.of(
                Plan.builder()
                    .id(planId)
                    .name(planId)
                    .mode(PlanMode.STANDARD)
                    .status(PlanStatus.PUBLISHED)
                    .security(PlanSecurity.builder().type("api-key").build())
                    .build()
            )
        );
        return api;
    }

    private static ReactableApiProduct product(String productId) {
        return ReactableApiProduct.builder().id(productId).environmentId(ENV_ID).apiIds(Set.of(API_ID)).build();
    }
}
