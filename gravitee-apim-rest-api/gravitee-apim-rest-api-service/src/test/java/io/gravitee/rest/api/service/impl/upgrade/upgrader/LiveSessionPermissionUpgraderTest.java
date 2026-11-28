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
import static io.gravitee.rest.api.model.permissions.RoleScope.API;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class LiveSessionPermissionUpgraderTest {

    private static final String LIVE_SESSION = ApiPermission.LIVE_SESSION.getName();
    private static final String LOG = ApiPermission.LOG.getName();
    private static final char[] CRUD = { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };
    private static final String OWNER_ID = "owner-id";
    private static final String ORG_A = "org-a";
    private static final String ORG_B = "org-b";

    @InjectMocks
    LiveSessionPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void should_give_owner_crud_when_log_is_crud() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = ownerWithoutLiveSession(OWNER_ID);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verifyOwnerGranted(owner, CRUD);
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_give_owner_read_only_when_log_is_read_only() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = ownerWithoutLiveSession(OWNER_ID);
        owner.getPermissions().put(LOG, new char[] { READ.getId() });
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verifyOwnerGranted(owner, new char[] { READ.getId() });
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_not_update_owner_when_log_is_absent() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = ownerWithoutLiveSession(OWNER_ID);
        owner.getPermissions().remove(LOG);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_not_update_owner_when_log_is_empty() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = ownerWithoutLiveSession(OWNER_ID);
        owner.getPermissions().put(LOG, new char[0]);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_keep_existing_permission_when_owner_already_permitted() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = ownerWithoutLiveSession(OWNER_ID);
        char[] customized = { READ.getId() };
        owner.getPermissions().put(LIVE_SESSION, customized);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        assertThat(owner.getPermissions().get(LIVE_SESSION)).isEqualTo(customized);
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_not_update_owner_when_permissions_null() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = new RoleEntity();
        owner.setId(OWNER_ID);
        owner.setName(ROLE_API_OWNER.getName());
        owner.setPermissions(null);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_only_sync_primary_owner_when_no_owner_role() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_permit_owner_for_each_organization() throws TechnicalException, UpgraderException {
        mockOrganizations(ORG_A, ORG_B);
        List.of(ORG_A, ORG_B).forEach(orgId ->
            when(roleService.findByScopeAndName(eq(API), eq(ROLE_API_OWNER.getName()), eq(orgId))).thenReturn(
                Optional.of(ownerWithoutLiveSession("owner-" + orgId))
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> roles = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(2)).update(any(ExecutionContext.class), roles.capture());
        assertThat(roles.getAllValues()).extracting(UpdateRoleEntity::getId).containsExactlyInAnyOrder("owner-" + ORG_A, "owner-" + ORG_B);
        verifyPrimaryOwnerSync(ORG_A);
        verifyPrimaryOwnerSync(ORG_B);
    }

    @Test
    public void throws_UpgraderException_when_organization_repository_throws_RuntimeException() throws TechnicalException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException("OrganizationRepository connection failure"));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
    }

    @Test
    public void throws_UpgraderException_when_role_service_throws_RuntimeException() throws TechnicalException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(
            Optional.of(ownerWithoutLiveSession(OWNER_ID))
        );
        doThrow(new RuntimeException("RoleService update failure"))
            .when(roleService)
            .update(any(ExecutionContext.class), any(UpdateRoleEntity.class));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
    }

    private void mockOrganizations(String... ids) throws TechnicalException {
        Set<Organization> organizations = new HashSet<>();
        Arrays.stream(ids).forEach(id -> {
            Organization organization = mock(Organization.class);
            when(organization.getId()).thenReturn(id);
            organizations.add(organization);
        });
        when(organizationRepository.findAll()).thenReturn(organizations);
    }

    private void verifyOwnerGranted(RoleEntity owner, char[] expectedLiveSession) {
        ArgumentCaptor<UpdateRoleEntity> role = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), role.capture());

        assertThat(role.getValue().getId()).isEqualTo(owner.getId());
        Map<String, char[]> updated = role.getValue().getPermissions();
        assertThat(updated.get(LIVE_SESSION)).isEqualTo(expectedLiveSession);
        Map<String, char[]> others = new HashMap<>(updated);
        others.remove(LIVE_SESSION);
        assertThat(others).containsExactlyInAnyOrderEntriesOf(owner.getPermissions());
    }

    private void verifyPrimaryOwnerSync(String organizationId) {
        ArgumentCaptor<Permission[]> permissions = ArgumentCaptor.forClass(Permission[].class);
        verify(roleService).createOrUpdateSystemRole(
            any(ExecutionContext.class),
            eq(PRIMARY_OWNER),
            eq(API),
            permissions.capture(),
            eq(organizationId)
        );
        Permission[] expected = Arrays.stream(ApiPermission.values())
            .filter(permission -> permission != ApiPermission.REVIEWS)
            .toArray(Permission[]::new);
        assertThat(permissions.getValue()).containsExactlyInAnyOrder(expected).contains(ApiPermission.LIVE_SESSION);
    }

    private static RoleEntity ownerWithoutLiveSession(String id) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(ROLE_API_OWNER.getName());
        Map<String, char[]> permissions = new HashMap<>(ROLE_API_OWNER.getPermissions());
        permissions.remove(LIVE_SESSION);
        role.setPermissions(permissions);
        return role;
    }
}
