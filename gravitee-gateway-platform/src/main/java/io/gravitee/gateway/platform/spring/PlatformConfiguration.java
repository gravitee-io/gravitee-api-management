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
package io.gravitee.gateway.platform.spring;

import io.gravitee.gateway.flow.FlowResolver;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.platform.OrganizationFlowResolver;
import io.gravitee.gateway.platform.PlatformPolicyManager;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.platform.manager.impl.OrganizationManagerImpl;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class PlatformConfiguration {

    @Bean
    public OrganizationManager organizationManager(PlatformPolicyManager policyManager) {
        return new OrganizationManagerImpl(policyManager);
    }

    @Bean
    public FlowResolver flowResolver(OrganizationManager organizationManager) {
        return new OrganizationFlowResolver(organizationManager);
    }

    @Bean
    public PolicyChainFactory policyChainFactory(PlatformPolicyManager platformPolicyManager) {
        return new PolicyChainFactory(platformPolicyManager);
    }

    @Bean
    public PlatformPolicyManager platformPolicyManager(PolicyFactory factory) {
        return new PlatformPolicyManager(factory);
    }

    @Bean
    public PolicyConfigurationFactory policyConfigurationFactory() {
        return new CachedPolicyConfigurationFactory();
    }

    @Bean
    public ResourceLifecycleManager resourceLifecycleManager() {
        return new ResourceManagerImpl();
    }

    @Bean
    public ResourceConfigurationFactory resourceConfigurationFactory() {
        return new ResourceConfigurationFactoryImpl();
    }
}
