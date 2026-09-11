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

import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormPermissionUpgraderTest {

    private static final String METADATA = EnvironmentPermission.METADATA.getName();
    private static final String SUBSCRIPTION_FORM = EnvironmentPermission.SUBSCRIPTION_FORM.getName();
    private static final char[] METADATA_ACLS = { 'C', 'R', 'U', 'D' };
    private static final String ORG_ID = "org-a";

    @InjectMocks
    SubscriptionFormPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    void should_have_the_expected_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.SUBSCRIPTION_FORM_PERMISSION_UPGRADER);
    }

    @Test
    void should_copy_metadata_acls_when_role_has_metadata_permission() throws Exception {
        mockOrganizations(ORG_ID);
        when(roleService.findByScope(ENVIRONMENT, ORG_ID)).thenReturn(
            List.of(roleWithPermissions("role-id", Map.of(METADATA, METADATA_ACLS)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> updated = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), updated.capture());
        assertThat(updated.getValue().getPermissions().get(SUBSCRIPTION_FORM)).isEqualTo(METADATA_ACLS);
        assertThat(updated.getValue().getPermissions().get(METADATA)).isEqualTo(METADATA_ACLS);
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(ORG_ID));
    }

    @Test
    void should_not_update_role_without_metadata_permission() throws Exception {
        mockOrganizations(ORG_ID);
        when(roleService.findByScope(ENVIRONMENT, ORG_ID)).thenReturn(
            List.of(roleWithPermissions("role-id", Map.of(EnvironmentPermission.API.getName(), METADATA_ACLS)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(ORG_ID));
    }

    @Test
    void should_preserve_existing_subscription_form_acls() throws Exception {
        mockOrganizations(ORG_ID);
        char[] existing = { 'R' };
        when(roleService.findByScope(ENVIRONMENT, ORG_ID)).thenReturn(
            List.of(roleWithPermissions("role-id", Map.of(METADATA, METADATA_ACLS, SUBSCRIPTION_FORM, existing)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_skip_system_roles() throws Exception {
        mockOrganizations(ORG_ID);
        RoleEntity systemRole = roleWithPermissions("admin", Map.of(METADATA, METADATA_ACLS));
        systemRole.setSystem(true);
        when(roleService.findByScope(ENVIRONMENT, ORG_ID)).thenReturn(List.of(systemRole));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(ORG_ID));
    }

    @Test
    void should_process_each_organization() throws Exception {
        mockOrganizations("org-a", "org-b");
        when(roleService.findByScope(eq(ENVIRONMENT), any())).thenReturn(List.of());

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq("org-a"));
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq("org-b"));
    }

    @Test
    void should_wrap_repository_failures() throws Exception {
        when(organizationRepository.findAll()).thenThrow(new TechnicalException("db error"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    private void mockOrganizations(String... ids) throws TechnicalException {
        Set<Organization> organizations = new HashSet<>();
        for (String id : ids) {
            Organization organization = mock(Organization.class);
            when(organization.getId()).thenReturn(id);
            organizations.add(organization);
        }
        when(organizationRepository.findAll()).thenReturn(organizations);
    }

    private static RoleEntity roleWithPermissions(String id, Map<String, char[]> permissions) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(id);
        role.setPermissions(new HashMap<>(permissions));
        return role;
    }
}
