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
package io.gravitee.gateway.handlers.api.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.handlers.api.manager.impl.ApiProductManagerImpl;
import io.gravitee.gateway.handlers.api.manager.impl.DefaultApiProductResourceManagerFactory;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.registry.impl.ApiProductRegistryImpl;
import io.gravitee.gateway.handlers.api.secrets.ApiProductDefinitionSecretRefsFinder;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.v4.flow.BestMatchFlowSelector;
import io.gravitee.gateway.reactive.v4.flow.selection.ConditionSelectorConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.selection.HttpSelectorConditionFilter;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ApiProductConfiguration {

    @Bean
    public DefinitionSecretRefsFinder<?> apiProductDefinitionSecretRefsFinder() {
        return new ApiProductDefinitionSecretRefsFinder();
    }

    @Bean
    public ApiProductRegistry apiProductRegistry(GatewayConfiguration gatewayConfiguration) {
        return new ApiProductRegistryImpl(gatewayConfiguration);
    }

    @Bean
    public ApiProductResourceManagerFactory apiProductResourceManagerFactory(ApplicationContext applicationContext) {
        return new DefaultApiProductResourceManagerFactory(applicationContext);
    }

    @Bean(destroyMethod = "clear")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ApiProductRuntimeRegistry apiProductRuntimeRegistry(
        ApplicationContext applicationContext,
        @Value("${api.pending_requests_timeout:10000}") long drainTimeoutMs,
        DefaultClassLoader classLoader,
        PolicyFactoryManager policyFactoryManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        GatewayConfiguration gatewayConfiguration,
        ApiProductResourceManagerFactory apiProductResourceManagerFactory,
        LicenseManager licenseManager
    ) {
        FlowResolverFactory flowResolverFactory = new FlowResolverFactory(
            new CompositeConditionFilter(new HttpSelectorConditionFilter()),
            new ConditionSelectorConditionFilter(),
            new BestMatchFlowSelector()
        );
        ApiProductRuntimeFactory runtimeFactory = new ApiProductRuntimeFactory(
            classLoader,
            policyFactoryManager,
            ApiProductRuntimeFactory.policyPluginManager(applicationContext),
            policyClassLoaderFactory,
            componentProvider,
            flowResolverFactory,
            gatewayConfiguration,
            apiProductResourceManagerFactory,
            licenseManager
        );
        return new ApiProductRuntimeRegistry(runtimeFactory, drainTimeoutMs, Schedulers.io());
    }

    @Bean
    public ApiProductManager apiProductManager(
        ApiProductRegistry apiProductRegistry,
        ApiProductRuntimeRegistry apiProductRuntimeRegistry,
        EventManager eventManager,
        LicenseManager licenseManager,
        GatewayConfiguration gatewayConfiguration
    ) {
        return new ApiProductManagerImpl(apiProductRegistry, apiProductRuntimeRegistry, eventManager, licenseManager, gatewayConfiguration);
    }
}
