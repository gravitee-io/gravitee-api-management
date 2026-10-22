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
import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class GammaAuthzPdpPermissionUpgraderTest {

    private static final String AUTHORIZATION_PERMISSION = EnvironmentPermission.AUTHORIZATION.getName();
    private static final String AUTHZ_PDP_PERMISSION = EnvironmentPermission.AUTHZ_PDP.getName();
    private static final String API_PERMISSION = EnvironmentPermission.API.getName();

    @InjectMocks
    GammaAuthzPdpPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void upgrade_should_fail_when_organization_repository_throws() throws TechnicalException {
        assertThrows(UpgraderException.class, () -> {
            when(organizationRepository.findAll()).thenThrow(new RuntimeException("db error"));
            upgrader.upgrade();
        });
    }

    @Test
    public void should_copy_authorization_actions_to_authz_pdp_and_keep_the_other_permissions()
        throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentRoles(
            organizationId,
            customRole("pdp-operator", Map.of(AUTHORIZATION_PERMISSION, actions(READ, CREATE, DELETE), API_PERMISSION, actions(READ)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        UpdateRoleEntity update = captureSingleUpdate();
        assertThat(update.getId()).isEqualTo("pdp-operator");
        assertThat(update.getPermissions().get(AUTHZ_PDP_PERMISSION)).containsExactlyInAnyOrder(
            READ.getId(),
            CREATE.getId(),
            DELETE.getId()
        );
        assertThat(update.getPermissions().get(AUTHORIZATION_PERMISSION)).containsExactlyInAnyOrder(
            READ.getId(),
            CREATE.getId(),
            DELETE.getId()
        );
        assertThat(update.getPermissions().get(API_PERMISSION)).containsExactly(READ.getId());
    }

    @Test
    public void should_merge_with_the_authz_pdp_actions_the_role_already_has() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentRoles(
            organizationId,
            customRole("deployer", Map.of(AUTHORIZATION_PERMISSION, actions(READ), AUTHZ_PDP_PERMISSION, actions(UPDATE)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(captureSingleUpdate().getPermissions().get(AUTHZ_PDP_PERMISSION)).containsExactlyInAnyOrder(
            READ.getId(),
            UPDATE.getId()
        );
    }

    @Test
    public void should_not_update_a_role_that_already_has_every_authorization_action_on_authz_pdp()
        throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentRoles(
            organizationId,
            customRole("migrated", Map.of(AUTHORIZATION_PERMISSION, actions(READ), AUTHZ_PDP_PERMISSION, actions(READ, UPDATE)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    public void should_not_update_roles_without_authorization() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        RoleEntity nullPermissions = customRole("no-permissions", Map.of());
        nullPermissions.setPermissions(null);
        givenEnvironmentRoles(organizationId, customRole("api-only", Map.of(API_PERMISSION, actions(READ))), nullPermissions);

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    public void should_skip_system_roles() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        RoleEntity admin = customRole("ADMIN", Map.of(AUTHORIZATION_PERMISSION, actions(READ)));
        admin.setSystem(true);
        givenEnvironmentRoles(organizationId, admin);

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    public void should_process_each_organization() throws TechnicalException, UpgraderException {
        givenOrganizations("org-a", "org-b");
        givenEnvironmentRoles("org-a", customRole("role-a", Map.of(AUTHORIZATION_PERMISSION, actions(READ))));
        givenEnvironmentRoles("org-b", customRole("role-b", Map.of(AUTHORIZATION_PERMISSION, actions(READ))));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, times(2)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
    }

    @Test
    public void getOrder_matches_gamma_authz_pdp_permission_upgrader() {
        Assertions.assertEquals(UpgraderOrder.GAMMA_AUTHZ_PDP_PERMISSION_UPGRADER, upgrader.getOrder());
    }

    private void givenOrganizations(String... organizationIds) throws TechnicalException {
        Set<Organization> organizations = new HashSet<>();
        for (String organizationId : organizationIds) {
            Organization organization = mock(Organization.class);
            when(organization.getId()).thenReturn(organizationId);
            organizations.add(organization);
        }
        when(organizationRepository.findAll()).thenReturn(organizations);
    }

    private void givenEnvironmentRoles(String organizationId, RoleEntity... roles) {
        when(roleService.findByScope(ENVIRONMENT, organizationId)).thenReturn(List.of(roles));
    }

    private static RoleEntity customRole(String name, Map<String, char[]> permissions) {
        return RoleEntity.builder().id(name).name(name).scope(ENVIRONMENT).permissions(new HashMap<>(permissions)).build();
    }

    private static char[] actions(io.gravitee.rest.api.model.permissions.RolePermissionAction... actions) {
        char[] ids = new char[actions.length];
        for (int i = 0; i < actions.length; i++) {
            ids[i] = actions[i].getId();
        }
        return ids;
    }

    private UpdateRoleEntity captureSingleUpdate() {
        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(1)).update(any(ExecutionContext.class), updateCaptor.capture());
        return updateCaptor.getValue();
    }
}
