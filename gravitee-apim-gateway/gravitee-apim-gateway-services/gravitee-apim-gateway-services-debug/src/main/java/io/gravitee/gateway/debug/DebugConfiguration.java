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
package io.gravitee.gateway.debug;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.debug.handlers.api.DebugApiContextHandlerFactory;
import io.gravitee.gateway.debug.platform.manager.DebugOrganizationManager;
import io.gravitee.gateway.debug.policy.impl.PolicyDebugDecoratorFactoryCreator;
import io.gravitee.gateway.debug.vertx.VertxDebugService;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.flow.FlowResolver;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.policy.PolicyFactory;
import io.gravitee.gateway.platform.OrganizationFlowResolver;
import io.gravitee.gateway.platform.PlatformPolicyManager;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.platform.providers.OnRequestPlatformPolicyChainProvider;
import io.gravitee.gateway.platform.providers.OnResponsePlatformPolicyChainProvider;
import io.gravitee.gateway.policy.ConfigurablePolicyChainProvider;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.impl.PolicyFactoryCreatorImpl;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactoryManager;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.impl.DefaultEntrypointResolver;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

@Configuration
public class DebugConfiguration {

    private final ApplicationContext applicationContext;

    private final Node node;

    private final io.gravitee.node.api.configuration.Configuration configuration;

    public DebugConfiguration(
        ApplicationContext applicationContext,
        Node node,
        io.gravitee.node.api.configuration.Configuration configuration
    ) {
        this.applicationContext = applicationContext;
        this.node = node;
        this.configuration = configuration;
    }

    @Bean
    public VertxDebugService vertxDebugService() {
        return new VertxDebugService();
    }

    @Bean
    @Qualifier("debugV3PolicyFactoryCreator")
    public io.gravitee.gateway.policy.PolicyFactoryCreator debugPolicyFactoryCreator(final PolicyPluginFactory policyPluginFactory) {
        return new PolicyDebugDecoratorFactoryCreator(
            new PolicyFactoryCreatorImpl(configuration, policyPluginFactory, new ExpressionLanguageStringConditionEvaluator())
        );
    }

    @Bean
    @Qualifier("debugReactorHandlerFactory")
    public ReactorHandlerFactory<Api> reactorHandlerFactory(
        @Qualifier("debugV3PolicyFactoryCreator") io.gravitee.gateway.policy.PolicyFactoryCreator v3PolicyFactoryCreator,
        PolicyFactory policyFactory,
        @Qualifier("debugPolicyChainProviderLoader") PolicyChainProviderLoader policyChainProviderLoader,
        ApiProcessorChainFactory apiProcessorChainFactory
    ) {
        return new DebugApiContextHandlerFactory(
            applicationContext.getParent(),
            configuration,
            node,
            v3PolicyFactoryCreator,
            policyFactory,
            policyChainProviderLoader,
            apiProcessorChainFactory
        );
    }

    @Bean
    @Qualifier("debugReactorHandlerFactoryManager")
    public ReactorHandlerFactoryManager reactorHandlerFactoryManager(
        @Qualifier("debugReactorHandlerFactory") ReactorHandlerFactory reactorHandlerFactory
    ) {
        return new ReactorHandlerFactoryManager(reactorHandlerFactory);
    }

    @Bean
    @Qualifier("debugReactorHandlerRegistry")
    public ReactorHandlerRegistry reactorHandlerRegistry(ReactorHandlerFactoryManager reactorHandlerFactoryManager) {
        return new DefaultReactorHandlerRegistry(reactorHandlerFactoryManager);
    }

    @Bean
    @Qualifier("debugEntryPointResolver")
    public EntrypointResolver debugEntryPointResolver(
        @Qualifier("debugReactorHandlerRegistry") ReactorHandlerRegistry reactorHandlerRegistry
    ) {
        return new DefaultEntrypointResolver(reactorHandlerRegistry);
    }

    @Bean
    @Qualifier("debugPolicyChainProviderLoader")
    public PolicyChainProviderLoader policyChainProviderLoader(
        @Qualifier("debugConfigurablePolicyChainProvider") List<ConfigurablePolicyChainProvider> providers
    ) {
        return new PolicyChainProviderLoader(providers);
    }

    @Bean
    @Qualifier("debugConfigurablePolicyChainProvider")
    public OnRequestPlatformPolicyChainProvider onRequestPlatformPolicyChainProvider(
        @Qualifier("debugFlowResolver") FlowResolver flowResolver,
        @Qualifier("debugPlatformPolicyChainFactory") PolicyChainFactory policyChainFactory
    ) {
        return new OnRequestPlatformPolicyChainProvider(flowResolver, policyChainFactory, new FlowPolicyResolverFactory());
    }

    @Bean
    @Qualifier("debugConfigurablePolicyChainProvider")
    public OnResponsePlatformPolicyChainProvider onResponsePlatformPolicyChainProvider(
        @Qualifier("debugFlowResolver") FlowResolver flowResolver,
        @Qualifier("debugPlatformPolicyChainFactory") PolicyChainFactory policyChainFactory
    ) {
        return new OnResponsePlatformPolicyChainProvider(flowResolver, policyChainFactory, new FlowPolicyResolverFactory());
    }

    @Bean
    @Qualifier("debugFlowResolver")
    public FlowResolver flowResolver(@Qualifier("debugOrganizationManager") OrganizationManager organizationManager) {
        return new OrganizationFlowResolver(organizationManager);
    }

    @Bean
    @Qualifier("debugPlatformPolicyChainFactory")
    public PolicyChainFactory policyChainFactory(@Qualifier("debugPlatformPolicyManager") PlatformPolicyManager platformPolicyManager) {
        return new PolicyChainFactory(platformPolicyManager);
    }

    @Bean
    @Qualifier("debugOrganizationManager")
    public OrganizationManager organizationManager(
        @Qualifier("debugPlatformPolicyManager") PlatformPolicyManager policyManager,
        EventManager eventManager
    ) {
        return new DebugOrganizationManager(policyManager, eventManager);
    }

    @Bean
    @Qualifier("debugPlatformPolicyManager")
    public PlatformPolicyManager platformPolicyManager(
        @Qualifier("debugPolicyFactoryCreator") io.gravitee.gateway.policy.PolicyFactoryCreator factory,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ResourceLifecycleManager resourceLifecycleManager,
        ComponentProvider componentProvider
    ) {
        final ApplicationContext contextParent = applicationContext.getParent();
        String[] beanNamesForType = contextParent.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> cpm = (ConfigurablePluginManager<PolicyPlugin<?>>) contextParent.getBean(
            beanNamesForType[0]
        );

        return new PlatformPolicyManager(
            configuration.getProperty("classloader.legacy.enabled", Boolean.class, false),
            contextParent.getBean(DefaultClassLoader.class),
            factory.create(),
            policyConfigurationFactory,
            cpm,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
    }
}
