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
import static io.gravitee.rest.api.model.permissions.RolePermission.INTEGRATION_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.ORGANIZATION_TENANT;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
    protected PermissionService permissionService;

    @Mock
    protected Permissions permissions;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    private static final String API_ID = "API_ID";

    private static final String APPLICATION_ID = "APPLICATION_ID";

    private static final String ENVIRONMENT_ID = "DEFAULT";

    private static final String ORGANIZATION_ID = "ORG_ID";

    private static final String INTEGRATION_ID = "INTEGRATION_ID";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class Api {

        @BeforeEach
        public void initApiMock() {
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

        @Test
        public void shouldThrowForbiddenExceptionWhenNoApiPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        public void shouldBeAuthorizedWhenApiPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
            verify(permissionService, times(1)).hasPermission(any(), eq(API_ANALYTICS), eq(API_ID), eq(UPDATE));
        }
    }

    @Nested
    class Application {

        @BeforeEach
        public void initApplicationMock() {
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

        @Test
        public void shouldThrowForbiddenExceptionWhenNoApplicationPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        public void shouldBeAuthorizedWhenApplicationPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(APPLICATION_ANALYTICS), eq(APPLICATION_ID), eq(UPDATE));
        }
    }

    @Nested
    class Environment {

        @BeforeEach
        public void initManagementMocks() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(ENVIRONMENT_API);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        }

        @Test
        public void shouldThrowForbiddenExceptionWhenNoEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        public void shouldBeAuthorizedWhenEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_API), eq(ENVIRONMENT_ID), eq(UPDATE));
        }
    }

    @Nested
    class Organization {

        @BeforeEach
        public void initOrganizationMocks() {
            GraviteeContext.setCurrentEnvironment(null);

            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(ORGANIZATION_TENANT);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { DELETE });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        }

        @Test
        public void shouldThrowForbiddenExceptionWhenNoOrganizationPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        public void shouldBeAuthorizedWhenOrganizationPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(ORGANIZATION_TENANT), eq(ORGANIZATION_ID), eq(DELETE));
        }
    }

    @Nested
    class Integration {

        @BeforeEach
        public void initIntegrationMock() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(INTEGRATION_DEFINITION);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { UPDATE });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put("integrationId", Collections.singletonList(INTEGRATION_ID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        }

        @Test
        public void shouldThrowForbiddenExceptionWhenNoIntegrationPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        public void shouldBeAuthorizedWhenIntegrationPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
            verify(permissionService, times(1)).hasPermission(any(), eq(INTEGRATION_DEFINITION), eq(INTEGRATION_ID), eq(UPDATE));
        }
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
