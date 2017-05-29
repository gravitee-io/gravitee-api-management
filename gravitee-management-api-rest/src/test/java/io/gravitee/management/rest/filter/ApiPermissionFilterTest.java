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
package io.gravitee.management.rest.filter;

import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.permissions.RoleScope;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.RoleService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class ApiPermissionFilterTest {

    @InjectMocks
    protected PermissionsFilter apiPermissionFilter;

    @Mock
    protected ApiService apiService;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected RoleService roleService;

    @Mock
    protected Permissions apiPermissions;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithoutGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(apiPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getRole(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(null);
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            apiPermissionFilter.filter(apiPermissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getRole(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        api.setGroup(new GroupEntity());
        api.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(apiPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getRole(any(MembershipReferenceType.class), anyString(), eq(user.getName()))).thenReturn(null);
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            apiPermissionFilter.filter(apiPermissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(2)).getRole(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsInsufficient() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(apiPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(role.getPermissions()).thenReturn(Collections.singletonMap(ApiPermission.ANALYTICS.getName(), new char[]{RolePermissionAction.CREATE.getId()}));
        when(membershipService.getRole(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(role);
        when(roleService.hasPermission(role, perm.value().getPermission(), perm.acls())).thenReturn(false);

        try {
            apiPermissionFilter.filter(apiPermissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getRole(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithoutGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(apiPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(role.getPermissions()).thenReturn(Collections.singletonMap(ApiPermission.ANALYTICS.getName(), new char[]{RolePermissionAction.UPDATE.getId()}));
        when(membershipService.getRole(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(role);
        when(roleService.hasPermission(role, perm.value().getPermission(), perm.acls())).thenReturn(true);

        apiPermissionFilter.filter(apiPermissions, containerRequestContext);
        verify(membershipService, times(1)).getRole(any(), any(), any());
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        api.setGroup(new GroupEntity());
        api.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(apiPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(role.getPermissions()).thenReturn(Collections.singletonMap(ApiPermission.ANALYTICS.getName(), new char[]{RolePermissionAction.UPDATE.getId()}));
        when(membershipService.getRole(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(null);
        when(membershipService.getRole(MembershipReferenceType.API_GROUP, api.getGroup().getId(), user.getName())).thenReturn(role);
        when(roleService.hasPermission(role, perm.value().getPermission(), perm.acls())).thenReturn(true);

        apiPermissionFilter.filter(apiPermissions, containerRequestContext);
        verify(membershipService, times(2)).getRole(any(), any(), any());
    }
}
