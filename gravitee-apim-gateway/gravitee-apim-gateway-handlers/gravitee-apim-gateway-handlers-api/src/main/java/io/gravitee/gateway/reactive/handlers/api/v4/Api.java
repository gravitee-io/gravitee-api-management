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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api extends ReactableApi<io.gravitee.definition.model.v4.Api> {

    public Api() {
        super();
    }

    public Api(io.gravitee.definition.model.v4.Api api) {
        super(api);
    }

    @Override
    public String getApiVersion() {
        return definition.getApiVersion();
    }

    @Override
    public DefinitionVersion getDefinitionVersion() {
        return definition.getDefinitionVersion();
    }

    @Override
    public Set<String> getTags() {
        return definition.getTags();
    }

    @Override
    public String getId() {
        return definition.getId();
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public Set<String> getSubscribablePlans() {
        return definition.getPlans() != null
            ? definition.getPlans().stream().filter(Plan::isSubscribable).map(Plan::getId).collect(Collectors.toSet())
            : Set.of();
    }

    @Override
    public Set<String> getApiKeyPlans() {
        return definition.getPlans() != null
            ? definition.getPlans().stream().filter(Plan::isApiKey).map(Plan::getId).collect(Collectors.toSet())
            : Set.of();
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        if (Policy.class.equals(type)) {
            return (Set<D>) policies();
        } else if (Resource.class.equals(type) && definition.getResources() != null) {
            return (Set<D>) new HashSet<>(definition.getResources());
        }

        return Collections.emptySet();
    }

    private Set<Policy> policies() {
        Set<Policy> policies = new HashSet<>();

        // Load policies from Plans
        if (definition.getPlans() != null) {
            definition
                .getPlans()
                .forEach(plan -> {
                    // TODO: associate plan security with its policy (https://github.com/gravitee-io/issues/issues/8427)
                    if (plan.useStandardMode()) {
                        PlanSecurity security = plan.getSecurity();
                        Policy secPolicy = new Policy();
                        secPolicy.setName(security.getType());

                        if (secPolicy.getName() != null) {
                            policies.add(secPolicy);
                        }
                    }

                    if (plan.getFlows() != null) {
                        List<Flow> flows = plan.getFlows();
                        addFlowsPolicies(policies, flows);
                    }
                });
        }

        // Load policies from flows
        if (definition.getFlows() != null) {
            List<Flow> flows = definition.getFlows();
            addFlowsPolicies(policies, flows);
        }

        return policies;
    }

    private void addFlowsPolicies(final Set<Policy> policies, final List<Flow> flows) {
        flows
            .stream()
            .filter(Flow::isEnabled)
            .forEach(flow -> {
                policies.addAll(getPolicies(flow.getRequest()));
                policies.addAll(getPolicies(flow.getResponse()));
                policies.addAll(getPolicies(flow.getSubscribe()));
                policies.addAll(getPolicies(flow.getPublish()));
            });
    }

    private List<Policy> getPolicies(List<Step> flowStep) {
        if (flowStep == null || flowStep.isEmpty()) {
            return List.of();
        }
        return flowStep
            .stream()
            .filter(Step::isEnabled)
            .map(step -> {
                Policy policy = new Policy();
                policy.setName(step.getPolicy());
                policy.setConfiguration(step.getConfiguration());
                return policy;
            })
            .collect(Collectors.toList());
    }
}
