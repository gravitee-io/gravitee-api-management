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

import static fixtures.MembershipModelFixtures.aGroupMembershipForApi;
import static fixtures.MembershipModelFixtures.aUserMembershipForApi;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MembershipDuplicateServiceTest {

    private static final String SOURCE_API_ID = "my-api";
    private static final String DUPLICATED_API_ID = "my-new-api";
    private static final String ENVIRONMENT_ID = "my-env";
    private static final String ORGANIZATION_ID = "my-org";
    private static final String PRIMARY_OWNER_ROLE_ID = "id-primary-owner";

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    private MembershipDuplicateServiceImpl membershipDuplicateService;

    @BeforeEach
    void setUp() throws Exception {
        membershipDuplicateService = new MembershipDuplicateServiceImpl(roleService, membershipService);

        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        lenient()
            .when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION_ID, RoleScope.API))
            .thenReturn(RoleEntity.builder().id(PRIMARY_OWNER_ROLE_ID).build());
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_do_nothing_when_no_primary_owner_role_found() {
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(null);

        membershipDuplicateService.duplicateMemberships(GraviteeContext.getExecutionContext(), SOURCE_API_ID, DUPLICATED_API_ID);

        verifyNoInteractions(membershipService);
    }

    @Test
    void should_duplicate_membership() {
        MembershipEntity member1 = aUserMembershipForApi()
            .toBuilder()
            .id("m1")
            .memberId("user1")
            .referenceId(SOURCE_API_ID)
            .roleId("role1")
            .build();
        MembershipEntity member2 = aGroupMembershipForApi()
            .toBuilder()
            .id("m2")
            .memberId("group1")
            .referenceId(SOURCE_API_ID)
            .roleId("role2")
            .build();
        when(membershipService.getMembershipsByReference(MembershipReferenceType.API, SOURCE_API_ID)).thenReturn(Set.of(member1, member2));

        when(membershipService.addRoleToMemberOnReference(any(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation ->
                MemberEntity
                    .builder()
                    .id(invocation.getArgument(4))
                    .type(invocation.getArgument(3))
                    .referenceType(invocation.getArgument(1))
                    .referenceId(invocation.getArgument(2))
                    .roles(List.of(RoleEntity.builder().id(invocation.getArgument(5)).build()))
                    .build()
            );

        var duplicated = membershipDuplicateService.duplicateMemberships(
            GraviteeContext.getExecutionContext(),
            SOURCE_API_ID,
            DUPLICATED_API_ID
        );

        Assertions
            .assertThat(duplicated)
            .hasSize(2)
            .extracting(MemberEntity::getReferenceId, MemberEntity::getId, MemberEntity::getRoles)
            .contains(
                tuple(DUPLICATED_API_ID, member1.getMemberId(), List.of(RoleEntity.builder().id("role1").build())),
                tuple(DUPLICATED_API_ID, member2.getMemberId(), List.of(RoleEntity.builder().id("role2").build()))
            );
    }

    @Test
    void should_ignore_source_primary_owner() {
        MembershipEntity member1 = aUserMembershipForApi()
            .toBuilder()
            .id("m1")
            .memberId("user1")
            .referenceId(SOURCE_API_ID)
            .roleId("role1")
            .build();
        MembershipEntity primaryOwner = aUserMembershipForApi()
            .toBuilder()
            .id("po")
            .memberId("po")
            .referenceId(SOURCE_API_ID)
            .roleId(PRIMARY_OWNER_ROLE_ID)
            .build();
        MembershipEntity member2 = aGroupMembershipForApi()
            .toBuilder()
            .id("m2")
            .memberId("group1")
            .referenceId(SOURCE_API_ID)
            .roleId("role2")
            .build();
        when(membershipService.getMembershipsByReference(MembershipReferenceType.API, SOURCE_API_ID))
            .thenReturn(Set.of(member1, primaryOwner, member2));

        when(membershipService.addRoleToMemberOnReference(any(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation ->
                MemberEntity
                    .builder()
                    .id(invocation.getArgument(4))
                    .type(invocation.getArgument(3))
                    .referenceType(invocation.getArgument(1))
                    .referenceId(invocation.getArgument(2))
                    .roles(List.of(RoleEntity.builder().id(invocation.getArgument(5)).build()))
                    .build()
            );

        var duplicated = membershipDuplicateService.duplicateMemberships(
            GraviteeContext.getExecutionContext(),
            SOURCE_API_ID,
            DUPLICATED_API_ID
        );

        Assertions.assertThat(duplicated).hasSize(2).extracting(MemberEntity::getId).doesNotContain(primaryOwner.getId());
    }
}
