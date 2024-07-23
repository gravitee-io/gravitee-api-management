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
package io.gravitee.gateway.handlers.sharedpolicygroup.reactor.impl;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.SharedPolicyGroupPolicyManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.policy.DefaultSharedPolicyGroupPolicyChainFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactor;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactorFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultSharedPolicyGroupReactorFactory implements SharedPolicyGroupReactorFactory {

    private final DefaultClassLoader defaultClassLoader;
    private final ApplicationContext applicationContext;
    private final PolicyFactoryManager policyFactoryManager;
    private final PolicyClassLoaderFactory policyClassLoaderFactory;
    private final ComponentProvider componentProvider;
    private final io.gravitee.node.api.configuration.Configuration configuration;

    @Override
    public SharedPolicyGroupReactor create(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        SharedPolicyGroupPolicyManager sharedPolicyGroupPolicyManager = policyManager(reactableSharedPolicyGroup);
        DefaultSharedPolicyGroupPolicyChainFactory policyChainFactory = policyChainFactory(
            reactableSharedPolicyGroup,
            sharedPolicyGroupPolicyManager
        );
        return new DefaultSharedPolicyGroupReactor(reactableSharedPolicyGroup, policyChainFactory, sharedPolicyGroupPolicyManager);
    }

    protected DefaultSharedPolicyGroupPolicyChainFactory policyChainFactory(
        final ReactableSharedPolicyGroup reactableSharedPolicyGroup,
        final SharedPolicyGroupPolicyManager sharedPolicyGroupPolicyManager
    ) {
        return new DefaultSharedPolicyGroupPolicyChainFactory(
            "shared-policy-group-" + reactableSharedPolicyGroup.getId() + "-" + reactableSharedPolicyGroup.getEnvironmentId(),
            sharedPolicyGroupPolicyManager,
            configuration
        );
    }

    protected SharedPolicyGroupPolicyManager policyManager(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> configurablePluginManager =
            (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(beanNamesForType[0]);

        return new SharedPolicyGroupPolicyManager(
            defaultClassLoader,
            policyFactoryManager,
            new CachedPolicyConfigurationFactory(),
            configurablePluginManager,
            policyClassLoaderFactory,
            componentProvider,
            reactableSharedPolicyGroup
        );
    }
}
