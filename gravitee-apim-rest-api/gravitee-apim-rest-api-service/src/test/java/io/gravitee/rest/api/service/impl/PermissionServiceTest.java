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
package io.gravitee.rest.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.*;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PermissionService.MemberSearchAccess;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PermissionServiceTest {

    @InjectMocks
    private PermissionService permissionService = new PermissionServiceImpl();

    @Mock
    protected UserService userService;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected RoleService roleService;

    private static final String USER_NAME = "username";
    private static final String API_ID = "api-id";

    @Test
    public void shouldGetConfigurationWithRandomRoles() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        user.setRoles(Collections.singleton(new UserRoleEntity()));
        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);
        assertFalse(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetConfigurationWithOrganizationRoles() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        UserRoleEntity userCUDEnvironment = new UserRoleEntity();
        userCUDEnvironment.setScope(RoleScope.ORGANIZATION);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(OrganizationPermission.ROLE.name(), new char[] { 'C', 'U', 'D' });

        userCUDEnvironment.setPermissions(perms);
        user.setRoles(Collections.singleton(userCUDEnvironment));
        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);

        assertTrue(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetConfigurationWithEnvironmentRoles() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        UserRoleEntity userCUDEnvironment = new UserRoleEntity();
        userCUDEnvironment.setScope(RoleScope.ENVIRONMENT);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(EnvironmentPermission.API.name(), new char[] { 'C', 'U', 'D' });

        userCUDEnvironment.setPermissions(perms);
        user.setRoles(Collections.singleton(userCUDEnvironment));
        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);

        assertTrue(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetConfigurationWithOnlyEnvironmentApplicationPermission() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        UserRoleEntity userCUDEnvironment = new UserRoleEntity();
        userCUDEnvironment.setScope(RoleScope.ENVIRONMENT);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(EnvironmentPermission.APPLICATION.name(), new char[] { 'C', 'U', 'D' });

        userCUDEnvironment.setPermissions(perms);
        user.setRoles(Collections.singleton(userCUDEnvironment));
        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);

        assertFalse(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetConfigurationWithAPIRolesWithCUDPermissions() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        user.setRoles(Collections.emptySet());
        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.ALERT.name(), new char[] { 'R' });
        perms.put(ApiPermission.ANALYTICS.name(), new char[] { 'C', 'D' });

        UserMembership userMembership = new UserMembership();
        userMembership.setReference("apiId");
        doReturn(Collections.singletonList(userMembership))
            .when(membershipService)
            .findUserMembership(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, USER_NAME);

        RoleEntity apiRole = new RoleEntity();
        apiRole.setPermissions(perms);
        doReturn(Collections.singleton(apiRole))
            .when(membershipService)
            .getRoles(MembershipReferenceType.API, "apiId", MembershipMemberType.USER, USER_NAME);

        assertTrue(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetConfigurationWithoutManagementPartBecauseOfRatingPermission() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.RATING.name(), new char[] { 'C', 'U' });
        RoleEntity apiRole = new RoleEntity();
        apiRole.setName("RATING");
        apiRole.setScope(RoleScope.API);
        apiRole.setPermissions(perms);

        UserMembership userMembership = new UserMembership();
        userMembership.setRoles(Map.of(apiRole.getScope().name(), apiRole.getName()));
        userMembership.setReference(API_ID);

        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        user.setRoles(Collections.emptySet());

        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);

        doReturn(List.of(userMembership))
            .when(membershipService)
            .findUserMembership(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, USER_NAME);

        doReturn(Collections.singleton(apiRole))
            .when(membershipService)
            .getRoles(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, USER_NAME);

        assertFalse(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetConfigurationWithoutManagementPartBecauseOfReadPermission() {
        setSecurityContext(false);
        reset(userService);
        reset(membershipService);

        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        user.setRoles(Collections.emptySet());

        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);

        UserMembership userMembership = new UserMembership();
        userMembership.setReference(API_ID);

        doReturn(List.of(userMembership))
            .when(membershipService)
            .findUserMembership(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, USER_NAME);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.ALERT.name(), new char[] { 'R' });
        RoleEntity apiRole = new RoleEntity();
        apiRole.setPermissions(perms);

        doReturn(Collections.singleton(apiRole))
            .when(membershipService)
            .getRoles(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, USER_NAME);

        assertFalse(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldByPassChecksBecauseIsOrganizationAdmin() {
        setSecurityContext(true);
        assertTrue(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        assertTrue(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_MEMBER,
                "reference-id",
                RolePermissionAction.UPDATE
            )
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldNotByPassChecksWithoutAuthentication() {
        final SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        reset(userService);
        reset(membershipService);

        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        user.setRoles(Collections.emptySet());

        doReturn(user).when(userService).findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME);
        doReturn(null).when(membershipService).getUserMemberPermissions(any(), any(), any(), any());
        assertFalse(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME));
        assertFalse(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_MEMBER,
                "reference-id",
                RolePermissionAction.UPDATE
            )
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    public void should_let_organization_admin_manage_members_without_looking_at_memberships() {
        setSecurityContext(true);

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));

        verifyNoInteractions(membershipService, roleService);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void should_let_user_who_reads_organization_users_manage_members() {
        givenUserRoles(role(RoleScope.ORGANIZATION, OrganizationPermission.USER, "R"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_organization_users_manage_members() {
        givenUserRoles(role(RoleScope.ORGANIZATION, OrganizationPermission.USER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_organization_roles_manage_members() {
        givenUserRoles(role(RoleScope.ORGANIZATION, OrganizationPermission.ROLE, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_environment_groups_manage_members() {
        givenUserRoles(role(RoleScope.ENVIRONMENT, EnvironmentPermission.GROUP, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_api_manage_members() {
        givenUserRoles(role(RoleScope.API, ApiPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_application_manage_members() {
        givenUserRoles(role(RoleScope.APPLICATION, ApplicationPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_integration_manage_members() {
        givenUserRoles(role(RoleScope.INTEGRATION, IntegrationPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_a_cluster_manage_members() {
        givenUserRoles(role(RoleScope.CLUSTER, ClusterPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_through_a_group_role_manage_members() {
        givenUserRoles(role(RoleScope.GROUP, GroupPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    // Adding a member is a CREATE: the add endpoints accept it and the Console shows the member picker on it.
    @Test
    public void should_let_user_who_may_only_add_members_of_an_api_manage_members() {
        givenUserRoles(role(RoleScope.API, ApiPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_an_application_manage_members() {
        givenUserRoles(role(RoleScope.APPLICATION, ApplicationPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_an_integration_manage_members() {
        givenUserRoles(role(RoleScope.INTEGRATION, IntegrationPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_a_cluster_manage_members() {
        givenUserRoles(role(RoleScope.CLUSTER, ClusterPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_through_a_group_role_manage_members() {
        givenUserRoles(role(RoleScope.GROUP, GroupPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_create_environment_groups_manage_members() {
        givenUserRoles(role(RoleScope.ENVIRONMENT, EnvironmentPermission.GROUP, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_create_organization_roles_manage_members() {
        givenUserRoles(role(RoleScope.ORGANIZATION, OrganizationPermission.ROLE, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_api_product_manage_members() {
        givenUserRoles(role(RoleScope.API_PRODUCT, ApiProductPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_an_api_product_manage_members() {
        givenUserRoles(role(RoleScope.API_PRODUCT, ApiProductPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_ai_catalog_manage_members() {
        givenUserRoles(role(RoleScope.AI_CATALOG, AiCatalogPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_an_ai_catalog_manage_members() {
        givenUserRoles(role(RoleScope.AI_CATALOG, AiCatalogPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_explorer_manage_members() {
        givenUserRoles(role(RoleScope.EXPLORER, ExplorerPermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_an_explorer_manage_members() {
        givenUserRoles(role(RoleScope.EXPLORER, ExplorerPermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_members_of_an_ai_workspace_manage_members() {
        givenUserRoles(role(RoleScope.AI_WORKSPACE, AiWorkspacePermission.MEMBER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_members_of_an_ai_workspace_manage_members() {
        givenUserRoles(role(RoleScope.AI_WORKSPACE, AiWorkspacePermission.MEMBER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_updates_users_of_an_ai_workspace_manage_members() {
        givenUserRoles(role(RoleScope.AI_WORKSPACE, AiWorkspacePermission.USER, "U"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_let_user_who_may_only_add_users_of_an_ai_workspace_manage_members() {
        givenUserRoles(role(RoleScope.AI_WORKSPACE, AiWorkspacePermission.USER, "C"));

        assertTrue(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_refuse_user_whose_roles_only_read_members() {
        givenUserRoles(
            role(RoleScope.ORGANIZATION, OrganizationPermission.ROLE, "R"),
            role(RoleScope.ENVIRONMENT, EnvironmentPermission.GROUP, "R"),
            role(RoleScope.API, ApiPermission.MEMBER, "R"),
            role(RoleScope.APPLICATION, ApplicationPermission.MEMBER, "R"),
            role(RoleScope.API_PRODUCT, ApiProductPermission.MEMBER, "R"),
            role(RoleScope.AI_CATALOG, AiCatalogPermission.MEMBER, "R"),
            role(RoleScope.EXPLORER, ExplorerPermission.MEMBER, "R"),
            role(RoleScope.AI_WORKSPACE, AiWorkspacePermission.USER, "R"),
            role(RoleScope.AI_WORKSPACE, AiWorkspacePermission.MEMBER, "R"),
            role(RoleScope.GROUP, GroupPermission.MEMBER, "R")
        );

        assertFalse(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_refuse_user_with_default_organization_and_environment_roles_only() {
        // The default roles manage no members: creating applications is not managing their members
        RoleEntity organizationUser = role(RoleScope.ORGANIZATION, OrganizationPermission.ENVIRONMENT, "R");
        organizationUser.getPermissions().put(OrganizationPermission.ROLE.getName(), "R".toCharArray());
        RoleEntity environmentUser = role(RoleScope.ENVIRONMENT, EnvironmentPermission.APPLICATION, "CRUD");
        environmentUser.getPermissions().put(EnvironmentPermission.API.getName(), "R".toCharArray());
        environmentUser.getPermissions().put(EnvironmentPermission.GROUP.getName(), "R".toCharArray());
        givenUserRoles(organizationUser, environmentUser);

        assertFalse(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    @Test
    public void should_flag_application_only_manager() {
        givenUserRoles(role(RoleScope.APPLICATION, ApplicationPermission.MEMBER, "U"));

        assertMemberSearchAccessIs(MemberSearchAccess.APPLICATION_ONLY);
    }

    @Test
    public void should_not_flag_application_manager_with_other_scopes() {
        givenUserRoles(role(RoleScope.APPLICATION, ApplicationPermission.MEMBER, "U"), role(RoleScope.API, ApiPermission.MEMBER, "U"));

        assertMemberSearchAccessIs(MemberSearchAccess.FULL);
    }

    @Test
    public void should_not_flag_non_application_manager() {
        givenUserRoles(role(RoleScope.API, ApiPermission.MEMBER, "U"));

        assertMemberSearchAccessIs(MemberSearchAccess.FULL);
    }

    @Test
    public void should_not_flag_organization_admin_as_application_only() {
        setSecurityContext(true);

        assertMemberSearchAccessIs(MemberSearchAccess.FULL);

        verifyNoInteractions(membershipService, roleService);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void should_not_flag_user_without_membership_as_application_only() {
        setSecurityContext(false);
        when(membershipService.getRoleIdsByMember(MembershipMemberType.USER, USER_NAME)).thenReturn(Set.of());

        assertMemberSearchAccessIs(MemberSearchAccess.NONE);

        SecurityContextHolder.clearContext();
    }

    @Test
    public void should_refuse_user_without_any_membership() {
        setSecurityContext(false);
        when(membershipService.getRoleIdsByMember(MembershipMemberType.USER, USER_NAME)).thenReturn(Set.of());

        assertFalse(permissionService.canManageMembers(GraviteeContext.getExecutionContext(), USER_NAME));

        verifyNoInteractions(roleService);
        SecurityContextHolder.clearContext();
    }

    private void assertMemberSearchAccessIs(MemberSearchAccess expected) {
        assertEquals(expected, permissionService.memberSearchAccess(GraviteeContext.getExecutionContext(), USER_NAME));
    }

    private void givenUserRoles(RoleEntity... roles) {
        setSecurityContext(false);
        Map<String, RoleEntity> rolesById = new HashMap<>();
        for (RoleEntity role : roles) {
            rolesById.put(role.getId(), role);
        }
        when(membershipService.getRoleIdsByMember(MembershipMemberType.USER, USER_NAME)).thenReturn(rolesById.keySet());
        when(roleService.findByIds(rolesById.keySet())).thenReturn(rolesById);
    }

    private static RoleEntity role(RoleScope scope, Permission permission, String acls) {
        RoleEntity role = new RoleEntity();
        role.setId(scope.name() + "-" + permission.getName() + "-" + acls);
        role.setScope(scope);
        Map<String, char[]> permissions = new HashMap<>();
        permissions.put(permission.getName(), acls.toCharArray());
        role.setPermissions(permissions);
        return role;
    }

    private static void setSecurityContext(boolean isOrganizationAdmin) {
        final Authentication authentication = mock(Authentication.class);
        final GrantedAuthority grantedAuthority = mock(GrantedAuthority.class);
        if (isOrganizationAdmin) {
            when(grantedAuthority.getAuthority()).thenReturn(RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name());
        }
        doReturn(List.of(grantedAuthority)).when(authentication).getAuthorities();
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterAll
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }
}
