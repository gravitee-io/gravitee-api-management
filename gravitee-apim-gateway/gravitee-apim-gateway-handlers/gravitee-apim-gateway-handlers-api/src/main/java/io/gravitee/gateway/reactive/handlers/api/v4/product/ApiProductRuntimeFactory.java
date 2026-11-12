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

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.handlers.api.sharding.ApiProductShardingFilter;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.pathparameters.PathParametersExtractor;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * Builds the runtime of one deployed version of an API Product. A product with no saved flow mode runs its plan flows
 * in {@code DEFAULT} mode, whatever the mode of its member APIs.
 */
@RequiredArgsConstructor
public class ApiProductRuntimeFactory {

    private static final String PRODUCT_PLAN_FLOW_CHAIN_ID = "product-plan";

    private final DefaultClassLoader classLoader;
    private final PolicyFactoryManager policyFactoryManager;
    private final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager;
    private final PolicyClassLoaderFactory policyClassLoaderFactory;
    private final ComponentProvider componentProvider;
    private final FlowResolverFactory flowResolverFactory;
    private final GatewayConfiguration gatewayConfiguration;
    private final ApiProductResourceManagerFactory resourceManagerFactory;
    private final LicenseManager licenseManager;

    /**
     * The policy plugin manager the runtimes load their policies from.
     *
     * @throws IllegalStateException unless exactly one matches, rather than picking one at random
     */
    @SuppressWarnings("unchecked")
    public static ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager(ApplicationContext applicationContext) {
        String[] names = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );
        if (names.length != 1) {
            throw new IllegalStateException("Expected exactly one policy plugin manager, found " + Arrays.toString(names));
        }
        return (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(names[0]);
    }

    public ApiProductRuntime create(ReactableApiProduct product, ApiProductRuntime previousRuntime) {
        List<Plan> plans = plansServedHere(product);
        PolicyManager policyManager = new ApiProductPolicyManager(
            classLoader,
            policyFactoryManager,
            new CachedPolicyConfigurationFactory(),
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider,
            plans
        );
        Map<String, List<Flow>> flowsByPlanId = flowsByPlanId(plans);
        FlowExecution flowExecution = product.getFlowExecution();
        String chainFactoryId = "api-product-" + product.getId();

        List<LifecycleComponent<?>> components = new ArrayList<>();
        ResourceLifecycleManager resourceManager = EmptyProductResourceLifecycleManager.INSTANCE;
        if (ProductResourcesComponent.hasConfiguredResources(product)) {
            ProductResourcesComponent resources = resourcesComponent(product, previousRuntime);
            components.add(resources);
            resourceManager = resources.resourceLifecycleManager();
        }
        components.add(policyManager);

        return new ApiProductRuntime(
            product,
            productPlanFlowChain(flowExecution, flowsByPlanId, new HttpPolicyChainFactory(chainFactoryId, policyManager, false), false),
            productPlanFlowChain(flowExecution, flowsByPlanId, new HttpPolicyChainFactory(chainFactoryId, policyManager, true), true),
            pathParametersExtractor(plans),
            List.copyOf(components),
            resourceManager
        );
    }

    private ProductResourcesComponent resourcesComponent(ReactableApiProduct product, ApiProductRuntime previousRuntime) {
        ProductResourcesComponent previousComponent = ProductResourcesComponent.findOn(previousRuntime);
        if (previousComponent != null && ProductResourcesComponent.sameResources(previousRuntime.product(), product)) {
            return new ProductResourcesComponent(previousComponent.sharedResources());
        }
        return new ProductResourcesComponent(product, resourceManagerFactory, licenseManager);
    }

    private FlowChain productPlanFlowChain(
        FlowExecution flowExecution,
        Map<String, List<Flow>> flowsByPlanId,
        HttpPolicyChainFactory policyChainFactory,
        boolean tracing
    ) {
        FlowChain chain = new FlowChain(
            PRODUCT_PLAN_FLOW_CHAIN_ID,
            flowResolverFactory.forApiProductPlan(flowExecution, flowsByPlanId),
            policyChainFactory,
            flowResolverFactory.deferredConditionFilter(flowExecution),
            true,
            false
        );
        if (tracing) {
            chain.addHooks(List.of(new TracingHook("flow")));
        }
        return chain;
    }

    /**
     * The plans this gateway exposes, as the registry indexes them: a policy only a plan of another shard uses must not
     * keep the product from starting here.
     */
    private List<Plan> plansServedHere(ReactableApiProduct product) {
        if (product.getPlans() == null) {
            return List.of();
        }
        return product
            .getPlans()
            .stream()
            .filter(plan -> ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan))
            .toList();
    }

    private static Map<String, List<Flow>> flowsByPlanId(List<Plan> plans) {
        Map<String, List<Flow>> flowsByPlanId = new HashMap<>();
        plans
            .stream()
            .filter(plan -> plan.getId() != null && plan.getFlows() != null)
            .forEach(plan -> flowsByPlanId.put(plan.getId(), List.copyOf(plan.getFlows())));
        return Map.copyOf(flowsByPlanId);
    }

    private static PathParametersExtractor pathParametersExtractor(List<Plan> plans) {
        Api plansOnly = new Api();
        plansOnly.setPlans(plans);
        PathParametersExtractor extractor = new PathParametersExtractor(plansOnly);
        return extractor.canExtractPathParams() ? extractor : null;
    }
}
