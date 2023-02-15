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
package io.gravitee.rest.api.management.v4.rest.resource;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.management.v4.rest.JerseySpringTest;
import io.gravitee.rest.api.management.v4.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Autowired
    protected ApiRepository apiRepository;

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiSearchService apiSearchServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiStateService apiStateServiceV4;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected PlanService planService;

    @Autowired
    protected EnvironmentService environmentService;

    @Autowired
    protected EntrypointConnectorPluginService entrypointConnectorPluginService;

    @Autowired
    protected EndpointConnectorPluginService endpointConnectorPluginService;

    @Before
    public void setUp() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Priority(50)
    public static class NotAdminAuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return () -> USER_NAME;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return true;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "BASIC";
                    }
                }
            );
        }
    }
}
