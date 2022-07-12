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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.Maps;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UpdateGroupEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Optional;
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
    private AuditService auditService;

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
        )
            .thenReturn(true);
        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updatedGroupEntity);

        verify(groupRepository)
            .update(
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

        verify(membershipService)
            .addRoleToMemberOnReference(
                eq(GraviteeContext.getExecutionContext()),
                argThat(
                    membershipReference ->
                        membershipReference.getType() == MembershipReferenceType.API && membershipReference.getId() == null
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

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(new Group()));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_GROUP),
                eq("DEFAULT"),
                eq(CREATE),
                eq(UPDATE),
                eq(DELETE)
            )
        )
            .thenReturn(true);
        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        groupService.update(GraviteeContext.getExecutionContext(), GROUP_ID, updatedGroupEntity);

        verify(membershipService, never()).deleteReferenceMember(eq(GraviteeContext.getExecutionContext()), any(), any(), any(), any());
        verify(membershipService, never()).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
    }
}
