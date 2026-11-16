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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

class ApiProductRuntimeFactoryTest {

    private final FlowResolverFactory flowResolverFactory = mock(FlowResolverFactory.class);
    private final GatewayConfiguration gatewayConfiguration = mock(GatewayConfiguration.class);

    @SuppressWarnings("unchecked")
    private final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager = mock(ConfigurablePluginManager.class);

    private final ApiProductResourceManagerFactory resourceManagerFactory = mock(ApiProductResourceManagerFactory.class);

    private final LicenseManager licenseManager = mock(LicenseManager.class);

    private final ApiProductRuntimeFactory cut = new ApiProductRuntimeFactory(
        mock(DefaultClassLoader.class),
        mock(PolicyFactoryManager.class),
        policyPluginManager,
        mock(PolicyClassLoaderFactory.class),
        mock(ComponentProvider.class),
        flowResolverFactory,
        gatewayConfiguration,
        resourceManagerFactory,
        licenseManager
    );

    @BeforeEach
    void stubResolverFactory() {
        when(flowResolverFactory.forApiProductPlan(any(), anyMap())).thenReturn(mock(FlowResolver.class));
        lenient()
            .when(gatewayConfiguration.hasMatchingTags(any()))
            .thenAnswer(invocation -> !Set.of("external").equals(invocation.getArgument(0)));
    }

    @Nested
    class PlansServedByThisGateway {

        @Test
        @SuppressWarnings("unchecked")
        void should_leave_out_the_flows_of_a_plan_this_gateway_does_not_serve() {
            Flow servedFlow = Flow.builder().name("served").build();
            Plan served = publishedPlan("plan-served").flows(List.of(servedFlow)).build();
            Plan otherShard = publishedPlan("plan-other-shard").tags(Set.of("external")).flows(List.of(Flow.builder().build())).build();
            Plan staging = Plan.builder().id("plan-staging").status(PlanStatus.STAGING).flows(List.of(Flow.builder().build())).build();
            ArgumentCaptor<Map<String, List<Flow>>> flowsByPlanId = ArgumentCaptor.forClass(Map.class);

            cut.create(product(null, List.of(served, otherShard, staging)), null);

            verify(flowResolverFactory, atLeastOnce()).forApiProductPlan(isNull(), flowsByPlanId.capture());
            assertThat(flowsByPlanId.getValue()).containsOnlyKeys("plan-served");
        }

        @Test
        void should_start_without_the_policies_of_a_plan_this_gateway_does_not_serve() {
            Plan otherShard = publishedPlan("plan-other-shard")
                .tags(Set.of("external"))
                .flows(List.of(flowUsing("not-installed-here")))
                .build();

            ApiProductRuntime runtime = cut.create(product(null, List.of(otherShard)), null);

            assertThatCode(runtime::start).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_to_start_without_a_policy_of_a_plan_this_gateway_serves() {
            Plan served = publishedPlan("plan-served").flows(List.of(flowUsing("not-installed-here"))).build();

            ApiProductRuntime runtime = cut.create(product(null, List.of(served)), null);

            assertThatThrownBy(runtime::start).hasMessageContaining("not-installed-here");
        }

        private Flow flowUsing(String policy) {
            return Flow.builder().enabled(true).request(List.of(Step.builder().enabled(true).policy(policy).build())).build();
        }
    }

    @Nested
    class SecurityPlans {

        @Test
        void should_refuse_to_start_without_the_security_policy_of_a_plan_this_gateway_serves() {
            Plan served = publishedPlan("plan-served").mode(PlanMode.STANDARD).security(security("not-installed-security")).build();

            ApiProductRuntime runtime = cut.create(product(null, List.of(served)), null);

            assertThatThrownBy(runtime::start).hasMessageContaining("not-installed-security");
        }

        @Test
        void should_refuse_to_start_when_the_security_of_a_plan_cannot_be_built() {
            Plan withoutSecurityType = publishedPlan("plan-broken").name("Broken").mode(PlanMode.STANDARD).security(security(null)).build();

            ApiProductRuntime runtime = cut.create(product(null, List.of(withoutSecurityType)), null);

            assertThatThrownBy(runtime::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Broken")
                .hasMessageContaining("product-1");
        }

        @Test
        void should_build_no_security_plan_for_a_plan_in_push_mode() throws Exception {
            Plan push = publishedPlan("plan-push").mode(PlanMode.PUSH).build();

            ApiProductRuntime runtime = cut.create(product(null, List.of(push)), null);
            runtime.start();

            assertThat(runtime.securityPlans()).isEmpty();
        }

        @Test
        void should_build_no_security_plan_for_a_plan_this_gateway_does_not_serve() throws Exception {
            Plan otherShard = publishedPlan("plan-other-shard")
                .tags(Set.of("external"))
                .mode(PlanMode.STANDARD)
                .security(security("not-installed-security"))
                .build();

            ApiProductRuntime runtime = cut.create(product(null, List.of(otherShard)), null);
            runtime.start();

            assertThat(runtime.securityPlans()).isEmpty();
        }

        private PlanSecurity security(String type) {
            return PlanSecurity.builder().type(type).build();
        }
    }

