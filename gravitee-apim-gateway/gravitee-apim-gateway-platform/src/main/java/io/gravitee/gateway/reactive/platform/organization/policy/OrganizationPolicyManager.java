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
package io.gravitee.gateway.reactive.platform.organization.policy;

import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.reactive.policy.AbstractHttpPolicyManager;
import io.gravitee.gateway.reactive.policy.HttpPolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationPolicyManager extends AbstractHttpPolicyManager {

    private final ReactableOrganization reactableOrganization;

    public OrganizationPolicyManager(
        final DefaultClassLoader classLoader,
        final PolicyFactoryManager<HttpPolicyFactory> policyFactoryManager,
        final PolicyConfigurationFactory policyConfigurationFactory,
        final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        final PolicyClassLoaderFactory policyClassLoaderFactory,
        final ComponentProvider componentProvider,
        final ReactableOrganization reactableOrganization
    ) {
        super(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider
        );
        this.reactableOrganization = reactableOrganization;
    }

    @Override
    protected Set<Policy> dependencies() {
        return reactableOrganization.dependencies(Policy.class);
    }
}
