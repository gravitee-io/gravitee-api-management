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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipService_GetPrimaryOwnerUserIdTest {

    private static final String ORG_ID = "org-id";
    private static final String API_ID = "api-id";
    private static final String GROUP_ID = "group-id";
    private static final String PO_ROLE_ID = "po-role-id";
    private static final String USER_ID = "user-id";

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private GroupService groupService;

    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipServiceImpl(
            null,
            null,
            null,
            null,
            null,
            null,
            membershipRepository,
            roleService,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            groupService,
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
    void should_return_user_id_when_primary_owner_is_user() throws Exception {
        var poRole = RoleEntity.builder().id(PO_ROLE_ID).build();
        when(roleService.findScopeByMembershipReferenceType(io.gravitee.rest.api.model.MembershipReferenceType.API)).thenReturn(
            RoleScope.API
        );
        when(roleService.findPrimaryOwnerRoleByOrganization(ORG_ID, RoleScope.API)).thenReturn(poRole);

        var membership = new Membership();
        membership.setMemberId(USER_ID);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(API_ID);
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(PO_ROLE_ID);
        when(membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, API_ID, PO_ROLE_ID)).thenReturn(Set.of(membership));

        String result = membershipService.getPrimaryOwnerUserId(ORG_ID, io.gravitee.rest.api.model.MembershipReferenceType.API, API_ID);

        assertThat(result).isEqualTo(USER_ID);
    }

    @Test
    void should_return_user_id_when_primary_owner_is_group_with_api_primary_owner() throws Exception {
        var poRole = RoleEntity.builder().id(PO_ROLE_ID).build();
        when(roleService.findScopeByMembershipReferenceType(io.gravitee.rest.api.model.MembershipReferenceType.API)).thenReturn(
            RoleScope.API
        );
        when(roleService.findPrimaryOwnerRoleByOrganization(ORG_ID, RoleScope.API)).thenReturn(poRole);

        var membership = new Membership();
        membership.setMemberId(GROUP_ID);
        membership.setMemberType(MembershipMemberType.GROUP);
        membership.setReferenceId(API_ID);
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(PO_ROLE_ID);
        when(membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, API_ID, PO_ROLE_ID)).thenReturn(Set.of(membership));

        when(groupService.findByIds(Set.of(GROUP_ID))).thenReturn(
            Set.of(GroupEntity.builder().id(GROUP_ID).apiPrimaryOwner(USER_ID).build())
        );

        String result = membershipService.getPrimaryOwnerUserId(ORG_ID, io.gravitee.rest.api.model.MembershipReferenceType.API, API_ID);

        assertThat(result).isEqualTo(USER_ID);
    }

    @Test
    void should_return_null_when_primary_owner_is_group_with_no_api_primary_owner() throws Exception {
        var poRole = RoleEntity.builder().id(PO_ROLE_ID).build();
        when(roleService.findScopeByMembershipReferenceType(io.gravitee.rest.api.model.MembershipReferenceType.API)).thenReturn(
            RoleScope.API
        );
        when(roleService.findPrimaryOwnerRoleByOrganization(ORG_ID, RoleScope.API)).thenReturn(poRole);

        var membership = new Membership();
        membership.setMemberId(GROUP_ID);
        membership.setMemberType(MembershipMemberType.GROUP);
        membership.setReferenceId(API_ID);
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(PO_ROLE_ID);
        when(membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, API_ID, PO_ROLE_ID)).thenReturn(Set.of(membership));

        when(groupService.findByIds(Set.of(GROUP_ID))).thenReturn(Set.of(GroupEntity.builder().id(GROUP_ID).apiPrimaryOwner(null).build()));

        String result = membershipService.getPrimaryOwnerUserId(ORG_ID, io.gravitee.rest.api.model.MembershipReferenceType.API, API_ID);

        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_no_primary_owner_membership_exists() throws Exception {
        var poRole = RoleEntity.builder().id(PO_ROLE_ID).build();
        when(roleService.findScopeByMembershipReferenceType(io.gravitee.rest.api.model.MembershipReferenceType.API)).thenReturn(
            RoleScope.API
        );
        when(roleService.findPrimaryOwnerRoleByOrganization(ORG_ID, RoleScope.API)).thenReturn(poRole);

        when(membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, API_ID, PO_ROLE_ID)).thenReturn(Set.of());

        String result = membershipService.getPrimaryOwnerUserId(ORG_ID, io.gravitee.rest.api.model.MembershipReferenceType.API, API_ID);

        assertThat(result).isNull();
    }
}
