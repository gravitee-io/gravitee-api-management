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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.el.TemplateContext;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.internal.DefaultEndpointConnectorPluginManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultEndpointManagerTest {

    private static final String ENDPOINT_TYPE = "test";
    private static final String ENDPOINT_GROUP_CONFIG = "{ \"groupSharedConfig\": \"something\"}";
    private static final String ENDPOINT_CONFIG = "{ \"config\": \"something\"}";
    private static final String MOCK_EXCEPTION = "Mock exception";
    private static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    private static final ApiType SUPPORTED_API_TYPE = ApiType.MESSAGE;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private DefaultEndpointConnectorPluginManager pluginManager;

    @Mock
    private EntrypointConnector entrypointConnector;

    @Mock
    private EndpointConnectorFactory connectorFactory;

    @Mock
    private DeploymentContext deploymentContext;

    @BeforeEach
    void init() {
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        lenient().when(entrypointConnector.supportedModes()).thenReturn(SUPPORTED_MODES);
        lenient().when(entrypointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);
        lenient().when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenReturn(connectorFactory);
    }

    @Test
    void shouldStartAllEndpoints() throws Exception {
        final Api api = buildApi();

        // 2 groups with 2 endpoints each. 4 connectors to instantiate.
        final EndpointConnector connector1 = mock(EndpointConnector.class);
        final EndpointConnector connector2 = mock(EndpointConnector.class);
        final EndpointConnector connector3 = mock(EndpointConnector.class);
        final EndpointConnector connector4 = mock(EndpointConnector.class);

        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG))
            .thenReturn(connector1, connector2, connector3, connector4);
        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();

        verify(pluginManager, times(4)).getFactoryById(ENDPOINT_TYPE);
        verify(connectorFactory, times(4)).createConnector(deploymentContext, ENDPOINT_CONFIG);
        verify(connector1).start();
        verify(connector2).start();
        verify(connector3).start();
        verify(connector4).start();
    }

    @Test
    void shouldProvideEndpointsTemplateVariable() throws Exception {
        final TemplateContext templateContext = mock(TemplateContext.class);
        final Api api = buildApi();

        // Rename groups and endpoint to ease assertions.
        final AtomicInteger i = new AtomicInteger(0);

        api.getEndpointGroups().forEach(g -> g.setName("group" + i.incrementAndGet()));

        i.set(0);
        Stream
            .concat(api.getEndpointGroups().get(0).getEndpoints().stream(), api.getEndpointGroups().get(1).getEndpoints().stream())
            .forEachOrdered(e -> e.setName("endpoint" + i.incrementAndGet()));

        final EndpointConnector connector = mock(EndpointConnector.class);

        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);
        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);

        cut.provide(templateContext);
        verify(templateContext).setVariable("endpoints", Collections.emptyMap());
        reset(templateContext);

        cut.start();
        cut.provide(templateContext);

        final ArgumentCaptor<Map<String, String>> endpointsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());

        assertThat(endpointsCaptor.getValue()).containsEntry("group1", "group1:");
        assertThat(endpointsCaptor.getValue()).containsEntry("endpoint1", "endpoint1:");
        assertThat(endpointsCaptor.getValue()).containsEntry("endpoint2", "endpoint2:");
        assertThat(endpointsCaptor.getValue()).containsEntry("group2", "group2:");
        assertThat(endpointsCaptor.getValue()).containsEntry("endpoint3", "endpoint3:");
        assertThat(endpointsCaptor.getValue()).containsEntry("endpoint4", "endpoint4:");
    }

    @Test
    void shouldProvideUpdatedEndpointsTemplateVariableWhenEndpointIsRemoved() throws Exception {
        final TemplateContext templateContext = mock(TemplateContext.class);
        final Api api = buildApi();

        final EndpointConnector connector = mock(EndpointConnector.class);

        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);
        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);

        cut.start();
        cut.provide(templateContext);

        final ArgumentCaptor<Map<String, String>> endpointsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());

        final String endpointToRemove = api.getEndpointGroups().get(0).getEndpoints().get(0).getName();
        assertThat(endpointsCaptor.getValue()).hasSize(6);
        assertThat(endpointsCaptor.getValue()).containsKey(endpointToRemove);

        reset(templateContext);
        cut.removeEndpoint(endpointToRemove);
        cut.provide(templateContext);

        verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());
        assertThat(endpointsCaptor.getValue()).hasSize(5);
        assertThat(endpointsCaptor.getValue()).doesNotContainKey(endpointToRemove);
    }

    @Test
    void shouldStartEndpointUsingGroupSharedConfiguration() throws Exception {
        final Api api = buildApi();

        // Make one endpoint of each group inherits the group configuration.
        api.getEndpointGroups().get(0).getEndpoints().get(0).setInheritConfiguration(true);
        api.getEndpointGroups().get(1).getEndpoints().get(0).setInheritConfiguration(true);

        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connectorFactory.createConnector(eq(deploymentContext), anyString())).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next();

        assertThat(next).isNotNull();

        // 2 connectors have been created with endpoint configuration
        verify(connectorFactory, times(2)).createConnector(deploymentContext, ENDPOINT_CONFIG);
        // 2 connectors have been created with endpoint group configuration, cause endpoint2 inherits group configuration
        verify(connectorFactory, times(2)).createConnector(deploymentContext, ENDPOINT_GROUP_CONFIG);
    }

    @Test
    void shouldReturnNullManagedEndpointWhenNotStarted() {
        final Api api = buildApi();

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        assertThat(cut.next()).isNull();
    }

    @Test
    void shouldReturnNextManagedEndpoint() throws Exception {
        final Api api = buildApi();

        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next();

        assertThat(next).isNotNull();
        assertThat(next.getDefinition()).isEqualTo(api.getEndpointGroups().get(0).getEndpoints().get(0));
        assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
        assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(next.getGroup()).isNotNull();
        assertThat(next.getGroup().getDefinition()).isEqualTo(api.getEndpointGroups().get(0));
    }

    @Test
    void shouldReturnNextManagedEndpointByName() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
        final String endpointName = expectedEndpoint.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(new EndpointCriteria(endpointName, null, null));

        assertThat(next).isNotNull();
        assertThat(next.getDefinition()).isEqualTo(expectedEndpoint);
        assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
        assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(next.getGroup()).isNotNull();
        assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
    }

    @Test
    void shouldReturnNextManagedEndpointByGroupName() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final String groupName = expectedEndpointGroup.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(new EndpointCriteria(groupName, null, null));

        assertThat(next).isNotNull();
        assertThat(next.getDefinition()).isEqualTo(expectedEndpointGroup.getEndpoints().get(0)); // First endpoint of the group is expected.
        assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
        assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(next.getGroup()).isNotNull();
        assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
    }

    @Test
    void shouldReturnNullWhenEndpointByNameIsNotAvailable() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
        final String endpointName = expectedEndpoint.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        ManagedEndpoint next = cut.next(new EndpointCriteria(endpointName, null, null));

        assertThat(next).isNotNull();
        cut.disable(next);

        next = cut.next(new EndpointCriteria(endpointName, null, null));
        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNextManagedEndpointWhenEndpointByNameAvailableAgain() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
        final String endpointName = expectedEndpoint.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        final EndpointCriteria criteria = new EndpointCriteria(endpointName, null, null);

        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        ManagedEndpoint next = cut.next(criteria);
        cut.disable(next);

        // Should be null because disabled.
        assertThat(cut.next(criteria)).isNull();

        cut.enable(next);
        assertThat(cut.next(criteria)).isNotNull();
    }

    @Test
    void shouldReturnNullWhenNameNotFound() throws Exception {
        final Api api = buildApi();
        final EndpointConnector connector = mock(EndpointConnector.class);

        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        ManagedEndpoint next = cut.next(new EndpointCriteria("UNKNOWN", null, null));

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNullEndpointWhenNoConnectorFactoryFound() throws Exception {
        final Api api = buildApi();

        // Simulate no connector factory available.
        when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenReturn(null);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        ManagedEndpoint next = cut.next();

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNullEndpointWhenExceptionOccurred() throws Exception {
        final Api api = buildApi();

        // Simulate an unexpected exception.
        when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenThrow(new RuntimeException(MOCK_EXCEPTION));

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        ManagedEndpoint next = cut.next();

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNullEndpointWhenNoConnectorCreated() throws Exception {
        final Api api = buildApi();

        // Simulate connector factory returns null connector.
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(null);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        ManagedEndpoint next = cut.next();

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNullWhenNotSupportingModes() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final String groupName = expectedEndpointGroup.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH));
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(
            new EndpointCriteria(groupName, null, Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
        );

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNullManagedEndpointByNameWhenNotSupportingMode() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
        final String endpointName = expectedEndpoint.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH));
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(
            new EndpointCriteria(endpointName, null, Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
        );

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnManagedEndpointWhenSupportingModes() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final String groupName = expectedEndpointGroup.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE));
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(
            new EndpointCriteria(groupName, null, Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
        );

        assertThat(next).isNotNull();
        assertThat(next.getDefinition()).isEqualTo(expectedEndpointGroup.getEndpoints().get(0)); // First endpoint of the group is expected.
        assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
        assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(next.getGroup()).isNotNull();
        assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
    }

    @Test
    void shouldReturnNullWhenNotSupportingApiType() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final String groupName = expectedEndpointGroup.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connector.supportedApi()).thenReturn(ApiType.MESSAGE);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(new EndpointCriteria(groupName, ApiType.PROXY, null));

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnNullManagedEndpointByNameWhenNotSupportingApiType() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
        final String endpointName = expectedEndpoint.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connector.supportedApi()).thenReturn(ApiType.MESSAGE);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        final ManagedEndpoint next = cut.next(new EndpointCriteria(endpointName, ApiType.PROXY, null));

        assertThat(next).isNull();
    }

    @Test
    void shouldReturnManagedEndpointWhenSupportingApiType() throws Exception {
        final Api api = buildApi();

        final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
        final String groupName = expectedEndpointGroup.getName();
        final EndpointConnector connector = mock(EndpointConnector.class);
        when(connector.supportedApi()).thenReturn(ApiType.MESSAGE);
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG)).thenReturn(connector);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();

        final ManagedEndpoint next = cut.next(new EndpointCriteria(groupName, ApiType.MESSAGE, null));

        assertThat(next).isNotNull();
        assertThat(next.getDefinition()).isEqualTo(expectedEndpointGroup.getEndpoints().get(0)); // First endpoint of the group is expected.
        assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
        assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
        assertThat(next.getGroup()).isNotNull();
        assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
    }

    @Test
    void shouldIgnoreErrorWhenPreStopEndpointConnectors() throws Exception {
        final Api api = buildApi();

        final EndpointConnector connector1 = mock(EndpointConnector.class);
        final EndpointConnector connector2 = mock(EndpointConnector.class);
        final EndpointConnector connector3 = mock(EndpointConnector.class);
        final EndpointConnector connector4 = mock(EndpointConnector.class);

        when(connector2.preStop()).thenThrow(new Exception(MOCK_EXCEPTION));
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG))
            .thenReturn(connector1, connector2, connector3, connector4);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        cut.preStop();

        verify(connector1).preStop();
        verify(connector2).preStop();
        verify(connector3).preStop();
        verify(connector4).preStop();
    }

    @Test
    void shouldIgnoreErrorWhenStopEndpointConnectors() throws Exception {
        final Api api = buildApi();

        final EndpointConnector connector1 = mock(EndpointConnector.class);
        final EndpointConnector connector2 = mock(EndpointConnector.class);
        final EndpointConnector connector3 = mock(EndpointConnector.class);
        final EndpointConnector connector4 = mock(EndpointConnector.class);

        when(connector2.stop()).thenThrow(new Exception(MOCK_EXCEPTION));
        when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG))
            .thenReturn(connector1, connector2, connector3, connector4);

        final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext);
        cut.start();
        cut.stop();

        verify(connector1).stop();
        verify(connector2).stop();
        verify(connector3).stop();
        verify(connector4).stop();
    }

    private Api buildApi() {
        final Api api = new Api();
        final ArrayList<EndpointGroup> endpointGroups = new ArrayList<>();
        api.setEndpointGroups(endpointGroups);

        endpointGroups.add(buildEndpointGroup());
        endpointGroups.add(buildEndpointGroup());
        return api;
    }

    private EndpointGroup buildEndpointGroup() {
        final EndpointGroup endpointGroup = new EndpointGroup();
        final ArrayList<Endpoint> endpoints = new ArrayList<>();

        endpointGroup.setName(randomUUID().toString());
        endpointGroup.setType(ENDPOINT_TYPE);
        endpointGroup.setEndpoints(endpoints);
        endpointGroup.setSharedConfiguration(ENDPOINT_GROUP_CONFIG);

        endpoints.add(buildEndpoint());
        endpoints.add(buildEndpoint());

        return endpointGroup;
    }

    private Endpoint buildEndpoint() {
        final Endpoint endpoint = new Endpoint();
        endpoint.setName(randomUUID().toString());
        endpoint.setType(ENDPOINT_TYPE);
        endpoint.setConfiguration(ENDPOINT_CONFIG);
        return endpoint;
    }
}
