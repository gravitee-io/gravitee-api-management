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
package io.gravitee.gateway.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.debug.handlers.api.DebugApiReactorHandlerFactory;
import io.gravitee.gateway.debug.platform.manager.DebugOrganizationManager;
import io.gravitee.gateway.debug.policy.impl.PolicyDebugDecoratorFactoryCreator;
import io.gravitee.gateway.debug.reactor.DebugReactor;
import io.gravitee.gateway.debug.reactor.processor.DebugResponseProcessorChainFactory;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.debug.vertx.VertxDebugService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.flow.FlowResolver;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.OrganizationFlowResolver;
import io.gravitee.gateway.platform.PlatformPolicyManager;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.platform.providers.OnRequestPlatformPolicyChainProvider;
import io.gravitee.gateway.platform.providers.OnResponsePlatformPolicyChainProvider;
import io.gravitee.gateway.policy.ConfigurablePolicyChainProvider;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.impl.PolicyFactoryCreatorImpl;
import io.gravitee.gateway.reactive.debug.DebugReactorEventListener;
import io.gravitee.gateway.reactive.debug.policy.DebugPolicyChainFactory;
import io.gravitee.gateway.reactive.debug.policy.condition.DebugExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.debug.reactor.DebugHttpRequestDispatcher;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.policy.DefaultPolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.gateway.reactive.reactor.handler.DefaultHttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.handler.AcceptorResolver;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.impl.DefaultAcceptorResolver;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.repository.management.api.EventRepository;
import io.vertx.core.Vertx;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

@Configuration
public class DebugConfiguration {

    private final ApplicationContext applicationContext;

    private final Node node;

    private final io.gravitee.node.api.configuration.Configuration configuration;

    public DebugConfiguration(
        ApplicationContext applicationContext,
        Node node,
        io.gravitee.node.api.configuration.Configuration configuration
    ) {
        this.applicationContext = applicationContext;
        this.node = node;
        this.configuration = configuration;
    }

    /*******************
     *  V3 Beans
     ******************/

    @Bean
    public io.gravitee.gateway.policy.PolicyFactoryCreator debugV3PolicyFactoryCreator(final PolicyPluginFactory policyPluginFactory) {
        return new PolicyDebugDecoratorFactoryCreator(
            new PolicyFactoryCreatorImpl(configuration, policyPluginFactory, new ExpressionLanguageStringConditionEvaluator())
        );
    }

    @Bean
    public AcceptorResolver debugV3EntrypointResolver(
        @Qualifier("debugReactorHandlerRegistry") ReactorHandlerRegistry reactorHandlerRegistry
    ) {
        return new DefaultAcceptorResolver(reactorHandlerRegistry);
    }

    @Bean
    public Reactor debugReactor(
        final @Qualifier("debugV3EntrypointResolver") AcceptorResolver acceptorResolver,
        final GatewayConfiguration gatewayConfiguration,
        final @Qualifier("v3RequestProcessorChainFactory") RequestProcessorChainFactory requestProcessorChainFactory,
        final @Qualifier("debugV3ResponseProcessorChainFactory") ResponseProcessorChainFactory responseProcessorChainFactory,
        final @Qualifier(
            "v3NotFoundProcessorChainFactory"
        ) io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory notFoundProcessorChainFactory
    ) {
        return new DebugReactor(
            acceptorResolver,
            gatewayConfiguration,
            requestProcessorChainFactory,
            responseProcessorChainFactory,
            notFoundProcessorChainFactory
        );
    }

    @Bean
    public PolicyChainProviderLoader debugV3PolicyChainProviderLoader(
        @Qualifier("debugV3ConfigurablePolicyChainProvider") List<ConfigurablePolicyChainProvider> providers
    ) {
        return new PolicyChainProviderLoader(providers);
    }

    @Bean
    @Qualifier("debugV3ConfigurablePolicyChainProvider")
    public OnRequestPlatformPolicyChainProvider debugV3OnRequestPlatformPolicyChainProvider(
        @Qualifier("debugV3FlowResolver") FlowResolver flowResolver,
        @Qualifier("debugV3PlatformPolicyChainFactory") PolicyChainFactory policyChainFactory
    ) {
        return new OnRequestPlatformPolicyChainProvider(flowResolver, policyChainFactory, new FlowPolicyResolverFactory());
    }

