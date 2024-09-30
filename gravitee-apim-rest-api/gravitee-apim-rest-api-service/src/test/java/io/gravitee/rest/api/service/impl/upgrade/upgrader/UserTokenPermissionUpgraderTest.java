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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTokenPermissionUpgraderTest {

    private static final String DEFAULT_ORGANIZATION_ID = "DEFAULT";
    private static final String USER_ROLE = "USER";
    private static final String READ_USER_ROLE = "READ_USER_ROLE";
    private static final String CREATE_READ_USER_ROLE = "CREATE_READ_USER_ROLE";
    private static final String UPDATE_USER_ROLE = "UPDATE_USER_ROLE";
    private static final String DELETE_USER_ROLE = "DELETE_USER_ROLE";

    @Mock
    private RoleService roleService;

    @Mock
    private OrganizationRepository organizationRepository;

    private UserTokenPermissionUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new UserTokenPermissionUpgrader(roleService, organizationRepository);
    }

    @Test
    void should_throw_technical_exception() {}

    @Test
    @SneakyThrows
    void should_upgrade_roles() {
        var defaultOrganization = new Organization();
        defaultOrganization.setId(DEFAULT_ORGANIZATION_ID);
        when(organizationRepository.findAll()).thenReturn(Set.of(defaultOrganization));

        var systemAdmin = getSystemAdminRole();
        var simpleUser = getUserRoleWithPermissionAndName(USER_ROLE, new char[] {});
        var readUser = getUserRoleWithPermissionAndName(READ_USER_ROLE, new char[] { 'R' });
        var createUser = getUserRoleWithPermissionAndName(CREATE_READ_USER_ROLE, new char[] { 'C', 'R' });
        var updateUser = getUserRoleWithPermissionAndName(UPDATE_USER_ROLE, new char[] { 'U' });
        var deleteUser = getUserRoleWithPermissionAndName(DELETE_USER_ROLE, new char[] { 'D' });

        when(roleService.findByScope(RoleScope.ORGANIZATION, DEFAULT_ORGANIZATION_ID))
            .thenReturn(List.of(systemAdmin, simpleUser, readUser, createUser, updateUser, deleteUser));

        assertThat(upgrader.upgrade()).isTrue();

        var updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(5)).update(any(ExecutionContext.class), updateCaptor.capture());

        var updatedRoles = updateCaptor.getAllValues();
        assertThat(updatedRoles.size()).isEqualTo(5);

        assertRolePermissions(updatedRoles.get(0), simpleUser, new char[] {});
        assertRolePermissions(updatedRoles.get(1), readUser, new char[] { 'C', 'R', 'D' });
        assertRolePermissions(updatedRoles.get(2), createUser, new char[] { 'C', 'R', 'D' });
        assertRolePermissions(updatedRoles.get(3), updateUser, new char[] { 'U' });
        assertRolePermissions(updatedRoles.get(4), deleteUser, new char[] { 'D' });
    }

    private static RoleEntity getUserRoleWithPermissionAndName(String name, char[] permissions) {
        var role = new RoleEntity();
        role.setId(name);
        role.setName(name);
        role.setScope(RoleScope.ORGANIZATION);

        Map<String, char[]> permissionsMap = new HashMap<>();
        permissionsMap.put(OrganizationPermission.USER.getName(), permissions);
        role.setPermissions(permissionsMap);
        return role;
    }

    private static RoleEntity getSystemAdminRole() {
        var systemAdmin = new RoleEntity();
        systemAdmin.setId(SystemRole.ADMIN.name());
        systemAdmin.setName(SystemRole.ADMIN.name());
        systemAdmin.setScope(RoleScope.ORGANIZATION);
        systemAdmin.setPermissions(Map.of(OrganizationPermission.USER.getName(), new char[] { 'C', 'R', 'U', 'D' }));
        return systemAdmin;
    }

    private static void assertRolePermissions(UpdateRoleEntity updatedRole, RoleEntity originalRole, char[] expectedPermissions) {
        assertThat(updatedRole.getId()).isEqualTo(originalRole.getId());
        assertThat(updatedRole.getName()).isEqualTo(originalRole.getName());
        assertThat(updatedRole.getScope()).isEqualTo(originalRole.getScope());
        assertThat(updatedRole.getPermissions().get(OrganizationPermission.USER_TOKEN.getName()))
            .containsExactlyInAnyOrder(expectedPermissions);
    }
}
