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
package io.gravitee.gateway.core.endpoint.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.connector.api.ConnectorBuilder;
import io.gravitee.connector.api.ConnectorFactory;
import io.gravitee.definition.model.*;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.connector.ConnectorRegistry;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.core.endpoint.ManagedEndpoint;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.EndpointGroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.node.api.configuration.Configuration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class EndpointGroupLifecycleManagerTest {

    private EndpointGroupLifecycleManager endpointLifecycleManager;

    @Mock
    private Api api;

    @Mock
    private Proxy proxy;

    @Mock
    private EndpointGroup group;

    @Mock
    private EndpointFactory endpointFactory;

    @Mock
    private ReferenceRegister referenceRegister;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ConnectorFactory connectorFactory;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ConnectorRegistry connectorRegistry;

    @Mock
    private io.gravitee.connector.api.Connector connector;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private Configuration configuration;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        when(connectorFactory.create(anyString(), anyString(), any(ConnectorBuilder.class))).thenReturn(connector);
        when(connectorRegistry.getConnector(any())).thenReturn(connectorFactory);

        JsonNode node = mock(ObjectNode.class);
        lenient().when(node.has(anyString())).thenReturn(false);
        lenient().when(mapper.readTree(anyString())).thenReturn(node);

        endpointLifecycleManager = new EndpointGroupLifecycleManager(
            api,
            group,
            endpointFactory,
            referenceRegister,
            connectorRegistry,
            configuration,
            mapper
        );

        lenient().when(api.getProxy()).thenReturn(proxy);
        lenient().when(proxy.getGroups()).thenReturn(Collections.singleton(group));
    }

    @Test
    public void shouldNotStartEndpoint_noEndpoint() throws Exception {
        endpointLifecycleManager.start();

        verify(endpointFactory, never()).create(
            any(io.gravitee.definition.model.Endpoint.class),
            any(io.gravitee.connector.api.Connector.class)
        );

        assertThat(endpointLifecycleManager.endpoints()).isEmpty();
    }

    @Test
    public void shouldStartEndpoint_backupEndpoint() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(true);
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTarget()).thenReturn("target");
        when(endpoint.getConfiguration()).thenReturn("");
        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        endpointLifecycleManager.start();

        verify(endpointFactory, atLeast(1)).create(
            any(io.gravitee.definition.model.Endpoint.class),
            any(io.gravitee.connector.api.Connector.class)
        );

        assertThat(endpointLifecycleManager.endpoints()).isEmpty();
    }

    @Test
    public void shouldStartEndpoint() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTarget()).thenReturn("target");
        when(endpoint.getConfiguration()).thenReturn("");

        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        Endpoint registeredEndpoint = mock(Endpoint.class);
        when(registeredEndpoint.connector()).thenReturn(mock(Connector.class));
        when(registeredEndpoint.name()).thenReturn("endpoint");

        when(endpointFactory.create(any(), any(io.gravitee.connector.api.Connector.class))).thenReturn(registeredEndpoint);

        endpointLifecycleManager.start();

        Endpoint httpClientEndpoint = endpointLifecycleManager.get("endpoint");

        assertThat(httpClientEndpoint).isNotNull();

        verify(endpointFactory, times(1)).create(eq(endpoint), any(io.gravitee.connector.api.Connector.class));
        verify(httpClientEndpoint.connector(), times(1)).start();

        assertThat(endpointLifecycleManager.get("endpoint")).isEqualTo(httpClientEndpoint);
        assertThat(endpointLifecycleManager.get("unknown")).isNull();
        assertThat(endpointLifecycleManager.endpoints()).isNotEmpty();
    }

    @Test
    public void shouldStartEndpointWithSpEL() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = new io.gravitee.definition.model.Endpoint(
            "http",
            "endpoint",
            "{#properties['backend']}"
        );
        endpoint.setBackup(false);
        endpoint.setConfiguration("");

        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        Properties properties = new Properties();
        properties.setProperties(List.of(new Property("backend", "http://localhost:8080")));
        when(api.getProperties()).thenReturn(properties);

        Endpoint registeredEndpoint = new ManagedEndpoint(endpoint, mock(Connector.class));
        when(endpointFactory.create(any(), any(io.gravitee.connector.api.Connector.class))).thenReturn(registeredEndpoint);

        endpointLifecycleManager.start();

        Endpoint httpClientEndpoint = endpointLifecycleManager.get("endpoint");

        assertThat(httpClientEndpoint).isNotNull();
        assertThat(endpointLifecycleManager.get("endpoint")).isEqualTo(httpClientEndpoint);
        assertThat(httpClientEndpoint.target()).isEqualTo("http://localhost:8080");

        verify(endpointFactory, times(1)).create(eq(endpoint), any(io.gravitee.connector.api.Connector.class));
        verify(httpClientEndpoint.connector(), times(1)).start();
    }

    @Test
    public void shouldStopEndpoint() throws Exception {
        // First, start an endpoint
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTarget()).thenReturn("target");
        when(endpoint.getConfiguration()).thenReturn("");
        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        Endpoint registeredEndpoint = mock(Endpoint.class);
        when(registeredEndpoint.connector()).thenReturn(mock(Connector.class));
        when(registeredEndpoint.name()).thenReturn("endpoint");

        when(endpointFactory.create(any(), any(io.gravitee.connector.api.Connector.class))).thenReturn(registeredEndpoint);

        endpointLifecycleManager.start();

        assertThat(endpointLifecycleManager.endpoints()).isNotEmpty();

        Endpoint httpClientEndpoint = endpointLifecycleManager.get("endpoint");

        // Then, stop endpoint
        endpointLifecycleManager.stop();

        // Verify that the HTTP client is correctly stopped
        verify(httpClientEndpoint.connector(), times(1)).stop();

        assertThat(endpointLifecycleManager.endpoints()).isEmpty();
    }

    @Test
    public void shouldNotStartEndpoint_endpointException() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTarget()).thenReturn("target");
        when(endpoint.getConfiguration()).thenReturn("");

        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        Endpoint registeredEndpoint = mock(Endpoint.class);
        Connector connector = mock(Connector.class);
        when(registeredEndpoint.connector()).thenReturn(connector);
        when(connector.start()).thenThrow(EndpointException.class);
        lenient().when(registeredEndpoint.name()).thenReturn("endpoint");

        when(endpointFactory.create(any(), any(io.gravitee.connector.api.Connector.class))).thenReturn(registeredEndpoint);

        endpointLifecycleManager.start();

        Endpoint httpClientEndpoint = endpointLifecycleManager.get("endpoint");

        assertThat(httpClientEndpoint).isNull();

        verify(endpointFactory, times(1)).create(eq(endpoint), any(io.gravitee.connector.api.Connector.class));
        verify(connector, times(1)).start();

        assertThat(endpointLifecycleManager.get("endpoint")).isNull();
        assertThat(endpointLifecycleManager.endpoints()).isEmpty();
    }
}
