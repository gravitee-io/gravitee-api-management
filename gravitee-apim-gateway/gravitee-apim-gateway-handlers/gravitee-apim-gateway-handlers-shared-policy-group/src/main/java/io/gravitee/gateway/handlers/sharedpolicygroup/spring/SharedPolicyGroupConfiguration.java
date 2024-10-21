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
package io.gravitee.gateway.handlers.sharedpolicygroup.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.handlers.sharedpolicygroup.event.SharedPolicyGroupEventListener;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.impl.SharedPolicyGroupManagerImpl;
import io.gravitee.gateway.handlers.sharedpolicygroup.policy.SharedPolicyGroupPolicyFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.policy.plugin.SharedPolicyGroupPolicyPlugin;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactorFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactorFactoryManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.impl.DefaultSharedPolicyGroupReactorFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.registry.SharedPolicyGroupRegistry;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class SharedPolicyGroupConfiguration {

    @Autowired
    ApplicationContext applicationContext;

    @PostConstruct
    void registerSharedPolicyGroupPolicyPlugin() {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> ppm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        ppm.register(new SharedPolicyGroupPolicyPlugin());
    }

    @Bean
    public SharedPolicyGroupEventListener sharedPolicyGroupEventListener(
        EventManager eventManager,
        SharedPolicyGroupRegistry sharedPolicyGroupRegistry
    ) {
        return new SharedPolicyGroupEventListener(eventManager, sharedPolicyGroupRegistry);
    }

    @Bean
    public SharedPolicyGroupReactorFactory sharedPolicyGroupReactorFactory(
        DefaultClassLoader classLoader,
        ApplicationContext applicationContext,
        PolicyFactoryManager policyFactoryManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        OpenTelemetryConfiguration openTelemetryConfiguration
    ) {
        return new DefaultSharedPolicyGroupReactorFactory(
            classLoader,
            applicationContext,
            policyFactoryManager,
            policyClassLoaderFactory,
            componentProvider,
            openTelemetryConfiguration
        );
    }

    @Bean
    public SharedPolicyGroupRegistry sharedPolicyGroupRegistry(
        SharedPolicyGroupReactorFactoryManager sharedPolicyGroupReactorFactoryManager
    ) {
        return new SharedPolicyGroupRegistry(sharedPolicyGroupReactorFactoryManager);
    }

    @Bean
    public SharedPolicyGroupManager sharedPolicyGroupManager(EventManager eventManager, LicenseManager licenseManager) {
        return new SharedPolicyGroupManagerImpl(eventManager, licenseManager);
    }

    @Bean
    public PolicyFactory sharedPolicyGroupPolicyFactory(
        final io.gravitee.node.api.configuration.Configuration configuration,
        final PolicyPluginFactory policyPluginFactory
    ) {
        return new SharedPolicyGroupPolicyFactory(configuration, policyPluginFactory, new ExpressionLanguageConditionFilter<>());
    }

    @Bean
    public SharedPolicyGroupReactorFactoryManager sharedPolicyGroupReactorFactoryManager(
        List<SharedPolicyGroupReactorFactory> reactorFactories
    ) {
        return new SharedPolicyGroupReactorFactoryManager(reactorFactories);
    }
}
