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

import static io.gravitee.rest.api.model.permissions.RolePermission.*;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.security.Principal;
import java.util.Collections;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PermissionFilterTest {

    @InjectMocks
    protected PermissionsFilter permissionFilter;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected PermissionService permissionService;

    @Mock
    protected Permissions permissions;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    private static final String USERNAME = "USERNAME";

    public static final String API_ID = "API_ID";

    public static final String APPLICATION_ID = "APPLICATION_ID";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * API Tests
     */
    private ApiEntity initApiMocks() {
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Principal user = () -> USERNAME;
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
        when(permissions.value()).thenReturn(new Permission[] { perm });
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        return api;
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoApiPermissions() {
        initApiMocks();
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
        } catch (ForbiddenAccessException e) {
            verify(permissionService, times(1)).hasPermission(any(), eq(API_ANALYTICS), eq(API_ID), eq(UPDATE));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenApiPermissions() {
        initApiMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
        verify(permissionService, times(1)).hasPermission(any(), eq(API_ANALYTICS), eq(API_ID), eq(UPDATE));
    }

    /**
     * APPLICATION Tests
     */
    private ApplicationEntity initApplicationMocks() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        Principal user = () -> USERNAME;
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
        when(permissions.value()).thenReturn(new Permission[] { perm });
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        return application;
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoApplicationPermissions() {
        initApplicationMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
        } catch (ForbiddenAccessException e) {
            verify(permissionService, times(1)).hasPermission(any(), eq(APPLICATION_ANALYTICS), eq(APPLICATION_ID), eq(UPDATE));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenApplicationPermissions() {
        initApplicationMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

        verify(permissionService, times(1)).hasPermission(any(), eq(APPLICATION_ANALYTICS), eq(APPLICATION_ID), eq(UPDATE));
    }

    /**
     * ENVIRONMENT Tests
     */

    private void initManagementMocks() {
        Principal user = () -> USERNAME;
        when(securityContext.getUserPrincipal()).thenReturn(user);
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(ENVIRONMENT_API);
        when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
        when(permissions.value()).thenReturn(new Permission[] { perm });
        UriInfo uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoManagementPermissions() {
        initManagementMocks();
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
        } catch (ForbiddenAccessException e) {
            verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_API), eq("DEFAULT"), eq(UPDATE));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenManagementPermissions() {
        initManagementMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

        verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_API), eq("DEFAULT"), eq(UPDATE));
    }
}
