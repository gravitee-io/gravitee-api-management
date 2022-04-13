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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.CustomUserFieldEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomUserFieldsResourceNotAdminTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/custom-user-fields";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(CustomUserFieldsResourceNotAdminTest.AuthenticationFilter.class);
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

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

    @Test
    public void shouldNotCreateField() {
        reset(customUserFieldService);
        final CustomUserFieldEntity field = new CustomUserFieldEntity();
        field.setKey("TestResCreate");
        field.setLabel("TestResCreate");

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        final Response response = orgTarget().request().post(Entity.json(field));

        assertEquals(FORBIDDEN_403, response.getStatus());
        verify(customUserFieldService, never()).create(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldNotUpdateField() {
        reset(customUserFieldService);
        final CustomUserFieldEntity field = new CustomUserFieldEntity();
        field.setKey("test-update");
        field.setLabel("Test");

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        final Response response = orgTarget("/" + field.getKey()).request().put(Entity.json(field));

        assertEquals(FORBIDDEN_403, response.getStatus());
        verify(customUserFieldService, never()).update(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldNotDeleteField() {
        reset(customUserFieldService);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        final Response response = orgTarget("/some-key").request().delete();

        assertEquals(FORBIDDEN_403, response.getStatus());
        verify(customUserFieldService, never()).delete(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldListAllFields() {
        reset(customUserFieldService);

        final Response response = orgTarget().request().get();

        assertEquals(OK_200, response.getStatus());
        verify(customUserFieldService).listAllFields(any());
    }
}
