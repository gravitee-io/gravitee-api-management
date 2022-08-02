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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import java.util.Optional;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GroupService_FindByIdTest extends TestCase {

    private static final String GROUP_ID = "my-group-id";
    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";

    @InjectMocks
    private final GroupService groupService = new GroupServiceImpl();

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PermissionService permissionService;

    @Test(expected = GroupNotFoundException.class)
    public void shouldThrowGroupNotFoundException() throws TechnicalException {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());
        groupService.findById(null, GROUP_ID);
    }

    @Test
    public void shouldSetGroupAsManageable() throws TechnicalException {
        final Group group = new Group();
        group.setId(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        when(
            permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_GROUP,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE,
                RolePermissionAction.UPDATE,
                RolePermissionAction.DELETE
            )
        )
            .thenReturn(true);

        var result = groupService.findById(executionContext, GROUP_ID);

        verify(permissionService)
            .hasPermission(
                eq(executionContext),
                eq(RolePermission.ENVIRONMENT_GROUP),
                eq(ENVIRONMENT_ID),
                eq(RolePermissionAction.CREATE),
                eq(RolePermissionAction.UPDATE),
                eq(RolePermissionAction.DELETE)
            );
        assertThat(result).isNotNull().extracting(GroupEntity::getId, GroupEntity::isManageable).containsExactly(GROUP_ID, true);
    }

    @Test
    public void shouldCheckAdminSystemRole() throws TechnicalException {
        final Group group = new Group();
        group.setId(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, null);

        when(roleService.findByScopeAndName(RoleScope.GROUP, SystemRole.ADMIN.name(), ORGANIZATION_ID)).thenReturn(Optional.empty());

        var result = groupService.findById(executionContext, GROUP_ID);

        verify(permissionService, times(0))
            .hasPermission(
                eq(executionContext),
                eq(RolePermission.ENVIRONMENT_GROUP),
                eq(ENVIRONMENT_ID),
                eq(RolePermissionAction.CREATE),
                eq(RolePermissionAction.UPDATE),
                eq(RolePermissionAction.DELETE)
            );
        verify(roleService).findByScopeAndName(eq(RoleScope.GROUP), eq(SystemRole.ADMIN.name()), eq(ORGANIZATION_ID));
        verify(membershipService, times(0)).getMembershipsByMemberAndReferenceAndRole(any(), any(), any(), any());
        assertThat(result).isNotNull().extracting(GroupEntity::getId, GroupEntity::isManageable).containsExactly(GROUP_ID, false);
    }
}
