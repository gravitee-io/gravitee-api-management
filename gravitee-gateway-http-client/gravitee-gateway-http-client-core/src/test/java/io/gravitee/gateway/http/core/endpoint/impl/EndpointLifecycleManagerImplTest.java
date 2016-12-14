package io.gravitee.gateway.http.core.endpoint.impl;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;
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
public class EndpointLifecycleManagerImplTest {

    private EndpointLifecycleManagerImpl endpointLifecycleManager;

    @Mock
    private Api api;

    @Mock
    private Proxy proxy;

    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        endpointLifecycleManager = new EndpointLifecycleManagerImpl();
        endpointLifecycleManager.setApi(api);
        endpointLifecycleManager.setApplicationContext(applicationContext);

        when(api.getProxy()).thenReturn(proxy);
    }

    @Test
    public void shouldNotStartEndpoint_noEndpoint() throws Exception {
        endpointLifecycleManager.doStart();

        verify(applicationContext, never()).getBean(eq(HttpClient.class), any(Endpoint.class));

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldNotStartEndpoint_backupEndpoint() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.isBackup()).thenReturn(true);
        when(proxy.getEndpoints()).thenReturn(Collections.singletonList(endpoint));

        endpointLifecycleManager.doStart();

        verify(applicationContext, never()).getBean(eq(HttpClient.class), any(Endpoint.class));

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStartEndpoint() throws Exception {
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(proxy.getEndpoints()).thenReturn(Collections.singletonList(endpoint));
        when(applicationContext.getBean(HttpClient.class, endpoint)).thenReturn(mock(HttpClient.class));
        endpointLifecycleManager.doStart();

        HttpEndpoint httpClientEndpoint = (HttpEndpoint) endpointLifecycleManager.get("endpoint");

        assertNotNull(httpClientEndpoint);

        verify(applicationContext, times(1)).getBean(eq(HttpClient.class), any(Endpoint.class));
        verify(httpClientEndpoint.getHttpClient(), times(1)).start();

        assertEquals(httpClientEndpoint, endpointLifecycleManager.getOrDefault("endpoint"));
        assertEquals(httpClientEndpoint, endpointLifecycleManager.getOrDefault("unknown"));

        assertFalse(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertFalse(endpointLifecycleManager.endpoints().isEmpty());
    }

    @Test
    public void shouldStopEndpoint() throws Exception {
        // First, start an endpoint
        io.gravitee.definition.model.Endpoint endpoint = mock(io.gravitee.definition.model.Endpoint.class);

        when(endpoint.getName()).thenReturn("endpoint");
        when(endpoint.isBackup()).thenReturn(false);
        when(proxy.getEndpoints()).thenReturn(Collections.singletonList(endpoint));
        when(applicationContext.getBean(HttpClient.class, endpoint)).thenReturn(mock(HttpClient.class));
        endpointLifecycleManager.doStart();

        assertFalse(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertFalse(endpointLifecycleManager.endpoints().isEmpty());

        HttpEndpoint httpClientEndpoint = (HttpEndpoint) endpointLifecycleManager.get("endpoint");

        // Then, stop endpoint
        endpointLifecycleManager.doStop();

        // Verify that the HTTP client is correctly stopped
        verify(httpClientEndpoint.getHttpClient(), times(1)).stop();

        assertTrue(endpointLifecycleManager.targetByEndpoint().isEmpty());
        assertTrue(endpointLifecycleManager.endpoints().isEmpty());
    }
}
