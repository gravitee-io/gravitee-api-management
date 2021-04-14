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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 */
public class ApiResourceNotAdminTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String UNKNOWN_API = "unknown";

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

    private ApiEntity mockApi;
    private UpdateApiEntity updateApiEntity;

    @Before
    public void init() {
        mockApi = new ApiEntity();
        mockApi.setId(API);
        mockApi.setName(API);
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        mockApi.setProxy(proxy);
        mockApi.setUpdatedAt(new Date());
        doReturn(mockApi).when(apiService).findById(API);
    }

    @After
    public void cleanUp() {
        reset(membershipService);
        reset(roleService);
        reset(apiService);
    }

    @Test
    public void shouldGetApi() {
        final Response response = target(API).request().get();

        assertEquals(OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNotNull(responseApi);
        assertEquals(API, responseApi.getName());
    }

    @Test
    public void shouldNotAccessToApiState_BecauseNotAMember() {
        final Response response = target(API + "/state").request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldAccessToApiState_BecauseDirectMember() {
        MembershipEntity membershipEntity = mock(MembershipEntity.class);
        when(membershipEntity.getReferenceType()).thenReturn(MembershipReferenceType.API);
        when(membershipEntity.getReferenceId()).thenReturn(API);
        when(
            membershipService.getMembershipsByMemberAndReference(
                eq(MembershipMemberType.USER),
                eq(USER_NAME),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Sets.newSet(membershipEntity));

        final Response response = target(API + "/state").request().get();

        assertEquals(OK_200, response.getStatus());
        ApiStateEntity stateEntity = response.readEntity(ApiStateEntity.class);
        assertNotNull(stateEntity);
        assertEquals(API, stateEntity.getApiId());
    }

    @Test
    public void shouldAccessToApiState_BecauseGroupMember_onApi() {
        final String groupId = "group_id";
        final String roleId = "role_id";
        MembershipEntity membershipEntity = mock(MembershipEntity.class);
        when(membershipEntity.getRoleId()).thenReturn(roleId);
        when(membershipEntity.getReferenceId()).thenReturn(groupId);
        when(membershipEntity.getReferenceType()).thenReturn(MembershipReferenceType.GROUP);

        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);

        when(
            membershipService.getMembershipsByMemberAndReference(
                eq(MembershipMemberType.USER),
                eq(USER_NAME),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.emptySet());

        when(
            membershipService.getMembershipsByMemberAndReference(
                eq(MembershipMemberType.USER),
                eq(USER_NAME),
                eq(MembershipReferenceType.GROUP)
            )
        )
            .thenReturn(Sets.newSet(membershipEntity));

        when(roleService.findById(eq(roleId))).thenReturn(role);
        when(apiService.searchIds(any())).thenReturn(Collections.singletonList(mockApi.getId()));

        final Response response = target(API + "/state").request().get();

        assertEquals(OK_200, response.getStatus());
        ApiStateEntity stateEntity = response.readEntity(ApiStateEntity.class);
        assertNotNull(stateEntity);
        assertEquals(API, stateEntity.getApiId());
    }

    public void shouldNotAccessToApiState_BecauseGroupMember_onApplication() {
        final String groupId = "group_id";
        final String roleId = "role_id";
        MembershipEntity membershipEntity = mock(MembershipEntity.class);
        when(membershipEntity.getRoleId()).thenReturn(roleId);
        when(membershipEntity.getReferenceId()).thenReturn(groupId);
        when(membershipEntity.getReferenceType()).thenReturn(MembershipReferenceType.GROUP);

        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.APPLICATION);

        when(
            membershipService.getMembershipsByMemberAndReference(
                eq(MembershipMemberType.USER),
                eq(USER_NAME),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.emptySet());

        when(
            membershipService.getMembershipsByMemberAndReference(
                eq(MembershipMemberType.USER),
                eq(USER_NAME),
                eq(MembershipReferenceType.GROUP)
            )
        )
            .thenReturn(Sets.newSet(membershipEntity));

        when(roleService.findById(eq(roleId))).thenReturn(role);

        final Response response = target(API + "/state").request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());
    }
}
