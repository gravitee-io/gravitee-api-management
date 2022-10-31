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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.CLASSLOADER_LEGACY_ENABLED_PROPERTY;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.jupiter.handlers.api.ApiPolicyManager;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.ApiMessageProcessorChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.policy.DefaultPolicyChainFactory;
import io.gravitee.gateway.jupiter.policy.PolicyChainFactory;
import io.gravitee.gateway.jupiter.policy.PolicyFactory;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AsyncReactorFactory implements ReactorFactory<Api> {

    protected final ApplicationContext applicationContext;
    protected final Configuration configuration;
    protected final PolicyFactory policyFactory;
    protected final EntrypointConnectorPluginManager entrypointConnectorPluginManager;
    protected final EndpointConnectorPluginManager endpointConnectorPluginManager;
    protected final PolicyChainFactory platformPolicyChainFactory;
    protected final OrganizationManager organizationManager;
    protected final ApiProcessorChainFactory apiProcessorChainFactory;
    protected final ApiMessageProcessorChainFactory apiMessageProcessorChainFactory;
    protected final io.gravitee.gateway.jupiter.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory;
    protected final FlowResolverFactory v4FlowResolverFactory;
    private final Node node;
    private final Logger logger = LoggerFactory.getLogger(AsyncReactorFactory.class);

    public AsyncReactorFactory(
        final ApplicationContext applicationContext,
        final Configuration configuration,
        final Node node,
        final PolicyFactory policyFactory,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final EndpointConnectorPluginManager endpointConnectorPluginManager,
        final PolicyChainFactory platformPolicyChainFactory,
        final OrganizationManager organizationManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final ApiMessageProcessorChainFactory apiMessageProcessorChainFactory,
        final io.gravitee.gateway.jupiter.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory,
        final FlowResolverFactory v4FlowResolverFactory
    ) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.node = node;
        this.policyFactory = policyFactory;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.platformPolicyChainFactory = platformPolicyChainFactory;
        this.organizationManager = organizationManager;
        this.apiProcessorChainFactory = apiProcessorChainFactory;
        this.apiMessageProcessorChainFactory = apiMessageProcessorChainFactory;
        this.flowResolverFactory = flowResolverFactory;
        this.v4FlowResolverFactory = v4FlowResolverFactory;
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
            api.getDefinition().getType() == ApiType.ASYNC &&
            api
                .getDefinition()
                .getListeners()
                .stream()
                .anyMatch(listener -> listener.getType() == ListenerType.HTTP || listener.getType() == ListenerType.SUBSCRIPTION)
        );
    }

    @Override
    public ReactorHandler create(final Api api) {
        try {
            if (api.isEnabled()) {
                final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
                final CustomComponentProvider customComponentProvider = new CustomComponentProvider();
                customComponentProvider.add(Api.class, api);
                customComponentProvider.add(io.gravitee.definition.model.v4.Api.class, api.getDefinition());

                final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(
                    api,
                    applicationContext.getBean(ResourceClassLoaderFactory.class),
                    new ResourceConfigurationFactoryImpl(),
                    applicationContext
                );

                customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);

                final CompositeComponentProvider apiComponentProvider = new CompositeComponentProvider(
                    customComponentProvider,
                    globalComponentProvider
                );

                final PolicyManager policyManager = policyManager(
                    api,
                    policyFactory,
                    new CachedPolicyConfigurationFactory(),
                    applicationContext.getBean(PolicyClassLoaderFactory.class),
                    apiComponentProvider
                );

                final io.gravitee.gateway.jupiter.policy.PolicyChainFactory policyChainFactory = new DefaultPolicyChainFactory(
                    api.getId(),
                    policyManager,
                    configuration
                );

                final io.gravitee.gateway.jupiter.v4.policy.PolicyChainFactory v4PolicyChainFactory = new io.gravitee.gateway.jupiter.v4.policy.DefaultPolicyChainFactory(
                    api.getId(),
                    policyManager,
                    configuration
                );

                final FlowChainFactory flowChainFactory = new FlowChainFactory(
                    platformPolicyChainFactory,
                    policyChainFactory,
                    organizationManager,
                    configuration,
                    flowResolverFactory
                );

                final io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory = new io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChainFactory(
                    v4PolicyChainFactory,
                    configuration,
                    v4FlowResolverFactory
                );

                return new AsyncApiReactor(
                    api,
                    apiComponentProvider,
                    policyManager,
                    entrypointConnectorPluginManager,
                    endpointConnectorPluginManager,
                    resourceLifecycleManager,
                    apiProcessorChainFactory,
                    apiMessageProcessorChainFactory,
                    flowChainFactory,
                    v4FlowChainFactory,
                    configuration,
                    node
                );
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating AsyncApiReactor", ex);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public ResourceLifecycleManager resourceLifecycleManager(
        Api api,
        ResourceClassLoaderFactory resourceClassLoaderFactory,
        ResourceConfigurationFactory resourceConfigurationFactory,
        ApplicationContext applicationContext
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> cpm = (ConfigurablePluginManager<ResourcePlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ResourceManagerImpl(
            configuration.getProperty(CLASSLOADER_LEGACY_ENABLED_PROPERTY, Boolean.class, false),
            applicationContext.getBean(DefaultClassLoader.class),
            api,
            cpm,
            resourceClassLoaderFactory,
            resourceConfigurationFactory,
            applicationContext
        );
    }

    @SuppressWarnings("unchecked")
    public PolicyManager policyManager(
        Api api,
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
}
