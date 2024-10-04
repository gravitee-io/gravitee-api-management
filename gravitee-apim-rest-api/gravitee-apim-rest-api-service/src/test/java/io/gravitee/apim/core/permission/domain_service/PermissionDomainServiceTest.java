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
package io.gravitee.apim.core.permission.domain_service;

import static assertions.CoreAssertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PermissionDomainServiceTest {

    PermissionDomainService permissionDomainService = new PermissionDomainService() {
        @Override
        public boolean hasExactPermissions(
            String organizationId,
            String userId,
            RolePermission permission,
            String referenceId,
            RolePermissionAction... acls
        ) {
            return false;
        }

        @Override
        public boolean hasPermission(
            String organizationId,
            String userId,
            RolePermission permission,
            String referenceId,
            RolePermissionAction... acls
        ) {
            return false;
        }
    };

    @Nested
    class CheckForExactPermissions {

        @Test
        void should_return_true_when_user_permissions_contains_requested_permission_with_exact_acls() {
            // Given
            var requestedPermission = RolePermission.INTEGRATION_DEFINITION.getPermission();
            var userPermissions = Map.of(requestedPermission.getName(), new char[] { 'C', 'R', 'U', 'D' });

            // When
            var result = permissionDomainService.checkForExactPermissions(
                userPermissions,
                requestedPermission,
                RolePermissionAction.CREATE,
                RolePermissionAction.READ,
                RolePermissionAction.UPDATE,
                RolePermissionAction.DELETE
            );

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void should_return_false_when_user_permissions_contains_requested_permission_but_acls_not_match_exactly() {
            // Given
            var requestedPermission = RolePermission.INTEGRATION_DEFINITION.getPermission();
            var userPermissions = Map.of(requestedPermission.getName(), new char[] { 'C', 'R', 'U', 'D' });

            // When
            var result = permissionDomainService.checkForExactPermissions(
                userPermissions,
                requestedPermission,
                RolePermissionAction.CREATE,
                RolePermissionAction.UPDATE
            );

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void should_return_false_when_user_permissions_provide_is_null() {
            var result = permissionDomainService.checkForExactPermissions(null, null, RolePermissionAction.CREATE);

            assertThat(result).isFalse();
        }

        @Test
        void should_return_false_when_user_permissions_does_not_contain_requested_permission() {
            // Given
            var userPermissions = Map.of("permission", new char[] { 'C', 'R', 'U', 'D' });
            var requestedPermission = RolePermission.INTEGRATION_DEFINITION.getPermission();

            // When
            var result = permissionDomainService.checkForExactPermissions(
                userPermissions,
                requestedPermission,
                RolePermissionAction.CREATE
            );

            // Then
            assertThat(result).isFalse();
        }
    }
}
