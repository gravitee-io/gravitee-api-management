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

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsUtils;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultEndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.ApiPolicyManager;
import io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.reactive.policy.HttpPolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactive.v4.flow.BestMatchFlowSelector;
import io.gravitee.gateway.reactive.v4.flow.selection.ConditionSelectorConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.selection.HttpSelectorConditionFilter;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.opentelemetry.InstrumenterTracerFactory;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultApiReactorFactory extends AbstractReactorFactory<Api> {

    protected final Node node;
    protected final EntrypointConnectorPluginManager entrypointConnectorPluginManager;
    protected final EndpointConnectorPluginManager endpointConnectorPluginManager;
    protected final ApiServicePluginManager apiServicePluginManager;
    protected final OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager;
    protected final OrganizationManager organizationManager;
    protected final AccessPointManager accessPointManager;
    protected final EventManager eventManager;
    protected final OpenTelemetryConfiguration openTelemetryConfiguration;
    protected final OpenTelemetryFactory openTelemetryFactory;
    protected final List<InstrumenterTracerFactory> instrumenterTracerFactories;
    protected final ApiProcessorChainFactory apiProcessorChainFactory;
    protected final io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory;
    protected final FlowResolverFactory v4FlowResolverFactory;
    protected final RequestTimeoutConfiguration requestTimeoutConfiguration;
    protected final ReporterService reporterService;
    private final Logger logger = LoggerFactory.getLogger(DefaultApiReactorFactory.class);

    public DefaultApiReactorFactory(
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
        final OpenTelemetryConfiguration openTelemetryConfiguration,
        final OpenTelemetryFactory openTelemetryFactory,
        final List<InstrumenterTracerFactory> instrumenterTracerFactories
    ) {
        super(applicationContext, policyFactoryManager, configuration);
        this.node = node;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.apiServicePluginManager = apiServicePluginManager;
        this.organizationPolicyChainFactoryManager = organizationPolicyChainFactoryManager;
        this.organizationManager = organizationManager;
        this.accessPointManager = accessPointManager;
        this.eventManager = eventManager;
        this.openTelemetryConfiguration = openTelemetryConfiguration;
        this.apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);
        this.flowResolverFactory = flowResolverFactory;
        this.v4FlowResolverFactory = flowResolverFactory();
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.reporterService = reporterService;
        this.openTelemetryFactory = openTelemetryFactory;
        this.instrumenterTracerFactories = instrumenterTracerFactories;
    }

    // FIXME: this constructor is here to keep compatibility with Message Reactor plugin. it will be deleted when Message Reactor has been updated
    public DefaultApiReactorFactory(
        final ApplicationContext applicationContext,
        final Configuration configuration,
        final Node node,
        final PolicyFactory policyFactory,
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
        final OpenTelemetryConfiguration openTelemetryConfiguration,
        final OpenTelemetryFactory openTelemetryFactory,
        final List<InstrumenterTracerFactory> instrumenterTracerFactories
    ) {
        super(applicationContext, new PolicyFactoryManager(new HashSet<>(Set.of(policyFactory))), configuration);
        this.node = node;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.apiServicePluginManager = apiServicePluginManager;
        this.organizationPolicyChainFactoryManager = organizationPolicyChainFactoryManager;
        this.organizationManager = organizationManager;
        this.accessPointManager = accessPointManager;
        this.eventManager = eventManager;
        this.openTelemetryConfiguration = openTelemetryConfiguration;
        this.apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);
        this.flowResolverFactory = flowResolverFactory;
        this.v4FlowResolverFactory = flowResolverFactory();
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.reporterService = reporterService;
        this.openTelemetryFactory = openTelemetryFactory;
        this.instrumenterTracerFactories = instrumenterTracerFactories;
    }

    @SuppressWarnings("java:S1845")
    protected FlowResolverFactory flowResolverFactory() {
        return new FlowResolverFactory(
            new CompositeConditionFilter(new HttpSelectorConditionFilter(), new ConditionSelectorConditionFilter()),
            new BestMatchFlowSelector()
        );
    }

    @Override
    public boolean support(final Class<? extends Reactable> clazz) {
        return Api.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canCreate(Api api) {
        // Check that the API contains at least one subscription listener.
        return (
            api.getDefinitionVersion() == DefinitionVersion.V4 &&
            api.getDefinition().getType() == ApiType.PROXY &&
            api.getDefinition().getListeners().stream().anyMatch(listener -> listener.getType() == ListenerType.HTTP)
        );
    }

    @Override
    protected void addExtraComponents(
        CustomComponentProvider customComponentProvider,
        Api reactableApi,
        DefaultDeploymentContext deploymentContext
    ) {
        customComponentProvider.add(Api.class, reactableApi);
        customComponentProvider.add(io.gravitee.definition.model.v4.Api.class, reactableApi.getDefinition());

        final DefaultEndpointManager endpointManager = new DefaultEndpointManager(
            reactableApi.getDefinition(),
            endpointConnectorPluginManager,
            deploymentContext
        );

        customComponentProvider.add(EndpointManager.class, endpointManager);
    }

    @Override
    protected PolicyManager getPolicyManager(
        DefaultClassLoader classLoader,
        Api reactableApi,
        PolicyFactoryManager factoryManager,
        PolicyConfigurationFactory policyConfigurationFactory,
        ConfigurablePluginManager<PolicyPlugin<?>> ppm,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider
    ) {
        return new ApiPolicyManager(
            classLoader,
            reactableApi,
            factoryManager,
            policyConfigurationFactory,
            ppm,
            policyClassLoaderFactory,
            componentProvider
        );
    }

    @Override
    protected ApiReactor<Api> buildApiReactor(
        Api api,
        CompositeComponentProvider componentProvider,
        PolicyManager policyManager,
        DefaultDeploymentContext deploymentContext,
        ResourceLifecycleManager resourceLifecycleManager
    ) {
        final HttpPolicyChainFactory policyChainFactory = new HttpPolicyChainFactory(api.getId(), policyManager, isApiTracingEnabled(api));

        final io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory v4PolicyChainFactory = policyChainFactory(api, policyManager);

        final FlowChainFactory flowChainFactory = new FlowChainFactory(
            organizationPolicyChainFactoryManager,
            policyChainFactory,
            organizationManager,
            flowResolverFactory
        );

        final io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory =
            new io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory(v4PolicyChainFactory, v4FlowResolverFactory);

        DefaultEndpointManager endpointManager = (DefaultEndpointManager) componentProvider.getComponent(EndpointManager.class);

        final List<TemplateVariableProvider> ctxTemplateVariableProviders = ctxTemplateVariableProviders(api);
        ctxTemplateVariableProviders.add(endpointManager);

        return buildApiReactor(
            api,
            deploymentContext,
            componentProvider,
            ctxTemplateVariableProviders,
            policyManager,
            endpointManager,
            resourceLifecycleManager,
            flowChainFactory,
            v4FlowChainFactory
        );
    }

    protected ApiReactor<Api> buildApiReactor(
        Api api,
        DefaultDeploymentContext deploymentContext,
        CompositeComponentProvider componentProvider,
        List<TemplateVariableProvider> ctxTemplateVariableProviders,
        PolicyManager policyManager,
        DefaultEndpointManager endpointManager,
        ResourceLifecycleManager resourceLifecycleManager,
        FlowChainFactory flowChainFactory,
        io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory
    ) {
        return new DefaultApiReactor(
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
            createTracingContext(api, "API_V4")
        );
    }

    protected TracingContext createTracingContext(final Api api, final String serviceNameSpace) {
        if (isApiTracingEnabled(api)) {
            Tracer tracer = openTelemetryFactory.createTracer(
                api.getId(),
                api.getName(),
                serviceNameSpace,
                api.getApiVersion(),
                instrumenterTracerFactories
            );
            return new TracingContext(tracer, isApiTracingEnabled(api), isApiTracingVerboseEnabled(api));
        } else {
            return TracingContext.noop();
        }
    }

    protected boolean isApiTracingEnabled(final Api api) {
        return AnalyticsUtils.isTracingEnabled(
            openTelemetryConfiguration,
            api.getDefinition() != null ? api.getDefinition().getAnalytics() : null
        );
    }

    protected boolean isApiTracingVerboseEnabled(final Api api) {
        return AnalyticsUtils.isTracingVerbose(
            openTelemetryConfiguration,
            api.getDefinition() != null ? api.getDefinition().getAnalytics() : null
        );
    }

    protected io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory policyChainFactory(Api api, PolicyManager policyManager) {
        return new io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory(api.getId(), policyManager, isApiTracingEnabled(api));
    }

    private List<TemplateVariableProvider> ctxTemplateVariableProviders(Api api) {
        final List<TemplateVariableProvider> requestTemplateVariableProviders = commonTemplateVariableProviders(api);
        if (api.getDefinition().getType() == ApiType.PROXY) {
            requestTemplateVariableProviders.add(new ContentTemplateVariableProvider());
        }

        return requestTemplateVariableProviders;
    }
}
