/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.*;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

    @InjectMocks
    private PermissionService permissionService = new PermissionServiceImpl();

    @Mock
    protected UserService userService;

    @Mock
    protected MembershipService membershipService;

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
}
