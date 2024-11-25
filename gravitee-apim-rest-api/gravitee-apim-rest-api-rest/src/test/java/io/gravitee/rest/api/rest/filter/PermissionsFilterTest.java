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
package io.gravitee.rest.api.rest.filter;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_API;
import static io.gravitee.rest.api.model.permissions.RolePermission.ORGANIZATION_TENANT;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PermissionsFilterTest {

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

    private static final String API_ID = "API_ID";

    private static final String APPLICATION_ID = "APPLICATION_ID";

    private static final String ENVIRONMENT_ID = "DEFAULT";

    private static final String ORGANIZATION_ID = "ORG_ID";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    /**
     * API Tests
     */
    private void initApiMock() {
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(API_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
        when(permissions.value()).thenReturn(new Permission[] { perm });
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(API_ID));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoApiPermissions() {
        initApiMock();
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
        initApiMock();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
        verify(permissionService, times(1)).hasPermission(any(), eq(API_ANALYTICS), eq(API_ID), eq(UPDATE));
    }

    /**
     * APPLICATION Tests
     */
    private void initApplicationMock() {
        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(APPLICATION_ANALYTICS);
        when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
        when(permissions.value()).thenReturn(new Permission[] { perm });
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(APPLICATION_ID));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoApplicationPermissions() {
        initApplicationMock();
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
        initApplicationMock();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

        verify(permissionService, times(1)).hasPermission(any(), eq(APPLICATION_ANALYTICS), eq(APPLICATION_ID), eq(UPDATE));
    }

    /**
     * ENVIRONMENT Tests
     */

    private void initManagementMocks() {
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
            verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_API), eq(ENVIRONMENT_ID), eq(UPDATE));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenManagementPermissions() {
        initManagementMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

        verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_API), eq(ENVIRONMENT_ID), eq(UPDATE));
    }

    /**
     * ORGANIZATION Tests
     */

    private void initOrganizationMocks() {
        GraviteeContext.setCurrentEnvironment(null);

        Permission perm = mock(Permission.class);
        when(perm.value()).thenReturn(ORGANIZATION_TENANT);
        when(perm.acls()).thenReturn(new RolePermissionAction[] { DELETE });
        when(permissions.value()).thenReturn(new Permission[] { perm });
        UriInfo uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenNoOrganizationPermissions() {
        initOrganizationMocks();
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

        try {
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
        } catch (ForbiddenAccessException e) {
            verify(permissionService, times(1)).hasPermission(any(), eq(ORGANIZATION_TENANT), eq(ORGANIZATION_ID), eq(DELETE));
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldBeAuthorizedWhenOrganizationPermissions() {
        initOrganizationMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

        verify(permissionService, times(1)).hasPermission(any(), eq(ORGANIZATION_TENANT), eq(ORGANIZATION_ID), eq(DELETE));
    }

    @Test
    public void shouldBeAuthorizedWhenOrganizationAndEnvironmentPermissions() {
        GraviteeContext.setCurrentEnvironment(null);
        Permission orgPerm = mock(Permission.class);
        when(orgPerm.value()).thenReturn(ORGANIZATION_TENANT);
        when(orgPerm.acls()).thenReturn(new RolePermissionAction[] { DELETE });
        Permission envPerm = mock(Permission.class);
        when(envPerm.value()).thenReturn(ENVIRONMENT_API);
        when(envPerm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
        when(permissions.value()).thenReturn(new Permission[] { envPerm, orgPerm });

        UriInfo uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        when(permissionService.hasPermission(any(), eq(ORGANIZATION_TENANT), any(), any())).thenReturn(true);

        permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

        verify(permissionService, times(1)).hasPermission(any(), eq(ORGANIZATION_TENANT), eq(ORGANIZATION_ID), eq(DELETE));
    }
}
