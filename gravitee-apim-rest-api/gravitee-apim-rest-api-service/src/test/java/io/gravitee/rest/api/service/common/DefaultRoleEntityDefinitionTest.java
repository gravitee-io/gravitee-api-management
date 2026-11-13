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
package io.gravitee.rest.api.service.common;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultRoleEntityDefinitionTest {

    @Test
    void default_environment_user_role_grants_read_and_update_on_hitl_decisions_only() {
        Map<String, char[]> permissions = DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions();

        assertThat(permissions.get(EnvironmentPermission.HITL_DECISIONS.getName())).containsExactlyInAnyOrder(READ.getId(), UPDATE.getId());
        assertThat(permissions).doesNotContainKeys(
            EnvironmentPermission.HITL_RULES.getName(),
            EnvironmentPermission.HITL_NOTIFICATIONS.getName()
        );
    }
}
