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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.GroupMemberEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiGroupsResourceTest extends AbstractResourceTest {

    private static final String API_ID = "8b8a03a1-429e-4cd2-a860-b258300c2b80";
    private static final String ENVIRONMENT = "DEFAULT";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(ApiResourceNotAdminTest.AuthenticationFilter.class);
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

    @Before
    public void init() {
        Mockito.reset(apiService);
        Mockito.reset(roleService);
    }

    @Test
    public void shouldReturn500IfTechnicalException() {
        ApiEntity api = Mockito.mock(ApiEntity.class);

        when(apiService.findById(API_ID)).thenReturn(api);

        when(roleService.hasPermission(any(), eq(ApiPermission.MEMBER), eq(new RolePermissionAction[] { RolePermissionAction.READ })))
            .thenReturn(true);

        when(apiService.getGroupsWithMembers(ENVIRONMENT, API_ID)).thenThrow(new TechnicalManagementException());

        Response response = envTarget(API_ID).path("groups").request().get();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldReturn403IfNotGranted() {
        Response response = envTarget(API_ID).path("groups").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldReturnGroupsIfGranted() {
        ApiEntity api = Mockito.mock(ApiEntity.class);

        when(apiService.findById(API_ID)).thenReturn(api);

        Map<String, char[]> userPermissions = Map.of(ApiPermission.MEMBER.name(), "R".toCharArray());

        when(membershipService.getUserMemberPermissions(ENVIRONMENT, api, USER_NAME)).thenReturn(userPermissions);

        when(roleService.hasPermission(eq(userPermissions), any(), any())).thenReturn(true);

        when(apiService.getGroupsWithMembers(ENVIRONMENT, API_ID))
            .thenReturn(Map.of(UuidString.generateRandom(), List.of(new GroupMemberEntity())));

        Response response = envTarget(API_ID).path("groups").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Map<String, List<MemberEntity>> responseBody = response.readEntity(new GenericType<>() {});

        assertEquals(1, responseBody.size());
    }
}
