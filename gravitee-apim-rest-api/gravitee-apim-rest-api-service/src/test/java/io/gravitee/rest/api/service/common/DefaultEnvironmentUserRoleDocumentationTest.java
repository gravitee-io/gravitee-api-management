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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import org.junit.jupiter.api.Test;

class DefaultEnvironmentUserRoleDocumentationTest {

    @Test
    void stock_environment_user_role_should_not_grant_management_documentation_access() {
        var permissions = DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions();

        assertThat(permissions).doesNotContainKey(EnvironmentPermission.DOCUMENTATION.getName());
    }

    @Test
    void stock_environment_user_role_should_keep_its_other_consumer_permissions() {
        var permissions = DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions();

        assertThat(permissions).containsOnlyKeys(
            EnvironmentPermission.API.getName(),
            EnvironmentPermission.APPLICATION.getName(),
            EnvironmentPermission.GROUP.getName(),
            EnvironmentPermission.INTEGRATION.getName(),
            EnvironmentPermission.API_PRODUCT.getName(),
            EnvironmentPermission.SHARED_POLICY_GROUP.getName()
        );
        assertThat(permissions.get(EnvironmentPermission.API.getName())).containsExactly(READ.getId());
    }
}
