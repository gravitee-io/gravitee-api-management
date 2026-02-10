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
package io.gravitee.gateway.reactive.debug.handlers.api.v4;

import io.gravitee.common.event.EventManager;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultEndpointManager;
import io.gravitee.gateway.reactive.debug.handlers.api.DebugV4ApiReactor;
import io.gravitee.gateway.reactive.debug.policy.DebugPolicyChainFactory;
import io.gravitee.gateway.reactive.debug.policy.DebugV4PolicyChainFactory;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.ApiProductPlanPolicyManagerFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.DefaultApiReactorFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.reactive.policy.HttpPolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.report.guard.LogGuardService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.List;
import org.springframework.context.ApplicationContext;

public class DebugV4ApiReactorHandlerFactory extends DefaultApiReactorFactory {

    private final ApiProductRegistry apiProductRegistry;

    public DebugV4ApiReactorHandlerFactory(
        final ApplicationContext applicationContext,
        final Configuration configuration,
        final Node node,
        final PolicyFactoryManager policyFactoryManager,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final EndpointConnectorPluginManager endpointConnectorPluginManager,
        final ApiServicePluginManager apiServicePluginManager,
        final OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager,
        final OrganizationManager organizationManager,
        final io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory,
        final RequestTimeoutConfiguration requestTimeoutConfiguration,
        final ReporterService reporterService,
        final AccessPointManager accessPointManager,
        final EventManager eventManager,
        final HttpAcceptorFactory httpAcceptorFactory,
        final GatewayConfiguration gatewayConfiguration,
        final DictionaryManager dictionaryManager,
        final ConnectionDrainManager connectionDrainManager,
        final ApiProductRegistry apiProductRegistry
    ) {
        super(
            applicationContext,
            configuration,
            node,
            policyFactoryManager,
            entrypointConnectorPluginManager,
            endpointConnectorPluginManager,
            apiServicePluginManager,
            organizationPolicyChainFactoryManager,
            organizationManager,
            flowResolverFactory,
            requestTimeoutConfiguration,
            reporterService,
            accessPointManager,
            eventManager,
            httpAcceptorFactory,
            OpenTelemetryConfiguration.builder().tracesEnabled(false).build(),
            null,
            null,
            gatewayConfiguration,
            dictionaryManager,
            null,
            connectionDrainManager
        );
        this.apiProductRegistry = apiProductRegistry;
    }

    @Override
    protected HttpPolicyChainFactory createPolicyChainFactory(Api api, PolicyManager policyManager) {
        return new DebugPolicyChainFactory(api.getId(), policyManager, false);
    }

    @Override
    protected io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory policyChainFactory(Api api, PolicyManager policyManager) {
        return new DebugV4PolicyChainFactory(api.getId(), policyManager, false);
    }

    @Override
    protected ApiReactor<Api> buildApiReactor(
        Api api,
        DefaultDeploymentContext deploymentContext,
        CompositeComponentProvider componentProvider,
        List<TemplateVariableProvider> ctxTemplateVariableProviders,
        PolicyManager policyManager,
        DefaultEndpointManager endpointManager,
        ResourceLifecycleManager resourceLifecycleManager,
        FlowChainFactory flowChainFactory,
        io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory,
        LogGuardService logGuardService,
        ConnectionDrainManager connectionDrainManager
    ) {
        ApiProductPlanPolicyManagerFactory apiProductPlanPolicyManagerFactory = null;
        if (apiProductRegistry != null) {
            apiProductPlanPolicyManagerFactory = createApiProductPlanPolicyManagerFactory(componentProvider, apiProductRegistry);
        }
        return new DebugV4ApiReactor(
            api,
            deploymentContext,
            componentProvider,
            ctxTemplateVariableProviders,
            policyManager,
            entrypointConnectorPluginManager,
            apiServicePluginManager,
            endpointManager,
            resourceLifecycleManager,
            apiProcessorChainFactory,
            flowChainFactory,
            v4FlowChainFactory,
            configuration,
            node,
            requestTimeoutConfiguration,
            reporterService,
            accessPointManager,
            eventManager,
            httpAcceptorFactory,
            TracingContext.noop(),
            logGuardService,
            apiProductRegistry,
            apiProductPlanPolicyManagerFactory
        );
    }
}
