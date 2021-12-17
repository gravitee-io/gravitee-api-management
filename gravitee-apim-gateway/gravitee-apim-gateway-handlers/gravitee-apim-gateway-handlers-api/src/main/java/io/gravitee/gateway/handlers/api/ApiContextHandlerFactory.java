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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.connector.ConnectorRegistry;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.core.endpoint.GroupManager;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.factory.impl.EndpointFactoryImpl;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.DefaultGroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.ProxyEndpointResolver;
import io.gravitee.gateway.core.invoker.InvokerFactory;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.context.ApiTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.policy.security.PlanBasedAuthenticationHandlerEnhancer;
import io.gravitee.gateway.handlers.api.processor.OnErrorProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.PolicyFactoryCreator;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactory;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.gateway.reactor.handler.context.ExecutionContextFactory;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.ResourceManagerImpl;
import io.gravitee.gateway.security.core.*;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import io.vertx.core.Vertx;
import java.net.URL;
import java.net.URLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiContextHandlerFactory implements ReactorHandlerFactory<Api> {

    private final Logger logger = LoggerFactory.getLogger(ApiContextHandlerFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${reporters.logging.max_size:-1}")
    private int maxSizeLogMessage;

    @Value("${reporters.logging.excluded_response_types:#{null}}")
    private String excludedResponseTypes;

    @Value("${handlers.request.headers.x-forwarded-prefix:false}")
    private boolean overrideXForwardedPrefix;

    @Value("${classloader.legacy.enabled:true}")
    private boolean classLoaderLegacyMode;

    @Autowired
    private Node node;

    @Override
    public ReactorHandler create(Api api) {
        try {
            if (api.isEnabled()) {
                ReactorHandlerClassLoader parentClassLoader = new ReactorHandlerClassLoader(
                    ApiContextHandlerFactory.class.getClassLoader()
                );

                Class<?> handlerClass = parentClassLoader.loadClass(ApiReactorHandler.class.getName());

                final ApiReactorHandler handler = (ApiReactorHandler) handlerClass.getConstructor(Api.class).newInstance(api);
                final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
                final CustomComponentProvider customComponentProvider = new CustomComponentProvider();

                final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(
                    api,
                    applicationContext.getBean(ResourceClassLoaderFactory.class),
                    resourceConfigurationFactory(),
                    applicationContext
                );

                customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);
                customComponentProvider.add(io.gravitee.definition.model.Api.class, api);

                final CompositeComponentProvider apiComponentProvider = new CompositeComponentProvider(
                    customComponentProvider,
                    globalComponentProvider
                );

                // Force creation of a dedicated PolicyFactory for each api as it may involve cache we want to be released when api is undeployed.
                final PolicyFactory policyFactory = applicationContext.getBean(PolicyFactoryCreator.class).getObject();

                final PolicyManager policyManager = policyManager(
                    api,
                    policyFactory,
                    policyConfigurationFactory(),
                    applicationContext.getBean(PolicyClassLoaderFactory.class),
                    resourceLifecycleManager,
                    apiComponentProvider
                );

                final PolicyChainFactory policyChainFactory = policyChainFactory(policyManager);
                final RequestProcessorChainFactory requestProcessorChainFactory = requestProcessorChainFactory(
                    api,
                    policyChainFactory,
                    policyManager,
                    applicationContext.getBean(PolicyChainProviderLoader.class),
                    authenticationHandlerSelector(
                        authenticationHandlerManager(securityProviderLoader(), authenticationHandlerEnhancer(api), apiComponentProvider)
                    )
                );

                final DefaultReferenceRegister referenceRegister = referenceRegister();

                handler.setRequestProcessorChain(requestProcessorChainFactory);
                handler.setResponseProcessorChain(
                    responseProcessorChainFactory(api, policyChainFactory, applicationContext.getBean(PolicyChainProviderLoader.class))
                );
                handler.setErrorProcessorChain(errorProcessorChainFactory(api, policyChainFactory));

                final GroupLifecycleManager groupLifecycleManager = groupLifecyleManager(
                    api,
                    referenceRegister,
                    new EndpointFactoryImpl(),
                    applicationContext.getBean(GatewayConfiguration.class),
                    applicationContext.getBean(ConnectorRegistry.class),
                    applicationContext.getBean(Configuration.class),
                    applicationContext.getBean(ObjectMapper.class)
                );

                handler.setInvoker(
                    invokerFactory(api, applicationContext.getBean(Vertx.class), endpointResolver(referenceRegister, groupLifecycleManager))
                        .create()
                );

                handler.setPolicyManager(policyManager);
                handler.setGroupLifecycleManager(groupLifecycleManager);
                handler.setResourceLifecycleManager(resourceLifecycleManager);

                ExecutionContextFactory executionContextFactory = executionContextFactory(apiComponentProvider);

                executionContextFactory.addTemplateVariableProvider(new ApiTemplateVariableProvider(api));
                executionContextFactory.addTemplateVariableProvider(referenceRegister);
                applicationContext
                    .getBean(ApiTemplateVariableProviderFactory.class)
                    .getTemplateVariableProviders()
                    .forEach(executionContextFactory::addTemplateVariableProvider);

                handler.setExecutionContextFactory(executionContextFactory);

                return handler;
            } else {
                logger.warn("Api is disabled !");
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating API handler", ex);
        }

        return null;
    }

    public PolicyChainFactory policyChainFactory(PolicyManager policyManager) {
        return new PolicyChainFactory(policyManager);
    }

    public PolicyManager policyManager(
        Api api,
        PolicyFactory factory,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ResourceLifecycleManager resourceLifecycleManager,
        ComponentProvider componentProvider
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> ppm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ApiPolicyManager(
            classLoaderLegacyMode,
            applicationContext.getBean(DefaultClassLoader.class),
            api,
            factory,
            policyConfigurationFactory,
            ppm,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
    }

    public PolicyConfigurationFactory policyConfigurationFactory() {
        return new CachedPolicyConfigurationFactory();
    }

    public ResourceLifecycleManager resourceLifecycleManager(
        Api api,
        ResourceClassLoaderFactory resourceClassLoaderFactory,
        ResourceConfigurationFactory resourceConfigurationFactory,
        ApplicationContext applicationContext
    ) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> cpm = (ConfigurablePluginManager<ResourcePlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new ResourceManagerImpl(
            classLoaderLegacyMode,
            applicationContext.getBean(DefaultClassLoader.class),
            api,
            cpm,
            resourceClassLoaderFactory,
            resourceConfigurationFactory,
            applicationContext
        );
    }

    public ResourceConfigurationFactory resourceConfigurationFactory() {
        return new ResourceConfigurationFactoryImpl();
    }

    public SecurityProviderLoader securityProviderLoader() {
        return new SecurityProviderLoader();
    }

    public AuthenticationHandlerManager authenticationHandlerManager(
        SecurityProviderLoader securityProviderLoader,
        AuthenticationHandlerEnhancer authenticationHandlerEnhancer,
        ComponentProvider componentProvider
    ) {
        AuthenticationHandlerManager authenticationHandlerManager = new AuthenticationHandlerManager(
            securityProviderLoader,
            componentProvider
        );
        authenticationHandlerManager.setAuthenticationHandlerEnhancer(authenticationHandlerEnhancer);
        authenticationHandlerManager.afterPropertiesSet();
        return authenticationHandlerManager;
    }

    public AuthenticationHandlerEnhancer authenticationHandlerEnhancer(Api api) {
        return new PlanBasedAuthenticationHandlerEnhancer(api);
    }

    public AuthenticationHandlerSelector authenticationHandlerSelector(AuthenticationHandlerManager authenticationHandlerManager) {
        return new DefaultAuthenticationHandlerSelector(authenticationHandlerManager);
    }

    public ExecutionContextFactory executionContextFactory(ComponentProvider componentProvider) {
        return new ExecutionContextFactory(componentProvider);
    }

    public InvokerFactory invokerFactory(Api api, Vertx vertx, EndpointResolver endpointResolver) {
        return new InvokerFactory(api, vertx, endpointResolver);
    }

    public DefaultReferenceRegister referenceRegister() {
        return new DefaultReferenceRegister();
    }

    public GroupLifecycleManager groupLifecyleManager(
        Api api,
        ReferenceRegister referenceRegister,
        EndpointFactory endpointFactory,
        GatewayConfiguration gatewayConfiguration,
        ConnectorRegistry connectorRegistry,
        Configuration configuration,
        ObjectMapper mapper
    ) {
        return new DefaultGroupLifecycleManager(
            api,
            referenceRegister,
            endpointFactory,
            connectorRegistry,
            configuration,
            mapper,
            gatewayConfiguration.tenant()
        );
    }

    public EndpointResolver endpointResolver(ReferenceRegister referenceRegister, GroupManager groupManager) {
        return new ProxyEndpointResolver(referenceRegister, groupManager);
    }

    public RequestProcessorChainFactory requestProcessorChainFactory(
        Api api,
        PolicyChainFactory policyChainFactory,
        PolicyManager policyManager,
        PolicyChainProviderLoader policyChainProviderLoader,
        AuthenticationHandlerSelector authenticationHandlerSelector
    ) {
        RequestProcessorChainFactory.RequestProcessorChainFactoryOptions options = new RequestProcessorChainFactory.RequestProcessorChainFactoryOptions();
        options.setMaxSizeLogMessage(maxSizeLogMessage);
        options.setOverrideXForwardedPrefix(overrideXForwardedPrefix);
        options.setExcludedResponseTypes(excludedResponseTypes);

        return new RequestProcessorChainFactory(
            api,
            policyChainFactory,
            policyManager,
            options,
            policyChainProviderLoader,
            authenticationHandlerSelector
        );
    }

    public ResponseProcessorChainFactory responseProcessorChainFactory(
        Api api,
        PolicyChainFactory policyChainFactory,
        PolicyChainProviderLoader policyChainProviderLoader
    ) {
        return new ResponseProcessorChainFactory(api, policyChainFactory, policyChainProviderLoader, node);
    }

    public OnErrorProcessorChainFactory errorProcessorChainFactory(Api api, PolicyChainFactory policyChainFactory) {
        return new OnErrorProcessorChainFactory(api, policyChainFactory);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private static class ReactorHandlerClassLoader extends URLClassLoader {

        public ReactorHandlerClassLoader(ClassLoader parent) {
            super(new URL[] {}, parent);
        }
    }
}
