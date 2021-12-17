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
package io.gravitee.gateway.handlers.api;

import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.impl.DefaultPolicyManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPolicyManager extends DefaultPolicyManager {

    private final Reactable reactable;

    public ApiPolicyManager(
        final boolean legacyMode,
        final DefaultClassLoader classLoader,
        final Reactable reactable,
        final PolicyFactory policyFactory,
        final PolicyConfigurationFactory policyConfigurationFactory,
        final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        final PolicyClassLoaderFactory policyClassLoaderFactory,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ComponentProvider componentProvider
    ) {
        super(
            legacyMode,
            classLoader,
            policyFactory,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
        this.reactable = reactable;
    }

    @Override
    protected Set<Policy> dependencies() {
        return reactable.dependencies(Policy.class);
    }
}
