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
package io.gravitee.gateway.handlers.api;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.CLASSLOADER_LEGACY_ENABLED_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.HANDLERS_REQUEST_HEADERS_X_FORWARDED_PREFIX_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyFactoryCreator;
import io.gravitee.gateway.reactive.handlers.api.SyncApiReactor;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyChainFactoryManager;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiReactorHandlerFactoryTest {

    private ApiReactorHandlerFactory apiContextHandlerFactory;

    @Mock
    private Configuration configuration;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Api api;

    @Mock
    ApiProcessorChainFactory apiProcessorChainFactory;

    @Mock
    FlowResolverFactory flowResolverFactory;

    @Mock
    PolicyFactoryCreator v3PolicyFactoryCreator;

    @Mock
    PolicyChainProviderLoader policyChainProviderLoader;

    @Mock
    ApiTemplateVariableProviderFactory apiTemplateVariableProviderFactory;

    @Mock
    OrganizationPolicyChainFactoryManager organizationPolicyChainFactoryManager;

    @Mock
    private AccessPointManager accessPointManager;

    @Mock
    private EventManager eventManager;

    @Mock
    private OpenTelemetryConfiguration openTelemetryConfiguration;

    @Mock
    private OpenTelemetryFactory openTelemetryFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configuration.getProperty(HANDLERS_REQUEST_HEADERS_X_FORWARDED_PREFIX_PROPERTY, Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty(CLASSLOADER_LEGACY_ENABLED_PROPERTY, Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);
        when(openTelemetryConfiguration.isTracesEnabled()).thenReturn(false);
        when(applicationContext.getBean(GatewayConfiguration.class)).thenReturn(gatewayConfiguration);
        when(applicationContext.getBean(ApiTemplateVariableProviderFactory.class)).thenReturn(apiTemplateVariableProviderFactory);
        when(applicationContext.getBeanNamesForType(any(ResolvableType.class)))
            .thenReturn(new String[] { "configurablePluginManager", "resourcePlugin" });

        apiContextHandlerFactory =
            new ApiReactorHandlerFactory(
                applicationContext,
                configuration,
                null,
                v3PolicyFactoryCreator,
                null,
                organizationPolicyChainFactoryManager,
                null,
                policyChainProviderLoader,
                apiProcessorChainFactory,
                flowResolverFactory,
                new RequestTimeoutConfiguration(2000L, 10L),
                accessPointManager,
                eventManager,
                openTelemetryConfiguration,
                openTelemetryFactory,
                List.of()
            );
    }

    @Test
    public void shouldNotCreateContext() {
        when(api.isEnabled()).thenReturn(false);
        ReactorHandler handler = apiContextHandlerFactory.create(api);
        assertThat(handler).isNull();
    }

    @Test
    public void shouldDefaultToV4ExecutionMode() {
        io.gravitee.definition.model.Api definition = mock(io.gravitee.definition.model.Api.class);
        when(definition.getProxy()).thenReturn(mock(io.gravitee.definition.model.Proxy.class));
        when(api.isEnabled()).thenReturn(true);
        when(api.getDefinition()).thenReturn(definition);
        ReactorHandler handler = apiContextHandlerFactory.create(api);
        assertThat(handler).isInstanceOf(SyncApiReactor.class);
    }

    @Test
    public void shouldUseV3ExecutionMode() {
        when(api.isEnabled()).thenReturn(true);
        io.gravitee.definition.model.Api definition = mock(io.gravitee.definition.model.Api.class);
        when(api.getDeployedAt()).thenReturn(new Date());
        when(definition.getProxy()).thenReturn(mock(io.gravitee.definition.model.Proxy.class));
        when(api.getDefinition()).thenReturn(definition);
        when(definition.getExecutionMode()).thenReturn(ExecutionMode.V3);
        ReactorHandler handler = apiContextHandlerFactory.create(api);
        assertThat(handler).isInstanceOf(ApiReactorHandler.class);
    }
}
