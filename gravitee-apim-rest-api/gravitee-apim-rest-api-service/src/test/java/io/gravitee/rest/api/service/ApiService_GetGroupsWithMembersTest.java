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
package io.gravitee.rest.api.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Test
    public void shouldGetApiGroupsWithMembers_WithPrimaryOwnerPrivilege_ifGroupIsTheApiPrimaryOwner() throws TechnicalException {
        final String expectedUserRole = "PRIMARY_OWNER";

        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();

        Api api = mock(Api.class);
        when(api.getId()).thenReturn(apiId);
        when(api.getGroups()).thenReturn(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = Mockito.mock(GroupEntity.class);
        when(group.getId()).thenReturn(groupId);
        when(groupService.findById(eq(ENVIRONMENT), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = Mockito.mock(MemberEntity.class);
        when(groupMember.getReferenceId()).thenReturn(groupId);
        when(groupMember.getRoles()).thenReturn(List.of(groupMemberApiRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.GROUP, List.of(groupId), null))
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = Mockito.mock(MembershipEntity.class);
        when(apiPrimaryOwnerMembership.getMemberType()).thenReturn(MembershipMemberType.GROUP);
        when(apiPrimaryOwnerMembership.getMemberId()).thenReturn(groupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(ENVIRONMENT, apiId);
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

        UserEntity apiPrimaryOwner = Mockito.mock(UserEntity.class);
        when(apiPrimaryOwner.getId()).thenReturn(apiPrimaryOwnerUserId);
        when(userService.findById(apiPrimaryOwnerUserId)).thenReturn(apiPrimaryOwner);

        Api api = mock(Api.class);
        when(api.getId()).thenReturn(apiId);
        when(api.getGroups()).thenReturn(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = Mockito.mock(GroupEntity.class);
        when(group.getId()).thenReturn(groupId);
        when(group.getRoles()).thenReturn(Map.of(RoleScope.API, expectedUserRole));
        when(groupService.findById(eq(ENVIRONMENT), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = Mockito.mock(MemberEntity.class);
        when(groupMember.getReferenceId()).thenReturn(groupId);
        when(groupMember.getRoles()).thenReturn(List.of(groupMemberApiRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.GROUP, List.of(groupId), null))
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = Mockito.mock(MembershipEntity.class);
        when(apiPrimaryOwnerMembership.getMemberType()).thenReturn(MembershipMemberType.USER);
        when(apiPrimaryOwnerMembership.getMemberId()).thenReturn(apiPrimaryOwnerUserId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        RoleEntity groupDefaultApiRole = Mockito.mock(RoleEntity.class);
        when(groupDefaultApiRole.getName()).thenReturn(expectedUserRole);
        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(expectedUserRole))).thenReturn(Optional.of(groupDefaultApiRole));

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(ENVIRONMENT, apiId);
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

        GroupEntity apiPrimaryOwner = Mockito.mock(GroupEntity.class);
        when(apiPrimaryOwner.getId()).thenReturn(apiPrimaryOwnerGroupId);
        when(groupService.findById(anyString(), eq(apiPrimaryOwnerGroupId))).thenReturn(apiPrimaryOwner);

        Api api = mock(Api.class);
        when(api.getId()).thenReturn(apiId);
        when(api.getGroups()).thenReturn(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = Mockito.mock(GroupEntity.class);
        when(group.getId()).thenReturn(groupId);
        when(group.getRoles()).thenReturn(Map.of(RoleScope.API, expectedUserRole));
        when(groupService.findById(eq(ENVIRONMENT), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = Mockito.mock(MemberEntity.class);
        when(groupMember.getReferenceId()).thenReturn(groupId);
        when(groupMember.getRoles()).thenReturn(List.of(groupMemberApiRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.GROUP, List.of(groupId), null))
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = Mockito.mock(MembershipEntity.class);
        when(apiPrimaryOwnerMembership.getMemberType()).thenReturn(MembershipMemberType.GROUP);
        when(apiPrimaryOwnerMembership.getMemberId()).thenReturn(apiPrimaryOwnerGroupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        RoleEntity groupDefaultApiRole = Mockito.mock(RoleEntity.class);
        when(groupDefaultApiRole.getName()).thenReturn(expectedUserRole);
        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(expectedUserRole))).thenReturn(Optional.of(groupDefaultApiRole));

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(ENVIRONMENT, apiId);
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

        GroupEntity apiPrimaryOwner = Mockito.mock(GroupEntity.class);
        when(apiPrimaryOwner.getId()).thenReturn(apiPrimaryOwnerGroupId);
        when(groupService.findById(eq(ENVIRONMENT), eq(apiPrimaryOwnerGroupId))).thenReturn(apiPrimaryOwner);

        Api api = mock(Api.class);
        when(api.getId()).thenReturn(apiId);
        when(api.getGroups()).thenReturn(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = Mockito.mock(GroupEntity.class);
        when(group.getId()).thenReturn(groupId);
        when(group.getRoles()).thenReturn(Map.of());
        when(groupService.findById(anyString(), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = Mockito.mock(MemberEntity.class);
        when(groupMember.getReferenceId()).thenReturn(groupId);
        when(groupMember.getRoles()).thenReturn(List.of(groupMemberApiRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.GROUP, List.of(groupId), null))
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = Mockito.mock(MembershipEntity.class);
        when(apiPrimaryOwnerMembership.getMemberType()).thenReturn(MembershipMemberType.GROUP);
        when(apiPrimaryOwnerMembership.getMemberId()).thenReturn(apiPrimaryOwnerGroupId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(ENVIRONMENT, apiId);
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
    public void shouldGetApiGroupsWithMembers_RemovingPrimaryOwnerPrivilege_ifGroupIsNotTheApiPrimaryOwner_andDefaultRoleHasBeenDeleted()
        throws TechnicalException {
        final String deletedRole = "USER";

        String apiId = UuidString.generateRandom();
        String groupId = UuidString.generateRandom();
        String apiPrimaryOwnerUserId = UuidString.generateRandom();

        UserEntity apiPrimaryOwner = Mockito.mock(UserEntity.class);
        when(apiPrimaryOwner.getId()).thenReturn(apiPrimaryOwnerUserId);
        when(userService.findById(apiPrimaryOwnerUserId)).thenReturn(apiPrimaryOwner);

        Api api = mock(Api.class);
        when(api.getId()).thenReturn(apiId);
        when(api.getGroups()).thenReturn(Set.of(groupId));
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        GroupEntity group = Mockito.mock(GroupEntity.class);
        when(group.getId()).thenReturn(groupId);
        when(group.getRoles()).thenReturn(Map.of(RoleScope.API, deletedRole));
        when(groupService.findById(eq(ENVIRONMENT), eq(groupId))).thenReturn(group);

        RoleEntity groupMemberApiRole = new RoleEntity();
        groupMemberApiRole.setName(SystemRole.PRIMARY_OWNER.name());
        groupMemberApiRole.setScope(RoleScope.API);

        MemberEntity groupMember = Mockito.mock(MemberEntity.class);
        when(groupMember.getReferenceId()).thenReturn(groupId);
        when(groupMember.getRoles()).thenReturn(List.of(groupMemberApiRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.GROUP, List.of(groupId), null))
            .thenReturn(Set.of(groupMember));

        MembershipEntity apiPrimaryOwnerMembership = Mockito.mock(MembershipEntity.class);
        when(apiPrimaryOwnerMembership.getMemberType()).thenReturn(MembershipMemberType.USER);
        when(apiPrimaryOwnerMembership.getMemberId()).thenReturn(apiPrimaryOwnerUserId);
        when(membershipService.getPrimaryOwner(any(), eq(MembershipReferenceType.API), eq(apiId))).thenReturn(apiPrimaryOwnerMembership);

        when(roleService.findByScopeAndName(eq(RoleScope.API), eq(deletedRole))).thenReturn(Optional.empty());

        Map<String, List<GroupMemberEntity>> groupsWithMembers = apiService.getGroupsWithMembers(ENVIRONMENT, apiId);
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
