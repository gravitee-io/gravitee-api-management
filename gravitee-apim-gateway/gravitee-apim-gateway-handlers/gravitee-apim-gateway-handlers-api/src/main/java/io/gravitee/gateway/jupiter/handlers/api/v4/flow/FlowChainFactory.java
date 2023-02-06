/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.handlers.api.v4.flow;

import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.gateway.jupiter.api.hook.ChainHook;
import io.gravitee.gateway.jupiter.core.tracing.TracingHook;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.jupiter.v4.policy.PolicyChainFactory;
import io.gravitee.node.api.configuration.Configuration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
public class FlowChainFactory {

    protected final List<ChainHook> flowHooks = new ArrayList<>();
    private final PolicyChainFactory policyChainFactory;
    private final FlowResolverFactory flowResolverFactory;

    public FlowChainFactory(
        final PolicyChainFactory policyChainFactory,
        final Configuration configuration,
        final FlowResolverFactory flowResolverFactory
    ) {
        this.policyChainFactory = policyChainFactory;
        this.flowResolverFactory = flowResolverFactory;
        boolean tracing = configuration.getProperty("services.tracing.enabled", Boolean.class, false);
        if (tracing) {
            flowHooks.add(new TracingHook("flow"));
        }
    }

    public FlowChain createPlanFlow(final Api api) {
        FlowChain flowPlanChain = new FlowChain("plan", flowResolverFactory.forApiPlan(api), policyChainFactory, true, false);
        flowPlanChain.addHooks(flowHooks);
        return flowPlanChain;
    }

    public FlowChain createApiFlow(final Api api) {
        FlowExecution flowExecution = api.getDefinition().getFlowExecution();
        FlowChain flowApiChain = new FlowChain(
            "api",
            flowResolverFactory.forApi(api),
            policyChainFactory,
            true,
            flowExecution != null && flowExecution.isMatchRequired()
        );
        flowApiChain.addHooks(flowHooks);
        return flowApiChain;
    }
}
