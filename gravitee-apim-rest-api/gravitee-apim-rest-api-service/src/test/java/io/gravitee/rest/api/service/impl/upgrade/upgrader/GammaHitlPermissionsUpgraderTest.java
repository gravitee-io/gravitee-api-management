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
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import java.util.Map;
import java.util.Optional;
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
public class GammaHitlPermissionsUpgraderTest {

    private static final String USER = DEFAULT_ROLE_ENVIRONMENT_USER.getName();
    private static final String HITL_RULES = EnvironmentPermission.HITL_RULES.getName();
    private static final String HITL_DECISIONS = EnvironmentPermission.HITL_DECISIONS.getName();
    private static final String HITL_NOTIFICATIONS = EnvironmentPermission.HITL_NOTIFICATIONS.getName();
    private static final String APPLICATION = EnvironmentPermission.APPLICATION.getName();
    private static final char[] FULL_ACLS = { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };

    @InjectMocks
    GammaHitlPermissionsUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void upgrade_should_fail_when_organization_repository_throws() throws TechnicalException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException("db error"));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
    }

    @Test
    public void should_grant_read_and_update_on_decisions_to_the_environment_user_role_and_keep_the_rest() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenRole(organizationId, USER, role("user-id", USER, userPermissionsWithoutHitl()));

        assertThat(upgrader.upgrade()).isTrue();

        UpdateRoleEntity update = captureSingleUpdate();
        assertThat(update.getId()).isEqualTo("user-id");
        assertThat(update.getName()).isEqualTo(USER);
        assertThat(update.getScope()).isEqualTo(ENVIRONMENT);
        assertThat(update.getPermissions().get(HITL_DECISIONS)).containsExactlyInAnyOrder(READ.getId(), UPDATE.getId());
        assertThat(update.getPermissions()).doesNotContainKeys(HITL_RULES, HITL_NOTIFICATIONS);
        assertThat(update.getPermissions().get(APPLICATION)).containsExactlyInAnyOrder(FULL_ACLS);
    }

    @Test
    public void should_keep_the_actions_a_user_role_already_has_on_decisions() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        Map<String, char[]> permissions = userPermissionsWithoutHitl();
        permissions.put(HITL_DECISIONS, new char[] { DELETE.getId() });
        givenRole(organizationId, USER, role("user-id", USER, permissions));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(captureSingleUpdate().getPermissions().get(HITL_DECISIONS)).containsExactlyInAnyOrder(
            DELETE.getId(),
            READ.getId(),
            UPDATE.getId()
        );
    }

    @Test
    public void should_add_update_when_the_user_role_only_reads_decisions() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        Map<String, char[]> permissions = userPermissionsWithoutHitl();
        permissions.put(HITL_DECISIONS, new char[] { READ.getId() });
        givenRole(organizationId, USER, role("user-id", USER, permissions));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(captureSingleUpdate().getPermissions().get(HITL_DECISIONS)).containsExactlyInAnyOrder(READ.getId(), UPDATE.getId());
    }

    @Test
    public void should_grant_decisions_when_the_user_permissions_map_is_null() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenRole(organizationId, USER, role("user-id", USER, null));

        assertThat(upgrader.upgrade()).isTrue();

        Map<String, char[]> permissions = captureSingleUpdate().getPermissions();
        assertThat(permissions).containsOnlyKeys(HITL_DECISIONS);
        assertThat(permissions.get(HITL_DECISIONS)).containsExactlyInAnyOrder(READ.getId(), UPDATE.getId());
    }

    @Test
    public void should_not_update_a_user_role_that_already_reads_and_updates_decisions() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenRole(organizationId, USER, role("user-id", USER, userPermissionsWithHitl()));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService).findByScopeAndName(ENVIRONMENT, USER, organizationId);
        verifyNoMoreInteractions(roleService);
    }

    @Test
    public void upgrade_should_fail_when_the_role_update_fails() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenRole(organizationId, USER, role("user-id", USER, userPermissionsWithoutHitl()));
        when(roleService.update(any(), any())).thenThrow(new RuntimeException("db error"));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
    }

    @Test
    public void should_do_nothing_when_the_user_role_is_absent() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        when(roleService.findByScopeAndName(ENVIRONMENT, USER, organizationId)).thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    public void should_process_each_organization() throws Exception {
        givenOrganizations("org-a", "org-b");
        for (String organizationId : new String[] { "org-a", "org-b" }) {
            givenRole(organizationId, USER, role("user-" + organizationId, USER, userPermissionsWithoutHitl()));
        }

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, times(2)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
    }

    @Test
    public void getOrder_matches_gamma_hitl_permissions_upgrader() {
        Assertions.assertEquals(UpgraderOrder.GAMMA_HITL_PERMISSIONS_UPGRADER, upgrader.getOrder());
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

    private void givenRole(String organizationId, String name, RoleEntity role) {
        when(roleService.findByScopeAndName(eq(ENVIRONMENT), eq(name), eq(organizationId))).thenReturn(Optional.of(role));
    }

    private static Map<String, char[]> userPermissionsWithoutHitl() {
        Map<String, char[]> permissions = new HashMap<>(DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions());
        permissions.remove(HITL_DECISIONS);
        return permissions;
    }

    private static Map<String, char[]> userPermissionsWithHitl() {
        Map<String, char[]> permissions = new HashMap<>(DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions());
        permissions.put(HITL_DECISIONS, new char[] { READ.getId(), UPDATE.getId() });
        return permissions;
    }

    private static RoleEntity role(String id, String name, Map<String, char[]> permissions) {
        return RoleEntity.builder().id(id).name(name).scope(ENVIRONMENT).permissions(permissions).build();
    }

    private UpdateRoleEntity captureSingleUpdate() {
        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(1)).update(any(ExecutionContext.class), updateCaptor.capture());
        return updateCaptor.getValue();
    }
}