    @Test
    void should_build_the_plan_flow_chains_in_default_mode_when_the_product_saved_no_mode() {
        cut.create(product(null, List.of()), null);

        verify(flowResolverFactory, times(2)).forApiProductPlan(isNull(), anyMap());
        verify(flowResolverFactory, times(2)).deferredConditionFilter((FlowExecution) isNull());
    }

    @Test
    void should_build_the_plan_flow_chains_in_the_mode_the_product_saved() {
        FlowExecution bestMatch = new FlowExecution();
        bestMatch.setMode(FlowMode.BEST_MATCH);

        cut.create(product(bestMatch, List.of()), null);

        verify(flowResolverFactory, times(2)).forApiProductPlan(same(bestMatch), anyMap());
        verify(flowResolverFactory, never()).forApiProductPlan(isNull(), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_hand_the_flows_of_each_plan_to_the_resolver() {
        Flow planAFlow = Flow.builder().name("plan A flow").build();
        Plan planA = publishedPlan("plan-a").flows(List.of(planAFlow)).build();
        Plan planWithoutFlows = publishedPlan("plan-b").build();
        ArgumentCaptor<Map<String, List<Flow>>> flowsByPlanId = ArgumentCaptor.forClass(Map.class);

        cut.create(product(null, List.of(planA, planWithoutFlows)), null);

        verify(flowResolverFactory, atLeastOnce()).forApiProductPlan(isNull(), flowsByPlanId.capture());
        assertThat(flowsByPlanId.getValue()).containsOnlyKeys("plan-a");
        assertThat(flowsByPlanId.getValue().get("plan-a")).containsExactly(planAFlow);
    }

    @Test
    void should_reuse_the_shared_resources_component_when_the_resource_list_is_unchanged() {
        when(resourceManagerFactory.create(any())).thenReturn(mock(io.gravitee.gateway.resource.ResourceLifecycleManager.class));
        Resource resource = Resource.builder().name("cache").type("cache").configuration("{}").enabled(true).build();
        ReactableApiProduct first = product(null, List.of());
        first.setResources(List.of(resource));
        ApiProductRuntime previous = cut.create(first, null);

        ReactableApiProduct updated = product(null, List.of());
        updated.setResources(List.of(resource));
        updated.setVersion("2.0");

        ApiProductRuntime next = cut.create(updated, previous);

        ProductResourcesComponent previousComponent = ProductResourcesComponent.findOn(previous);
        ProductResourcesComponent nextComponent = ProductResourcesComponent.findOn(next);
        assertThat(previousComponent).isNotNull();
        assertThat(nextComponent).isNotNull();
        assertThat(nextComponent.sharedResources()).isSameAs(previousComponent.sharedResources());
    }

    @Test
    void should_build_one_chain_with_and_one_without_tracing() {
        ApiProductRuntime runtime = cut.create(product(null, List.of()), null);

        assertThat(runtime.productPlanFlowChain(true)).isNotNull().isNotSameAs(runtime.productPlanFlowChain(false));
        assertThat(runtime.productPlanFlowChain(false)).isNotNull();
    }

    @Test
    void should_compile_no_path_parameter_extractor_when_no_plan_flow_declares_one() {
        HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        Plan plan = publishedPlan("plan-a").flows(List.of(flowOn("/orders"))).build();

        cut.create(product(null, List.of(plan)), null).extractPathParameters(ctx);

        verify(ctx, never()).request();
    }

    @Nested
    class PolicyPluginManagerLookup {

        private final ApplicationContext applicationContext = mock(ApplicationContext.class);

        @Test
        void should_return_the_only_policy_plugin_manager() {
            ConfigurablePluginManager<?> pluginManager = mock(ConfigurablePluginManager.class);
            when(applicationContext.getBeanNamesForType(any(ResolvableType.class))).thenReturn(new String[] { "policyPluginManager" });
            when(applicationContext.getBean("policyPluginManager")).thenReturn(pluginManager);

            assertThat((Object) ApiProductRuntimeFactory.policyPluginManager(applicationContext)).isSameAs(pluginManager);
        }

        @Test
        void should_fail_when_several_policy_plugin_managers_match() {
            when(applicationContext.getBeanNamesForType(any(ResolvableType.class))).thenReturn(new String[] { "first", "second" });

            assertThatThrownBy(() -> ApiProductRuntimeFactory.policyPluginManager(applicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("first")
                .hasMessageContaining("second");
        }

        @Test
        void should_fail_when_no_policy_plugin_manager_matches() {
            when(applicationContext.getBeanNamesForType(any(ResolvableType.class))).thenReturn(new String[0]);

            assertThatThrownBy(() -> ApiProductRuntimeFactory.policyPluginManager(applicationContext)).isInstanceOf(
                IllegalStateException.class
            );
        }
    }

    private static Plan.PlanBuilder<?, ?> publishedPlan(String id) {
        return Plan.builder().id(id).status(PlanStatus.PUBLISHED);
    }

    private static Flow flowOn(String path) {
        HttpSelector selector = new HttpSelector();
        selector.setPath(path);
        return Flow.builder().enabled(true).selectors(List.of(selector)).build();
    }

    private static ReactableApiProduct product(FlowExecution flowExecution, List<Plan> plans) {
        return ReactableApiProduct.builder()
            .id("product-1")
            .environmentId("env-1")
            .apiIds(Set.of("api-1"))
            .plans(plans)
            .flowExecution(flowExecution)
            .build();
    }
}
