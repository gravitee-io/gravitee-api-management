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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.reactive.policy.AbstractPolicyManager;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Policy manager for API Product plans. Loads security policies from product plans
 * associated with an API via ApiProductRegistry. Used by HttpSecurityChain for
 * product-first validation.
 *
 * @author GraviteeSource Team
 */
public class ApiProductPlanPolicyManager extends AbstractPolicyManager {

    private final String apiId;
    private final String environmentId;
    private final ApiProductRegistry apiProductRegistry;

    public ApiProductPlanPolicyManager(
        DefaultClassLoader classLoader,
        PolicyFactoryManager policyFactoryManager,
        PolicyConfigurationFactory policyConfigurationFactory,
        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        String apiId,
        String environmentId,
        ApiProductRegistry apiProductRegistry
    ) {
        super(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider
        );
        this.apiId = apiId;
        this.environmentId = environmentId;
        this.apiProductRegistry = apiProductRegistry;
    }

    @Override
    protected Set<Policy> dependencies() {
        if (apiProductRegistry == null) {
            return Set.of();
        }

        List<ApiProductRegistry.ApiProductPlanEntry> entries = apiProductRegistry.getApiProductPlanEntriesForApi(apiId, environmentId);

        // Deduplicate by policy name (multiple plans can share same security type)
        LinkedHashMap<String, Policy> byName = new LinkedHashMap<>();

        for (ApiProductRegistry.ApiProductPlanEntry entry : entries) {
            AbstractPlan plan = entry.plan();
            if (!plan.useStandardMode() || plan.getSecurity() == null) {
                continue;
            }

            PlanSecurity planSecurity = plan.getSecurity();
            if (planSecurity.getType() == null) {
                continue;
            }

            String policyName = planSecurity.getType().toLowerCase().replaceAll("_", "-");
            if (!byName.containsKey(policyName)) {
                Policy policy = new Policy();
                policy.setName(policyName);
                policy.setConfiguration(planSecurity.getConfiguration());
                byName.put(policyName, policy);
            }
        }

        return Set.copyOf(byName.values());
    }
}
