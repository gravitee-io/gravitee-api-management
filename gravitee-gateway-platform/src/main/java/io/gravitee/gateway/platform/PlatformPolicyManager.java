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
package io.gravitee.gateway.platform;

import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.DefaultPolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlatformPolicyManager extends DefaultPolicyManager {

    private final Logger logger = LoggerFactory.getLogger(PlatformPolicyManager.class);

    private Set<Policy> dependencies;

    public PlatformPolicyManager(
        final PolicyFactory policyFactory,
        final PolicyConfigurationFactory policyConfigurationFactory,
        final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        final PolicyClassLoaderFactory policyClassLoaderFactory,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ComponentProvider componentProvider
    ) {
        super(
            policyFactory,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
    }

    public void setDependencies(Set<Policy> dependencies) {
        this.dependencies = dependencies;
        this.restart();
    }

    private void restart() {
        try {
            logger.info("Restart policy manager");
            stop();
            start();
        } catch (Exception e) {
            logger.error("Impossible to restart platform policy manager", e);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        // TODO: how /when to clear cache
        // policyFactory.clearCache();
        if (policyConfigurationFactory instanceof CachedPolicyConfigurationFactory) {
            ((CachedPolicyConfigurationFactory) policyConfigurationFactory).clear();
        }
    }

    @Override
    public Set<Policy> dependencies() {
        return dependencies;
    }
}
