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
package io.gravitee.apim.plugin.reactor;

import io.gravitee.apim.plugin.reactor.processor.ApiProcessorChainFactory;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.api.services.dlq.DefaultDlqServiceFactory;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.service.dlq.DlqServiceFactory;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultEndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.ApiPolicyManager;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.policy.DefaultPolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * At this time, contains everything related to V4, both proxy and message
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class V4ApiReactorFactory implements ReactorFactory<Api> {

    protected final ApplicationContext applicationContext;
    protected final Configuration configuration;
    protected final PolicyFactory policyFactory;
    protected final EntrypointConnectorPluginManager entrypointConnectorPluginManager;
    protected final EndpointConnectorPluginManager endpointConnectorPluginManager;
    private final ApiServicePluginManager apiServicePluginManager;
    protected final PolicyChainFactory platformPolicyChainFactory;
    protected final OrganizationManager organizationManager;
    protected final ApiProcessorChainFactory apiProcessorChainFactory;
    protected final io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory;
    protected final FlowResolverFactory v4FlowResolverFactory;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final ReporterService reporterService;
    private final Node node;

    public V4ApiReactorFactory(
        final ApplicationContext applicationContext,
        final Configuration configuration,
        final Node node,
        final PolicyFactory policyFactory,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final EndpointConnectorPluginManager endpointConnectorPluginManager,
        final ApiServicePluginManager apiServicePluginManager,
        @Qualifier("platformPolicyChainFactory") final PolicyChainFactory platformPolicyChainFactory,
        final OrganizationManager organizationManager,
        final io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory,
        final FlowResolverFactory v4FlowResolverFactory,
        final RequestTimeoutConfiguration requestTimeoutConfiguration,
        final ReporterService reporterService
    ) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.node = node;
        this.policyFactory = policyFactory;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.apiServicePluginManager = apiServicePluginManager;
        this.platformPolicyChainFactory = platformPolicyChainFactory;
        this.organizationManager = organizationManager;
        this.apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);
        this.flowResolverFactory = flowResolverFactory;
        this.v4FlowResolverFactory = v4FlowResolverFactory;
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.reporterService = reporterService;
    }

    @Override
    public boolean support(Class<? extends Reactable> clazz) {
        // TODO: when splitting proxy and event-native, test should have more criteria
        return io.gravitee.gateway.reactive.handlers.api.v4.Api.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canCreate(Api api) {
        // Check that the API contains at least one subscription listener.
        return (
            api.getDefinitionVersion() == DefinitionVersion.V4 &&
            api
                .getDefinition()
                .getListeners()
                .stream()
                .anyMatch(listener -> listener.getType() == ListenerType.HTTP || listener.getType() == ListenerType.SUBSCRIPTION)
        );
    }

    @Override
    public ReactorHandler create(Api api) {
        try {
            if (api.isEnabled()) {
                final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
                final CustomComponentProvider customComponentProvider = new CustomComponentProvider();
                customComponentProvider.add(io.gravitee.gateway.reactive.handlers.api.v4.Api.class, api);
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
                    policyFactory,
                    new CachedPolicyConfigurationFactory(),
                    applicationContext.getBean(PolicyClassLoaderFactory.class),
                    componentProvider
                );

                final PolicyChainFactory policyChainFactory = new DefaultPolicyChainFactory(api.getId(), policyManager, configuration);

                final io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory v4PolicyChainFactory =
                    new io.gravitee.gateway.reactive.v4.policy.DefaultPolicyChainFactory(api.getId(), policyManager, configuration);

                final FlowChainFactory flowChainFactory = new FlowChainFactory(
                    platformPolicyChainFactory,
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

                final DefaultDlqServiceFactory dlqServiceFactory = new DefaultDlqServiceFactory(api.getDefinition(), endpointManager);
                customComponentProvider.add(DlqServiceFactory.class, dlqServiceFactory);

                final List<TemplateVariableProvider> ctxTemplateVariableProviders = ctxTemplateVariableProviders(api);
                ctxTemplateVariableProviders.add(endpointManager);

                return new V4ApiReactor(
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
                    reporterService
                );
            }
        } catch (Exception ex) {
            log.error("Unexpected error while creating V4ApiReactor", ex);
        }
        return null;
    }

    public ResourceLifecycleManager resourceLifecycleManager(
        io.gravitee.gateway.reactive.handlers.api.v4.Api api,
        ResourceClassLoaderFactory resourceClassLoaderFactory,
        ResourceConfigurationFactory resourceConfigurationFactory,
        ApplicationContext applicationContext,
        DeploymentContext deploymentContext
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
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
        io.gravitee.gateway.reactive.handlers.api.v4.Api api,
        PolicyFactory factory,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> ppm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ApiPolicyManager(
            applicationContext.getBean(DefaultClassLoader.class),
            api,
            factory,
            policyConfigurationFactory,
            ppm,
            policyClassLoaderFactory,
            componentProvider
        );
    }

    protected List<TemplateVariableProvider> commonTemplateVariableProviders(io.gravitee.gateway.reactive.handlers.api.v4.Api api) {
        final List<TemplateVariableProvider> templateVariableProviders = new ArrayList<>();
        templateVariableProviders.add(new ApiTemplateVariableProvider(api));
        templateVariableProviders.addAll(
            applicationContext.getBean(ApiTemplateVariableProviderFactory.class).getTemplateVariableProviders()
        );

        return templateVariableProviders;
    }

    protected List<TemplateVariableProvider> ctxTemplateVariableProviders(io.gravitee.gateway.reactive.handlers.api.v4.Api api) {
        final List<TemplateVariableProvider> requestTemplateVariableProviders = commonTemplateVariableProviders(api);
        if (api.getDefinition().getType() == ApiType.PROXY) {
            requestTemplateVariableProviders.add(new ContentTemplateVariableProvider());
        }

        return requestTemplateVariableProviders;
    }
}
