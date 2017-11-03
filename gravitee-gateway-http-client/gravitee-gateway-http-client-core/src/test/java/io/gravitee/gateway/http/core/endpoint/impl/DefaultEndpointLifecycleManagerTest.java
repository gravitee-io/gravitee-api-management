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
package io.gravitee.gateway.http.core.endpoint.impl;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEndpointLifecycleManagerTest {

    @InjectMocks
    private DefaultEndpointLifecycleManager endpointLifecycleManager;

    @Mock
    private Api api;

    @Mock
    private Proxy proxy;

    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(api.getProxy()).thenReturn(proxy);
    }

    @Test
    public void shouldNotStartEndpoint_noEndpoint() throws Exception {
        endpointLifecycleManager.doStart();

        verify(applicationContext, never()).getBean(eq(Connector.class), any(Endpoint.class));

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldNotStartEndpoint_backupEndpoint() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.isBackup()).thenReturn(true);
        when(proxy.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        endpointLifecycleManager.start();

        verify(applicationContext, never()).getBean(eq(Connector.class), any(Endpoint.class));

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStartEndpoint() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);
        when(proxy.getEndpoints()).thenReturn(Collections.singleton(endpoint));
        when(applicationContext.getBean(Connector.class, endpoint)).thenReturn(mock(Connector.class));
        endpointLifecycleManager.start();

        HttpEndpoint httpClientEndpoint = (HttpEndpoint) endpointLifecycleManager.get("endpoint");

        assertNotNull(httpClientEndpoint);

        verify(applicationContext, times(1)).getBean(eq(Connector.class), any(Endpoint.class));
        verify(httpClientEndpoint.connector(), times(1)).start();

        assertEquals(httpClientEndpoint, endpointLifecycleManager.get("endpoint"));
        assertNull(endpointLifecycleManager.get("unknown"));

        assertFalse(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertFalse(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStopEndpoint() throws Exception {
        // First, start an endpoint
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);
        when(proxy.getEndpoints()).thenReturn(Collections.singleton(endpoint));
        when(applicationContext.getBean(Connector.class, endpoint)).thenReturn(mock(Connector.class));
        endpointLifecycleManager.start();

        assertFalse(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertFalse(endpointLifecycleManager.endpoints().isEmpty());

        HttpEndpoint httpClientEndpoint = (HttpEndpoint) endpointLifecycleManager.get("endpoint");

        // Then, stop endpoint
        endpointLifecycleManager.stop();

        // Verify that the HTTP client is correctly stopped
        verify(httpClientEndpoint.connector(), times(1)).stop();

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    /*
    @Test
    public void shouldCreateSingleEndpointLoadBalancer() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);
        when(proxy.getEndpoints()).thenReturn(Collections.singleton(endpoint));
        when(applicationContext.getBean(Connector.class, endpoint)).thenReturn(mock(Connector.class));
        endpointLifecycleManager.start();

        LoadBalancerStrategy loadbalancer = endpointLifecycleManager.loadbalancer();
        assertNotNull(loadbalancer);
        assertEquals(SingleEndpointLoadBalancerStrategy.class, loadbalancer.getClass());
    }
    */
}
