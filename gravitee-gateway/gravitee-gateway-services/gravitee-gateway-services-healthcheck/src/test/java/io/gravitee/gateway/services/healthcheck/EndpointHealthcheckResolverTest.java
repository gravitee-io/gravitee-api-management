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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.gateway.env.GatewayConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointHealthcheckResolverTest {
    @Mock
    private GatewayConfiguration mockGatewayConfiguration;

    @Mock
    private Api mockApi;

    @Mock
    private Proxy mockProxy;

    @Mock
    private EndpointGroup mockEndpointGroup;

    @Mock
    private HttpEndpoint mockEndpoint;

    @Mock
    private EndpointHealthCheckService mockEndpointHealthcheckService;

    @InjectMocks
    private EndpointHealthcheckResolver endpointHealthcheckResolver = new EndpointHealthcheckResolver();

    @Before
    public void before() {
        reset();
        when(mockApi.getId()).thenReturn("api-id");
        when(mockApi.getProxy()).thenReturn(mockProxy);

        when(mockProxy.getGroups()).thenReturn(Collections.singleton(mockEndpointGroup));

        when(mockEndpointGroup.getEndpoints()).thenReturn(new HashSet<>(Arrays.asList(mockEndpoint, mock(Endpoint.class))));

        when(mockEndpoint.getType()).thenReturn(EndpointType.HTTP);
        when(mockEndpoint.isBackup()).thenReturn(false);

        when(mockGatewayConfiguration.tenant()).thenReturn(Optional.empty());
    }

    /**
     * Cases when no HC must be run :
     * no global HC && no local HC
     * global HC disabled && no local HC
     * global HC disabled && local HC disabled
     * global HC enabled && local HC disabled
     * no global HC && local HC disabled
     */

    @Test
    public void shouldNotResolveEndpointWithoutGlobalNorLocal() {
        Services services = new Services();
        services.set(Collections.emptyList());
        when(mockApi.getServices()).thenReturn(services);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithGlobalDisabledAndNoLocal() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);
        services.set(Collections.singleton(healthCheckService));
        when(mockApi.getServices()).thenReturn(services);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithGlobalDisabledAndLocalDisabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);
        services.set(Collections.singleton(healthCheckService));
        when(mockApi.getServices()).thenReturn(services);
        when(mockEndpointHealthcheckService.isEnabled()).thenReturn(false);
        when(mockEndpoint.getHealthCheck()).thenReturn(mockEndpointHealthcheckService);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithoutGlobalAndLocalDisabled() {
        Services services = new Services();
        services.set(Collections.emptyList());
        when(mockApi.getServices()).thenReturn(services);
        when(mockEndpointHealthcheckService.isEnabled()).thenReturn(false);
        when(mockEndpoint.getHealthCheck()).thenReturn(mockEndpointHealthcheckService);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithGlobalEnabledAndLocalDisabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Collections.singleton(healthCheckService));
        when(mockApi.getServices()).thenReturn(services);
        when(mockEndpointHealthcheckService.isEnabled()).thenReturn(false);
        when(mockEndpoint.getHealthCheck()).thenReturn(mockEndpointHealthcheckService);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    /**
     * Cases when HC must be ran :
     * global HC enabled and no local HC
     * global HC enabled and local HC enabled
     * global HC disabled and local HC enabled
     * no global HC and local HC enabled
     */

    @Test
    public void shouldResolveEndpointWithGlobalEnabledAndNoLocal() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Collections.singleton(healthCheckService));
        when(mockApi.getServices()).thenReturn(services);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertEquals("api-id", resolve.get(0).api());
    }

    @Test
    public void shouldResolveEndpointWithGlobalEnabledAndLocalEnabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Collections.singleton(healthCheckService));
        when(mockApi.getServices()).thenReturn(services);
        when(mockEndpointHealthcheckService.isEnabled()).thenReturn(true);
        when(mockEndpoint.getHealthCheck()).thenReturn(mockEndpointHealthcheckService);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertEquals("api-id", resolve.get(0).api());
    }

    @Test
    public void shouldResolveEndpointWithGlobalDisabledAndLocalEnabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);
        services.set(Collections.singleton(healthCheckService));
        when(mockApi.getServices()).thenReturn(services);
        when(mockEndpointHealthcheckService.isEnabled()).thenReturn(true);
        when(mockEndpoint.getHealthCheck()).thenReturn(mockEndpointHealthcheckService);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertEquals("api-id", resolve.get(0).api());
    }

    @Test
    public void shouldResolveEndpointWithoutGlobalAndLocalEnabled() {
        Services services = new Services();
        services.set(Collections.emptyList());
        when(mockApi.getServices()).thenReturn(services);
        when(mockEndpointHealthcheckService.isEnabled()).thenReturn(true);
        when(mockEndpoint.getHealthCheck()).thenReturn(mockEndpointHealthcheckService);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertEquals("api-id", resolve.get(0).api());
    }
}
