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
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.permissions.RoleScope;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApplicationService;
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
import java.io.IOException;
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
public class ApplicationPermissionFilterTest {

    @InjectMocks
    protected PermissionsFilter applicationPermissionFilter;

    @Mock
    protected ApplicationService applicationService;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected RoleService roleService;

    @Mock
    protected Permissions applicationPermissions;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithoutGroup() throws IOException {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(RolePermissionAction.values());
        when(applicationPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getRole(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(null);
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            applicationPermissionFilter.filter(applicationPermissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getRole(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        application.setGroup(new GroupEntity());
        application.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(RolePermissionAction.values());
        when(applicationPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getRole(any(MembershipReferenceType.class), anyString(), eq(user.getName()))).thenReturn(null);
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            applicationPermissionFilter.filter(applicationPermissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(2)).getRole(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenRolePermissionActionIsInsufficient() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(applicationPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.APPLICATION);
        when(role.getPermissions()).thenReturn(Collections.singletonMap(ApplicationPermission.ANALYTICS.getName(), new char[]{RolePermissionAction.CREATE.getId()}));
        when(membershipService.getRole(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(role);
        when(roleService.hasPermission(role, perm.value().getPermission(), perm.acls())).thenReturn(false);

        try {
            applicationPermissionFilter.filter(applicationPermissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getRole(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithoutGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(applicationPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.APPLICATION);
        when(role.getPermissions()).thenReturn(Collections.singletonMap(ApplicationPermission.ANALYTICS.getName(), new char[]{RolePermissionAction.UPDATE.getId()}));
        when(membershipService.getRole(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(role);
        when(roleService.hasPermission(role, perm.value().getPermission(), perm.acls())).thenReturn(true);

        applicationPermissionFilter.filter(applicationPermissions, containerRequestContext);
        verify(membershipService, times(1)).getRole(any(), any(), any());
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        application.setGroup(new GroupEntity());
        application.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(applicationPermissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.APPLICATION);
        when(role.getPermissions()).thenReturn(Collections.singletonMap(ApplicationPermission.ANALYTICS.getName(), new char[]{RolePermissionAction.UPDATE.getId()}));
        when(membershipService.getRole(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(null);
        when(membershipService.getRole(MembershipReferenceType.APPLICATION_GROUP, application.getGroup().getId(), user.getName())).thenReturn(role);
        when(roleService.hasPermission(role, perm.value().getPermission(), perm.acls())).thenReturn(true);

        applicationPermissionFilter.filter(applicationPermissions, containerRequestContext);
        verify(membershipService, times(2)).getRole(any(), any(), any());
    }
}
