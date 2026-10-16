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
package io.gravitee.rest.api.rest.security;

import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_APPLICATION;
import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_DICTIONARY;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.Test;

class CreateOrUpdateAuthorizationTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    private final PermissionService permissionService = mock(PermissionService.class);

    @Test
    void should_require_create_on_the_environment_when_the_object_is_absent() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        when(permissionService.hasPermission(EXECUTION_CONTEXT, ENVIRONMENT_APPLICATION, ENVIRONMENT_ID, CREATE)).thenReturn(false);

        assertThatThrownBy(() -> require(null)).isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void should_allow_creation_with_create_on_the_environment() {
        when(permissionService.hasPermission(EXECUTION_CONTEXT, ENVIRONMENT_APPLICATION, ENVIRONMENT_ID, CREATE)).thenReturn(true);

        assertThatCode(() -> require(null)).doesNotThrowAnyException();
    }

    @Test
    void should_require_update_on_the_object_when_it_exists() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        when(permissionService.hasPermission(EXECUTION_CONTEXT, APPLICATION_DEFINITION, APPLICATION_ID, UPDATE)).thenReturn(false);

        assertThatThrownBy(() -> require(APPLICATION_ID)).isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void should_allow_update_with_update_on_the_object() {
        when(permissionService.hasPermission(EXECUTION_CONTEXT, APPLICATION_DEFINITION, APPLICATION_ID, UPDATE)).thenReturn(true);

        assertThatCode(() -> require(APPLICATION_ID)).doesNotThrowAnyException();
    }

    @Test
    void should_require_update_on_the_environment_for_an_environment_scoped_update_permission() {
        when(permissionService.hasPermission(eq(EXECUTION_CONTEXT), eq(ENVIRONMENT_DICTIONARY), eq(ENVIRONMENT_ID), eq(UPDATE))).thenReturn(
            true
        );

        assertThatCode(() ->
            CreateOrUpdateAuthorization.require(
                permissionService,
                EXECUTION_CONTEXT,
                ENVIRONMENT_DICTIONARY,
                ENVIRONMENT_DICTIONARY,
                "dictionary-id"
            )
        ).doesNotThrowAnyException();
    }

    @Test
    void should_refuse_a_create_permission_that_is_not_environment_scoped() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
            CreateOrUpdateAuthorization.require(permissionService, EXECUTION_CONTEXT, APPLICATION_DEFINITION, APPLICATION_DEFINITION, null)
        ).isInstanceOf(IllegalStateException.class);
    }

    private void require(String existingId) {
        CreateOrUpdateAuthorization.require(
            permissionService,
            EXECUTION_CONTEXT,
            ENVIRONMENT_APPLICATION,
            APPLICATION_DEFINITION,
            existingId
        );
    }
}
