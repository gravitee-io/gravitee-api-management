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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.impl.AbstractService.convert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.search.GroupCriteria;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GroupServiceImplTest {

    @InjectMocks
    private GroupServiceImpl service;

    @Mock
    private PermissionService permissionService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Test
    public void search_hasNotPermissionCreateUpdateDelete() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        int pageNumber = 1;
        int pageSize = 2;
        Pageable pageable = new PageableImpl(pageNumber, pageSize);
        String searchTerm = "test";

        when(
            permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_GROUP,
                executionContext.getEnvironmentId(),
                CREATE,
                UPDATE,
                DELETE
            )
        )
            .thenReturn(false);

        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Set.of());

        RoleEntity roleEntity = RoleEntity.builder().id("re1").build();
        when(roleService.findByScopeAndName(RoleScope.GROUP, SystemRole.ADMIN.name(), executionContext.getOrganizationId()))
            .thenReturn(Optional.of(roleEntity));

        Set<MembershipEntity> memberships = Set.of(
            MembershipEntity.builder().id("m1").referenceId("gr1").build(),
            MembershipEntity.builder().id("m2").referenceId("gr2").build()
        );
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                eq(MembershipMemberType.USER),
                any(),
                eq(MembershipReferenceType.GROUP),
                eq(roleEntity.getId())
            )
        )
            .thenReturn(memberships);

        GroupCriteria groupCriteria = GroupCriteria
            .builder()
            .environmentId(executionContext.getEnvironmentId())
            .query(searchTerm)
            .idIn(Set.of("gr1", "gr2"))
            .build();
        List<Group> groups = List.of(Group.builder().id("gr1").build(), Group.builder().id("gr2").build());
        Page<Group> groupsPage = new Page<>(groups, pageNumber, pageSize, 13);
        when(groupRepository.search(groupCriteria, convert(pageable), null)).thenReturn(groupsPage);

        Page<GroupEntity> searchResult = service.search(executionContext, pageable, null, "test");

        assertAll(
            () -> assertThat(searchResult.getContent()).isEqualTo(groups.stream().map(service::map).toList()),
            () -> assertThat(searchResult.getTotalElements()).isEqualTo(13),
            () -> assertThat(searchResult.getPageNumber()).isEqualTo(pageNumber),
            () -> assertThat(searchResult.getPageElements()).isEqualTo(pageSize)
        );
    }

    @Test
    public void search_hasPermissionCreateUpdateDelete() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        int pageNumber = 1;
        int pageSize = 2;
        Pageable pageable = new PageableImpl(pageNumber, pageSize);
        String searchTerm = "test";

        when(
            permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_GROUP,
                executionContext.getEnvironmentId(),
                CREATE,
                UPDATE,
                DELETE
            )
        )
            .thenReturn(true);

        GroupCriteria groupCriteria = GroupCriteria.builder().environmentId(executionContext.getEnvironmentId()).query(searchTerm).build();
        List<Group> groups = List.of(Group.builder().id("gr1").build(), Group.builder().id("gr2").build());
        Page<Group> groupsPage = new Page<>(groups, pageNumber, pageSize, 13);
        when(groupRepository.search(groupCriteria, convert(pageable), null)).thenReturn(groupsPage);

        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Set.of());

        Page<GroupEntity> searchResult = service.search(executionContext, pageable, null, "test");

        assertAll(
            () -> assertThat(searchResult.getContent()).isEqualTo(groups.stream().map(service::map).toList()),
            () -> assertThat(searchResult.getTotalElements()).isEqualTo(13),
            () -> assertThat(searchResult.getPageNumber()).isEqualTo(pageNumber),
            () -> assertThat(searchResult.getPageElements()).isEqualTo(pageSize)
        );
    }
}
