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

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.security.policy.SecurityPolicyFactory;
import io.gravitee.gateway.reactive.policy.AbstractPolicyManager;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads the security policies and the plan flow policies of one deployed version of an API Product, once for all its
 * member APIs.
 */
class ApiProductPolicyManager extends AbstractPolicyManager {

    private final List<Plan> plans;

    ApiProductPolicyManager(
        DefaultClassLoader classLoader,
        PolicyFactoryManager policyFactoryManager,
        PolicyConfigurationFactory policyConfigurationFactory,
        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        List<Plan> plans
    ) {
        super(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider
        );
        this.plans = plans;
    }

    @Override
    protected Set<Policy> dependencies() {
        return Stream.concat(securityPolicies(), flowPolicies()).collect(Collectors.toSet());
    }

    private Stream<Policy> securityPolicies() {
        return plans
            .stream()
            .filter(plan -> plan.useStandardMode() && plan.getSecurity() != null && plan.getSecurity().getType() != null)
            .map(plan -> policy(SecurityPolicyFactory.policyName(plan.getSecurity()), plan.getSecurity().getConfiguration()));
    }

    private Stream<Policy> flowPolicies() {
        return plans
            .stream()
            .map(Plan::getFlows)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(Flow::isEnabled)
            .flatMap(flow -> Stream.of(flow.getRequest(), flow.getResponse(), flow.getSubscribe(), flow.getPublish()))
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(Step::isEnabled)
            .map(step -> policy(step.getPolicy(), step.getConfiguration()));
    }

    private static Policy policy(String name, String configuration) {
        Policy policy = new Policy();
        policy.setName(name);
        policy.setConfiguration(configuration);
        return policy;
    }
}
