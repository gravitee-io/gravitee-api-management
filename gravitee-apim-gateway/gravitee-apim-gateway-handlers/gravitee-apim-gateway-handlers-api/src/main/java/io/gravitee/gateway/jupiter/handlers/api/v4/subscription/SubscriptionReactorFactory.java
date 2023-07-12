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
package io.gravitee.gateway.jupiter.handlers.api.v4.subscription;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.jupiter.handlers.api.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.handlers.api.v4.AsyncReactorFactory;
import io.gravitee.gateway.jupiter.policy.PolicyChainFactory;
import io.gravitee.gateway.jupiter.policy.PolicyFactory;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.resource.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * A concrete implementation of {@link io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorFactory} specifically
 * used for managing subscription type of {@link Api}.
 *
 * This factory will be only used for v4 {@link Api} which are containing at least one {@link io.gravitee.definition.model.v4.listener.Listener}
 * with the <code>ListenerType.SUBSCRIPTION</code> type.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionReactorFactory extends AsyncReactorFactory {

    private final Logger logger = LoggerFactory.getLogger(SubscriptionReactorFactory.class);

    public SubscriptionReactorFactory(
        ApplicationContext applicationContext,
        Configuration configuration,
        PolicyFactory policyFactory,
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        EndpointConnectorPluginManager endpointConnectorPluginManager,
        PolicyChainFactory platformPolicyChainFactory,
        OrganizationManager organizationManager,
        FlowResolverFactory flowResolverFactory
    ) {
        super(
            applicationContext,
            configuration,
            policyFactory,
            entrypointConnectorPluginManager,
            endpointConnectorPluginManager,
            platformPolicyChainFactory,
            organizationManager,
            flowResolverFactory
        );
    }

    @Override
    public boolean support(Class<? extends Reactable> clazz) {
        return Api.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canCreate(Api api) {
        // Check that the API contains at least one subscription listener.
        return (
            api.getDefinitionVersion() == DefinitionVersion.V4 &&
            api.getDefinition().getType() == ApiType.ASYNC &&
            api.getDefinition().getListeners().stream().anyMatch(listener -> listener.getType() == ListenerType.SUBSCRIPTION)
        );
    }

    @Override
    public ReactorHandler create(Api api) {
        try {
            if (api.isEnabled()) {
                final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
                final CustomComponentProvider customComponentProvider = new CustomComponentProvider();
                customComponentProvider.add(Api.class, api);
                customComponentProvider.add(io.gravitee.definition.model.v4.Api.class, api.getDefinition());

                final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(
                    api,
                    applicationContext.getBean(ResourceClassLoaderFactory.class),
                    new ResourceConfigurationFactoryImpl(),
                    applicationContext
                );

                customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);
                customComponentProvider.add(Api.class, api);

                final CompositeComponentProvider apiComponentProvider = new CompositeComponentProvider(
                    customComponentProvider,
                    globalComponentProvider
                );

                return new SubscriptionReactor(
                    api,
                    apiComponentProvider,
                    entrypointConnectorPluginManager,
                    new EndpointInvoker(new DefaultEndpointConnectorResolver(api.getDefinition(), endpointConnectorPluginManager))
                );
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating AsyncApiReactor", ex);
        }
        return null;
    }
}
