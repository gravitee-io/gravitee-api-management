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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.CLASSLOADER_LEGACY_ENABLED_PROPERTY;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.HttpEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.jupiter.handlers.api.ApiPolicyManager;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.flow.resolver.FlowResolverFactory;
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

    private final Logger logger = LoggerFactory.getLogger(AsyncReactorFactory.class);
    protected final ApplicationContext applicationContext;
    protected final Configuration configuration;
    protected final PolicyFactory policyFactory;

    protected final EntrypointConnectorPluginManager entrypointConnectorPluginManager;
    protected final EndpointConnectorPluginManager endpointConnectorPluginManager;
    protected final PolicyChainFactory platformPolicyChainFactory;
    protected final OrganizationManager organizationManager;
    protected final FlowResolverFactory flowResolverFactory;

    public AsyncReactorFactory(
        final ApplicationContext applicationContext,
        final Configuration configuration,
        final PolicyFactory policyFactory,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        EndpointConnectorPluginManager endpointConnectorPluginManager,
        final PolicyChainFactory platformPolicyChainFactory,
        final OrganizationManager organizationManager,
        final FlowResolverFactory flowResolverFactory
    ) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.policyFactory = policyFactory;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.platformPolicyChainFactory = platformPolicyChainFactory;
        this.organizationManager = organizationManager;
        this.flowResolverFactory = flowResolverFactory;
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
                customComponentProvider.add(io.gravitee.definition.model.v4.Api.class, api.getDefinition());

                final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(
                    api,
                    applicationContext.getBean(ResourceClassLoaderFactory.class),
                    new ResourceConfigurationFactoryImpl(),
                    applicationContext
                );

                customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);
                customComponentProvider.add(Api.class, api);

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

                final FlowChainFactory flowChainFactory = new FlowChainFactory(
                    platformPolicyChainFactory,
                    policyChainFactory,
                    organizationManager,
                    configuration,
                    flowResolverFactory
                );

                return new AsyncApiReactor(
                    api,
                    apiComponentProvider,
                    policyManager,
                    new HttpEntrypointConnectorResolver(api.getDefinition(), entrypointConnectorPluginManager),
                    new EndpointInvoker(new DefaultEndpointConnectorResolver(api.getDefinition(), endpointConnectorPluginManager)),
                    flowChainFactory
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

        System.out.println(beanNamesForType[0]);
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
