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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiService_GetGroupsWithMembersTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @InjectMocks
    private final ApiServiceImpl apiService = new ApiServiceImpl();

    private static final String ENVIRONMENT = "DEFAULT";

    @Before
    public void setUp() throws Exception {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetApiGroupsWithMembers_WithPrimaryOwnerPrivilege_ifGroupIsTheApiPrimaryOwner() throws TechnicalException {
        final String expectedUserRole = "PRIMARY_OWNER";

        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();

        Api api = new Api();
        api.setId(apiId);
        api.setGroups(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = new MemberEntity();
        groupMember.setReferenceId(groupId);
        groupMember.setRoles(List.of(groupMemberApiRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.GROUP,
                List.of(groupId),
                null
            )
        )
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = new MembershipEntity();
        apiPrimaryOwnerMembership.setMemberType(MembershipMemberType.GROUP);
        apiPrimaryOwnerMembership.setMemberId(groupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(
            GraviteeContext.getExecutionContext(),
            apiId
        );
        assertEquals(1, groupsWithMembers.size());

        List<GroupMemberEntity> groupMembers = groupsWithMembers.get(groupId);
        assertNotNull(groupMembers);
        assertEquals(1, groupMembers.size());

        GroupMemberEntity updatedGroupMember = groupMembers.get(0);
        assertNotNull(updatedGroupMember.getRoles());
        assertEquals(1, updatedGroupMember.getRoles().size());

        String userRole = updatedGroupMember.getRoles().get("API");
        assertEquals(expectedUserRole, userRole);
    }

    @Test
    public void shouldGetApiGroupsWithMembers_RevokingPrimaryOwnerPrivilege_ifApiPrimaryOwnerIsOfTypeUser() throws TechnicalException {
        final String expectedUserRole = "OWNER";

        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();
        String apiPrimaryOwnerUserId = UuidString.generateRandom();

        UserEntity apiPrimaryOwner = new UserEntity();
        apiPrimaryOwner.setId(apiPrimaryOwnerUserId);
        when(userService.findById(GraviteeContext.getExecutionContext(), apiPrimaryOwnerUserId)).thenReturn(apiPrimaryOwner);

        Api api = new Api();
        api.setId(apiId);
        api.setGroups(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        group.setRoles(Map.of(RoleScope.API, expectedUserRole));
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = new MemberEntity();
        groupMember.setReferenceId(groupId);
        groupMember.setRoles(List.of(groupMemberApiRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.GROUP,
                List.of(groupId),
                null
            )
        )
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = new MembershipEntity();
        apiPrimaryOwnerMembership.setMemberType(MembershipMemberType.USER);
        apiPrimaryOwnerMembership.setMemberId(apiPrimaryOwnerUserId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        RoleEntity groupDefaultApiRole = new RoleEntity();
        groupDefaultApiRole.setScope(RoleScope.API);
        groupDefaultApiRole.setName(expectedUserRole);
        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(expectedUserRole), eq(GraviteeContext.getCurrentOrganization())))
            .thenReturn(Optional.of(groupDefaultApiRole));

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(
            GraviteeContext.getExecutionContext(),
            apiId
        );
        assertEquals(1, groupsWithMembers.size());

        List<GroupMemberEntity> groupMembers = groupsWithMembers.get(groupId);
        assertNotNull(groupMembers);
        assertEquals(1, groupMembers.size());

        GroupMemberEntity updatedGroupMember = groupMembers.get(0);
        assertNotNull(updatedGroupMember.getRoles());
        assertEquals(1, updatedGroupMember.getRoles().size());

        String userRole = updatedGroupMember.getRoles().get(RoleScope.API.name());
        assertEquals(expectedUserRole, userRole);
    }

    @Test
    public void shouldGetApiGroupsWithMembers_RevokingPrimaryOwnerPrivilege_ifGroupIsNotTheApiPrimaryOwner() throws TechnicalException {
        final String expectedUserRole = "REVIEWER";

        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();
        String apiPrimaryOwnerGroupId = UuidString.generateRandom();

        GroupEntity apiPrimaryOwner = new GroupEntity();
        apiPrimaryOwner.setId(apiPrimaryOwnerGroupId);
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(apiPrimaryOwnerGroupId))).thenReturn(apiPrimaryOwner);

        Api api = new Api();
        api.setId(apiId);
        api.setGroups(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        group.setRoles(Map.of(RoleScope.API, expectedUserRole));
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = new MemberEntity();
        groupMember.setReferenceId(groupId);
        groupMember.setRoles(List.of(groupMemberApiRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.GROUP,
                List.of(groupId),
                null
            )
        )
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = new MembershipEntity();
        apiPrimaryOwnerMembership.setMemberType(MembershipMemberType.GROUP);
        apiPrimaryOwnerMembership.setMemberId(apiPrimaryOwnerGroupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        RoleEntity groupDefaultApiRole = Mockito.mock(RoleEntity.class);
        when(groupDefaultApiRole.getName()).thenReturn(expectedUserRole);
        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(expectedUserRole), eq(GraviteeContext.getCurrentOrganization())))
            .thenReturn(Optional.of(groupDefaultApiRole));

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(
            GraviteeContext.getExecutionContext(),
            apiId
        );
        assertEquals(1, groupsWithMembers.size());

        List<GroupMemberEntity> groupMembers = groupsWithMembers.get(groupId);
        assertNotNull(groupMembers);
        assertEquals(1, groupMembers.size());

        GroupMemberEntity updatedGroupMember = groupMembers.get(0);
        assertNotNull(updatedGroupMember.getRoles());
        assertEquals(1, updatedGroupMember.getRoles().size());

        String updatedMemberRole = updatedGroupMember.getRoles().get(RoleScope.API.name());
        assertEquals(expectedUserRole, updatedMemberRole);
    }

    @Test
    public void shouldGetApiGroupsWithMembers_RemovingPrimaryOwnerPrivilege_ifGroupIsNotTheApiPrimaryOwner_andNoDefaultRole()
        throws TechnicalException {
        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();
        String apiPrimaryOwnerGroupId = UuidString.generateRandom();

        GroupEntity apiPrimaryOwner = new GroupEntity();
        apiPrimaryOwner.setId(apiPrimaryOwnerGroupId);
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(apiPrimaryOwnerGroupId))).thenReturn(apiPrimaryOwner);

        Api api = new Api();
        api.setId(apiId);
        api.setGroups(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        group.setRoles(Map.of());
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = new MemberEntity();
        groupMember.setReferenceId(groupId);
        groupMember.setRoles(List.of(groupMemberApiRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.GROUP,
                List.of(groupId),
                null
            )
        )
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = new MembershipEntity();
        apiPrimaryOwnerMembership.setMemberType(MembershipMemberType.GROUP);
        apiPrimaryOwnerMembership.setMemberId(apiPrimaryOwnerGroupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(
            GraviteeContext.getExecutionContext(),
            apiId
        );
        assertEquals(1, groupsWithMembers.size());

        List<GroupMemberEntity> groupMembers = groupsWithMembers.get(groupId);
        assertNotNull(groupMembers);
        assertEquals(1, groupMembers.size());

        GroupMemberEntity updatedGroupMember = groupMembers.get(0);
        assertNotNull(updatedGroupMember.getRoles());
        assertEquals(1, updatedGroupMember.getRoles().size());

        String userRole = updatedGroupMember.getRoles().get(RoleScope.API.name());
        assertNull(userRole);
    }

    @Test
    public void shouldGetApiGroupsWithMembers_RemovingPrimaryOwnerPrivilege_withTwoGroupsAssociated() throws TechnicalException {
        final String expectedRoleForGroup1 = "REVIEWER";
        final String expectedRoleForGroup2 = "USER";

        String apiId = UuidString.generateRandom();

        String groupId1 = UuidString.generateRandom();
        String groupId2 = UuidString.generateRandom();

        String apiPrimaryOwnerGroupId = UuidString.generateRandom();

        Api api = new Api();
        api.setId(apiId);
        api.setGroups(Set.of(groupId1, groupId2));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity apiPrimaryOwner = new GroupEntity();
        apiPrimaryOwner.setId(apiPrimaryOwnerGroupId);
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(apiPrimaryOwnerGroupId))).thenReturn(apiPrimaryOwner);

        GroupEntity group1 = new GroupEntity();
        group1.setId(groupId1);
        group1.setRoles(Map.of(RoleScope.API, expectedRoleForGroup1));
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId1))).thenReturn(group1);

        GroupEntity group2 = new GroupEntity();
        group2.setId(groupId2);
        group2.setRoles(Map.of(RoleScope.API, expectedRoleForGroup2));
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId2))).thenReturn(group2);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMemberForGroup1 = new MemberEntity();
        groupMemberForGroup1.setId(UuidString.generateRandom());
        groupMemberForGroup1.setReferenceId(groupId1);
        groupMemberForGroup1.setRoles(List.of(groupMemberApiRole));

        MemberEntity groupMemberForGroup2 = new MemberEntity();
        groupMemberForGroup2.setId(UuidString.generateRandom());
        groupMemberForGroup2.setReferenceId(groupId2);
        groupMemberForGroup2.setRoles(List.of(groupMemberApiRole));

        when(
            membershipService.getMembersByReferencesAndRole(
                eq(GraviteeContext.getExecutionContext()),
                eq(MembershipReferenceType.GROUP),
                argThat(groupIds -> groupIds.containsAll(List.of(groupId1, groupId2))),
                isNull()
            )
        )
            .thenReturn(Set.of(groupMemberForGroup1, groupMemberForGroup2));

        MembershipEntity apiPrimaryOwnerMembership = new MembershipEntity();
        apiPrimaryOwnerMembership.setMemberType(MembershipMemberType.GROUP);
        apiPrimaryOwnerMembership.setMemberId(apiPrimaryOwnerGroupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        RoleEntity defaultApiRoleForGroup1 = new RoleEntity();
        defaultApiRoleForGroup1.setScope(RoleScope.API);
        defaultApiRoleForGroup1.setName(expectedRoleForGroup1);
        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(expectedRoleForGroup1), eq(GraviteeContext.getCurrentOrganization())))
            .thenReturn(Optional.of(defaultApiRoleForGroup1));

        RoleEntity defaultApiRoleForGroup2 = new RoleEntity();
        defaultApiRoleForGroup2.setScope(RoleScope.API);
        defaultApiRoleForGroup2.setName(expectedRoleForGroup2);
        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(expectedRoleForGroup2), eq(GraviteeContext.getCurrentOrganization())))
            .thenReturn(Optional.of(defaultApiRoleForGroup2));

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(
            GraviteeContext.getExecutionContext(),
            apiId
        );
        assertEquals(2, groupsWithMembers.size());

        List<GroupMemberEntity> membersForGroup1 = groupsWithMembers.get(groupId1);
        assertNotNull(membersForGroup1);
        assertEquals(1, membersForGroup1.size());
        GroupMemberEntity updatedMemberForGroup1 = membersForGroup1.get(0);
        String userRoleForGroup1 = updatedMemberForGroup1.getRoles().get(RoleScope.API.name());
        assertEquals(expectedRoleForGroup1, userRoleForGroup1);

        List<GroupMemberEntity> membersForGroup2 = groupsWithMembers.get(groupId2);
        assertNotNull(membersForGroup2);
        assertEquals(1, membersForGroup2.size());
        GroupMemberEntity updatedMemberForGroup2 = membersForGroup2.get(0);
        String userRoleForGroup2 = updatedMemberForGroup2.getRoles().get(RoleScope.API.name());
        assertEquals(expectedRoleForGroup2, userRoleForGroup2);
    }

    @Test
    public void shouldGetApiGroupsWithMembers_RemovingPrimaryOwnerPrivilege_ifGroupIsNotTheApiPrimaryOwner_andDefaultRoleHasBeenDeleted()
        throws TechnicalException {
        final String deletedRole = "USER";

        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();
        String apiPrimaryOwnerUserId = UuidString.generateRandom();

        UserEntity apiPrimaryOwner = new UserEntity();
        apiPrimaryOwner.setId(apiPrimaryOwnerUserId);
        when(userService.findById(GraviteeContext.getExecutionContext(), apiPrimaryOwnerUserId)).thenReturn(apiPrimaryOwner);

        Api api = new Api();
        api.setId(apiId);
        api.setGroups(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        group.setRoles(Map.of(RoleScope.API, deletedRole));
        when(groupService.findById(eq(GraviteeContext.getExecutionContext()), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = new MemberEntity();
        groupMember.setReferenceId(groupId);
        groupMember.setRoles(List.of(groupMemberApiRole));
        when(
            membershipService.getMembersByReferencesAndRole(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.GROUP,
                List.of(groupId),
                null
            )
        )
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = new MembershipEntity();
        apiPrimaryOwnerMembership.setMemberType(MembershipMemberType.USER);
        apiPrimaryOwnerMembership.setMemberId(apiPrimaryOwnerUserId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(deletedRole), eq(GraviteeContext.getCurrentOrganization())))
            .thenReturn(Optional.empty());

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(
            GraviteeContext.getExecutionContext(),
            apiId
        );
        assertEquals(1, groupsWithMembers.size());

        List<GroupMemberEntity> groupMembers = groupsWithMembers.get(groupId);
        assertNotNull(groupMembers);
        assertEquals(1, groupMembers.size());

        GroupMemberEntity updatedGroupMember = groupMembers.get(0);
        assertNotNull(updatedGroupMember.getRoles());
        assertEquals(1, updatedGroupMember.getRoles().size());

        String userRole = updatedGroupMember.getRoles().get(RoleScope.API.name());
        assertNull(userRole);
    }
}
