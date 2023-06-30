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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PrimaryOwnerServiceImplTest {

    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private UserService userService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private RoleService roleService;

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("TEST", "TEST");

    @Before
    public void setUp() {
        this.primaryOwnerService = new PrimaryOwnerServiceImpl(userService, membershipService, groupService, parameterService, roleService);
    }

    @Test
    public void getPrimaryOwner_should_throw_PrimaryOwnerNotFoundException_if_no_membership_found() {
        when(membershipService.getPrimaryOwner(EXECUTION_CONTEXT.getOrganizationId(), MembershipReferenceType.API, "my-api"))
            .thenReturn(null);
        PrimaryOwnerNotFoundException exception = assertThrows(
            PrimaryOwnerNotFoundException.class,
            () -> primaryOwnerService.getPrimaryOwner(EXECUTION_CONTEXT, "my-api")
        );
        assertEquals("Primary owner not found for API [my-api]", exception.getMessage());
        assertEquals("primaryOwner.notFound", exception.getTechnicalCode());
        assertEquals(500, exception.getHttpStatusCode());
        assertEquals(Map.of("api", "my-api"), exception.getParameters());
    }

    @Test
    public void shouldReturnPrimaryOwners() {
        List<String> apiIds = List.of("api1", "api2", "api3");

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        GroupEntity group = new GroupEntity();
        group.setId("group");

        MemberEntity poMember = new MemberEntity();
        poMember.setId(admin.getId());
        poMember.setReferenceId("api1");
        poMember.setRoles(Collections.singletonList(poRole));
        poMember.setType(MembershipMemberType.USER);

        MemberEntity poMember2 = new MemberEntity();
        poMember2.setId(user.getId());
        poMember2.setReferenceId("api2");
        poMember2.setType(MembershipMemberType.USER);

        MemberEntity poMember3 = new MemberEntity();
        poMember3.setId(group.getId());
        poMember3.setReferenceId("api3");
        poMember3.setType(MembershipMemberType.GROUP);
        when(membershipService.getMembersByReferencesAndRole(EXECUTION_CONTEXT, MembershipReferenceType.API, apiIds, "API_PRIMARY_OWNER"))
            .thenReturn(new LinkedHashSet<>(Arrays.asList(poMember, poMember2, poMember3)));

        when(userService.findByIds(eq(EXECUTION_CONTEXT), argThat(argument -> argument.containsAll(Set.of(admin.getId(), user.getId())))))
            .thenReturn(new LinkedHashSet<>(List.of(admin, user)));
        when(groupService.findByIds(new LinkedHashSet<>(List.of(group.getId())))).thenReturn(new LinkedHashSet<>(List.of(group)));

        Map<String, PrimaryOwnerEntity> primaryOwners = primaryOwnerService.getPrimaryOwners(EXECUTION_CONTEXT, apiIds);
        assertNotNull(primaryOwners);
        assertEquals(3, primaryOwners.size());
    }

    @Test
    public void getPrimaryOwner_should_return_group_primary_owner_with_email_of_po_user() {
        final MembershipEntity groupMembership = primaryOwnerGroupMembership();
        groupMembership.setMemberId("member-id");
        when(membershipService.getPrimaryOwner(EXECUTION_CONTEXT.getOrganizationId(), MembershipReferenceType.API, "api"))
            .thenReturn(groupMembership);

        MemberEntity member = new MemberEntity();
        member.setId("some-member");
        member.setEmail("some-member@mail.com");
        member.setRoles(List.of());

        MemberEntity memberOther = new MemberEntity();
        memberOther.setId("other-member");
        memberOther.setEmail("other-member@mail.com");
        memberOther.setRoles(List.of());

        MemberEntity poMember = new MemberEntity();
        poMember.setId("po-member");
        poMember.setEmail("po-member@mail.com");
        final RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.API);
        role.setName(SystemRole.PRIMARY_OWNER.name());
        poMember.setRoles(List.of(role));

        when(membershipService.getMembersByReference(EXECUTION_CONTEXT, MembershipReferenceType.GROUP, groupMembership.getMemberId()))
            .thenReturn(Set.of(member, memberOther, poMember));

        final GroupEntity poGroup = primaryOwnerGroup();
        when(groupService.findById(EXECUTION_CONTEXT, groupMembership.getMemberId())).thenReturn(poGroup);

        final PrimaryOwnerEntity result = primaryOwnerService.getPrimaryOwner(EXECUTION_CONTEXT, "api");
        assertThat(result.getId()).isEqualTo(poGroup.getId());
        assertThat(result.getType()).isEqualTo("GROUP");
        assertThat(result.getEmail()).isEqualTo("po-member@mail.com");
    }

    @Test
    public void shouldReturnPrimaryOwnerEmailFromUser() {
        final String apiId = "some-api";

        when(
            membershipService.getPrimaryOwner(
                EXECUTION_CONTEXT.getOrganizationId(),
                io.gravitee.rest.api.model.MembershipReferenceType.API,
                apiId
            )
        )
            .thenReturn(primaryOwnerUserMembership());

        when(userService.findById(EXECUTION_CONTEXT, "some-user")).thenReturn(primaryOwnerUser());

        String email = primaryOwnerService.getPrimaryOwnerEmail(EXECUTION_CONTEXT, apiId);

        assertThat(email).isEqualTo("some-user@gravitee.test");
    }

    @Test
    public void shouldReturnPrimaryOwnerEmailFromGroupMember() {
        final String apiId = "some-api";

        when(
            membershipService.getPrimaryOwner(
                EXECUTION_CONTEXT.getOrganizationId(),
                io.gravitee.rest.api.model.MembershipReferenceType.API,
                apiId
            )
        )
            .thenReturn(primaryOwnerGroupMembership());

        when(roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name(), EXECUTION_CONTEXT.getOrganizationId()))
            .thenReturn(apiPrimaryOwnerRole());

        when(
            membershipService.getMembersByReferenceAndRole(
                EXECUTION_CONTEXT,
                MembershipReferenceType.GROUP,
                "some-group",
                "role-api-primary-owner"
            )
        )
            .thenReturn(primaryOwnerGroupMember());

        when(groupService.findById(EXECUTION_CONTEXT, "some-group")).thenReturn(primaryOwnerGroup());

        when(userService.findById(EXECUTION_CONTEXT, "some-member")).thenReturn(primaryOwnerUser());

        String email = primaryOwnerService.getPrimaryOwnerEmail(EXECUTION_CONTEXT, apiId);

        assertThat(email).isEqualTo("some-user@gravitee.test");
    }

    private static MembershipEntity primaryOwnerUserMembership() {
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberType(MembershipMemberType.USER);
        membership.setMemberId("some-user");
        return membership;
    }

    private static UserEntity primaryOwnerUser() {
        UserEntity user = new UserEntity();
        user.setId("some-user");
        user.setEmail("some-user@gravitee.test");
        return user;
    }

    private static MembershipEntity primaryOwnerGroupMembership() {
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberType(MembershipMemberType.GROUP);
        membership.setMemberId("some-group");
        return membership;
    }

    private static GroupEntity primaryOwnerGroup() {
        GroupEntity group = new GroupEntity();
        group.setId("some-group");
        return group;
    }

    private static Set<MemberEntity> primaryOwnerGroupMember() {
        MemberEntity member = new MemberEntity();
        member.setId("some-member");
        return Set.of(member);
    }

    private static Optional<RoleEntity> apiPrimaryOwnerRole() {
        RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.API);
        role.setName(SystemRole.PRIMARY_OWNER.name());
        role.setId("role-api-primary-owner");
        return Optional.of(role);
    }
}
