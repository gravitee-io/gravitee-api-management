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

import static io.gravitee.rest.api.model.permissions.RoleScope.API_PRODUCT;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_USER;
import static org.assertj.core.api.Assertions.assertThat;
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
import io.gravitee.rest.api.model.permissions.ApiProductPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiProductMemberPermissionUpgraderTest {

    @InjectMocks
    ApiProductMemberPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    private void verifyPrimaryOwnerSyncedForOrg(String organizationId) {
        verify(roleService).createOrUpdateSystemRole(
            any(ExecutionContext.class),
            eq(PRIMARY_OWNER),
            eq(API_PRODUCT),
            any(Permission[].class),
            eq(organizationId)
        );
    }

    @Test(expected = UpgraderException.class)
    public void upgrade_should_fail_when_organization_repository_throws() throws TechnicalException, UpgraderException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException("db error"));
        upgrader.upgrade();
    }

    @Test
    public void should_add_member_permission_to_owner_and_user_when_missing() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        RoleEntity ownerWithoutMember = roleWithoutMember(
            "owner-id",
            ROLE_API_PRODUCT_OWNER.getName(),
            ROLE_API_PRODUCT_OWNER.getPermissions()
        );
        RoleEntity userWithoutMember = roleWithoutMember(
            "user-id",
            ROLE_API_PRODUCT_USER.getName(),
            ROLE_API_PRODUCT_USER.getPermissions()
        );

        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_OWNER.getName(), organizationId)).thenReturn(
            Optional.of(ownerWithoutMember)
        );
        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_USER.getName(), organizationId)).thenReturn(
            Optional.of(userWithoutMember)
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(2)).update(any(ExecutionContext.class), updateCaptor.capture());

        List<UpdateRoleEntity> updates = updateCaptor.getAllValues();
        assertThat(updates).hasSize(2);

        char[] expectedOwnerMember = ROLE_API_PRODUCT_OWNER.getPermissions().get(ApiProductPermission.MEMBER.getName());
        char[] expectedUserMember = ROLE_API_PRODUCT_USER.getPermissions().get(ApiProductPermission.MEMBER.getName());

        for (UpdateRoleEntity update : updates) {
            Map<String, char[]> perms = update.getPermissions();
            assertThat(perms).containsKey(ApiProductPermission.MEMBER.getName());
            if (ROLE_API_PRODUCT_OWNER.getName().equals(update.getName())) {
                assertThat(perms.get(ApiProductPermission.MEMBER.getName())).isEqualTo(expectedOwnerMember);
            } else if (ROLE_API_PRODUCT_USER.getName().equals(update.getName())) {
                assertThat(perms.get(ApiProductPermission.MEMBER.getName())).isEqualTo(expectedUserMember);
            }
        }

        verifyPrimaryOwnerSyncedForOrg(organizationId);
    }

    @Test
    public void should_not_call_update_when_member_already_present_on_both_roles() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        RoleEntity owner = new RoleEntity();
        owner.setId("owner-id");
        owner.setName(ROLE_API_PRODUCT_OWNER.getName());
        owner.setPermissions(new HashMap<>(ROLE_API_PRODUCT_OWNER.getPermissions()));

        RoleEntity user = new RoleEntity();
        user.setId("user-id");
        user.setName(ROLE_API_PRODUCT_USER.getName());
        user.setPermissions(new HashMap<>(ROLE_API_PRODUCT_USER.getPermissions()));

        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_OWNER.getName(), organizationId)).thenReturn(Optional.of(owner));
        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_USER.getName(), organizationId)).thenReturn(Optional.of(user));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
        verifyPrimaryOwnerSyncedForOrg(organizationId);
    }

    @Test
    public void should_add_member_only_for_owner_when_user_role_absent() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        RoleEntity ownerWithoutMember = roleWithoutMember(
            "owner-id",
            ROLE_API_PRODUCT_OWNER.getName(),
            ROLE_API_PRODUCT_OWNER.getPermissions()
        );

        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_OWNER.getName(), organizationId)).thenReturn(
            Optional.of(ownerWithoutMember)
        );
        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_USER.getName(), organizationId)).thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService, times(1)).update(any(ExecutionContext.class), updateCaptor.capture());
        UpdateRoleEntity update = updateCaptor.getValue();
        assertThat(update.getName()).isEqualTo(ROLE_API_PRODUCT_OWNER.getName());
        assertThat(update.getPermissions()).containsKey(ApiProductPermission.MEMBER.getName());

        verifyPrimaryOwnerSyncedForOrg(organizationId);
    }

    @Test
    public void should_add_member_when_role_permissions_map_is_null() throws TechnicalException, UpgraderException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        RoleEntity owner = new RoleEntity();
        owner.setId("owner-id");
        owner.setName(ROLE_API_PRODUCT_OWNER.getName());
        owner.setPermissions(null);

        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_OWNER.getName(), organizationId)).thenReturn(Optional.of(owner));
        when(roleService.findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_USER.getName(), organizationId)).thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<UpdateRoleEntity> updateCaptor = ArgumentCaptor.forClass(UpdateRoleEntity.class);
        verify(roleService).update(any(ExecutionContext.class), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getPermissions()).containsEntry(
            ApiProductPermission.MEMBER.getName(),
            ROLE_API_PRODUCT_OWNER.getPermissions().get(ApiProductPermission.MEMBER.getName())
        );

        verifyPrimaryOwnerSyncedForOrg(organizationId);
    }

    @Test
    public void should_process_each_organization() throws TechnicalException, UpgraderException {
        Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn("org-a");
        Organization orgB = mock(Organization.class);
        when(orgB.getId()).thenReturn("org-b");
        when(organizationRepository.findAll()).thenReturn(Set.of(orgA, orgB));

        for (String orgId : List.of("org-a", "org-b")) {
            RoleEntity owner = roleWithoutMember(
                "owner-" + orgId,
                ROLE_API_PRODUCT_OWNER.getName(),
                ROLE_API_PRODUCT_OWNER.getPermissions()
            );
            RoleEntity user = roleWithoutMember("user-" + orgId, ROLE_API_PRODUCT_USER.getName(), ROLE_API_PRODUCT_USER.getPermissions());
            when(roleService.findByScopeAndName(eq(API_PRODUCT), eq(ROLE_API_PRODUCT_OWNER.getName()), eq(orgId))).thenReturn(
                Optional.of(owner)
            );
            when(roleService.findByScopeAndName(eq(API_PRODUCT), eq(ROLE_API_PRODUCT_USER.getName()), eq(orgId))).thenReturn(
                Optional.of(user)
            );
        }

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, times(4)).update(any(ExecutionContext.class), any(UpdateRoleEntity.class));
        verify(roleService, times(2)).createOrUpdateSystemRole(
            any(ExecutionContext.class),
            eq(PRIMARY_OWNER),
            eq(API_PRODUCT),
            any(Permission[].class),
            any(String.class)
        );
    }

    @Test
    public void getOrder_matches_api_product_member_permission_upgrader() {
        Assert.assertEquals(UpgraderOrder.API_PRODUCT_MEMBER_PERMISSION_UPGRADER, upgrader.getOrder());
    }

    private static RoleEntity roleWithoutMember(String id, String name, Map<String, char[]> fullPermissions) {
        Map<String, char[]> withoutMember = new HashMap<>(fullPermissions);
        withoutMember.remove(ApiProductPermission.MEMBER.getName());
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(name);
        role.setPermissions(withoutMember);
        return role;
    }
}
