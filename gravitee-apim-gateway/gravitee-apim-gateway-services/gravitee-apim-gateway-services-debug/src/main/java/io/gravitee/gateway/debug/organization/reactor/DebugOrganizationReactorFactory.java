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
package io.gravitee.gateway.debug.organization.reactor;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.reactive.debug.policy.DebugPolicyChainFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyManager;
import io.gravitee.gateway.reactive.platform.organization.reactor.DefaultOrganizationReactorFactory;
import io.gravitee.gateway.reactive.policy.DefaultPolicyChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DebugOrganizationReactorFactory extends DefaultOrganizationReactorFactory {

    public DebugOrganizationReactorFactory(
        final DefaultClassLoader defaultClassLoader,
        final ApplicationContext applicationContext,
        final PolicyFactoryManager policyFactoryManager,
        final PolicyClassLoaderFactory policyClassLoaderFactory,
        final ComponentProvider componentProvider,
        final Configuration configuration
    ) {
        super(defaultClassLoader, applicationContext, policyFactoryManager, policyClassLoaderFactory, componentProvider, configuration);
    }

    protected DefaultPolicyChainFactory policyChainFactory(
        final ReactableOrganization reactableOrganization,
        final OrganizationPolicyManager organizationPolicyManager
    ) {
        return new DebugPolicyChainFactory("platform-" + reactableOrganization.getId(), organizationPolicyManager, configuration);
    }
}