    @Bean
    @Qualifier("debugV3ConfigurablePolicyChainProvider")
    public OnResponsePlatformPolicyChainProvider debugV3OnResponsePlatformPolicyChainProvider(
        @Qualifier("debugV3FlowResolver") FlowResolver flowResolver,
        @Qualifier("debugV3PlatformPolicyChainFactory") PolicyChainFactory policyChainFactory
    ) {
        return new OnResponsePlatformPolicyChainProvider(flowResolver, policyChainFactory, new FlowPolicyResolverFactory());
    }

    @Bean
    public PlatformPolicyManager debugV3PlatformPolicyManager(
        @Qualifier("debugV3PolicyFactoryCreator") io.gravitee.gateway.policy.PolicyFactoryCreator factory,
        PolicyConfigurationFactory policyConfigurationFactory,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ResourceLifecycleManager resourceLifecycleManager,
        ComponentProvider componentProvider
    ) {
        final ApplicationContext contextParent = applicationContext.getParent();
        String[] beanNamesForType = contextParent.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> cpm = (ConfigurablePluginManager<PolicyPlugin<?>>) contextParent.getBean(
            beanNamesForType[0]
        );

        return new PlatformPolicyManager(
            configuration.getProperty("classloader.legacy.enabled", Boolean.class, false),
            contextParent.getBean(DefaultClassLoader.class),
            factory.create(),
            policyConfigurationFactory,
            cpm,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
    }

    @Bean
    public ResponseProcessorChainFactory debugV3ResponseProcessorChainFactory(EventRepository eventRepository, ObjectMapper objectMapper) {
        return new DebugResponseProcessorChainFactory(eventRepository, objectMapper);
    }

    @Bean
    public OrganizationManager debugV3OrganizationManager(
        @Qualifier("debugV3PlatformPolicyManager") PlatformPolicyManager policyManager,
        EventManager eventManager
    ) {
        return new DebugOrganizationManager(policyManager, eventManager);
    }

    @Bean
    public FlowResolver debugV3FlowResolver(@Qualifier("debugV3OrganizationManager") OrganizationManager organizationManager) {
        return new OrganizationFlowResolver(organizationManager);
    }

    @Bean
    public PolicyChainFactory debugV3PlatformPolicyChainFactory(
        @Qualifier("debugV3PlatformPolicyManager") PlatformPolicyManager platformPolicyManager
    ) {
        return new PolicyChainFactory(platformPolicyManager);
    }

    /*******************
     *  V4 emulation engine Beans
     ******************/

    @Bean
    public DebugReactorEventListener debugReactorEventListener(
        final io.vertx.rxjava3.core.Vertx vertx,
        final EventManager eventManager,
        final EventRepository eventRepository,
        final ObjectMapper objectMapper,
        final VertxDebugHttpClientConfiguration debugHttpClientConfiguration,
        @Qualifier("debugReactorHandlerRegistry") final ReactorHandlerRegistry reactorHandlerRegistry,
        OrganizationManager organizationManager,
        DataEncryptor dataEncryptor
    ) {
        return new DebugReactorEventListener(
            vertx,
            eventManager,
            eventRepository,
            objectMapper,
            debugHttpClientConfiguration,
            reactorHandlerRegistry,
            organizationManager,
            dataEncryptor
        );
    }

    @Bean
    public io.gravitee.gateway.reactive.policy.PolicyChainFactory debugPlatformPolicyChainFactory(
        io.gravitee.node.api.configuration.Configuration configuration,
        io.gravitee.gateway.reactive.platform.PlatformPolicyManager platformPolicyManager
    ) {
        return new DebugPolicyChainFactory("platform", platformPolicyManager, configuration);
    }

    @Bean
    public PolicyFactory debugPolicyFactory(final PolicyPluginFactory policyPluginFactory) {
        return new DefaultPolicyFactory(policyPluginFactory, new DebugExpressionLanguageConditionFilter());
    }

    @Bean
    public HttpRequestDispatcher debugHttpRequestDispatcher(
        GatewayConfiguration gatewayConfiguration,
        @Qualifier("debugHttpAcceptorResolver") HttpAcceptorResolver httpAcceptorResolver,
        IdGenerator idGenerator,
        ComponentProvider globalComponentProvider,
        RequestProcessorChainFactory v3RequestProcessorChainFactory,
        @Qualifier("debugV3ResponseProcessorChainFactory") ResponseProcessorChainFactory v3ResponseProcessorChainFactory,
        @Qualifier("debugPlatformProcessorChainFactory") DebugPlatformProcessorChainFactory debugPlatformProcessorChainFactory,
        NotFoundProcessorChainFactory notFoundProcessorChainFactory,
        @Value("${services.tracing.enabled:false}") boolean tracingEnabled,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        Vertx vertx
    ) {
        return new DebugHttpRequestDispatcher(
            gatewayConfiguration,
            httpAcceptorResolver,
            idGenerator,
            globalComponentProvider,
            v3RequestProcessorChainFactory,
            v3ResponseProcessorChainFactory,
            debugPlatformProcessorChainFactory,
            notFoundProcessorChainFactory,
            tracingEnabled,
            requestTimeoutConfiguration,
            vertx
        );
    }

    @Bean
    public HttpAcceptorResolver debugHttpAcceptorResolver(
        @Qualifier("debugReactorHandlerRegistry") ReactorHandlerRegistry debugReactorHandlerRegistry
    ) {
        return new DefaultHttpAcceptorResolver(debugReactorHandlerRegistry);
    }

    @Bean
    public DebugPlatformProcessorChainFactory debugPlatformProcessorChainFactory(
        TransactionPreProcessorFactory transactionHandlerFactory,
        @Value("${handlers.request.trace-context.enabled:false}") boolean traceContext,
        @Value("${handlers.request.x-forward.enabled:false}") boolean xForwardProcessor,
        ReporterService reporterService,
        AlertEventProducer eventProducer,
        Node node,
        @Value("${debug.http.port:8482}") String httpPort,
        @Value("${services.tracing.enabled:false}") boolean tracing,
        GatewayConfiguration gatewayConfiguration,
        EventRepository eventRepository,
        ObjectMapper objectMapper
    ) {
        return new DebugPlatformProcessorChainFactory(
            transactionHandlerFactory,
            traceContext,
            xForwardProcessor,
            reporterService,
            eventProducer,
            node,
            httpPort,
            tracing,
            gatewayConfiguration,
            eventRepository,
            objectMapper
        );
    }

    /*******************
     *  Commons Beans
     ******************/

    @Bean
    public VertxDebugService vertxDebugService() {
        return new VertxDebugService();
    }

    @Bean
    public ReactorFactory<Api> debugReactorHandlerFactory(
        @Qualifier("debugV3PolicyFactoryCreator") io.gravitee.gateway.policy.PolicyFactoryCreator v3PolicyFactoryCreator,
        @Qualifier("debugPolicyFactory") PolicyFactory policyFactory,
        @Qualifier("debugPlatformPolicyChainFactory") io.gravitee.gateway.reactive.policy.PolicyChainFactory platformPolicyChainFactory,
        OrganizationManager organizationManager,
        @Qualifier("debugV3PolicyChainProviderLoader") PolicyChainProviderLoader policyChainProviderLoader,
        ApiProcessorChainFactory apiProcessorChainFactory,
        FlowResolverFactory flowResolverFactory,
        RequestTimeoutConfiguration requestTimeoutConfiguration
    ) {
        return new DebugApiReactorHandlerFactory(
            applicationContext.getParent(),
            configuration,
            node,
            v3PolicyFactoryCreator,
            policyFactory,
            platformPolicyChainFactory,
            organizationManager,
            policyChainProviderLoader,
            apiProcessorChainFactory,
            flowResolverFactory,
            requestTimeoutConfiguration
        );
    }

    @Bean
    public ReactorFactoryManager debugReactorHandlerFactoryManager(
        @Qualifier("debugReactorHandlerFactory") List<ReactorFactory> reactorFactories
    ) {
        return new ReactorFactoryManager(reactorFactories);
    }

    @Bean
    public ReactorHandlerRegistry debugReactorHandlerRegistry(
        @Qualifier("debugReactorHandlerFactoryManager") ReactorFactoryManager reactorFactoryManager
    ) {
        return new DefaultReactorHandlerRegistry(reactorFactoryManager);
    }
}
