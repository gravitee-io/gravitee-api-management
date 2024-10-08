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
import io.gravitee.gateway.debug.organization.event.DebugOrganizationEventListener;
import io.gravitee.gateway.debug.organization.reactor.DebugOrganizationReactorFactory;
import io.gravitee.gateway.debug.policy.impl.PolicyDebugDecoratorFactoryCreator;
import io.gravitee.gateway.debug.reactor.DebugReactor;
import io.gravitee.gateway.debug.reactor.processor.DebugResponseProcessorChainFactory;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.debug.vertx.VertxDebugService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.organization.flow.OrganizationFlowResolver;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.platform.organization.policy.V3OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.platform.organization.providers.OnRequestPlatformPolicyChainProvider;
import io.gravitee.gateway.platform.organization.providers.OnResponsePlatformPolicyChainProvider;
import io.gravitee.gateway.platform.organization.reactor.V3OrganizationReactorFactory;
import io.gravitee.gateway.policy.ConfigurablePolicyChainProvider;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.impl.PolicyFactoryCreatorImpl;
import io.gravitee.gateway.reactive.debug.DebugReactorEventListener;
import io.gravitee.gateway.reactive.debug.policy.condition.DebugExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.debug.reactor.DebugHttpRequestDispatcher;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.DefaultPlatformPolicyChainFactoryManager;
import io.gravitee.gateway.reactive.platform.organization.reactor.DefaultOrganizationReactorFactory;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorFactory;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorRegistry;
import io.gravitee.gateway.reactive.policy.HttpPolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
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
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.repository.management.api.EventRepository;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public DebugOrganizationEventListener debugOrganizationEventListener(
        final EventManager eventManager,
        final OrganizationReactorRegistry debugV3OrganizationReactorRegistry,
        final OrganizationReactorRegistry debugOrganizationReactorRegistry
    ) {
        return new DebugOrganizationEventListener(eventManager, debugV3OrganizationReactorRegistry, debugOrganizationReactorRegistry);
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
    public V3OrganizationReactorFactory debugV3OrganizationReactorFactory(
        DefaultClassLoader classLoader,
        @Qualifier("debugV3PolicyFactoryCreator") io.gravitee.gateway.policy.PolicyFactoryCreator factoryCreator,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        ResourceClassLoaderFactory resourceClassLoaderFactory
    ) {
        return new V3OrganizationReactorFactory(
            configuration.getProperty("classloader.legacy.enabled", Boolean.class, false),
            classLoader,
            applicationContext.getParent(),
            factoryCreator,
            policyClassLoaderFactory,
            componentProvider,
            resourceClassLoaderFactory
        );
    }

    @Bean
    public OrganizationReactorRegistry debugV3OrganizationReactorRegistry(
        @Qualifier("debugV3OrganizationReactorFactory") V3OrganizationReactorFactory debugV3OrganizationReactorFactory
    ) {
        return new OrganizationReactorRegistry(debugV3OrganizationReactorFactory);
    }

    @Bean
    public OrganizationPolicyChainFactoryManager debugV3OrganizationPolicyChainFactoryManager(
        @Qualifier("debugV3OrganizationReactorRegistry") OrganizationReactorRegistry debugV3OrganizationReactorRegistry
    ) {
        return new V3OrganizationPolicyChainFactoryManager(debugV3OrganizationReactorRegistry);
    }

    @Bean
    @Qualifier("debugV3ConfigurablePolicyChainProvider")
    public OnRequestPlatformPolicyChainProvider debugV3OnRequestPlatformPolicyChainProvider(
        OrganizationFlowResolver organizationFlowResolver,
        @Qualifier(
            "debugV3OrganizationPolicyChainFactoryManager"
        ) OrganizationPolicyChainFactoryManager debugV3OrganizationPolicyChainFactoryManager
    ) {
        return new OnRequestPlatformPolicyChainProvider(
            organizationFlowResolver,
            debugV3OrganizationPolicyChainFactoryManager,
            new FlowPolicyResolverFactory()
        );
    }

    @Bean
    @Qualifier("debugV3ConfigurablePolicyChainProvider")
    public OnResponsePlatformPolicyChainProvider debugV3OnResponsePlatformPolicyChainProvider(
        OrganizationFlowResolver organizationFlowResolver,
        @Qualifier(
            "debugV3OrganizationPolicyChainFactoryManager"
        ) OrganizationPolicyChainFactoryManager debugV3OrganizationPolicyChainFactoryManager
    ) {
        return new OnResponsePlatformPolicyChainProvider(
            organizationFlowResolver,
            debugV3OrganizationPolicyChainFactoryManager,
            new FlowPolicyResolverFactory()
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
    public ResponseProcessorChainFactory debugV3ResponseProcessorChainFactory(EventRepository eventRepository, ObjectMapper objectMapper) {
        return new DebugResponseProcessorChainFactory(eventRepository, objectMapper);
    }

    /*******************
     *  V4 emulation engine Beans
     ******************/

    @Bean
    public DefaultOrganizationReactorFactory debugOrganizationReactorFactory(
        DefaultClassLoader classLoader,
        PolicyFactoryManager policyFactoryManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider,
        io.gravitee.node.api.configuration.Configuration configuration
    ) {
        return new DebugOrganizationReactorFactory(
            classLoader,
            applicationContext.getParent(),
            policyFactoryManager,
            policyClassLoaderFactory,
            componentProvider,
            configuration
        );
    }

    @Bean
    public OrganizationReactorRegistry debugOrganizationReactorRegistry(
        @Qualifier("debugOrganizationReactorFactory") OrganizationReactorFactory defaultOrganizationReactorFactory
    ) {
        return new OrganizationReactorRegistry(defaultOrganizationReactorFactory);
    }

    @Bean
    public io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager debugOrganizationPolicyChainFactoryManager(
        @Qualifier("debugOrganizationReactorRegistry") OrganizationReactorRegistry platformPolicyManagerRegistry
    ) {
        return new DefaultPlatformPolicyChainFactoryManager(platformPolicyManagerRegistry);
    }

    @Bean
    public DebugReactorEventListener debugReactorEventListener(
        final io.vertx.rxjava3.core.Vertx vertx,
        final EventManager eventManager,
        final EventRepository eventRepository,
        final ObjectMapper objectMapper,
        final VertxDebugHttpClientConfiguration debugHttpClientConfiguration,
        @Qualifier("debugReactorHandlerRegistry") final ReactorHandlerRegistry reactorHandlerRegistry,
        final AccessPointManager accessPointManager,
        final DataEncryptor dataEncryptor
    ) {
        return new DebugReactorEventListener(
            vertx,
            eventManager,
            eventRepository,
            objectMapper,
            debugHttpClientConfiguration,
            reactorHandlerRegistry,
            accessPointManager,
            dataEncryptor
        );
    }

    @Bean
    public PolicyFactoryManager debugPolicyFactoryManager(Set<PolicyFactory> policyFactories) {
        return new PolicyFactoryManager(policyFactories);
    }

    @Bean
    public PolicyFactory debugPolicyFactory(final PolicyPluginFactory policyPluginFactory) {
        return new HttpPolicyFactory(policyPluginFactory, new DebugExpressionLanguageConditionFilter());
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
        RequestClientAuthConfiguration requestClientAuthConfiguration,
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
            requestClientAuthConfiguration,
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
        @Qualifier("debugPolicyFactoryManager") PolicyFactoryManager policyFactoryManager,
        @Qualifier(
            "debugOrganizationPolicyChainFactoryManager"
        ) io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager debugOrganizationPolicyChainFactoryManager,
        OrganizationManager organizationManager,
        @Qualifier("debugV3PolicyChainProviderLoader") PolicyChainProviderLoader policyChainProviderLoader,
        ApiProcessorChainFactory apiProcessorChainFactory,
        FlowResolverFactory flowResolverFactory,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        AccessPointManager accessPointManager,
        EventManager eventManager
    ) {
        return new DebugApiReactorHandlerFactory(
            applicationContext.getParent(),
            configuration,
            node,
            v3PolicyFactoryCreator,
            policyFactoryManager,
            debugOrganizationPolicyChainFactoryManager,
            organizationManager,
            policyChainProviderLoader,
            apiProcessorChainFactory,
            flowResolverFactory,
            requestTimeoutConfiguration,
            accessPointManager,
            eventManager
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
