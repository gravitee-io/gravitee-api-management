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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.policy.impl.PolicyLoader;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.ApiPolicyManager;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.resource.internal.v4.DefaultResourceManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.container.spring.SpringEnvironmentConfiguration;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DefaultApiReactorFactoryTest {

    @Mock
    ConfigurableApplicationContext applicationContext;

    @Mock
    ConfigurableListableBeanFactory applicationContextListable;

    Configuration configuration = new SpringEnvironmentConfiguration(new StandardEnvironment());

    @Mock
    Node node;

    @Mock
    PolicyFactoryManager policyFactoryManager;

    @Mock
    PolicyFactory policyFactory;

    @Mock
    EntrypointConnectorPluginManager entrypointConnectorPluginManager;

    @Mock
    EndpointConnectorPluginManager endpointConnectorPluginManager;

    @Mock
    OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager;

    @Mock
    OrganizationManager organizationManager;

    @Mock
    ApiProcessorChainFactory apiProcessorChainFactory;

    @Mock
    io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory flowResolverFactory;

    @Mock
    FlowResolverFactory v4FlowResolverFactory;

    @Mock
    RequestTimeoutConfiguration requestTimeoutConfiguration;

    @Mock
    ReporterService reporterService;

    @Mock
    private AccessPointManager accessPointManager;

    @Mock
    private EventManager eventManager;

    private DefaultApiReactorFactory cut;

    @Mock
    private io.gravitee.definition.model.v4.Api definition;

    @Mock
    private ApiServicePluginManager apiServicePluginManager;

    @Mock
    OpenTelemetryConfiguration openTelemetryConfiguration;

    @Mock
    private OpenTelemetryFactory openTelemetryFactory;

    @BeforeEach
    public void init() {
        lenient().when(applicationContext.getBeanFactory()).thenReturn(applicationContextListable);
        lenient().when(policyFactoryManager.get(any())).thenReturn(policyFactory);
        cut =
            new DefaultApiReactorFactory(
                applicationContext,
                configuration,
                node,
                policyFactoryManager,
                entrypointConnectorPluginManager,
                endpointConnectorPluginManager,
                apiServicePluginManager,
                organizationPolicyChainFactoryManager,
                organizationManager,
                flowResolverFactory,
                requestTimeoutConfiguration,
                reporterService,
                accessPointManager,
                eventManager,
                openTelemetryConfiguration,
                openTelemetryFactory,
                List.of()
            );
    }

    @Nested
    class CanCreate {

        @BeforeEach
        void setUp() {
            lenient().when(definition.getType()).thenReturn(ApiType.PROXY);
        }

        @Test
        public void should_create_api_with_http_listener() {
            when(definition.getListeners()).thenReturn(Collections.singletonList(new HttpListener()));

            boolean create = cut.canCreate(anApi());
            assertTrue(create);
        }

        @Test
        public void should_not_create_api_with_subscription_listener() {
            when(definition.getListeners()).thenReturn(Collections.singletonList(new SubscriptionListener()));

            boolean create = cut.canCreate(anApi());
            assertFalse(create);
        }

        @Test
        public void should_not_create_api_with_definition_v2() {
            boolean create = cut.canCreate(anApiV2());
            assertFalse(create);
        }

        @Test
        public void should_not_create_api_if_disabled() {
            ReactorHandler handler = cut.create(aDisabledApi());
            assertNull(handler);
        }

        @Test
        public void should_not_create_api_with_no_listener() {
            when(definition.getListeners()).thenReturn(Collections.emptyList());

            boolean create = cut.canCreate(anApi());
            assertFalse(create);
        }

        @Test
        public void should_not_create_api_with_no_http_or_subscription_listener() {
            when(definition.getListeners()).thenReturn(Collections.singletonList(new TcpListener()));

            boolean create = cut.canCreate(anApi());
            assertFalse(create);
        }
    }

    @Nested
    class Create {

        ComponentProvider globalComponentProvider;
        ConfigurablePluginManager<?> resourcePluginManager;
        ConfigurablePluginManager<?> policyPluginManager;
        List<TemplateVariableProvider> registeredApiTemplateVariableProvider;

        @BeforeEach
        void setUp() {
            globalComponentProvider = registerGlobalComponentProvider();
            resourcePluginManager = registerResourcePluginManager();
            policyPluginManager = registerPolicyPluginManager();
            registeredApiTemplateVariableProvider = registerApiTemplateVariableProvider(List.of(mock(TemplateVariableProvider.class)));
        }

        @Test
        void should_create_api_reactor_with_default_configuration() {
            var api = anApi();
            var reactor = cut.create(api);

            assertThat(reactor).extracting("node").isSameAs(node);
            assertThat(reactor).extracting("tracingContext").extracting("enabled", "verbose").containsOnly(false, false);
            assertThat(reactor).extracting("pendingRequestsTimeout").isEqualTo(10_000L);
            assertThat(reactor).extracting("loggingExcludedResponseType").isNull();
            assertThat(reactor).extracting("loggingMaxSize").isNull();
        }

        @Test
        void should_create_api_reactor_with_a_ComponentProvider_initialized() {
            var api = anApi();
            var reactor = cut.create(api);

            assertThat(reactor).isInstanceOf(DefaultApiReactor.class);
            var componentProvider = ((DefaultApiReactor) reactor).getComponentProvider();

            assertThat(componentProvider)
                .isInstanceOf(CompositeComponentProvider.class)
                .extracting("componentProviders")
                .asList()
                .contains(globalComponentProvider);

            assertThat(componentProvider.getComponent(Api.class)).isSameAs(api);
            assertThat(componentProvider.getComponent(ReactableApi.class)).isSameAs(api);
            assertThat(componentProvider.getComponent(io.gravitee.definition.model.v4.Api.class)).isSameAs(definition);
            assertThat(componentProvider.getComponent(ResourceManager.class)).isNotNull();
            assertThat(componentProvider.getComponent(EndpointManager.class)).isNotNull();
        }

        @Test
        void should_create_api_reactor_with_ResourceManager() {
            var api = anApi();
            var reactor = cut.create(api);

            assertThat(reactor)
                .extracting("resourceLifecycleManager")
                .isInstanceOf(DefaultResourceManager.class)
                .extracting("resourcePluginManager")
                .isSameAs(resourcePluginManager);
        }

        @Test
        void should_create_api_reactor_with_PolicyManager() {
            var api = anApi();
            var reactor = cut.create(api);

            assertThat(reactor)
                .extracting("policyManager")
                .isInstanceOf(ApiPolicyManager.class)
                .extracting("policyLoader")
                .isInstanceOf(PolicyLoader.class)
                .extracting("policyPluginManager")
                .isSameAs(policyPluginManager);
        }

        @Test
        void should_create_api_reactor_with_TemplateVariableProviders() {
            var api = anApi();
            var reactor = cut.create(api);

            assertThat(reactor).isInstanceOf(DefaultApiReactor.class);
            var templateVariableProviders = ((DefaultApiReactor) reactor).getCtxTemplateVariableProviders();

            assertThat(templateVariableProviders)
                .hasSize(2 + registeredApiTemplateVariableProvider.size())
                .satisfies(list -> {
                    assertThat(list.stream().filter(p -> p instanceof ApiTemplateVariableProvider).findFirst()).isPresent();
                    assertThat(list.stream().filter(p -> registeredApiTemplateVariableProvider.contains(p)).findFirst()).isPresent();
                    assertThat(list.stream().filter(p -> p instanceof EndpointManager).findFirst()).isPresent();
                });
        }

        private List<TemplateVariableProvider> registerApiTemplateVariableProvider(List<TemplateVariableProvider> providers) {
            var apiTemplateVariableProviderFactory = mock(ApiTemplateVariableProviderFactory.class);
            when(apiTemplateVariableProviderFactory.getTemplateVariableProviders()).thenReturn(providers);
            lenient()
                .when(applicationContext.getBean(ApiTemplateVariableProviderFactory.class))
                .thenReturn(apiTemplateVariableProviderFactory);
            return providers;
        }

        private ComponentProvider registerGlobalComponentProvider() {
            var globalComponentProvider = mock(ComponentProvider.class);
            lenient().when(applicationContext.getBean(ComponentProvider.class)).thenReturn(globalComponentProvider);
            return globalComponentProvider;
        }

        private ConfigurablePluginManager<?> registerResourcePluginManager() {
            var resourcePluginManager = mock(ConfigurablePluginManager.class);
            lenient()
                .when(
                    applicationContextListable.getBeanNamesForType(
                        ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
                    )
                )
                .thenReturn(new String[] { "resourcePluginManager" });
            lenient().when(applicationContext.getBean("resourcePluginManager")).thenReturn(resourcePluginManager);
            return resourcePluginManager;
        }

        private ConfigurablePluginManager<?> registerPolicyPluginManager() {
            var policyPluginManager = mock(ConfigurablePluginManager.class);
            lenient()
                .when(
                    applicationContextListable.getBeanNamesForType(
                        ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
                    )
                )
                .thenReturn(new String[] { "policyPluginManager" });
            lenient().when(applicationContext.getBean("policyPluginManager")).thenReturn(policyPluginManager);
            return policyPluginManager;
        }
    }

    private Api anApi() {
        Api api = mock(Api.class);
        lenient().when(api.isEnabled()).thenReturn(true);
        lenient().when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        lenient().when(api.getDefinition()).thenReturn(definition);
        return api;
    }

    private Api aDisabledApi() {
        Api api = mock(Api.class);
        lenient().when(api.isEnabled()).thenReturn(false);
        lenient().when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        lenient().when(api.getDefinition()).thenReturn(definition);
        return api;
    }

    private Api anApiV2() {
        Api api = mock(Api.class);
        lenient().when(api.isEnabled()).thenReturn(true);
        lenient().when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
        lenient().when(api.getDefinition()).thenReturn(definition);
        return api;
    }
}
