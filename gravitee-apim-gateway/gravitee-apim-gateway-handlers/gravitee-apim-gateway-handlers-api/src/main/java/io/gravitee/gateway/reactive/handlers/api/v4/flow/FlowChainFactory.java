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
package io.gravitee.gateway.reactive.handlers.api.v4.flow;

import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
public class FlowChainFactory {

    private final PolicyChainFactory policyChainFactory;
    private final FlowResolverFactory flowResolverFactory;
    private TracingHook tracingHook;

    public FlowChainFactory(final PolicyChainFactory policyChainFactory, final FlowResolverFactory flowResolverFactory) {
        this.policyChainFactory = policyChainFactory;
        this.flowResolverFactory = flowResolverFactory;
        this.tracingHook = new TracingHook("flow");
    }

    public FlowChain createPlanFlow(final Api api, final TracingContext tracingContext) {
        FlowChain flowPlanChain = new FlowChain("plan", flowResolverFactory.forApiPlan(api), policyChainFactory, true, false);
        flowPlanChain.addHooks(flowHooks(tracingContext));
        return flowPlanChain;
    }

    public FlowChain createApiFlow(final Api api, final TracingContext tracingContext) {
        FlowExecution flowExecution = api.getDefinition().getFlowExecution();
        FlowChain flowApiChain = new FlowChain(
            "api",
            flowResolverFactory.forApi(api),
            policyChainFactory,
            true,
            flowExecution != null && flowExecution.isMatchRequired()
        );
        flowApiChain.addHooks(flowHooks(tracingContext));
        return flowApiChain;
    }

    private List<ChainHook> flowHooks(final TracingContext tracingContext) {
        if (tracingContext.isEnabled()) {
            return List.of(tracingHook);
        }
        return List.of();
    }
}
