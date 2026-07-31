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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import java.util.*;
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
public class KafkaExplorerPermissionUpgraderTest {

    private static final String CLUSTER = EnvironmentPermission.CLUSTER.getName();
    private static final String EXPLORER = EnvironmentPermission.EXPLORER.getName();
    private static final char[] CLUSTER_ACLS = { 'C', 'R', 'U', 'D' };
    private static final String ORG_A = "org-a";
    private static final String ORG_B = "org-b";

    @InjectMocks
    KafkaExplorerPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void should_copy_cluster_acls_when_role_has_cluster_permission() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity role = roleWithPermissions("role-id", Map.of(CLUSTER, CLUSTER_ACLS));
        when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(List.of(role));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> updated = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), updated.capture());

        Map<String, char[]> permissions = updated.getValue().getPermissions();
        assertThat(permissions.get(EXPLORER)).isEqualTo(CLUSTER_ACLS);
        assertThat(permissions.get(CLUSTER)).isEqualTo(CLUSTER_ACLS);

        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(orgId));
    }

    @Test
    public void should_not_update_role_without_cluster_permission() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity role = roleWithPermissions("role-id", Map.of(EnvironmentPermission.API.getName(), CLUSTER_ACLS));
        when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(List.of(role));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(orgId));
    }

    @Test
    public void should_preserve_existing_kafka_explorer_acls() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        char[] customAcls = { 'R' };
        RoleEntity role = roleWithPermissions("role-id", Map.of(CLUSTER, CLUSTER_ACLS, EXPLORER, customAcls));
        when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(List.of(role));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(orgId));
    }

    @Test
    public void should_skip_system_roles() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity systemRole = roleWithPermissions("admin-id", Map.of(CLUSTER, CLUSTER_ACLS));
        systemRole.setSystem(true);
        when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(List.of(systemRole));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(orgId));
    }

    @Test
    public void should_ignore_role_with_null_permissions() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity role = new RoleEntity();
        role.setId("role-id");
        role.setName("no-permissions");
        role.setPermissions(null);
        when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(List.of(role));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verify(roleService).createOrUpdateSystemRoles(any(ExecutionContext.class), eq(orgId));
    }

    @Test
    public void should_process_each_organization() throws TechnicalException, UpgraderException {
        Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(ORG_A);
        Organization orgB = mock(Organization.class);
        when(orgB.getId()).thenReturn(ORG_B);
        when(organizationRepository.findAll()).thenReturn(Set.of(orgA, orgB));

        List.of(ORG_A, ORG_B).forEach(orgId ->
            when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(
                List.of(roleWithPermissions("role-" + orgId, Map.of(CLUSTER, CLUSTER_ACLS)))
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, times(2)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
        verify(roleService, times(2)).createOrUpdateSystemRoles(any(ExecutionContext.class), any(String.class));
    }

    @Test
    public void throws_UpgraderException_when_organization_repository_throws_RuntimeException() throws TechnicalException {
        assertThrows(UpgraderException.class, () -> {
            when(organizationRepository.findAll()).thenThrow(new RuntimeException("OrganizationRepository connection failure"));
            upgrader.upgrade();
        });
    }

    @Test
    public void throws_UpgraderException_when_role_service_throws_RuntimeException() throws TechnicalException {
        assertThrows(UpgraderException.class, () -> {
            String orgId = GraviteeContext.getDefaultOrganization();
            mockOrganizations(orgId);

            when(roleService.findByScope(ENVIRONMENT, orgId)).thenReturn(
                List.of(roleWithPermissions("role-id", Map.of(CLUSTER, CLUSTER_ACLS)))
            );
            doThrow(new RuntimeException("RoleService update failure"))
                .when(roleService)
                .update(any(ExecutionContext.class), any(UpdateRoleEntity.class));

            upgrader.upgrade();
        });
    }

    private void mockOrganizations(String... ids) throws TechnicalException {
        Set<Organization> orgs = new HashSet<>();
        Arrays.stream(ids).forEach(id -> {
            Organization org = mock(Organization.class);
            when(org.getId()).thenReturn(id);
            orgs.add(org);
        });

        when(organizationRepository.findAll()).thenReturn(orgs);
    }

    private static RoleEntity roleWithPermissions(String id, Map<String, char[]> permissions) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(id);
        role.setPermissions(new HashMap<>(permissions));
        return role;
    }
}
