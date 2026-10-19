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

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceUpdateUserRolesTest {

    private static final String USER_NAME = "tuser";
    private static final String ORGANIZATION = "organization#Id";
    private static final String ENVIRONMENT = "environment#Id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION, ENVIRONMENT);

    private static final String CALLER_ID = "caller";
    private static final String ADMIN_ROLE_ID = "admin-role";
    private static final String USER_ROLE_ID = "user-role";

    private final RoleEntity adminRole = role(ADMIN_ROLE_ID, "ADMIN", Map.of("USER", "CRUD".toCharArray(), "ROLE", "CRUD".toCharArray()));
    private final RoleEntity userRole = role(USER_ROLE_ID, "USER", Map.of("USER", "R".toCharArray()));

    @InjectMocks
    private UserServiceImpl userService = new UserServiceImpl();

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserMetadataService userMetadataService;

    @Mock
    private UserConverter userConverter;

    @BeforeEach
    void setUp() throws TechnicalException {
        when(userConverter.toUserEntity(any(User.class), any())).thenCallRealMethod();
        User target = new User();
        target.setId(USER_NAME);
        target.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(of(target));
        when(roleService.findById(ADMIN_ROLE_ID)).thenReturn(adminRole);
        when(roleService.findById(USER_ROLE_ID)).thenReturn(userRole);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_forbid_granting_a_role_with_permissions_the_caller_does_not_hold() {
        authenticateCaller();
        givenTargetMemberWithRoles(userRole);
        givenCallerPermissions(Map.of("USER", "RU".toCharArray()));

        assertThrows(ForbiddenAccessException.class, () ->
            userService.updateUserRoles(
                EXECUTION_CONTEXT,
                USER_NAME,
                MembershipReferenceType.ORGANIZATION,
                ORGANIZATION,
                new ArrayList<>(List.of(ADMIN_ROLE_ID))
            )
        );

        verify(membershipService, never()).removeRole(any(), any(), any(), any(), any());
        verify(membershipService, never()).addRoleToMemberOnReference(any(), any(), any(), any());
    }

    @Test
    void should_grant_a_role_whose_permissions_the_caller_holds() {
        authenticateCaller();
        givenTargetMemberWithRoles();
        givenCallerPermissions(Map.of("USER", "RU".toCharArray()));

        userService.updateUserRoles(
            EXECUTION_CONTEXT,
            USER_NAME,
            MembershipReferenceType.ORGANIZATION,
            ORGANIZATION,
            new ArrayList<>(List.of(USER_ROLE_ID))
        );

        verify(membershipService).addRoleToMemberOnReference(
            eq(EXECUTION_CONTEXT),
            eq(new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, ORGANIZATION)),
            eq(new MembershipService.MembershipMember(USER_NAME, null, MembershipMemberType.USER)),
            eq(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"))
        );
    }

    @Test
    void should_grant_any_role_when_the_caller_is_organization_admin() {
        authenticateCaller("ORGANIZATION:ADMIN");
        givenTargetMemberWithRoles(userRole);

        userService.updateUserRoles(
            EXECUTION_CONTEXT,
            USER_NAME,
            MembershipReferenceType.ORGANIZATION,
            ORGANIZATION,
            new ArrayList<>(List.of(ADMIN_ROLE_ID))
        );

        verify(membershipService).removeRole(
            MembershipReferenceType.ORGANIZATION,
            ORGANIZATION,
            MembershipMemberType.USER,
            USER_NAME,
            USER_ROLE_ID
        );
        verify(membershipService).addRoleToMemberOnReference(
            eq(EXECUTION_CONTEXT),
            any(),
            any(),
            eq(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN"))
        );
    }

    @Test
    void should_keep_a_role_the_user_already_holds_without_checking_the_caller() {
        authenticateCaller();
        givenTargetMemberWithRoles(adminRole);

        userService.updateUserRoles(
            EXECUTION_CONTEXT,
            USER_NAME,
            MembershipReferenceType.ORGANIZATION,
            ORGANIZATION,
            new ArrayList<>(List.of(ADMIN_ROLE_ID))
        );

        verify(membershipService, never()).removeRole(any(), any(), any(), any(), any());
        verify(membershipService, never()).addRoleToMemberOnReference(any(), any(), any(), any());
        verify(membershipService, never()).getUserMemberPermissions(any(), any(), any(), any());
    }

    @Test
    void should_forbid_removing_a_role_with_permissions_the_caller_does_not_hold() {
        authenticateCaller();
        givenTargetMemberWithRoles(adminRole, userRole);
        givenCallerPermissions(Map.of("USER", "RU".toCharArray()));

        assertThrows(ForbiddenAccessException.class, () ->
            userService.updateUserRoles(
                EXECUTION_CONTEXT,
                USER_NAME,
                MembershipReferenceType.ORGANIZATION,
                ORGANIZATION,
                new ArrayList<>(List.of(USER_ROLE_ID))
            )
        );

        verify(membershipService, never()).removeRole(any(), any(), any(), any(), any());
    }

    @Test
    void should_remove_a_role_whose_permissions_the_caller_holds() {
        authenticateCaller();
        givenTargetMemberWithRoles(userRole);
        givenCallerPermissions(Map.of("USER", "RU".toCharArray()));

        userService.updateUserRoles(EXECUTION_CONTEXT, USER_NAME, MembershipReferenceType.ORGANIZATION, ORGANIZATION, new ArrayList<>());

        verify(membershipService).removeRole(
            MembershipReferenceType.ORGANIZATION,
            ORGANIZATION,
            MembershipMemberType.USER,
            USER_NAME,
            USER_ROLE_ID
        );
    }

    @Test
    void should_forbid_granting_an_environment_role_when_the_caller_only_holds_its_permissions_on_the_organization() {
        authenticateCaller();
        RoleEntity environmentRole = role("env-role", "USER", Map.of("USER", "R".toCharArray()));
        environmentRole.setScope(RoleScope.ENVIRONMENT);
        when(roleService.findById("env-role")).thenReturn(environmentRole);
        givenCallerPermissions(Map.of("USER", "RU".toCharArray()));
        when(
            membershipService.getUserMemberPermissions(EXECUTION_CONTEXT, MembershipReferenceType.ENVIRONMENT, ENVIRONMENT, CALLER_ID)
        ).thenReturn(Map.of());

        assertThrows(ForbiddenAccessException.class, () ->
            userService.updateUserRoles(
                EXECUTION_CONTEXT,
                USER_NAME,
                MembershipReferenceType.ENVIRONMENT,
                ENVIRONMENT,
                new ArrayList<>(List.of("env-role"))
            )
        );

        verify(membershipService, never()).addRoleToMemberOnReference(any(), any(), any(), any());
    }

    private void authenticateCaller(String... authorities) {
        var principal = new UserDetails(CALLER_ID, "", Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.setContext(
            new SecurityContextImpl(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()))
        );
    }

    private void givenTargetMemberWithRoles(RoleEntity... roles) {
        MemberEntity member = new MemberEntity();
        member.setRoles(List.of(roles));
        when(membershipService.getUserMember(EXECUTION_CONTEXT, MembershipReferenceType.ORGANIZATION, ORGANIZATION, USER_NAME)).thenReturn(
            member
        );
    }

    private void givenCallerPermissions(Map<String, char[]> permissions) {
        when(
            membershipService.getUserMemberPermissions(EXECUTION_CONTEXT, MembershipReferenceType.ORGANIZATION, ORGANIZATION, CALLER_ID)
        ).thenReturn(permissions);
    }

    private static RoleEntity role(String id, String name, Map<String, char[]> permissions) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(name);
        role.setScope(RoleScope.ORGANIZATION);
        role.setPermissions(permissions);
        return role;
    }
}
