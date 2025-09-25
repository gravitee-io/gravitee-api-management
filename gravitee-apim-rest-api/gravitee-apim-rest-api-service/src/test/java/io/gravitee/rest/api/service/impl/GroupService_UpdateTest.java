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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.Maps;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateGroupEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
public class GroupService_UpdateTest {

    private static final String GROUP_ID = "my-group-id";

    @InjectMocks
    private final GroupService groupService = new GroupServiceImpl();

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RoleService roleService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Test
    public void shouldUpdateGroup() throws Exception {
        UpdateGroupEntity updatedGroupEntity = new UpdateGroupEntity();
        updatedGroupEntity.setDisableMembershipNotifications(true);
        updatedGroupEntity.setEmailInvitation(true);
        updatedGroupEntity.setEventRules(null);
        updatedGroupEntity.setLockApiRole(true);
        updatedGroupEntity.setLockApplicationRole(true);
        updatedGroupEntity.setMaxInvitation(100);
        updatedGroupEntity.setName("my-group-name");
        updatedGroupEntity.setRoles(Maps.<RoleScope, String>builder().put(RoleScope.API, "OWNER").build());
        updatedGroupEntity.setSystemInvitation(false);

        final Group group = new Group();
        group.setApiPrimaryOwner("api-primary-owner");
        group.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_GROUP),
                eq("DEFAULT"),
                eq(CREATE),
                eq(UPDATE),
                eq(DELETE)
            )
        ).thenReturn(true);
        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updatedGroupEntity);

        verify(groupRepository).update(
            argThat(
                updatedGroup ->
                    updatedGroup.isDisableMembershipNotifications() &&
                    updatedGroup.isEmailInvitation() &&
                    updatedGroup.getEventRules() == null &&
                    updatedGroup.isLockApiRole() &&
                    updatedGroup.isLockApplicationRole() &&
                    updatedGroup.getMaxInvitation() == 100 &&
                    updatedGroup.getName().equals("my-group-name") &&
                    !updatedGroup.isSystemInvitation() &&
                    updatedGroup.getApiPrimaryOwner().equals("api-primary-owner")
            )
        );

        verify(membershipService).addRoleToMemberOnReference(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                membershipReference -> membershipReference.getType() == MembershipReferenceType.API && membershipReference.getId() == null
            ),
            argThat(
                membershipMember ->
                    membershipMember.getMemberId().equals(GROUP_ID) &&
                    membershipMember.getReference() == null &&
                    membershipMember.getMemberType() == MembershipMemberType.GROUP
            ),
            argThat(membershipRole -> membershipRole.getScope() == RoleScope.API && membershipRole.getName().equals("OWNER"))
        );
    }

    @Test
    public void shouldNotUpdateDefaultRoleBecausePrimaryOwner() throws Exception {
        UpdateGroupEntity updatedGroupEntity = new UpdateGroupEntity();
        updatedGroupEntity.setRoles(
            Maps.<RoleScope, String>builder().put(RoleScope.API, "PRIMARY_OWNER").put(RoleScope.APPLICATION, "PRIMARY_OWNER").build()
        );

        final Group group = new Group();
        group.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_GROUP),
                eq("DEFAULT"),
                eq(CREATE),
                eq(UPDATE),
                eq(DELETE)
            )
        ).thenReturn(true);
        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updatedGroupEntity);

        verify(membershipService, never()).deleteReferenceMember(eq(GraviteeContext.getExecutionContext()), any(), any(), any(), any());
        verify(membershipService, never()).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
    }

    @Test(expected = GroupNotFoundException.class)
    public void shouldNotUpdateGroupBecauseDoesNotBelongToEnvironment() throws Exception {
        UpdateGroupEntity updatedGroupEntity = new UpdateGroupEntity();
        updatedGroupEntity.setRoles(
            Maps.<RoleScope, String>builder().put(RoleScope.API, "PRIMARY_OWNER").put(RoleScope.APPLICATION, "PRIMARY_OWNER").build()
        );

        final Group group = new Group();
        group.setEnvironmentId("Another_environment");

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updatedGroupEntity);

        verify(membershipService, never()).deleteReferenceMember(eq(GraviteeContext.getExecutionContext()), any(), any(), any(), any());
        verify(membershipService, never()).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
    }

    @Test
    public void shouldReindexApisIfGroupIsPrimaryOwner() throws Exception {
        UpdateGroupEntity updateGroup = new UpdateGroupEntity();
        updateGroup.setName("Updated Name");
        updateGroup.setRoles(Map.of(RoleScope.API, "PRIMARY_OWNER"));

        Group existingGroup = new Group();
        existingGroup.setId(GROUP_ID);
        existingGroup.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        Group updatedGroup = new Group();
        updatedGroup.setId(GROUP_ID);
        updatedGroup.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        updatedGroup.setApiPrimaryOwner("dummy-value");

        RoleEntity mockedRole = new RoleEntity();
        mockedRole.setId("po-role-id");

        GenericApiEntity apiEntity = mock(GenericApiEntity.class);

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(existingGroup));
        when(groupRepository.update(any())).thenReturn(updatedGroup);
        when(roleService.findByScopeAndName(RoleScope.API, "PRIMARY_OWNER", GraviteeContext.getCurrentOrganization())).thenReturn(
            Optional.of(mockedRole)
        );
        when(
            membershipService.getReferenceIdsByMemberAndReferenceAndRoleIn(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                Set.of("po-role-id")
            )
        ).thenReturn(Set.of("api-id-1"));
        when(apiSearchService.findGenericById(any(), eq("api-id-1"))).thenReturn(apiEntity);
        when(apiMetadataService.fetchMetadataForApi(any(), eq(apiEntity))).thenReturn(apiEntity);

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updateGroup);

        verify(searchEngineService).index(any(), eq(apiEntity), eq(false));
    }

    @Test
    public void shouldNotReindexApisIfGroupIsNotPrimaryOwner() throws Exception {
        UpdateGroupEntity updateGroup = new UpdateGroupEntity();
        updateGroup.setName("Updated Name");
        updateGroup.setRoles(Map.of(RoleScope.API, "OWNER")); // not PRIMARY_OWNER

        Group existingGroup = new Group();
        existingGroup.setId(GROUP_ID);
        existingGroup.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        Group updatedGroup = new Group();
        updatedGroup.setId(GROUP_ID);
        updatedGroup.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        updatedGroup.setApiPrimaryOwner(null); // not PO

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(existingGroup));
        when(groupRepository.update(any())).thenReturn(updatedGroup);

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updateGroup);

        verifyNoInteractions(apiSearchService, apiMetadataService, searchEngineService);
    }
}
