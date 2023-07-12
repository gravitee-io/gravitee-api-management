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
package io.gravitee.gateway.jupiter.core.v4.endpoint;

import static io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.AbstractConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.internal.DefaultEndpointConnectorPluginManager;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultEndpointConnectorResolverTest {

    protected static final String ENDPOINT_TYPE = "test";
    protected static final String ENDPOINT_GROUP_CONFIG = "{ \"groupSharedConfig\": \"something\"}";
    protected static final String ENDPOINT_CONFIG = "{ \"config\": \"something\"}";
    protected static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    protected static final ApiType SUPPORTED_API_TYPE = ApiType.ASYNC;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private DefaultEndpointConnectorPluginManager pluginManager;

    @Mock
    private EntrypointConnector entrypointConnector;

    @Mock
    private AbstractConnectorFactory<EndpointConnector> connectorFactory;

    @BeforeEach
    void init() {
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        lenient().when(entrypointConnector.supportedModes()).thenReturn(SUPPORTED_MODES);
        lenient().when(entrypointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);

        when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenReturn(connectorFactory);
    }

    @Test
    void shouldResolveEndpointConnector() {
        final Api api = buildApi();
        final EndpointConnector endpointConnector = mock(EndpointConnector.class);

        when(endpointConnector.supportedModes()).thenReturn(SUPPORTED_MODES);
        when(endpointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);
        when(connectorFactory.createConnector(ENDPOINT_CONFIG)).thenReturn(endpointConnector);

        final DefaultEndpointConnectorResolver cut = new DefaultEndpointConnectorResolver(api, pluginManager);
        final EndpointConnector resolvedEndpointConnector = cut.resolve(ctx);

        assertSame(endpointConnector, resolvedEndpointConnector);
    }

    @Test
    void shouldResolveFirstEndpointConnectorWhenMultipleEndpoints() {
        final Api api = buildApi();

        final Endpoint endpoint2 = buildEndpoint();
        final Endpoint endpoint3 = buildEndpoint();

        api.getEndpointGroups().get(0).getEndpoints().add(endpoint2);
        api.getEndpointGroups().get(0).getEndpoints().add(endpoint3);
        final EndpointConnector endpointConnector = mock(EndpointConnector.class);

        when(endpointConnector.supportedModes()).thenReturn(SUPPORTED_MODES);
        when(endpointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);
        when(connectorFactory.createConnector(ENDPOINT_CONFIG))
            .thenReturn(endpointConnector)
            .thenReturn(mock(EndpointConnector.class))
            .thenReturn(mock(EndpointConnector.class));

        final DefaultEndpointConnectorResolver cut = new DefaultEndpointConnectorResolver(api, pluginManager);
        final EndpointConnector resolvedEndpointConnector = cut.resolve(ctx);

        assertSame(endpointConnector, resolvedEndpointConnector);
        verify(connectorFactory, times(3)).createConnector(ENDPOINT_CONFIG);
    }

    @Test
    void shouldResolveFirstEndpointConnectorWhenMultipleEndpointGroups() {
        final Api api = buildApi();

        api.getEndpointGroups().add(buildEndpointGroup());
        api.getEndpointGroups().add(buildEndpointGroup());

        final EndpointConnector endpointConnector = mock(EndpointConnector.class);

        when(endpointConnector.supportedModes()).thenReturn(SUPPORTED_MODES);
        when(endpointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);
        when(connectorFactory.createConnector(ENDPOINT_CONFIG))
            .thenReturn(endpointConnector)
            .thenReturn(mock(EndpointConnector.class))
            .thenReturn(mock(EndpointConnector.class));

        final DefaultEndpointConnectorResolver cut = new DefaultEndpointConnectorResolver(api, pluginManager);
        final EndpointConnector resolvedEndpointConnector = cut.resolve(ctx);

        assertSame(endpointConnector, resolvedEndpointConnector);
        verify(connectorFactory, times(3)).createConnector(ENDPOINT_CONFIG);
    }

    @Test
    void shouldNotResolveWhenNotSupportingApiType() {
        final Api api = buildApi();
        final EndpointConnector endpointConnector = mock(EndpointConnector.class);

        when(endpointConnector.supportedApi()).thenReturn(ApiType.SYNC); // Not supporting the same type.
        lenient().when(endpointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE); // --> mock in lenient as filtering order could change.

        when(connectorFactory.createConnector(ENDPOINT_CONFIG)).thenReturn(endpointConnector);

        final DefaultEndpointConnectorResolver cut = new DefaultEndpointConnectorResolver(api, pluginManager);
        final EndpointConnector resolvedEndpointConnector = cut.resolve(ctx);

        assertNull(resolvedEndpointConnector);
    }

    @Test
    void shouldNotResolveWhenNotSupportingModes() {
        final Api api = buildApi();
        final EndpointConnector endpointConnector = mock(EndpointConnector.class);

        when(endpointConnector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH)); // Not supporting the SUBSCRIBE mode.
        when(endpointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);
        when(connectorFactory.createConnector(ENDPOINT_CONFIG)).thenReturn(endpointConnector);

        final DefaultEndpointConnectorResolver cut = new DefaultEndpointConnectorResolver(api, pluginManager);
        final EndpointConnector resolvedEndpointConnector = cut.resolve(ctx);

        assertNull(resolvedEndpointConnector);
    }

    @Test
    void shouldCreateConnectorUsingEndpointGroupSharedConfiguration() {
        final Api api = buildApi();

        final Endpoint endpoint2 = buildEndpoint();
        endpoint2.setInheritConfiguration(true);

        final Endpoint endpoint3 = buildEndpoint();

        api.getEndpointGroups().get(0).getEndpoints().add(endpoint2);
        api.getEndpointGroups().get(0).getEndpoints().add(endpoint3);

        when(connectorFactory.createConnector(any())).thenReturn(mock(EndpointConnector.class));

        new DefaultEndpointConnectorResolver(api, pluginManager).resolve(ctx);

        // 2 connector has been created with endpoint configuration
        verify(connectorFactory, times(2)).createConnector(ENDPOINT_CONFIG);
        // 1 connector has been created with endpoint group configuration, cause endpoint2 inherits group configuration
        verify(connectorFactory, times(1)).createConnector(ENDPOINT_GROUP_CONFIG);
    }

    private Api buildApi() {
        final Api api = new Api();
        final ArrayList<EndpointGroup> endpointGroups = new ArrayList<>();
        api.setEndpointGroups(endpointGroups);

        final EndpointGroup endpointGroup = buildEndpointGroup();
        endpointGroups.add(endpointGroup);
        return api;
    }

    private EndpointGroup buildEndpointGroup() {
        final EndpointGroup endpointGroup = new EndpointGroup();
        final ArrayList<Endpoint> endpoints = new ArrayList<>();

        endpointGroup.setType(ENDPOINT_TYPE);
        endpointGroup.setEndpoints(endpoints);
        endpointGroup.setSharedConfiguration(ENDPOINT_GROUP_CONFIG);

        final Endpoint endpoint = buildEndpoint();
        endpoints.add(endpoint);

        return endpointGroup;
    }

    private Endpoint buildEndpoint() {
        final Endpoint endpoint = new Endpoint();
        endpoint.setType(ENDPOINT_TYPE);
        endpoint.setConfiguration(ENDPOINT_CONFIG);
        return endpoint;
    }
}
