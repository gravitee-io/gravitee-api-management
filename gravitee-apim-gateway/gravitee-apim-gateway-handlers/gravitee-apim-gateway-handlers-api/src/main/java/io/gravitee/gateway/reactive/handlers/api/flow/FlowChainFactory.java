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
package io.gravitee.gateway.reactive.handlers.api.flow;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.Hook;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import io.gravitee.node.api.configuration.Configuration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowChainFactory {

    private final List<ChainHook> flowHooks = new ArrayList<>();
    private final PolicyChainFactory platformPolicyChainFactory;
    private final PolicyChainFactory apiPolicyChainFactory;
    private final OrganizationManager organizationManager;

    public FlowChainFactory(
        final PolicyChainFactory platformPolicyChainFactory,
        final PolicyChainFactory apiPolicyChainFactory,
        final OrganizationManager organizationManager,
        final Configuration configuration
    ) {
        this.platformPolicyChainFactory = platformPolicyChainFactory;
        this.apiPolicyChainFactory = apiPolicyChainFactory;
        this.organizationManager = organizationManager;
        boolean tracing = configuration.getProperty("services.tracing.enabled", Boolean.class, false);
        if (tracing) {
            flowHooks.add(new TracingHook("flow"));
        }
    }

    public FlowChain createPlatformFlow(final Api api) {
        FlowChain flowPlatformChain = new FlowChain(
            "flow-platform",
            FlowResolverFactory.forPlatform(api, organizationManager),
            platformPolicyChainFactory
        );
        flowPlatformChain.addHooks(flowHooks);
        return flowPlatformChain;
    }

    public FlowChain createPlanFlow(final Api api) {
        FlowChain flowPlanChain = new FlowChain("flow-api-plan", FlowResolverFactory.forApiPlan(api), apiPolicyChainFactory);
        flowPlanChain.addHooks(flowHooks);
        return flowPlanChain;
    }

    public FlowChain createApiFlow(final Api api) {
        FlowChain flowApiChain = new FlowChain("flow-api", FlowResolverFactory.forApi(api), apiPolicyChainFactory);
        flowApiChain.addHooks(flowHooks);
        return flowApiChain;
    }
}
