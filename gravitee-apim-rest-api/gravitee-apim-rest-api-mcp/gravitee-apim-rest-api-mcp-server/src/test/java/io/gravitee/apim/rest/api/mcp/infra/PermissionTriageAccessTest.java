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
package io.gravitee.apim.rest.api.mcp.infra;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PermissionTriageAccessTest {

    private final PermissionService permissions = mock(PermissionService.class);
    private final PermissionTriageAccess access = new PermissionTriageAccess(permissions);

    @ParameterizedTest
    @EnumSource(value = RolePermission.class, names = { "ENVIRONMENT_DASHBOARD", "ENVIRONMENT_API" })
    void should_let_a_reader_of_dashboards_or_of_the_apis_of_the_environment_read_traffic(RolePermission permission) {
        grant(permission, "DEFAULT");

        assertThat(access.canReadTraffic(CALLER)).isTrue();
    }

    @Test
    void should_refuse_traffic_to_a_user_with_neither_permission() {
        grant(RolePermission.ENVIRONMENT_APPLICATION, "DEFAULT");

        assertThat(access.canReadTraffic(CALLER)).isFalse();
    }

    @Test
    void should_ask_for_the_event_and_audit_permissions_of_the_api() {
        grant(RolePermission.API_EVENT, "orders-id");

        assertThat(access.canReadApiEvents(CALLER, "orders-id")).isTrue();
        assertThat(access.canReadApiEvents(CALLER, "stock-id")).isFalse();
        assertThat(access.canReadApiAudit(CALLER, "orders-id")).isFalse();
    }

    private void grant(RolePermission permission, String referenceId) {
        when(permissions.hasPermission(any(), eq("user-id"), eq(permission), eq(referenceId), eq(RolePermissionAction.READ))).thenReturn(
            true
        );
    }
}
