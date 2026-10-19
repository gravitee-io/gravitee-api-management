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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort.Action;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardPermissionPortAdapterTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final DashboardPermissionPortAdapter adapter = new DashboardPermissionPortAdapter(permissionService);

    @ParameterizedTest
    @CsvSource({ "READ, READ", "CREATE, CREATE", "UPDATE, UPDATE" })
    void should_check_the_dashboard_permission_of_the_environment(Action action, RolePermissionAction acl) {
        when(
            permissionService.hasPermission(
                eq(new ExecutionContext("org-1", "env-1")),
                eq(RolePermission.ENVIRONMENT_DASHBOARD),
                eq("env-1"),
                eq(acl)
            )
        ).thenReturn(true);

        assertThat(adapter.holds("org-1", "env-1", action)).isTrue();
        assertThat(adapter.holds("org-1", "other-env", action)).isFalse();
    }
}
