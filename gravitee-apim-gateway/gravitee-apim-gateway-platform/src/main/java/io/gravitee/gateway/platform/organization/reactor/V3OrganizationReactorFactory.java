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
package io.gravitee.gateway.platform.organization.reactor;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.policy.OrganizationPolicyManager;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyFactoryCreator;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactor;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class V3OrganizationReactorFactory implements OrganizationReactorFactory {

    private final boolean classLoaderLegacyMode;
    private final DefaultClassLoader defaultClassLoader;
    private final ApplicationContext applicationContext;
    private final PolicyFactoryCreator factoryCreator;
    private final PolicyClassLoaderFactory policyClassLoaderFactory;
    private final ComponentProvider componentProvider;
    private final ResourceClassLoaderFactory resourceClassLoaderFactory;

    public OrganizationReactor create(final ReactableOrganization reactableOrganization) {
        ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(reactableOrganization);
        OrganizationPolicyManager organizationPolicyManager = platformPolicyManager(reactableOrganization, resourceLifecycleManager);
        PolicyChainFactory policyChainFactory = new PolicyChainFactory(organizationPolicyManager);
        return new V3OrganizationReactor(reactableOrganization, policyChainFactory, resourceLifecycleManager, organizationPolicyManager);
    }

    private OrganizationPolicyManager platformPolicyManager(
        final ReactableOrganization reactableOrganization,
        final ResourceLifecycleManager resourceLifecycleManager
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> configurablePluginManager = (ConfigurablePluginManager<
            PolicyPlugin<?>
        >) applicationContext.getBean(beanNamesForType[0]);

        PolicyFactory policyFactory = factoryCreator.create();
        CachedPolicyConfigurationFactory policyConfigurationFactory = new CachedPolicyConfigurationFactory();

        return new OrganizationPolicyManager(
            classLoaderLegacyMode,
            defaultClassLoader,
            policyFactory,
            policyConfigurationFactory,
            configurablePluginManager,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider,
            reactableOrganization
        );
    }

    private ResourceLifecycleManager resourceLifecycleManager(ReactableOrganization reactableOrganization) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> cpm = (ConfigurablePluginManager<ResourcePlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ResourceManagerImpl(
            classLoaderLegacyMode,
            applicationContext.getBean(DefaultClassLoader.class),
            reactableOrganization,
            cpm,
            resourceClassLoaderFactory,
            new ResourceConfigurationFactoryImpl(),
            applicationContext,
            deploymentContext()
        );
    }

    private DefaultDeploymentContext deploymentContext() {
        DefaultDeploymentContext defaultDeploymentContext = new DefaultDeploymentContext();
        defaultDeploymentContext.componentProvider(componentProvider);
        return defaultDeploymentContext;
    }
}
