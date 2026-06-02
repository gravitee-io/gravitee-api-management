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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RoleScope.API;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_OWNER;
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
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.Permission;
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
public class NativeApiLogPermissionUpgraderTest {

    private static final String NATIVE_LOG = ApiPermission.NATIVE_LOG.getName();
    private static final String NATIVE_ANALYTICS = ApiPermission.NATIVE_ANALYTICS.getName();
    private static final char[] READ_ONLY = { READ.getId() };
    private static final String OWNER_ID = "owner-id";
    private static final String ORG_A = "org-a";
    private static final String ORG_B = "org-b";

    @InjectMocks
    NativeApiLogPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void should_permit_owner_when_not_permitted() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = restrictedRole(OWNER_ID);
        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> role = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), role.capture());

        Map<String, char[]> updated = role.getValue().getPermissions();
        assertThat(updated).containsKey(NATIVE_LOG).containsKey(NATIVE_ANALYTICS);
        assertThat(updated.get(NATIVE_LOG)).isEqualTo(READ_ONLY);
        assertThat(updated.get(NATIVE_ANALYTICS)).isEqualTo(READ_ONLY);

        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_preserve_existing_permission_on_permit_owner() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        // Pre-existing partial state: NATIVE_LOG present with a customised value (CRUD), NATIVE_ANALYTICS missing.
        char[] charPermission = { 'C', 'R', 'U', 'D' };
        RoleEntity owner = new RoleEntity();
        owner.setId(OWNER_ID);
        owner.setName(ROLE_API_OWNER.getName());
        Map<String, char[]> partial = new HashMap<>();
        partial.put(NATIVE_LOG, charPermission);
        owner.setPermissions(partial);

        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> role = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), role.capture());

        Map<String, char[]> updated = role.getValue().getPermissions();
        assertThat(updated).containsKey(NATIVE_LOG).containsKey(NATIVE_ANALYTICS);
        assertThat(updated.get(NATIVE_LOG)).isEqualTo(charPermission);
        assertThat(updated.get(NATIVE_ANALYTICS)).isEqualTo(READ_ONLY);

        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_not_permit_owner_when_already_permitted() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = new RoleEntity();
        owner.setId(OWNER_ID);
        owner.setName(ROLE_API_OWNER.getName());
        Map<String, char[]> existing = new HashMap<>();
        existing.put(NATIVE_LOG, READ_ONLY);
        existing.put(NATIVE_ANALYTICS, READ_ONLY);
        owner.setPermissions(existing);

        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_permit_owner_when_permissions_null() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        RoleEntity owner = new RoleEntity();
        owner.setId(OWNER_ID);
        owner.setName(ROLE_API_OWNER.getName());
        owner.setPermissions(null);

        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(owner));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getPermissions()).containsKey(NATIVE_LOG).containsKey(NATIVE_ANALYTICS);

        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_sync_primary_owner() throws TechnicalException, UpgraderException {
        String orgId = GraviteeContext.getDefaultOrganization();
        mockOrganizations(orgId);

        when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSync(orgId);
    }

    @Test
    public void should_permit_owner_for_each_organization() throws TechnicalException, UpgraderException {
        Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(ORG_A);
        Organization orgB = mock(Organization.class);
        when(orgB.getId()).thenReturn(ORG_B);
        when(organizationRepository.findAll()).thenReturn(Set.of(orgA, orgB));

        List.of(ORG_A, ORG_B).forEach(orgId ->
            when(roleService.findByScopeAndName(eq(API), eq(ROLE_API_OWNER.getName()), eq(orgId))).thenReturn(
                Optional.of(restrictedRole("owner-" + orgId))
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, times(2)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
        verify(roleService, times(2)).createOrUpdateSystemRole(
            any(ExecutionContext.class),
            eq(PRIMARY_OWNER),
            eq(API),
            any(Permission[].class),
            any(String.class)
        );
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

            when(roleService.findByScopeAndName(API, ROLE_API_OWNER.getName(), orgId)).thenReturn(Optional.of(restrictedRole(OWNER_ID)));
            doThrow(new RuntimeException("RoleService update failure"))
                .when(roleService)
                .update(any(ExecutionContext.class), any(UpdateRoleEntity.class));

            upgrader.upgrade();
        });
    }

    private void mockOrganizations(String... ids) throws TechnicalException {
        Set<Organization> orgs = new java.util.HashSet<>();
        Arrays.stream(ids).forEach(id -> {
            Organization org = mock(Organization.class);
            when(org.getId()).thenReturn(id);
            orgs.add(org);
        });

        when(organizationRepository.findAll()).thenReturn(orgs);
    }

    private void verifyPrimaryOwnerSync(String organizationId) {
        ArgumentCaptor<Permission[]> permission = ArgumentCaptor.forClass(Permission[].class);
        verify(roleService).createOrUpdateSystemRole(
            any(ExecutionContext.class),
            eq(PRIMARY_OWNER),
            eq(API),
            permission.capture(),
            eq(organizationId)
        );
        // Primary-owner sync mirrors the API role: includes NATIVE_*, excludes REVIEWS
        // (see RoleServiceImpl.createOrUpdateSystemRoles).
        assertThat(permission.getValue())
            .contains(ApiPermission.NATIVE_LOG, ApiPermission.NATIVE_ANALYTICS)
            .doesNotContain(ApiPermission.REVIEWS);
    }

    private static RoleEntity restrictedRole(String id) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(ROLE_API_OWNER.getName());
        Map<String, char[]> perms = new HashMap<>(ROLE_API_OWNER.getPermissions());
        perms.remove(NATIVE_LOG);
        perms.remove(NATIVE_ANALYTICS);
        role.setPermissions(perms);

        return role;
    }
}
