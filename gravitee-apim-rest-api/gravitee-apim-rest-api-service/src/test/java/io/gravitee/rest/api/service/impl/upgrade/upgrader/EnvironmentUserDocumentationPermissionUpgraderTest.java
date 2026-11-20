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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
class EnvironmentUserDocumentationPermissionUpgraderTest {

    private static final String DOCUMENTATION = EnvironmentPermission.DOCUMENTATION.getName();

    @InjectMocks
    EnvironmentUserDocumentationPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    void should_remove_the_stock_documentation_read_grant_and_keep_other_permissions() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentUserRole(organizationId, environmentUserRole("env-user-id", new char[] { READ.getId() }));

        assertThat(upgrader.upgrade()).isTrue();

        UpdateRoleEntity update = captureSingleUpdate();
        assertThat(update.getId()).isEqualTo("env-user-id");
        assertThat(update.getName()).isEqualTo(DEFAULT_ROLE_ENVIRONMENT_USER.getName());
        assertThat(update.getScope()).isEqualTo(ENVIRONMENT);
        assertThat(update.isDefaultRole()).isTrue();
        assertThat(update.getPermissions()).doesNotContainKey(DOCUMENTATION);
        assertThat(update.getPermissions()).containsOnlyKeys(DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions().keySet());
        assertThat(update.getPermissions().get(EnvironmentPermission.APPLICATION.getName())).isEqualTo(
            DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions().get(EnvironmentPermission.APPLICATION.getName())
        );
    }

    @Test
    void should_keep_a_documentation_grant_an_administrator_widened() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentUserRole(
            organizationId,
            environmentUserRole("env-user-id", new char[] { CREATE.getId(), READ.getId(), UPDATE.getId() })
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_not_update_when_documentation_is_already_absent() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentUserRole(organizationId, environmentUserRole("env-user-id", null));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_not_update_when_role_has_no_permissions() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEnvironmentUserRole(
            organizationId,
            RoleEntity.builder()
                .id("env-user-id")
                .name(DEFAULT_ROLE_ENVIRONMENT_USER.getName())
                .scope(ENVIRONMENT)
                .permissions(null)
                .build()
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_not_update_when_environment_user_role_is_absent() throws Exception {
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        when(roleService.findByScopeAndName(ENVIRONMENT, DEFAULT_ROLE_ENVIRONMENT_USER.getName(), organizationId)).thenReturn(
            Optional.empty()
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_process_each_organization() throws Exception {
        givenOrganizations("org-a", "org-b");
        givenEnvironmentUserRole("org-a", environmentUserRole("env-user-org-a", new char[] { READ.getId() }));
        givenEnvironmentUserRole("org-b", environmentUserRole("env-user-org-b", new char[] { READ.getId() }));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, times(2)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
    }

    @Test
    void should_fail_when_organization_repository_throws() throws TechnicalException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException("db error"));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
    }

    @Test
    void should_run_with_its_own_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.ENVIRONMENT_USER_DOCUMENTATION_PERMISSION_UPGRADER);
    }

    private void givenOrganizations(String... organizationIds) throws TechnicalException {
        Set<Organization> organizations = new LinkedHashSet<>();
        for (String organizationId : organizationIds) {
            Organization organization = mock(Organization.class);
            when(organization.getId()).thenReturn(organizationId);
            organizations.add(organization);
        }
        when(organizationRepository.findAll()).thenReturn(organizations);
    }

    private void givenEnvironmentUserRole(String organizationId, RoleEntity role) {
        when(roleService.findByScopeAndName(eq(ENVIRONMENT), eq(DEFAULT_ROLE_ENVIRONMENT_USER.getName()), eq(organizationId))).thenReturn(
            Optional.of(role)
        );
    }

    private static RoleEntity environmentUserRole(String id, char[] documentationAcls) {
        Map<String, char[]> permissions = new HashMap<>(DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions());
        if (documentationAcls != null) {
            permissions.put(DOCUMENTATION, documentationAcls);
        }
        return RoleEntity.builder()
            .id(id)
            .name(DEFAULT_ROLE_ENVIRONMENT_USER.getName())
            .scope(ENVIRONMENT)
            .defaultRole(true)
            .permissions(permissions)
            .build();
    }

    private UpdateRoleEntity captureSingleUpdate() {
        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(1)).update(any(ExecutionContext.class), updateCaptor.capture());
        return updateCaptor.getValue();
    }
}
