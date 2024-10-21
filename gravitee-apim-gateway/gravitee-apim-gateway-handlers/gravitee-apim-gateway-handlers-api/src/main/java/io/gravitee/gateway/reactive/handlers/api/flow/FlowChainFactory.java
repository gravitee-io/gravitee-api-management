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
package io.gravitee.gateway.reactive.handlers.api.flow;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowChainFactory {

    private final OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager;
    private final PolicyChainFactory policyChainFactory;
    private final OrganizationManager organizationManager;
    private final FlowResolverFactory flowResolverFactory;
    private final TracingHook tracingHook;

    public FlowChainFactory(
        final OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager,
        final PolicyChainFactory policyChainFactory,
        final OrganizationManager organizationManager,
        final FlowResolverFactory flowResolverFactory
    ) {
        this.organizationPolicyChainFactoryManager = organizationPolicyChainFactoryManager;
        this.policyChainFactory = policyChainFactory;
        this.organizationManager = organizationManager;
        this.flowResolverFactory = flowResolverFactory;
        this.tracingHook = new TracingHook("flow");
    }

    public FlowChain createOrganizationFlow(final ReactableApi<?> api, final TracingContext tracingContext) {
        String organizationId = api.getOrganizationId();
        FlowChain flowOrganizationChain = new FlowChain(
            "organization",
            flowResolverFactory.forOrganization(organizationId, organizationManager),
            organizationPolicyChainFactoryManager.get(organizationId)
        );
        flowOrganizationChain.addHooks(flowHooks(tracingContext));
        return flowOrganizationChain;
    }

    public FlowChain createPlanFlow(final Api api, final TracingContext tracingContext) {
        FlowChain flowPlanChain = new FlowChain("plan", flowResolverFactory.forApiPlan(api), policyChainFactory);
        flowPlanChain.addHooks(flowHooks(tracingContext));
        return flowPlanChain;
    }

    public FlowChain createApiFlow(final Api api, final TracingContext tracingContext) {
        FlowChain flowApiChain = new FlowChain("api", flowResolverFactory.forApi(api), policyChainFactory);
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
