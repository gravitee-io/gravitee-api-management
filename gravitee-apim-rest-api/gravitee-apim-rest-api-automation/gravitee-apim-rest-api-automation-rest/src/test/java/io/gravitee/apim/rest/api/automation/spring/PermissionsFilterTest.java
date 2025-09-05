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
package io.gravitee.apim.rest.api.automation.spring;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_SUBSCRIPTION;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_API;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
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
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
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

    private static final String HRID = "test-hrid";
    private static final String HRID_PARAM = "hrid";
    private static final String LEGACY_PARAM = "legacy";
    private static final String ORGANIZATION_ID = "ORG_ID";
    private static final String ENVIRONMENT_ID = "DEFAULT";

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
        void initApiMock() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(API_DEFINITION);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { READ });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HRID_PARAM, Collections.singletonList(HRID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
            queryParams.put(LEGACY_PARAM, Collections.singletonList("true"));
            when(uriInfo.getQueryParameters()).thenReturn(queryParams);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            when(containerRequestContext.getMethod()).thenReturn("GET");
        }

        @Test
        void shouldThrowForbiddenExceptionWhenNoApiPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenApiPermissionsWithLegacyHrid() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
            verify(permissionService, times(1)).hasPermission(any(), eq(API_DEFINITION), eq(HRID), eq(READ));
        }

        @Test
        void shouldBeAuthorizedWhenApiPermissions() {
            when(containerRequestContext.getUriInfo().getQueryParameters()).thenReturn(null);
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
            verify(permissionService, times(1))
                .hasPermission(
                    any(),
                    eq(API_DEFINITION),
                    eq(IdBuilder.builder(GraviteeContext.getExecutionContext(), HRID).buildId()),
                    eq(READ)
                );
        }
    }

    @Nested
    class Application {

        @BeforeEach
        void initApplicationMock() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(APPLICATION_DEFINITION);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { READ });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HRID_PARAM, Collections.singletonList(HRID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
            queryParams.put(LEGACY_PARAM, Collections.singletonList("true"));
            when(uriInfo.getQueryParameters()).thenReturn(queryParams);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            when(containerRequestContext.getMethod()).thenReturn("GET");
        }

        @Test
        void shouldThrowForbiddenExceptionWhenNoApplicationPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenApplicationPermissionsWithLegacyHrid() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(APPLICATION_DEFINITION), eq(HRID), eq(READ));
        }

        @Test
        void shouldBeAuthorizedWhenApplicationPermissions() {
            when(containerRequestContext.getUriInfo().getQueryParameters()).thenReturn(null);
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1))
                .hasPermission(
                    any(),
                    eq(APPLICATION_DEFINITION),
                    eq(IdBuilder.builder(GraviteeContext.getExecutionContext(), HRID).buildId()),
                    eq(READ)
                );
        }
    }

    @Nested
    class Environment {

        @BeforeEach
        void initManagementMocks() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(ENVIRONMENT_API);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { CREATE });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        }

        @Test
        void shouldThrowForbiddenExceptionWhenNoEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_API), eq(ENVIRONMENT_ID), eq(CREATE));
        }
    }

    @Nested
    class SharedPolicyGroups {

        @BeforeEach
        void initManagementMocks() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(ENVIRONMENT_SHARED_POLICY_GROUP);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { CREATE });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            UriInfo uriInfo = mock(UriInfo.class);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        }

        @Test
        void shouldThrowForbiddenExceptionWhenNoEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), any(), any())).thenReturn(false);

            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext()),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_SHARED_POLICY_GROUP), eq(ENVIRONMENT_ID), eq(CREATE));
        }
    }
}
