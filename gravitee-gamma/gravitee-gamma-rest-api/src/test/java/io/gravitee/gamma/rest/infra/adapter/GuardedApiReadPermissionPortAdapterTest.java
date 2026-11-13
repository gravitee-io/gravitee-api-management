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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The permission that names an API running a Guardian: reading the API's definition, and no weaker or other one.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardedApiReadPermissionPortAdapterTest {

    private final PermissionDomainService permissions = mock(PermissionDomainService.class);
    private final GuardedApiReadPermissionPortAdapter adapter = new GuardedApiReadPermissionPortAdapter(permissions);

    @Test
    void should_let_a_caller_read_an_api_whose_definition_they_may_read() {
        when(permissions.hasPermission("org-1", "user-1", RolePermission.API_DEFINITION, "api-1", RolePermissionAction.READ)).thenReturn(
            true
        );

        assertThat(adapter.canRead("org-1", "user-1", "api-1")).as("API whose definition the caller may read").isTrue();
    }

    @Test
    void should_not_let_a_caller_read_an_api_whose_definition_they_may_not_read() {
        when(permissions.hasPermission("org-1", "user-1", RolePermission.API_DEFINITION, "api-1", RolePermissionAction.READ)).thenReturn(
            false
        );

        assertThat(adapter.canRead("org-1", "user-1", "api-1")).as("API whose definition the caller may not read").isFalse();
    }
}
