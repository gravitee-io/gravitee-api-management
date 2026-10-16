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

import static io.gravitee.apim.rest.api.automation.helpers.ApiIdResolver.AUTHORIZED_API_ID_PROPERTY;
import static io.gravitee.apim.rest.api.automation.helpers.HRIDHelper.HEADER_X_GRAVITEE_SET_HRID;
import static io.gravitee.apim.rest.api.automation.spring.PermissionsFilter.API_HRID_PATH_PARAM;
import static io.gravitee.apim.rest.api.automation.spring.PermissionsFilter.HRID_CONTAINS_API_QUERY_UUID_PARAM;
import static io.gravitee.apim.rest.api.automation.spring.PermissionsFilter.HRID_CONTAINS_UUID_QUERY_PARAM;
import static io.gravitee.apim.rest.api.automation.spring.PermissionsFilter.HRID_PATH_PARAM;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_DOCUMENTATION;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_SUBSCRIPTION;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_API;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_DICTIONARY;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.rest.api.automation.resource.ApiDocumentationResource;
import io.gravitee.apim.rest.api.automation.resource.ApiResource;
import io.gravitee.apim.rest.api.automation.resource.ApiSubscriptionResource;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
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
class PermissionsFilterTest {

    @InjectMocks
    protected PermissionsFilter permissionFilter;

    @Mock
    protected PermissionService permissionService;

    @Mock
    protected Permissions permissions;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    @Mock
    protected ResourceInfo resourceInfo;

    private static final String HRID = "test-hrid";
    private static final String API_HRID = "api-hrid";
    private static final String ORGANIZATION_ID = "ORG_ID";
    private static final String ENVIRONMENT_ID = "DEFAULT";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class Api {

        private final MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        private final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();

        @BeforeEach
        void initApiMock() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(API_DEFINITION);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { READ });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            doReturn(ApiResource.class).when(resourceInfo).getResourceClass();
            UriInfo uriInfo = mock(UriInfo.class);
            MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(API_HRID_PATH_PARAM, Collections.singletonList(API_HRID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            when(uriInfo.getQueryParameters()).thenReturn(queryParams);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            when(containerRequestContext.getHeaders()).thenReturn(headers);
            when(containerRequestContext.getMethod()).thenReturn("GET");
            when(permissionService.hasPermission(any(), eq(API_DEFINITION), any(), eq(READ))).thenReturn(true);
        }

        @Test
        void should_throw_forbidden_when_no_api_permissions() {
            when(permissionService.hasPermission(any(), eq(API_DEFINITION), any(), eq(READ))).thenReturn(false);

            ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void should_authorize_the_hrid_derived_id_without_flags() {
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(hridDerivedId(API_HRID)), eq(READ));
        }

        @Test
        void should_authorize_the_raw_id_when_hrid_contains_uuid() {
            queryParams.put(HRID_CONTAINS_UUID_QUERY_PARAM, List.of("true"));

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(API_HRID), eq(READ));
        }

        @Test
        void should_hand_the_authorized_api_id_to_the_resource() {
            queryParams.put(HRID_CONTAINS_UUID_QUERY_PARAM, List.of("true"));

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(containerRequestContext).setProperty(AUTHORIZED_API_ID_PROPERTY, API_HRID);
        }

