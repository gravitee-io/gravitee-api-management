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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_GetMemberPermissionsTest {

    private static final String API_ID = "api-id-1";
    private static final String GROUP_ID1 = "GROUP_ID1";
    private static final String USERNAME = "johndoe";
    private static final String ROLENAME = "ROLE";
    private static final String ROLENAME2 = "ROLE2";

    private MembershipService membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @BeforeEach
    public void setUp() throws Exception {
        membershipService = new MembershipServiceImpl(
            null,
            userService,
            null,
            null,
            primaryOwnerService,
            null,
            membershipRepository,
            roleService,
            null,
            null,
            null,
            null,
            apiRepository,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Test
    public void shouldGetNoPermissionsIfNotMemberAndWithNoGroup() throws Exception {
        Api api = new Api();
        api.setGroups(Collections.emptySet());
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        doReturn(Collections.emptySet())
            .when(membershipRepository)
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                USERNAME,
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                API_ID
            );
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(
            GraviteeContext.getExecutionContext(),
            apiEntity,
            USERNAME
        );

        assertThat(permissions).isEmpty();
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            API_ID
        );
        verify(apiRepository, times(1)).findById(API_ID);
    }

    @Test
    public void shouldGetPermissionsIfMemberOfApi() throws Exception {
        Api api = new Api();
        api.setGroups(Collections.emptySet());
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        Membership membership = mock(Membership.class);
        doReturn("API_" + ROLENAME).when(membership).getRoleId();
        doReturn(new HashSet<>(List.of(membership)))
            .when(membershipRepository)
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                USERNAME,
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                API_ID
            );

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(GraviteeContext.getExecutionContext(), USERNAME);

        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(
            ApiPermission.DOCUMENTATION.getName(),
            new char[] { RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId() }
        );
        doReturn(rolePerms).when(roleEntity).getPermissions();
        when(roleService.findByIds(Set.of("API_" + ROLENAME))).thenAnswer(invocation -> {
            Set<String> ids = invocation.getArgument(0);
            Map<String, RoleEntity> roles = new HashMap<>();
            if (ids.contains("API_" + ROLENAME)) {
                roles.put("API_" + ROLENAME, roleEntity);
            }
            return roles;
        });

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(
            GraviteeContext.getExecutionContext(),
            apiEntity,
            USERNAME
        );

        assertThat(permissions).isNotNull();
        assertPermissions(rolePerms, permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            API_ID
        );
        verify(membershipRepository, never()).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.GROUP,
            GROUP_ID1
        );
        verify(apiRepository, times(1)).findById(API_ID);
        verify(userService, times(1)).findById(GraviteeContext.getExecutionContext(), USERNAME);
    }

    @Test
    public void shouldGetPermissionsIfMemberOfApiGroup() throws Exception {
        Api api = new Api();
        api.setGroups(Set.of(GROUP_ID1));
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        doReturn(Collections.emptySet())
            .when(membershipRepository)
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                USERNAME,
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                API_ID
            );

        Membership membership = mock(Membership.class);
        doReturn("API_" + ROLENAME).when(membership).getRoleId();
        doReturn(GROUP_ID1).when(membership).getReferenceId();
        doReturn(new HashSet<>(List.of(membership)))
            .when(membershipRepository)
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIds(
                USERNAME,
                MembershipMemberType.USER,
                MembershipReferenceType.GROUP,
                Set.of(GROUP_ID1)
            );

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(GraviteeContext.getExecutionContext(), USERNAME);

        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(
            ApiPermission.DOCUMENTATION.getName(),
            new char[] { RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId() }
        );
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(RoleScope.API).when(roleEntity).getScope();
        when(roleService.findByIds(Set.of("API_" + ROLENAME))).thenAnswer(invocation -> {
            Set<String> ids = invocation.getArgument(0);
            Map<String, RoleEntity> roles = new HashMap<>();
            if (ids.contains("API_" + ROLENAME)) {
                roles.put("API_" + ROLENAME, roleEntity);
            }
            return roles;
        });

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(
            GraviteeContext.getExecutionContext(),
            apiEntity,
            USERNAME
        );

        assertThat(permissions).isNotNull();
        assertPermissions(rolePerms, permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            API_ID
        );
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIds(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.GROUP,
            Set.of(GROUP_ID1)
        );
        verify(apiRepository, times(1)).findById(API_ID);
        verify(userService, times(1)).findById(GraviteeContext.getExecutionContext(), USERNAME);
    }

    @Test
    public void shouldGetMergedPermissionsIfMemberOfApiAndApiGroup() throws Exception {
        Api api = new Api();
        api.setGroups(Set.of(GROUP_ID1));
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        Membership membershipUser = mock(Membership.class);
        doReturn("API_" + ROLENAME).when(membershipUser).getRoleId();
        doReturn(new HashSet<>(List.of(membershipUser)))
            .when(membershipRepository)
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                USERNAME,
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                API_ID
            );

        Membership membershipGroup = mock(Membership.class);
        doReturn("API_" + ROLENAME2).when(membershipGroup).getRoleId();
        doReturn(GROUP_ID1).when(membershipGroup).getReferenceId();
        doReturn(new HashSet<>(List.of(membershipGroup)))
            .when(membershipRepository)
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIds(
                USERNAME,
                MembershipMemberType.USER,
                MembershipReferenceType.GROUP,
                Set.of(GROUP_ID1)
            );

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(GraviteeContext.getExecutionContext(), USERNAME);

        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(
            ApiPermission.DOCUMENTATION.getName(),
            new char[] { RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId() }
        );
        doReturn(rolePerms).when(roleEntity).getPermissions();
        lenient().doReturn(RoleScope.API).when(roleEntity).getScope();

        RoleEntity roleEntity2 = mock(RoleEntity.class);
        Map<String, char[]> rolePerms2 = new HashMap<>();
        rolePerms2.put(
            ApiPermission.DOCUMENTATION.getName(),
            new char[] { RolePermissionAction.READ.getId(), RolePermissionAction.DELETE.getId() }
        );
        rolePerms2.put(ApiPermission.PLAN.getName(), new char[] { RolePermissionAction.READ.getId() });
        doReturn(rolePerms2).when(roleEntity2).getPermissions();
        doReturn(RoleScope.API).when(roleEntity2).getScope();
        when(roleService.findByIds(Set.of("API_" + ROLENAME))).thenReturn(Map.of("API_" + ROLENAME, roleEntity));
        when(roleService.findByIds(Set.of("API_" + ROLENAME2))).thenReturn(Map.of("API_" + ROLENAME2, roleEntity2));

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(
            GraviteeContext.getExecutionContext(),
            apiEntity,
            USERNAME
        );

        assertThat(permissions).isNotNull();
        Map<String, char[]> expectedPermissions = new HashMap<>();
        expectedPermissions.put(
            ApiPermission.DOCUMENTATION.getName(),
            new char[] {
                RolePermissionAction.CREATE.getId(),
                RolePermissionAction.READ.getId(),
                RolePermissionAction.UPDATE.getId(),
                RolePermissionAction.DELETE.getId(),
            }
        );
        expectedPermissions.put(ApiPermission.PLAN.getName(), new char[] { RolePermissionAction.READ.getId() });
        assertPermissions(expectedPermissions, permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.API,
            API_ID
        );
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIds(
            USERNAME,
            MembershipMemberType.USER,
            MembershipReferenceType.GROUP,
            Set.of(GROUP_ID1)
        );
        verify(apiRepository, times(1)).findById(API_ID);
        verify(userService, times(1)).findById(GraviteeContext.getExecutionContext(), USERNAME);
    }

    private void assertPermissions(Map<String, char[]> expected, Map<String, char[]> actual) {
        assertThat(actual).as("there must be " + expected.size() + " permission").hasSameSizeAs(expected);
        for (Map.Entry<String, char[]> expectedEntry : expected.entrySet()) {
            assertThat(actual).as("must contains perm:" + expectedEntry.getKey()).containsKey(expectedEntry.getKey());
            Arrays.sort(expectedEntry.getValue());
            String expectedCRUD = new String(expectedEntry.getValue());
            Arrays.sort(actual.get(expectedEntry.getKey()));
            String actualCRUD = new String(actual.get(expectedEntry.getKey()));
            assertThat(actualCRUD).as("CRUD is OK").isEqualTo(expectedCRUD);
        }
    }
}
