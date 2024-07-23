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
package io.gravitee.gateway.reactive.platform.organization.reactor;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyManager;
import io.gravitee.gateway.reactive.policy.DefaultPolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
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
public class DefaultOrganizationReactorFactory implements OrganizationReactorFactory {

    protected final DefaultClassLoader defaultClassLoader;
    protected final ApplicationContext applicationContext;
    protected final PolicyFactoryManager policyFactoryManager;
    protected final PolicyClassLoaderFactory policyClassLoaderFactory;
    protected final ComponentProvider componentProvider;
    protected final io.gravitee.node.api.configuration.Configuration configuration;

    @Override
    public OrganizationReactor create(final ReactableOrganization reactableOrganization) {
        OrganizationPolicyManager organizationPolicyManager = platformPolicyManager(reactableOrganization);
        DefaultPolicyChainFactory policyChainFactory = policyChainFactory(reactableOrganization, organizationPolicyManager);
        return new DefaultOrganizationReactor(reactableOrganization, policyChainFactory, organizationPolicyManager);
    }

    protected DefaultPolicyChainFactory policyChainFactory(
        final ReactableOrganization reactableOrganization,
        final OrganizationPolicyManager organizationPolicyManager
    ) {
        return new DefaultPolicyChainFactory("organization-" + reactableOrganization.getId(), organizationPolicyManager, configuration);
    }

    protected OrganizationPolicyManager platformPolicyManager(ReactableOrganization reactableOrganization) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> configurablePluginManager =
            (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(beanNamesForType[0]);

        return new OrganizationPolicyManager(
            defaultClassLoader,
            policyFactoryManager,
            new CachedPolicyConfigurationFactory(),
            configurablePluginManager,
            policyClassLoaderFactory,
            componentProvider,
            reactableOrganization
        );
    }
}
