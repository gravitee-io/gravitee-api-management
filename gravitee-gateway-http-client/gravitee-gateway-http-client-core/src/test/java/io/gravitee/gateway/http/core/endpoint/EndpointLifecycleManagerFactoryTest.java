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
package io.gravitee.gateway.http.core.endpoint;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.http.core.endpoint.impl.DefaultEndpointLifecycleManager;
import io.gravitee.gateway.http.core.endpoint.impl.tenant.MultiTenantAwareEndpointLifecycleManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointLifecycleManagerFactoryTest {

    @InjectMocks
    private EndpointLifecycleManagerFactory endpointLifecycleManagerFactory;

    @Mock
    private Api api;

    @Mock
    private Proxy proxy;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(api.getProxy()).thenReturn(proxy);
    }

    @Test
    public void shouldCreateMultiTenantEndpointManager() throws Exception {
        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("asia"));
        EndpointLifecycleManager endpointLifecycleManager = endpointLifecycleManagerFactory.doCreateInstance();
        Assert.assertEquals(MultiTenantAwareEndpointLifecycleManager.class, endpointLifecycleManager.getClass());
    }

    @Test
    public void shouldCreateDefaultEndpointManager() throws Exception {
        when(gatewayConfiguration.tenant()).thenReturn(Optional.empty());
        EndpointLifecycleManager endpointLifecycleManager = endpointLifecycleManagerFactory.doCreateInstance();
        Assert.assertEquals(DefaultEndpointLifecycleManager.class, endpointLifecycleManager.getClass());
    }

    @Test
    public void shouldReturnEndpointManagerClass() {
        Class<?> objectType = endpointLifecycleManagerFactory.getObjectType();
        Assert.assertEquals(EndpointLifecycleManager.class, objectType);
    }
}
