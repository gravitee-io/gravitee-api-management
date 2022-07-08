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

import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.factory.spring.SpringFactoriesEndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.DefaultGroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.ProxyEndpointResolver;
import io.gravitee.gateway.core.invoker.InvokerFactory;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.context.ApiTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.OnErrorProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.handlers.api.security.PlanBasedAuthenticationHandlerEnhancer;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.gateway.reactor.handler.context.ExecutionContextFactory;
import io.gravitee.gateway.reactor.handler.context.TemplateVariableProviderFactory;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import io.gravitee.gateway.security.core.AuthenticationHandlerEnhancer;
import io.gravitee.gateway.security.core.AuthenticationHandlerManager;
import io.gravitee.gateway.security.core.AuthenticationHandlerSelector;
import io.gravitee.gateway.security.core.DefaultAuthenticationHandlerSelector;
import io.gravitee.gateway.security.core.SecurityProviderLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ApiHandlerConfiguration {

    @Bean
    public PolicyChainFactory policyChainFactory(PolicyManager policyManager) {
        return new PolicyChainFactory(policyManager);
    }

    @Bean
    public ReactorHandler apiReactorHandler(Api api) {
        return new ApiReactorHandler();
    }

    @Bean
    public PolicyManager policyManager(PolicyFactory factory) {
        return new ApiPolicyManager(factory);
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
    public AuthenticationHandlerManager authenticationHandlerManager() {
        return new AuthenticationHandlerManager();
    }

    @Bean
    public AuthenticationHandlerEnhancer authenticationHandlerEnhancer(Api api) {
        return new PlanBasedAuthenticationHandlerEnhancer(api);
    }

    @Bean
    public AuthenticationHandlerSelector authenticationHandlerSelector() {
        return new DefaultAuthenticationHandlerSelector();
    }

    @Bean
    public ExecutionContextFactory executionContextFactory() {
        return new ExecutionContextFactory();
    }

    @Bean
    public TemplateVariableProviderFactory templateVariableProviderFactory() {
        return new ApiTemplateVariableProviderFactory();
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
    public GroupLifecycleManager groupLifecyleManager() {
        return new DefaultGroupLifecycleManager();
    }

    @Bean
    public EndpointResolver endpointResolver() {
        return new ProxyEndpointResolver();
    }

    @Bean
    public EndpointFactory endpointFactory() {
        return new SpringFactoriesEndpointFactory();
    }

    @Bean
    public ApiTemplateVariableProvider apiTemplateVariableProvider() {
        return new ApiTemplateVariableProvider();
    }

    @Bean
    public RequestProcessorChainFactory requestProcessorChainFactory() {
        return new RequestProcessorChainFactory();
    }

    @Bean
    public ResponseProcessorChainFactory responseProcessorChainFactory() {
        return new ResponseProcessorChainFactory();
    }

    @Bean
    public OnErrorProcessorChainFactory errorProcessorChainFactory() {
        return new OnErrorProcessorChainFactory();
    }
}
