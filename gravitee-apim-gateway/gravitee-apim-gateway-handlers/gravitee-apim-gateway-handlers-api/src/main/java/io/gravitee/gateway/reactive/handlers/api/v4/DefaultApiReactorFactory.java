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
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultEndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.ApiPolicyManager;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
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
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.v4.flow.BestMatchFlowSelector;
import io.gravitee.gateway.reactive.v4.flow.selection.ConditionSelectorConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.selection.HttpSelectorConditionFilter;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.v4.DefaultResourceManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultApiReactorFactory implements ReactorFactory<Api> {

    protected final ApplicationContext applicationContext;
    protected final Configuration configuration;
    protected final Node node;
    protected final PolicyFactoryManager policyFactoryManager;
    protected final EntrypointConnectorPluginManager entrypointConnectorPluginManager;
    protected final EndpointConnectorPluginManager endpointConnectorPluginManager;
    protected final ApiServicePluginManager apiServicePluginManager;
    protected final OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager;
    protected final OrganizationManager organizationManager;
    protected final AccessPointManager accessPointManager;
    protected final EventManager eventManager;
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
        final EventManager eventManager
    ) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.node = node;
        this.policyFactoryManager = policyFactoryManager;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.apiServicePluginManager = apiServicePluginManager;
        this.organizationPolicyChainFactoryManager = organizationPolicyChainFactoryManager;
        this.organizationManager = organizationManager;
        this.accessPointManager = accessPointManager;
        this.eventManager = eventManager;
        this.apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);
        this.flowResolverFactory = flowResolverFactory;
        this.v4FlowResolverFactory = flowResolverFactory();
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.reporterService = reporterService;
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
        final EventManager eventManager
    ) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.node = node;
        this.policyFactoryManager = new PolicyFactoryManager(new HashSet<>(Set.of(policyFactory)));
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.apiServicePluginManager = apiServicePluginManager;
        this.organizationPolicyChainFactoryManager = organizationPolicyChainFactoryManager;
        this.organizationManager = organizationManager;
        this.accessPointManager = accessPointManager;
        this.eventManager = eventManager;
        this.apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);
        this.flowResolverFactory = flowResolverFactory;
        this.v4FlowResolverFactory = flowResolverFactory();
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.reporterService = reporterService;
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
    public ReactorHandler create(final Api api) {
        try {
            if (api.isEnabled()) {
                final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
                final CustomComponentProvider customComponentProvider = new CustomComponentProvider();
                customComponentProvider.add(Api.class, api);
                customComponentProvider.add(ReactableApi.class, api);
                customComponentProvider.add(io.gravitee.definition.model.v4.Api.class, api.getDefinition());

                final CompositeComponentProvider componentProvider = new CompositeComponentProvider(
                    customComponentProvider,
                    globalComponentProvider
                );

                final DefaultDeploymentContext deploymentContext = new DefaultDeploymentContext();
                deploymentContext.componentProvider(componentProvider);
                deploymentContext.templateVariableProviders(commonTemplateVariableProviders(api));

                final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(
                    api,
                    applicationContext.getBean(ResourceClassLoaderFactory.class),
                    new ResourceConfigurationFactoryImpl(),
                    applicationContext,
                    deploymentContext
                );
                customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);

                final PolicyManager policyManager = policyManager(
                    api,
                    policyFactoryManager,
                    new CachedPolicyConfigurationFactory(),
                    applicationContext.getBean(PolicyClassLoaderFactory.class),
                    componentProvider
                );

                final PolicyChainFactory policyChainFactory = new HttpPolicyChainFactory(api.getId(), policyManager, configuration);

                final io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory v4PolicyChainFactory = policyChainFactory(
                    api,
                    policyManager
                );

                final FlowChainFactory flowChainFactory = new FlowChainFactory(
                    organizationPolicyChainFactoryManager,
                    policyChainFactory,
                    organizationManager,
                    configuration,
                    flowResolverFactory
                );

                final DefaultEndpointManager endpointManager = new DefaultEndpointManager(
                    api.getDefinition(),
                    endpointConnectorPluginManager,
                    deploymentContext
                );

                customComponentProvider.add(EndpointManager.class, endpointManager);

                final io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory =
                    new io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory(
                        v4PolicyChainFactory,
                        configuration,
                        v4FlowResolverFactory
                    );

                customComponents(api, customComponentProvider);

                final List<TemplateVariableProvider> ctxTemplateVariableProviders = ctxTemplateVariableProviders(api);
                ctxTemplateVariableProviders.add(endpointManager);

                return buildApiReactor(
                    api,
                    componentProvider,
                    deploymentContext,
                    resourceLifecycleManager,
                    policyManager,
                    flowChainFactory,
                    endpointManager,
                    v4FlowChainFactory,
                    ctxTemplateVariableProviders
                );
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating AsyncApiReactor", ex);
        }
        return null;
    }

    protected DefaultApiReactor buildApiReactor(
        Api api,
        CompositeComponentProvider componentProvider,
        DefaultDeploymentContext deploymentContext,
        ResourceLifecycleManager resourceLifecycleManager,
        PolicyManager policyManager,
        FlowChainFactory flowChainFactory,
        DefaultEndpointManager endpointManager,
        io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory,
        List<TemplateVariableProvider> ctxTemplateVariableProviders
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
            eventManager
        );
    }

    protected void customComponents(Api api, CustomComponentProvider customComponentProvider) {}

    protected io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory policyChainFactory(Api api, PolicyManager policyManager) {
        return new io.gravitee.gateway.reactive.v4.policy.HttpPolicyChainFactory(api.getId(), policyManager, configuration);
    }

    /**
     * Search across tree of BeanFactory in order to find bean in a parent application context.
     * @param resolvableType
     * @return
     */
    private String[] getBeanNamesForType(ResolvableType resolvableType) {
        return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            ((ConfigurableApplicationContext) applicationContext).getBeanFactory(),
            resolvableType
        );
    }

    @SuppressWarnings("unchecked")
    public ResourceLifecycleManager resourceLifecycleManager(
        Api api,
        ResourceClassLoaderFactory resourceClassLoaderFactory,
        ResourceConfigurationFactory resourceConfigurationFactory,
        ApplicationContext applicationContext,
        DeploymentContext deploymentContext
    ) {
        String[] beanNamesForType = getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> cpm = (ConfigurablePluginManager<ResourcePlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new DefaultResourceManager(
            applicationContext.getBean(DefaultClassLoader.class),
            api,
            cpm,
            resourceClassLoaderFactory,
            resourceConfigurationFactory,
            applicationContext,
            deploymentContext
        );
    }

    @SuppressWarnings("unchecked")
    public PolicyManager policyManager(
        Api api,
        PolicyFactoryManager factoryManager,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider
    ) {
        String[] beanNamesForType = getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> ppm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ApiPolicyManager(
            applicationContext.getBean(DefaultClassLoader.class),
            api,
            factoryManager,
            policyConfigurationFactory,
            ppm,
            policyClassLoaderFactory,
            componentProvider
        );
    }

    protected List<TemplateVariableProvider> commonTemplateVariableProviders(Api api) {
        final List<TemplateVariableProvider> templateVariableProviders = new ArrayList<>();
        templateVariableProviders.add(new ApiTemplateVariableProvider(api));
        templateVariableProviders.addAll(
            applicationContext.getBean(ApiTemplateVariableProviderFactory.class).getTemplateVariableProviders()
        );

        return templateVariableProviders;
    }

    protected List<TemplateVariableProvider> ctxTemplateVariableProviders(Api api) {
        final List<TemplateVariableProvider> requestTemplateVariableProviders = commonTemplateVariableProviders(api);
        if (api.getDefinition().getType() == ApiType.PROXY) {
            requestTemplateVariableProviders.add(new ContentTemplateVariableProvider());
        }

        return requestTemplateVariableProviders;
    }
}
