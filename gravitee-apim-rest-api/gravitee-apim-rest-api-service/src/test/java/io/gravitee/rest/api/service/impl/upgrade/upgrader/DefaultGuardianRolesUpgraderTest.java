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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DefaultGuardianRolesUpgraderTest {

    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String GUARDIAN = EnvironmentPermission.GUARDIAN.getName();

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Captor
    ArgumentCaptor<UpdateRoleEntity> roleCaptor;

    DefaultGuardianRolesUpgrader upgrader;

    @BeforeEach
    public void setUp() {
        upgrader = new DefaultGuardianRolesUpgrader(roleService, organizationRepository);
    }

    @Test
    public void should_grant_the_full_guardian_permission_to_the_api_publisher() throws TechnicalException, UpgraderException {
        givenAnOrganizationWhereBothDefaultRolesExist();

        upgrader.upgrade();

        verify(roleService, times(2)).update(any(), roleCaptor.capture());
        assertThat(permissionsGrantedTo(ROLE_ENVIRONMENT_API_PUBLISHER.getName())).containsExactly(
            CREATE.getId(),
            READ.getId(),
            UPDATE.getId(),
            DELETE.getId()
        );
    }

    @Test
    public void should_grant_only_read_to_the_default_environment_user() throws TechnicalException, UpgraderException {
        givenAnOrganizationWhereBothDefaultRolesExist();

        upgrader.upgrade();

        verify(roleService, times(2)).update(any(), roleCaptor.capture());
        assertThat(permissionsGrantedTo(DEFAULT_ROLE_ENVIRONMENT_USER.getName())).containsExactly(READ.getId());
    }

    @Test
    public void should_keep_the_permissions_the_role_already_had() throws TechnicalException, UpgraderException {
        givenAnOrganizationWhereBothDefaultRolesExist();

        upgrader.upgrade();

        verify(roleService, times(2)).update(any(), roleCaptor.capture());
        assertThat(roleCaptor.getAllValues()).allSatisfy(role ->
            assertThat(role.getPermissions()).containsKey(EnvironmentPermission.API.getName())
        );
    }

    @Test
    public void should_do_nothing_when_the_default_roles_are_absent() throws TechnicalException, UpgraderException {
        when(organizationRepository.findAll()).thenReturn(Set.of(Organization.builder().id(ORGANIZATION_ID).build()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), any(), eq(ORGANIZATION_ID))).thenReturn(Optional.empty());

        upgrader.upgrade();

        verify(roleService, never()).update(any(), any());
    }

    private void givenAnOrganizationWhereBothDefaultRolesExist() throws TechnicalException {
        when(organizationRepository.findAll()).thenReturn(Set.of(Organization.builder().id(ORGANIZATION_ID).build()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), any(), eq(ORGANIZATION_ID))).thenAnswer(invocation ->
            Optional.of(aRoleNamed(invocation.getArgument(1)))
        );
        when(roleService.update(any(), any())).thenReturn(RoleEntity.builder().build());
    }

    private RoleEntity aRoleNamed(String name) {
        Map<String, char[]> existing = new HashMap<>();
        existing.put(EnvironmentPermission.API.getName(), new char[] { READ.getId() });
        return RoleEntity.builder().id(name).name(name).scope(RoleScope.ENVIRONMENT).permissions(existing).build();
    }

    private List<Character> permissionsGrantedTo(String roleName) {
        char[] granted = roleCaptor
            .getAllValues()
            .stream()
            .filter(role -> roleName.equals(role.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no update captured for role " + roleName))
            .getPermissions()
            .get(GUARDIAN);
        assertThat(granted).as("the %s permission was never written on %s", GUARDIAN, roleName).isNotNull();
        return new String(granted)
            .chars()
            .mapToObj(c -> (char) c)
            .toList();
    }
}
