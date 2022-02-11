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
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContextFactory;
import io.gravitee.gateway.handlers.api.ApiContextHandlerFactory;
import io.gravitee.gateway.handlers.api.ApiReactorHandler;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyFactoryCreator;
import io.gravitee.gateway.reactor.handler.context.ExecutionContextFactory;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiContextHandlerFactory extends ApiContextHandlerFactory {

    public DebugApiContextHandlerFactory(
        ApplicationContext applicationContext,
        Configuration configuration,
        Node node,
        PolicyFactoryCreator policyFactoryCreator,
        PolicyChainProviderLoader policyChainProviderLoader
    ) {
        super(applicationContext, configuration, node, policyFactoryCreator, policyChainProviderLoader);
    }

    @Override
    protected ApiReactorHandler getApiReactorHandler(Api api) {
        return new DebugApiReactorHandler(api);
    }

    @Override
    protected ExecutionContextFactory executionContextFactory(ComponentProvider componentProvider) {
        return new DebugExecutionContextFactory(componentProvider);
    }
}
