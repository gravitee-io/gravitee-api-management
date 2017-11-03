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
package io.gravitee.gateway.http.core.endpoint.impl.tenant;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.Optional;

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
    private ApplicationContext applicationContext;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(proxy.isMultiTenant()).thenReturn(true);
        when(api.getProxy()).thenReturn(proxy);
    }

    @Test (expected = IllegalStateException.class)
    public void shouldNotStartApi_noTenantConfigured() throws Exception {
        when(gatewayConfiguration.tenant()).thenReturn(Optional.empty());

        endpointLifecycleManager.start();
    }

    @Test
    public void shouldNotStartEndpoint_notInTenant() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("asia"));

        when(endpoint.getTenant()).thenReturn("europe");
        when(proxy.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        endpointLifecycleManager.start();

        verify(applicationContext, never()).getBean(eq(Connector.class), any(Endpoint.class));

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldNotStartEndpoint_backup() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(endpoint.isBackup()).thenReturn(true);
        when(proxy.getEndpoints()).thenReturn(Collections.singleton(endpoint));

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("europe"));
        endpointLifecycleManager.start();

        verify(applicationContext, never()).getBean(eq(Connector.class), any(Endpoint.class));

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStartEndpoint_inTenant() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.endpoint.HttpEndpoint.class);

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("europe"));

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.getTenant()).thenReturn("europe");
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
}
