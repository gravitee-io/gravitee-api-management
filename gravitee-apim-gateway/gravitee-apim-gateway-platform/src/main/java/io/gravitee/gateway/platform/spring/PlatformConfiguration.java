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
package io.gravitee.gateway.platform.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.platform.organization.event.OrganizationEventListener;
import io.gravitee.gateway.platform.organization.flow.OrganizationFlowResolver;
import io.gravitee.gateway.platform.organization.manager.DefaultOrganizationManager;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.platform.organization.policy.V3OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.platform.organization.providers.OnRequestPlatformPolicyChainProvider;
import io.gravitee.gateway.platform.organization.providers.OnResponsePlatformPolicyChainProvider;
import io.gravitee.gateway.platform.organization.reactor.V3OrganizationReactorFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.DefaultPlatformPolicyChainFactoryManager;
import io.gravitee.gateway.reactive.platform.organization.reactor.DefaultOrganizationReactorFactory;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorFactory;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorRegistry;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public OrganizationManager organizationManager(EventManager eventManager) {
        return new DefaultOrganizationManager(eventManager);
    }

    /*
     * Platform V3 engine
     */
    @Bean
    public OrganizationReactorFactory v3OrganizationReactorFactory(
        DefaultClassLoader classLoader,
        io.gravitee.gateway.policy.PolicyFactoryCreator factoryCreator,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        ResourceClassLoaderFactory resourceClassLoaderFactory
    ) {
        return new V3OrganizationReactorFactory(
            classLoaderLegacyMode,
            classLoader,
            applicationContext,
            factoryCreator,
            policyClassLoaderFactory,
            componentProvider,
            resourceClassLoaderFactory
        );
    }

    @Bean
    public OrganizationReactorRegistry v3OrganizationReactorRegistry(
        @Qualifier("v3OrganizationReactorFactory") OrganizationReactorFactory organizationReactorFactory
    ) {
        return new OrganizationReactorRegistry(organizationReactorFactory);
    }

    @Bean
    public OrganizationPolicyChainFactoryManager v3PlatformPolicyChainFactoryManager(
        @Qualifier("v3OrganizationReactorRegistry") OrganizationReactorRegistry organizationReactorRegistry
    ) {
        return new V3OrganizationPolicyChainFactoryManager(organizationReactorRegistry);
    }

    @Bean
    public OrganizationFlowResolver organizationFlowResolver(OrganizationManager organizationManager) {
        return new OrganizationFlowResolver(organizationManager);
    }

    @Bean
    public OnRequestPlatformPolicyChainProvider onRequestPlatformPolicyChainProvider(
        OrganizationFlowResolver organizationFlowResolver,
        OrganizationPolicyChainFactoryManager v3OrganizationPolicyChainFactoryManager
    ) {
        return new OnRequestPlatformPolicyChainProvider(
            organizationFlowResolver,
            v3OrganizationPolicyChainFactoryManager,
            new FlowPolicyResolverFactory()
        );
    }

    @Bean
    public OnResponsePlatformPolicyChainProvider onResponsePlatformPolicyChainProvider(
        OrganizationFlowResolver organizationFlowResolver,
        OrganizationPolicyChainFactoryManager v3OrganizationPolicyChainFactoryManager
    ) {
        return new OnResponsePlatformPolicyChainProvider(
            organizationFlowResolver,
            v3OrganizationPolicyChainFactoryManager,
            new FlowPolicyResolverFactory()
        );
    }

    /*
     * Platform Reactive engine
     */
    @Bean
    public OrganizationReactorFactory organizationReactorFactory(
        DefaultClassLoader classLoader,
        PolicyFactoryManager policyFactoryManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        OpenTelemetryConfiguration configuration
    ) {
        return new DefaultOrganizationReactorFactory(
            classLoader,
            applicationContext,
            policyFactoryManager,
            policyClassLoaderFactory,
            componentProvider,
            configuration
        );
    }

    @Bean
    public OrganizationReactorRegistry organizationReactorRegistry(
        @Qualifier("organizationReactorFactory") OrganizationReactorFactory organizationReactorFactory
    ) {
        return new OrganizationReactorRegistry(organizationReactorFactory);
    }

    @Bean
    public io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager platformPolicyChainFactoryManager(
        @Qualifier("organizationReactorRegistry") OrganizationReactorRegistry organizationReactorRegistry
    ) {
        return new DefaultPlatformPolicyChainFactoryManager(organizationReactorRegistry);
    }

    @Bean
    public OrganizationEventListener organizationEventListener(
        EventManager eventManager,
        @Qualifier("v3OrganizationReactorRegistry") OrganizationReactorRegistry v3OrganizationReactorRegistry,
        @Qualifier("organizationReactorRegistry") OrganizationReactorRegistry organizationReactorRegistry
    ) {
        return new OrganizationEventListener(eventManager, v3OrganizationReactorRegistry, organizationReactorRegistry);
    }
}
