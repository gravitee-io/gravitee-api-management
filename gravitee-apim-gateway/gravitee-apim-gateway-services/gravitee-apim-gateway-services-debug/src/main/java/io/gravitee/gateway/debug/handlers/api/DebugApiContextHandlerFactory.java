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
package io.gravitee.gateway.debug.handlers.api;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContextFactory;
import io.gravitee.gateway.debug.security.core.DebugSecurityPolicyResolver;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.ApiReactorHandler;
import io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.jupiter.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.policy.PolicyFactory;
import io.gravitee.gateway.reactor.handler.context.ExecutionContextFactory;
import io.gravitee.gateway.security.core.AuthenticationHandlerSelector;
import io.gravitee.gateway.security.core.SecurityPolicyResolver;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiContextHandlerFactory extends ApiReactorHandlerFactory {

    public DebugApiContextHandlerFactory(
        ApplicationContext applicationContext,
        Configuration configuration,
        Node node,
        io.gravitee.gateway.policy.PolicyFactoryCreator v3PolicyFactoryCreator,
        PolicyFactory policyFactory,
        PolicyChainProviderLoader policyChainProviderLoader,
        ApiProcessorChainFactory apiProcessorChainFactory
    ) {
        super(
            applicationContext,
            configuration,
            node,
            v3PolicyFactoryCreator,
            policyFactory,
            policyChainProviderLoader,
            apiProcessorChainFactory
        );
    }

    @Override
    protected ApiReactorHandler getApiReactorHandler(Api api) {
        return new DebugApiReactorHandler(api);
    }

    @Override
    protected RequestProcessorChainFactory getRequestProcessorChainFactory(
        Api api,
        PolicyChainFactory policyChainFactory,
        PolicyManager policyManager,
        PolicyChainProviderLoader policyChainProviderLoader,
        AuthenticationHandlerSelector authenticationHandlerSelector,
        FlowPolicyResolverFactory flowPolicyResolverFactory,
        RequestProcessorChainFactory.RequestProcessorChainFactoryOptions options,
        SecurityPolicyResolver securityPolicyResolver
    ) {
        return super.getRequestProcessorChainFactory(
            api,
            policyChainFactory,
            policyManager,
            policyChainProviderLoader,
            authenticationHandlerSelector,
            flowPolicyResolverFactory,
            options,
            new DebugSecurityPolicyResolver(policyManager, authenticationHandlerSelector)
        );
    }

    @Override
    protected ExecutionContextFactory v3ExecutionContextFactory(
        Api api,
        ComponentProvider componentProvider,
        DefaultReferenceRegister referenceRegister
    ) {
        return new DebugExecutionContextFactory(componentProvider);
    }
}
