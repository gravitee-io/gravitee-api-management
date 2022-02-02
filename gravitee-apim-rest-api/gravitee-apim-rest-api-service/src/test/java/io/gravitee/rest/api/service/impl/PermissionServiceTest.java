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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.impl.PermissionServiceImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

    @Test
    public void shouldGetConfigurationWithRandomRoles() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        user.setRoles(Collections.singleton(new UserRoleEntity()));
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        assertFalse(permissionService.hasManagementRights(USER_NAME));
    }

    @Test
    public void shouldGetConfigurationWithOrganizationRoles() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        UserRoleEntity userCUDEnvironment = new UserRoleEntity();
        userCUDEnvironment.setScope(RoleScope.ORGANIZATION);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(OrganizationPermission.ROLE.name(), new char[] { 'C', 'U', 'D' });

        userCUDEnvironment.setPermissions(perms);
        user.setRoles(Collections.singleton(userCUDEnvironment));
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        assertTrue(permissionService.hasManagementRights(USER_NAME));
    }

    @Test
    public void shouldGetConfigurationWithEnvironmentRoles() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        UserRoleEntity userCUDEnvironment = new UserRoleEntity();
        userCUDEnvironment.setScope(RoleScope.ENVIRONMENT);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(EnvironmentPermission.API.name(), new char[] { 'C', 'U', 'D' });

        userCUDEnvironment.setPermissions(perms);
        user.setRoles(Collections.singleton(userCUDEnvironment));
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        assertTrue(permissionService.hasManagementRights(USER_NAME));
    }

    @Test
    public void shouldGetConfigurationWithOnlyEnvironmentApplicationPermission() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        UserRoleEntity userCUDEnvironment = new UserRoleEntity();
        userCUDEnvironment.setScope(RoleScope.ENVIRONMENT);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(EnvironmentPermission.APPLICATION.name(), new char[] { 'C', 'U', 'D' });

        userCUDEnvironment.setPermissions(perms);
        user.setRoles(Collections.singleton(userCUDEnvironment));
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        assertFalse(permissionService.hasManagementRights(USER_NAME));
    }

    @Test
    public void shouldGetConfigurationWithAPIRolesWithCUDPermissions() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        user.setRoles(Collections.emptySet());
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.ALERT.name(), new char[] { 'R' });
        perms.put(ApiPermission.ANALYTICS.name(), new char[] { 'C', 'D' });

        UserMembership userMembership = new UserMembership();
        userMembership.setReference("apiId");
        doReturn(Collections.singletonList(userMembership))
            .when(membershipService)
            .findUserMembership(MembershipReferenceType.API, USER_NAME);

        RoleEntity apiRole = new RoleEntity();
        apiRole.setPermissions(perms);
        doReturn(Collections.singleton(apiRole))
            .when(membershipService)
            .getRoles(MembershipReferenceType.API, "apiId", MembershipMemberType.USER, USER_NAME);

        assertTrue(permissionService.hasManagementRights(USER_NAME));
    }

    @Test
    public void shouldGetConfigurationWithoutManagementPartBecauseOfRatingPermission() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        user.setRoles(Collections.emptySet());
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        UserMembership userMembership = new UserMembership();
        userMembership.setReference("apiId");
        doReturn(Collections.singletonList(userMembership))
            .when(membershipService)
            .findUserMembership(MembershipReferenceType.API, USER_NAME);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.RATING.name(), new char[] { 'C', 'U' });
        RoleEntity apiRole = new RoleEntity();
        apiRole.setPermissions(perms);
        doReturn(Collections.singleton(apiRole))
            .when(membershipService)
            .getRoles(MembershipReferenceType.API, "apiId", MembershipMemberType.USER, USER_NAME);

        assertFalse(permissionService.hasManagementRights(USER_NAME));
    }

    @Test
    public void shouldGetConfigurationWithoutManagementPartBecauseOfReadPermission() {
        reset(userService);
        reset(membershipService);
        UserEntity user = new UserEntity();
        user.setRoles(Collections.emptySet());
        doReturn(user).when(userService).findByIdWithRoles(USER_NAME);

        UserMembership userMembership = new UserMembership();
        userMembership.setReference("apiId");
        doReturn(Collections.singletonList(userMembership))
            .when(membershipService)
            .findUserMembership(MembershipReferenceType.API, USER_NAME);

        Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.ALERT.name(), new char[] { 'R' });
        RoleEntity apiRole = new RoleEntity();
        apiRole.setPermissions(perms);
        doReturn(Collections.singleton(apiRole))
            .when(membershipService)
            .getRoles(MembershipReferenceType.API, "apiId", MembershipMemberType.USER, USER_NAME);

        assertFalse(permissionService.hasManagementRights(USER_NAME));
    }
}
