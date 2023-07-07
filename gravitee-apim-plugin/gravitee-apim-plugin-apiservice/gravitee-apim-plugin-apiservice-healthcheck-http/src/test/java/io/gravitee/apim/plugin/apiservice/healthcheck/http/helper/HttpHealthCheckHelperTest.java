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
package io.gravitee.apim.plugin.apiservice.healthcheck.http.helper;

import static io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckService.HTTP_HEALTH_CHECK_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpointGroup;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class HttpHealthCheckHelperTest {

    public static final String TENANT_ID = "tenant";

    @Mock
    private EndpointGroup endpointGroup;

    @Mock
    private Endpoint endpoint;

    @Mock
    private Api api;

    private ManagedEndpoint managedEndpoint;

    @BeforeEach
    public void setup() {
        lenient().when(api.getEndpointGroups()).thenReturn(List.of(endpointGroup));
        this.managedEndpoint = new DefaultManagedEndpoint(endpoint, new DefaultManagedEndpointGroup(endpointGroup), null);
    }

    @Nested
    class CanNotHandle {

        @Test
        public void when_endpoint_group_is_empty() {
            assertFalse(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpoint_has_no_service() {
            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            assertFalse(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpointGroup_has_health_check_disabled() {
            final EndpointGroupServices services = new EndpointGroupServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(false);
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpointGroup.getServices()).thenReturn(services);

            assertFalse(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpointGroup_has_non_http_health_check_enabled() {
            final EndpointGroupServices services = new EndpointGroupServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType("unknown");
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpointGroup.getServices()).thenReturn(services);

            assertFalse(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpoint_has_health_check_disabled() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(false);
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpoint.getServices()).thenReturn(services);
            assertFalse(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpoint_has_non_http_health_check_enabled() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType("unknonwn");
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpoint.getServices()).thenReturn(services);
            assertFalse(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpoint_does_not_define_tenant() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(endpoint.getTenants()).thenReturn(List.of());

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpoint.getServices()).thenReturn(services);
            assertFalse(HttpHealthCheckHelper.canHandle(api, TENANT_ID));
        }

        @Test
        public void when_endpoint_defines_invalid_tenant() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(endpoint.getTenants()).thenReturn(List.of(UUID.randomUUID().toString()));

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpoint.getServices()).thenReturn(services);
            assertFalse(HttpHealthCheckHelper.canHandle(api, TENANT_ID));
        }
    }

    @Nested
    class CanHandle {

        @Test
        public void when_endpointGroup_has_health_check_enabled() {
            final EndpointGroupServices services = new EndpointGroupServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getServices()).thenReturn(services);

            assertTrue(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpoint_has_health_check_enabled() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpoint.getServices()).thenReturn(services);
            assertTrue(HttpHealthCheckHelper.canHandle(api, null));
        }

        @Test
        public void when_endpoint_defines_tenant() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(endpoint.getTenants()).thenReturn(List.of(TENANT_ID));

            when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));
            when(endpoint.getServices()).thenReturn(services);
            assertTrue(HttpHealthCheckHelper.canHandle(api, TENANT_ID));
        }
    }

    @Nested
    class AManagedEndpoint {

        @Test
        public void has_health_check_disable_when_no_service_is_defined() {
            assertFalse(HttpHealthCheckHelper.isServiceEnabled(managedEndpoint, TENANT_ID));
        }

        @Test
        public void has_health_check_disable_when_service_is_with_wrong_tenant() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);
            when(endpoint.getTenants()).thenReturn(List.of("unknown"));
            when(endpoint.getServices()).thenReturn(services);
            assertFalse(
                HttpHealthCheckHelper.isServiceEnabled(
                    new DefaultManagedEndpoint(endpoint, new DefaultManagedEndpointGroup(endpointGroup), null),
                    TENANT_ID
                )
            );
        }

        @Test
        public void has_health_check_enable_when_service_linked_to_right_tenant() {
            final EndpointServices services = new EndpointServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);
            when(endpoint.getTenants()).thenReturn(List.of(TENANT_ID));
            when(endpoint.getServices()).thenReturn(services);
            assertTrue(
                HttpHealthCheckHelper.isServiceEnabled(
                    new DefaultManagedEndpoint(endpoint, new DefaultManagedEndpointGroup(endpointGroup), null),
                    TENANT_ID
                )
            );
        }

        @Test
        public void has_health_check_enable_when_endpoint_group_enable_it() {
            final EndpointGroupServices services = new EndpointGroupServices();
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(endpointGroup.getServices()).thenReturn(services);
            assertTrue(
                HttpHealthCheckHelper.isServiceEnabled(
                    new DefaultManagedEndpoint(endpoint, new DefaultManagedEndpointGroup(endpointGroup), null),
                    TENANT_ID
                )
            );
        }
    }
}
