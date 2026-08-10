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
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_EDGE_MANAGER;
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
public class EdgeManagerApiCreatePermissionUpgraderTest {

    private static final String API_PERMISSION = EnvironmentPermission.API.getName();

    @InjectMocks
    EdgeManagerApiCreatePermissionUpgrader upgrader;

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
    public void should_add_create_acl_and_keep_other_permissions() throws TechnicalException, UpgraderException {
        // Given
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEdgeManagerRole(organizationId, edgeManagerRole("edge-manager-id", new char[] { READ.getId() }));

        // When
        assertThat(upgrader.upgrade()).isTrue();

        // Then
        UpdateRoleEntity update = captureSingleUpdate();
        assertThat(update.getId()).isEqualTo("edge-manager-id");
        assertThat(update.getName()).isEqualTo(ROLE_ENVIRONMENT_EDGE_MANAGER.getName());
        assertThat(update.getScope()).isEqualTo(ENVIRONMENT);
        assertThat(update.getPermissions().get(API_PERMISSION)).containsExactlyInAnyOrder(READ.getId(), CREATE.getId());
        assertThat(update.getPermissions().get(EnvironmentPermission.EDGE_CONFIGURATION.getName())).containsExactlyInAnyOrder(
            CREATE.getId(),
            READ.getId(),
            UPDATE.getId(),
            DELETE.getId()
        );
        assertThat(update.getPermissions().get(EnvironmentPermission.PLATFORM.getName())).containsExactly(READ.getId());
    }

    @Test
    public void should_not_update_when_create_acl_is_already_granted() throws TechnicalException, UpgraderException {
        // Given
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEdgeManagerRole(organizationId, edgeManagerRole("edge-manager-id", new char[] { CREATE.getId(), READ.getId() }));

        // When
        assertThat(upgrader.upgrade()).isTrue();

        // Then
        verify(roleService, never()).update(any(), any());
    }

    @Test
    public void should_not_update_when_edge_manager_role_is_absent() throws TechnicalException, UpgraderException {
        // Given
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        when(roleService.findByScopeAndName(ENVIRONMENT, ROLE_ENVIRONMENT_EDGE_MANAGER.getName(), organizationId)).thenReturn(
            Optional.empty()
        );

        // When
        assertThat(upgrader.upgrade()).isTrue();

        // Then
        verify(roleService, never()).update(any(), any());
    }

    @Test
    public void should_add_create_acl_when_role_has_no_api_permission() throws TechnicalException, UpgraderException {
        // Given
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        givenEdgeManagerRole(organizationId, edgeManagerRole("edge-manager-id", null));

        // When
        assertThat(upgrader.upgrade()).isTrue();

        // Then
        assertThat(captureSingleUpdate().getPermissions().get(API_PERMISSION)).containsExactly(CREATE.getId());
    }

    @Test
    public void should_add_create_acl_when_role_permissions_map_is_null() throws TechnicalException, UpgraderException {
        // Given
        String organizationId = GraviteeContext.getDefaultOrganization();
        givenOrganizations(organizationId);
        RoleEntity role = RoleEntity.builder()
            .id("edge-manager-id")
            .name(ROLE_ENVIRONMENT_EDGE_MANAGER.getName())
            .scope(ENVIRONMENT)
            .permissions(null)
            .build();
        givenEdgeManagerRole(organizationId, role);

        // When
        assertThat(upgrader.upgrade()).isTrue();

        // Then
        Map<String, char[]> permissions = captureSingleUpdate().getPermissions();
        assertThat(permissions).containsOnlyKeys(API_PERMISSION);
        assertThat(permissions.get(API_PERMISSION)).containsExactly(CREATE.getId());
    }

    @Test
    public void should_process_each_organization() throws TechnicalException, UpgraderException {
        // Given
        givenOrganizations("org-a", "org-b");
        givenEdgeManagerRole("org-a", edgeManagerRole("edge-manager-org-a", new char[] { READ.getId() }));
        givenEdgeManagerRole("org-b", edgeManagerRole("edge-manager-org-b", new char[] { READ.getId() }));

        // When
        assertThat(upgrader.upgrade()).isTrue();

        // Then
        verify(roleService, times(2)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
    }

    @Test
    public void getOrder_matches_edge_manager_api_create_permission_upgrader() {
        Assertions.assertEquals(UpgraderOrder.EDGE_MANAGER_API_CREATE_PERMISSION_UPGRADER, upgrader.getOrder());
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

    private void givenEdgeManagerRole(String organizationId, RoleEntity role) {
        when(roleService.findByScopeAndName(eq(ENVIRONMENT), eq(ROLE_ENVIRONMENT_EDGE_MANAGER.getName()), eq(organizationId))).thenReturn(
            Optional.of(role)
        );
    }

    private static RoleEntity edgeManagerRole(String id, char[] apiAcls) {
        Map<String, char[]> permissions = new HashMap<>(ROLE_ENVIRONMENT_EDGE_MANAGER.getPermissions());
        if (apiAcls == null) {
            permissions.remove(API_PERMISSION);
        } else {
            permissions.put(API_PERMISSION, apiAcls);
        }
        return RoleEntity.builder()
            .id(id)
            .name(ROLE_ENVIRONMENT_EDGE_MANAGER.getName())
            .scope(ENVIRONMENT)
            .permissions(permissions)
            .build();
    }

    private UpdateRoleEntity captureSingleUpdate() {
        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(1)).update(any(ExecutionContext.class), updateCaptor.capture());
        return updateCaptor.getValue();
    }
}
