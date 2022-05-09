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
package io.gravitee.gateway.platform.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.platform.OrganizationFlowResolver;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.platform.manager.impl.OrganizationManagerImpl;
import io.gravitee.gateway.platform.providers.OnRequestPlatformPolicyChainProvider;
import io.gravitee.gateway.platform.providers.OnResponsePlatformPolicyChainProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.platform.PlatformPolicyManager;
import io.gravitee.gateway.reactive.policy.DefaultPolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryCreator;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class PlatformConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${classloader.legacy.enabled:false}")
    private boolean classLoaderLegacyMode;

    @Bean
    public OrganizationManager organizationManager(
        PlatformPolicyManager policyManager,
        io.gravitee.gateway.platform.PlatformPolicyManager v3policyManager,
        EventManager eventManager
    ) {
        return new OrganizationManagerImpl(policyManager, v3policyManager, eventManager);
    }

    @Bean
    public OrganizationFlowResolver flowResolver(OrganizationManager organizationManager) {
        return new OrganizationFlowResolver(organizationManager);
    }

    @Bean
    public PolicyChainFactory policyChainFactory(io.gravitee.gateway.platform.PlatformPolicyManager v3PlatformPolicyManager) {
        return new PolicyChainFactory(v3PlatformPolicyManager);
    }

    @Bean
    public OnRequestPlatformPolicyChainProvider onRequestPlatformPolicyChainProvider(
        OrganizationFlowResolver organizationFlowResolver,
        PolicyChainFactory policyChainFactory
    ) {
        return new OnRequestPlatformPolicyChainProvider(organizationFlowResolver, policyChainFactory, new FlowPolicyResolverFactory());
    }

    @Bean
    public OnResponsePlatformPolicyChainProvider onResponsePlatformPolicyChainProvider(
        OrganizationFlowResolver organizationFlowResolver,
        PolicyChainFactory policyChainFactory
    ) {
        return new OnResponsePlatformPolicyChainProvider(organizationFlowResolver, policyChainFactory, new FlowPolicyResolverFactory());
    }

    @Bean
    public io.gravitee.gateway.platform.PlatformPolicyManager v3PlatformPolicyManager(
        io.gravitee.gateway.policy.PolicyFactoryCreator factoryCreator,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ResourceLifecycleManager resourceLifecycleManager,
        ComponentProvider componentProvider
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> cpm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new io.gravitee.gateway.platform.PlatformPolicyManager(
            classLoaderLegacyMode,
            applicationContext.getBean(DefaultClassLoader.class),
            factoryCreator.create(),
            policyConfigurationFactory,
            cpm,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
    }

    @Bean
    public PlatformPolicyManager platformPolicyManager(
        PolicyFactoryCreator factoryCreator,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> cpm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new PlatformPolicyManager(
            applicationContext.getBean(DefaultClassLoader.class),
            factoryCreator.create(),
            policyConfigurationFactory,
            cpm,
            policyClassLoaderFactory,
            componentProvider
        );
    }

    @Bean("platformPolicyChainFactory")
    public io.gravitee.gateway.reactive.policy.PolicyChainFactory platformPolicyChainFactory(PlatformPolicyManager platformPolicyManager) {
        return new DefaultPolicyChainFactory("platform", platformPolicyManager);
    }

    @Bean
    public PolicyConfigurationFactory policyConfigurationFactory() {
        return new CachedPolicyConfigurationFactory();
    }

    @Bean
    public ResourceLifecycleManager resourceLifecycleManager(
        ResourceClassLoaderFactory resourceClassLoaderFactory,
        ResourceConfigurationFactory resourceConfigurationFactory
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> cpm = (ConfigurablePluginManager<ResourcePlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ResourceManagerImpl(
            classLoaderLegacyMode,
            applicationContext.getBean(DefaultClassLoader.class),
            null,
            cpm,
            resourceClassLoaderFactory,
            resourceConfigurationFactory,
            applicationContext
        );
    }

    @Bean
    public ResourceConfigurationFactory resourceConfigurationFactory() {
        return new ResourceConfigurationFactoryImpl();
    }
}
