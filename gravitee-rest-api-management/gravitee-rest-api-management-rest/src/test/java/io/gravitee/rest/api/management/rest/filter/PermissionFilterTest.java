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
package io.gravitee.rest.api.management.rest.filter;

import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.filter.PermissionsFilter;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class PermissionFilterTest {

    @InjectMocks
    protected PermissionsFilter permissionFilter;

    @Mock
    protected ApiService apiService;

    @Mock
    protected ApplicationService applicationService;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected RoleService roleService;

    @Mock
    protected Permissions permissions;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    private final static String USERNAME = "USERNAME";

    public static final String API_ID = "API_ID";

    public static final String APPLICATION_ID = "APPLICATION_ID";

    @Before
    public void setUp() {
        initMocks(this);
    }

    /**
     * API Tests
     */
    private ApiEntity initApiMocks() {
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Principal user = () -> USERNAME;
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(permissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        return api;
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoApiPermissions() {
        ApiEntity api = initApiMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(apiService, times(1)).findById(api.getId());
            verify(applicationService, never()).findById(any());
            verify(roleService, times(1)).hasPermission(any(), any(), any());
            verify(membershipService, times(1)).getMemberPermissions(api, USERNAME);
            verify(membershipService, never()).getRole(any(), any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenApiPermissions() {
        ApiEntity api = initApiMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext);
        verify(apiService, times(1)).findById(api.getId());
        verify(applicationService, never()).findById(any());
        verify(roleService, times(1)).hasPermission(any(), any(), any());
        verify(membershipService, times(1)).getMemberPermissions(api, USERNAME);
        verify(membershipService, never()).getRole(any(), any(), any(), any());
    }

    /**
     * APPLICATION Tests
     */
    private ApplicationEntity initApplicationMocks() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        Principal user = () -> USERNAME;
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(permissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        return application;
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoApplicationPermissions() {
        ApplicationEntity application = initApplicationMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(applicationService, times(1)).findById(application.getId());
            verify(apiService, never()).findById(any());
            verify(roleService, times(1)).hasPermission(any(), any(), any());
            verify(membershipService, times(1)).getMemberPermissions(application, USERNAME);
            verify(membershipService, never()).getRole(any(), any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenApplicationPermissions() {
        ApplicationEntity application = initApplicationMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext);
        verify(apiService, never()).findById(any());
        verify(applicationService, times(1)).findById(application.getId());
        verify(roleService, times(1)).hasPermission(any(), any(), any());
        verify(membershipService, times(1)).getMemberPermissions(application, USERNAME);
        verify(membershipService, never()).getRole(any(), any(), any(), any());
    }

    /**
     * MANAGEMENT Tests
     */

    private void initManagementMocks() {
        Principal user = () -> USERNAME;
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.MANAGEMENT_API);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(permissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getRole(any(), any(), any(), any())).thenReturn(mock(RoleEntity.class));
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoManagementPermissions() {
        initManagementMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(applicationService, never()).findById(any());
            verify(apiService, never()).findById(any());
            verify(roleService, times(1)).hasPermission(any(), any(), any());
            verify(membershipService, never()).getMemberPermissions(any(ApiEntity.class), any());
            verify(membershipService, never()).getMemberPermissions(any(ApplicationEntity.class), any());
            verify(membershipService, times(1)).getRole(eq(MembershipReferenceType.MANAGEMENT), any(), any(), eq(RoleScope.MANAGEMENT));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenManagementPermissions() {
        initManagementMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext);

        verify(applicationService, never()).findById(any());
        verify(apiService, never()).findById(any());
        verify(roleService, times(1)).hasPermission(any(), any(), any());
        verify(membershipService, never()).getMemberPermissions(any(ApiEntity.class), any());
        verify(membershipService, never()).getMemberPermissions(any(ApplicationEntity.class), any());
        verify(membershipService, times(1)).getRole(eq(MembershipReferenceType.MANAGEMENT), any(), any(), eq(RoleScope.MANAGEMENT));
    }

    /**
     * PORTAL Tests
     */

    private void initPortalMocks() {
        Principal user = () -> USERNAME;
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(RolePermission.PORTAL_METADATA);
        when(perm.acls()).thenReturn(new RolePermissionAction[]{RolePermissionAction.UPDATE});
        when(permissions.value()).thenReturn(new Permission[]{perm});
        UriInfo uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getRole(any(), any(), any(), any())).thenReturn(mock(RoleEntity.class));
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoPortalPermissions() {
        initPortalMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(applicationService, never()).findById(any());
            verify(apiService, never()).findById(any());
            verify(roleService, times(1)).hasPermission(any(), any(), any());
            verify(membershipService, never()).getMemberPermissions(any(ApiEntity.class), any());
            verify(membershipService, never()).getMemberPermissions(any(ApplicationEntity.class), any());
            verify(membershipService, times(1)).getRole(eq(MembershipReferenceType.PORTAL), any(), any(), eq(RoleScope.PORTAL));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenPortalPermissions() {
        initPortalMocks();
        when(roleService.hasPermission(any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext);

        verify(applicationService, never()).findById(any());
        verify(apiService, never()).findById(any());
        verify(roleService, times(1)).hasPermission(any(), any(), any());
        verify(membershipService, never()).getMemberPermissions(any(ApiEntity.class), any());
        verify(membershipService, never()).getMemberPermissions(any(ApplicationEntity.class), any());
        verify(membershipService, times(1)).getRole(eq(MembershipReferenceType.PORTAL), any(), any(), eq(RoleScope.PORTAL));
    }
}
