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
package io.gravitee.gateway.handlers.sharedpolicygroup;

import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.policy.AbstractPolicyManager;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.Set;

public class SharedPolicyGroupPolicyManager extends AbstractPolicyManager {

    private final ReactableSharedPolicyGroup reactableSharedPolicyGroup;

    public SharedPolicyGroupPolicyManager(
        DefaultClassLoader classLoader,
        PolicyFactoryManager policyFactoryManager,
        PolicyConfigurationFactory policyConfigurationFactory,
        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        ReactableSharedPolicyGroup reactableSharedPolicyGroup
    ) {
        super(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider
        );
        this.reactableSharedPolicyGroup = reactableSharedPolicyGroup;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    public HttpPolicy create(ExecutionPhase executionPhase, PolicyMetadata policyMetadata) {
        return super.create(executionPhase, policyMetadata);
    }

    @Override
    protected Set<Policy> dependencies() {
        return reactableSharedPolicyGroup.dependencies(Policy.class);
    }
}
