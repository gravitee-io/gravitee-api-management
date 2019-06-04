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
package io.gravitee.rest.api.service;

import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.quality.ApiQualityMetricHealthcheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiQualityMetricHealthcheckTest {

    @InjectMocks
    private ApiQualityMetricHealthcheck srv = new ApiQualityMetricHealthcheck();

    @Test
    public void shouldBeValidWithGlobalHC() {
        ApiEntity api = mock(ApiEntity.class);
        HealthCheckService hcSrv = mock(HealthCheckService.class);
        when(hcSrv.isEnabled()).thenReturn(Boolean.TRUE);
        Services services = new Services();
        services.set(Collections.singletonList(hcSrv));
        when(api.getServices()).thenReturn(services);
        mockProxy(api, false);

        boolean valid = srv.isValid(api);

        assertTrue(valid);
    }

    @Test
    public void shouldBeValidWithEndpointsHC() {
        ApiEntity api = mock(ApiEntity.class);
        EndpointHealthCheckService hcSrv = mock(EndpointHealthCheckService.class);
        when(hcSrv.isEnabled()).thenReturn(Boolean.FALSE);
        Services services = new Services();
        services.set(Collections.singletonList(hcSrv));
        when(api.getServices()).thenReturn(services);
        mockProxy(api, true);

        boolean valid = srv.isValid(api);

        assertTrue(valid);
    }

    @Test
    public void shouldBeInvalidWithOnlyOneEndpointHC() {
        ApiEntity api = mock(ApiEntity.class);
        EndpointHealthCheckService hcSrv = mock(EndpointHealthCheckService.class);
        when(hcSrv.isEnabled()).thenReturn(Boolean.FALSE);
        Services services = new Services();
        services.set(Collections.singletonList(hcSrv));
        when(api.getServices()).thenReturn(services);
        mockProxy(api, false);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    @Test
    public void shouldNotBeValidWithoutServices() {
        ApiEntity api = mock(ApiEntity.class);
        mockProxy(api, false);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    @Test
    public void shouldNotBeValidWithEmptyServices() {
        ApiEntity api = mock(ApiEntity.class);
        Services services = new Services();
        services.set(Collections.emptyList());
        mockProxy(api, false);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }


    @Test
    public void shouldNotBeValidWithDisabledGlobalHC() {
        ApiEntity api = mock(ApiEntity.class);
        HealthCheckService hcSrv = mock(HealthCheckService.class);
        when(hcSrv.isEnabled()).thenReturn(Boolean.FALSE);
        Services services = new Services();
        services.set(Collections.singletonList(hcSrv));
        when(api.getServices()).thenReturn(services);
        mockProxy(api, false);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    @Test
    public void shouldNotBeValidWithAnotherService() {
        ApiEntity api = mock(ApiEntity.class);
        DynamicPropertyService dpSrv = mock(DynamicPropertyService.class);
        when(dpSrv.isEnabled()).thenReturn(Boolean.TRUE);
        Services services = new Services();
        services.set(Collections.singletonList(dpSrv));
        when(api.getServices()).thenReturn(services);
        mockProxy(api, false);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    private void mockProxy(final ApiEntity api, final boolean withHC) {
        final Proxy proxy = mock(Proxy.class);
        when(api.getProxy()).thenReturn(proxy);
        final EndpointGroup endpointGroup = mock(EndpointGroup.class);
        when(proxy.getGroups()).thenReturn(Collections.singleton(endpointGroup));
        final HttpEndpoint endpoint1 = mock(HttpEndpoint.class);
        final HttpEndpoint endpoint2 = mock(HttpEndpoint.class);
        when(endpointGroup.getEndpoints()).thenReturn(new HashSet<>(asList(endpoint1, endpoint2)));

        final EndpointHealthCheckService endpointHealthCheckService1 = mock(EndpointHealthCheckService.class);
        when(endpoint1.getHealthCheck()).thenReturn(endpointHealthCheckService1);
        when(endpointHealthCheckService1.isEnabled()).thenReturn(true);

        final EndpointHealthCheckService endpointHealthCheckService2 = mock(EndpointHealthCheckService.class);
        when(endpoint2.getHealthCheck()).thenReturn(endpointHealthCheckService2);
        when(endpointHealthCheckService2.isEnabled()).thenReturn(withHC);
    }
}
