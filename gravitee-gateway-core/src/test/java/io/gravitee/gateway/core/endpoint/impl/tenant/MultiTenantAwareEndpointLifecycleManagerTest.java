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
package io.gravitee.gateway.core.endpoint.impl.tenant;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.tenant.MultiTenantAwareEndpointLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MultiTenantAwareEndpointLifecycleManagerTest {

    @InjectMocks
    private MultiTenantAwareEndpointLifecycleManager endpointLifecycleManager;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        endpointLifecycleManager = new MultiTenantAwareEndpointLifecycleManager(group, "europe");
        endpointLifecycleManager.setEndpointFactory(endpointFactory);
        endpointLifecycleManager.setReferenceRegister(referenceRegister);

        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getGroups()).thenReturn(Collections.singleton(group));
    }

    @Test
    public void shouldNotStartEndpoint_notInTenant() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.getTenants()).thenReturn(Collections.singletonList("asia"));
        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        endpointLifecycleManager.start();

        verify(endpointFactory, never()).create(any(io.gravitee.definition.model.Endpoint.class));

        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldNotStartEndpoint_backup() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.isBackup()).thenReturn(true);
        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        endpointLifecycleManager.start();

        verify(endpointFactory, never()).create(any(io.gravitee.definition.model.Endpoint.class));

        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStartEndpoint_inTenant() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.getTenants()).thenReturn(Collections.singletonList("europe"));
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);
        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        Endpoint registeredEndpoint = mock(Endpoint.class);
        when(registeredEndpoint.connector()).thenReturn(mock(Connector.class));
        when(registeredEndpoint.name()).thenReturn("endpoint");

        when(endpointFactory.support(any())).thenReturn(true);
        when(endpointFactory.create(any())).thenReturn(registeredEndpoint);

        endpointLifecycleManager.start();

        Endpoint httpClientEndpoint = endpointLifecycleManager.get("endpoint");

        assertNotNull(httpClientEndpoint);

        verify(endpointFactory, times(1)).create(eq(endpoint));
        verify(httpClientEndpoint.connector(), times(1)).start();

        assertEquals(httpClientEndpoint, endpointLifecycleManager.get("endpoint"));
        assertNull(endpointLifecycleManager.get("unknown"));
        assertFalse(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStartEndpoint_noTenant() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);
        when(group.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        Endpoint registeredEndpoint = mock(Endpoint.class);
        when(registeredEndpoint.connector()).thenReturn(mock(Connector.class));
        when(registeredEndpoint.name()).thenReturn("endpoint");

        when(endpointFactory.support(any())).thenReturn(true);
        when(endpointFactory.create(any())).thenReturn(registeredEndpoint);

        endpointLifecycleManager.start();

        Endpoint httpClientEndpoint = endpointLifecycleManager.get("endpoint");

        assertNotNull(httpClientEndpoint);

        verify(endpointFactory, times(1)).create(eq(endpoint));
        verify(httpClientEndpoint.connector(), times(1)).start();

        assertEquals(httpClientEndpoint, endpointLifecycleManager.get("endpoint"));
        assertNull(endpointLifecycleManager.get("unknown"));
        assertFalse(endpointLifecycleManager.endpoints().isEmpty());
    }
}
