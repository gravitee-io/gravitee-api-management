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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.impl.legacy.LegacyPolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class DefaultPolicyManager extends LegacyPolicyManager {

    private final Logger logger = LoggerFactory.getLogger(DefaultPolicyManager.class);

    private final boolean legacyMode;
    private final PolicyLoader policyLoader;

    public DefaultPolicyManager(
        final boolean legacyMode,
        final DefaultClassLoader classLoader,
        final PolicyFactory policyFactory,
        final PolicyConfigurationFactory policyConfigurationFactory,
        final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        final PolicyClassLoaderFactory policyClassLoaderFactory,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ComponentProvider componentProvider
    ) {
        super(
            classLoader,
            policyFactory,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
        this.legacyMode = legacyMode;
        policyLoader = new PolicyLoader(classLoader, policyPluginManager, policyClassLoaderFactory, componentProvider);
    }

    @Override
    protected void initialize() {
        if (legacyMode) {
            super.initialize();
        } else {
            policies.putAll(policyLoader.load(dependencies()));
        }
    }

    @Override
    protected void doStart() throws Exception {
        // Init required policies
        super.doStart();

        // Activate policy context
        policyLoader.activatePolicyContext(policies);
    }

    @Override
    protected void doStop() throws Exception {
        // Deactivate policy context
        policyLoader.disablePolicyContext(policies, policyFactory::cleanup);
        super.doStop();
    }
}