        @Test
        void should_authorize_the_raw_id_when_set_hrid_header_is_sent_on_get() {
            givenSetHridHeader("true");

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(API_HRID), eq(READ));
        }

        @Test
        void should_authorize_the_raw_id_when_set_hrid_header_is_sent_on_head() {
            when(containerRequestContext.getMethod()).thenReturn("HEAD");
            givenSetHridHeader("true");

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(API_HRID), eq(READ));
        }

        @Test
        void should_read_the_first_set_hrid_header_like_the_resource_when_it_is_repeated() {
            givenSetHridHeader("true", "true");

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(API_HRID), eq(READ));
        }

        @Test
        void should_ignore_set_hrid_header_on_delete() {
            when(containerRequestContext.getMethod()).thenReturn("DELETE");
            givenSetHridHeader("true");

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(hridDerivedId(API_HRID)), eq(READ));
        }

        @Test
        void should_ignore_hrid_contains_api_uuid() {
            queryParams.put(HRID_CONTAINS_API_QUERY_UUID_PARAM, List.of("true"));

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DEFINITION), eq(hridDerivedId(API_HRID)), eq(READ));
        }

        // Mirrors a real request context: getHeaderString joins repeated headers with ','
        private void givenSetHridHeader(String... values) {
            headers.put(HEADER_X_GRAVITEE_SET_HRID, List.of(values));
            when(containerRequestContext.getHeaderString(HEADER_X_GRAVITEE_SET_HRID)).thenReturn(String.join(",", values));
        }
    }

    @Nested
    class ApiDocumentation {

        @BeforeEach
        void initApiDocumentationMock() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(API_DOCUMENTATION);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { READ });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            doReturn(ApiDocumentationResource.class).when(resourceInfo).getResourceClass();
            UriInfo uriInfo = mock(UriInfo.class);
            MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(API_HRID_PATH_PARAM, Collections.singletonList(API_HRID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
            queryParams.put(HRID_CONTAINS_UUID_QUERY_PARAM, List.of("true"));
            queryParams.put(HRID_CONTAINS_API_QUERY_UUID_PARAM, List.of("true"));
            when(uriInfo.getQueryParameters()).thenReturn(queryParams);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            when(containerRequestContext.getMethod()).thenReturn("GET");
            when(permissionService.hasPermission(any(), eq(API_DOCUMENTATION), any(), eq(READ))).thenReturn(true);
        }

        @Test
        void should_authorize_the_hrid_derived_id_whatever_the_flags() {
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_DOCUMENTATION), eq(hridDerivedId(API_HRID)), eq(READ));
        }
    }

    private static String hridDerivedId(String apiHrid) {
        return HRIDToUUID.api().context(GraviteeContext.getExecutionContext()).hrid(apiHrid).id();
    }

    @Nested
    class ApiSubscriptions {

        @BeforeEach
        void initApiMock() {
            Permission perm = mock(Permission.class);
            when(perm.value()).thenReturn(API_SUBSCRIPTION);
            when(perm.acls()).thenReturn(new RolePermissionAction[] { READ });
            when(permissions.value()).thenReturn(new Permission[] { perm });
            doReturn(ApiSubscriptionResource.class).when(resourceInfo).getResourceClass();
            UriInfo uriInfo = mock(UriInfo.class);
            MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(API_HRID_PATH_PARAM, Collections.singletonList(API_HRID));
            map.put(HRID_PATH_PARAM, Collections.singletonList(HRID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
            queryParams.put(HRID_CONTAINS_UUID_QUERY_PARAM, Collections.singletonList("true"));
            queryParams.put(HRID_CONTAINS_API_QUERY_UUID_PARAM, Collections.singletonList("true"));
            when(uriInfo.getQueryParameters()).thenReturn(queryParams);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            when(containerRequestContext.getMethod()).thenReturn("GET");
        }

        @Test
        void shouldThrowForbiddenExceptionWhenNoApiPermissions() {
            when(permissionService.hasPermission(any(), eq(API_SUBSCRIPTION), eq(API_HRID), eq(READ))).thenReturn(false);

            ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenApiPermissionsWithLegacyHrid() {
            when(permissionService.hasPermission(any(), eq(API_SUBSCRIPTION), eq(API_HRID), eq(READ))).thenReturn(true);
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
            verify(permissionService, times(1)).hasPermission(any(), eq(API_SUBSCRIPTION), eq(API_HRID), eq(READ));
        }

        @Test
        void shouldBeAuthorizedWhenApiPermissions() {
            when(containerRequestContext.getUriInfo().getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
            when(
                permissionService.hasPermission(
                    any(),
                    eq(API_SUBSCRIPTION),
                    eq(HRIDToUUID.api().context(GraviteeContext.getExecutionContext()).hrid(API_HRID).id()),
                    eq(READ)
                )
            ).thenReturn(true);
            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());
            verify(permissionService, times(1)).hasPermission(
                any(),
                eq(API_SUBSCRIPTION),
                eq(HRIDToUUID.api().context(GraviteeContext.getExecutionContext()).hrid(API_HRID).id()),
                eq(READ)
            );
        }

        @Test
        void should_authorize_the_hrid_derived_api_id_when_only_the_subscription_hrid_contains_uuid() {
            MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
            queryParams.put(HRID_CONTAINS_UUID_QUERY_PARAM, List.of("true"));
            when(containerRequestContext.getUriInfo().getQueryParameters()).thenReturn(queryParams);
            when(permissionService.hasPermission(any(), eq(API_SUBSCRIPTION), any(), eq(READ))).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService).hasPermission(any(), eq(API_SUBSCRIPTION), eq(hridDerivedId(API_HRID)), eq(READ));
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
            map.put(HRID_PATH_PARAM, Collections.singletonList(HRID));
            when(uriInfo.getPathParameters()).thenReturn(map);
            MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
            queryParams.put(HRID_CONTAINS_UUID_QUERY_PARAM, Collections.singletonList("true"));
            when(uriInfo.getQueryParameters()).thenReturn(queryParams);
            when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            when(containerRequestContext.getMethod()).thenReturn("GET");
        }

        @Test
        void shouldThrowForbiddenExceptionWhenNoApplicationPermissions() {
            when(permissionService.hasPermission(any(), eq(APPLICATION_DEFINITION), eq(HRID), eq(READ))).thenReturn(false);

            ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenApplicationPermissionsWithLegacyHrid() {
            when(permissionService.hasPermission(any(), eq(APPLICATION_DEFINITION), eq(HRID), eq(READ))).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(APPLICATION_DEFINITION), eq(HRID), eq(READ));
        }

        @Test
        void shouldBeAuthorizedWhenApplicationPermissions() {
            when(containerRequestContext.getUriInfo().getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
            when(
                permissionService.hasPermission(
                    any(),
                    eq(APPLICATION_DEFINITION),
                    eq(HRIDToUUID.api().context(GraviteeContext.getExecutionContext()).hrid(HRID).id()),
                    eq(READ)
                )
            ).thenReturn(true);

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(
                any(),
                eq(APPLICATION_DEFINITION),
                eq(HRIDToUUID.api().context(GraviteeContext.getExecutionContext()).hrid(HRID).id()),
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
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_API), eq(ENVIRONMENT_ID))).thenReturn(false);

            ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_API), eq(ENVIRONMENT_ID), eq(CREATE))).thenReturn(true);

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
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_SHARED_POLICY_GROUP), eq(ENVIRONMENT_ID))).thenReturn(false);

            ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            assertThrows(
                ForbiddenAccessException.class,
                () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                "You do not have sufficient rights to access this resource"
            );
        }

        @Test
        void shouldBeAuthorizedWhenEnvironmentPermissions() {
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_SHARED_POLICY_GROUP), eq(ENVIRONMENT_ID), eq(CREATE))).thenReturn(
                true
            );

            permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

            verify(permissionService, times(1)).hasPermission(any(), eq(ENVIRONMENT_SHARED_POLICY_GROUP), eq(ENVIRONMENT_ID), eq(CREATE));
        }
    }

    @Nested
    class Dictionaries {

        @Nested
        class CreateOrUpdate {

            @BeforeEach
            void initMocks() {
                Permission perm = mock(Permission.class);
                when(perm.value()).thenReturn(ENVIRONMENT_DICTIONARY);
                when(perm.acls()).thenReturn(new RolePermissionAction[] { CREATE, UPDATE });
                when(permissions.value()).thenReturn(new Permission[] { perm });
                UriInfo uriInfo = mock(UriInfo.class);
                when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            }

            @Test
            void shouldThrowForbiddenExceptionWhenNoDictionaryPermissions() {
                when(permissionService.hasPermission(any(), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID))).thenReturn(false);

                ExecutionContext executionContext = GraviteeContext.getExecutionContext();
                assertThrows(
                    ForbiddenAccessException.class,
                    () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                    "You do not have sufficient rights to access this resource"
                );
            }

            @Test
            void shouldBeAuthorizedWhenDictionaryCreatePermissions() {
                when(
                    permissionService.hasPermission(any(), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID), eq(CREATE), eq(UPDATE))
                ).thenReturn(true);

                permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

                verify(permissionService, times(1)).hasPermission(
                    any(),
                    eq(ENVIRONMENT_DICTIONARY),
                    eq(ENVIRONMENT_ID),
                    eq(CREATE),
                    eq(UPDATE)
                );
            }
        }

        @Nested
        class Read {

            @BeforeEach
            void initMocks() {
                Permission perm = mock(Permission.class);
                when(perm.value()).thenReturn(ENVIRONMENT_DICTIONARY);
                when(perm.acls()).thenReturn(new RolePermissionAction[] { RolePermissionAction.READ });
                when(permissions.value()).thenReturn(new Permission[] { perm });
                UriInfo uriInfo = mock(UriInfo.class);
                when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            }

            @Test
            void shouldThrowForbiddenExceptionWhenNoReadPermissions() {
                when(permissionService.hasPermission(any(), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID))).thenReturn(false);

                ExecutionContext executionContext = GraviteeContext.getExecutionContext();
                assertThrows(
                    ForbiddenAccessException.class,
                    () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                    "You do not have sufficient rights to access this resource"
                );
            }

            @Test
            void shouldBeAuthorizedWhenDictionaryReadPermissions() {
                when(
                    permissionService.hasPermission(any(), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID), eq(RolePermissionAction.READ))
                ).thenReturn(true);

                permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

                verify(permissionService, times(1)).hasPermission(
                    any(),
                    eq(ENVIRONMENT_DICTIONARY),
                    eq(ENVIRONMENT_ID),
                    eq(RolePermissionAction.READ)
                );
            }
        }

        @Nested
        class Delete {

            @BeforeEach
            void initMocks() {
                Permission perm = mock(Permission.class);
                when(perm.value()).thenReturn(ENVIRONMENT_DICTIONARY);
                when(perm.acls()).thenReturn(new RolePermissionAction[] { RolePermissionAction.DELETE });
                when(permissions.value()).thenReturn(new Permission[] { perm });
                UriInfo uriInfo = mock(UriInfo.class);
                when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
            }

            @Test
            void shouldThrowForbiddenExceptionWhenNoDeletePermissions() {
                when(permissionService.hasPermission(any(), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID))).thenReturn(false);

                ExecutionContext executionContext = GraviteeContext.getExecutionContext();
                assertThrows(
                    ForbiddenAccessException.class,
                    () -> permissionFilter.filter(permissions, containerRequestContext, executionContext),
                    "You do not have sufficient rights to access this resource"
                );
            }

            @Test
            void shouldBeAuthorizedWhenDictionaryDeletePermissions() {
                when(
                    permissionService.hasPermission(any(), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID), eq(RolePermissionAction.DELETE))
                ).thenReturn(true);

                permissionFilter.filter(permissions, containerRequestContext, GraviteeContext.getExecutionContext());

                verify(permissionService, times(1)).hasPermission(
                    any(),
                    eq(ENVIRONMENT_DICTIONARY),
                    eq(ENVIRONMENT_ID),
                    eq(RolePermissionAction.DELETE)
                );
            }
        }
    }
}
