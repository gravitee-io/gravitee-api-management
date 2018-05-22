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

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.factory.spring.SpringFactoriesEndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecyleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.DefaultGroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.core.endpoint.resolver.impl.TargetEndpointResolver;
import io.gravitee.gateway.core.invoker.InvokerFactory;
import io.gravitee.gateway.handlers.api.context.ExecutionContextFactory;
import io.gravitee.gateway.handlers.api.context.TemplateVariableProviderFactory;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.handlers.api.path.impl.ApiPathResolverImpl;
import io.gravitee.gateway.handlers.api.policy.security.PlanBasedAuthenticationHandlerEnhancer;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.DefaultPolicyManager;
import io.gravitee.gateway.policy.impl.PolicyFactoryImpl;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import io.gravitee.gateway.security.core.AuthenticationHandlerEnhancer;
import io.gravitee.gateway.security.core.SecurityProviderLoader;
import io.gravitee.gateway.security.core.SecurityProviderManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ApiHandlerConfiguration {

    @Bean
    public PathResolver pathResolver(Api api) {
        return new ApiPathResolverImpl(api);
    }

    @Bean
    public ReactorHandler apiReactorHandler() {
        return new ApiReactorHandler();
    }

    @Bean
    public PolicyFactory policyFactory() {
        return new PolicyFactoryImpl();
    }

    @Bean
    public PolicyManager policyManager() {
        return new DefaultPolicyManager();
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

    @Bean
    public SecurityProviderLoader securityProviderLoader() {
        return new SecurityProviderLoader();
    }

    @Bean
    public SecurityProviderManager securityManager() {
        return new SecurityProviderManager();
    }

    @Bean
    public AuthenticationHandlerEnhancer securityProviderFilter() {
        return new PlanBasedAuthenticationHandlerEnhancer();
    }

    @Bean
    public ExecutionContextFactory executionContextFactory() {
        return new ExecutionContextFactory();
    }

    @Bean
    public TemplateVariableProviderFactory templateVariableProviderFactory() {
        return new TemplateVariableProviderFactory();
    }

    @Bean
    public InvokerFactory httpInvokerFactory() {
        return new InvokerFactory();
    }

    @Bean
    public ReferenceRegister referenceRegister() {
        return new DefaultReferenceRegister();
    }
    @Bean
    public GroupLifecyleManager groupLifecyleManager() {
        return new DefaultGroupLifecycleManager();
    }

    @Bean
    public EndpointResolver endpointResolver() {
        return new TargetEndpointResolver();
    }

    @Bean
    public EndpointFactory endpointFactory() {
        return new SpringFactoriesEndpointFactory();
    }
}
