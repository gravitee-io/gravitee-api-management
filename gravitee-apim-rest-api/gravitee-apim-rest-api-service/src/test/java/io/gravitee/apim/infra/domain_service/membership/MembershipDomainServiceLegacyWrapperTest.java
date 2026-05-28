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
package io.gravitee.apim.infra.domain_service.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TransferOwnershipNotAllowedException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class MembershipDomainServiceLegacyWrapperTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");
    private static final String GROUP_ID = "group-id";
    private static final String USER_ID = "user-id";

    @Mock
    MembershipService membershipService;

    @Mock
    RoleService roleService;

    MembershipDomainServiceLegacyWrapper cut;

    @BeforeEach
    void setUp() {
        cut = new MembershipDomainServiceLegacyWrapper(membershipService, roleService);
    }

    @Nested
    class GetUserMemberPermissions {

        @Test
        void should_delegate_to_legacy_service() {
            when(
                membershipService.getUserMemberPermissions(
                    EXECUTION_CONTEXT,
                    io.gravitee.rest.api.model.MembershipReferenceType.API,
                    "api-id",
                    USER_ID
                )
            ).thenReturn(null);

            cut.getUserMemberPermissions(EXECUTION_CONTEXT, io.gravitee.rest.api.model.MembershipReferenceType.API, "api-id", USER_ID);

            verify(membershipService).getUserMemberPermissions(
                EXECUTION_CONTEXT,
                io.gravitee.rest.api.model.MembershipReferenceType.API,
                "api-id",
                USER_ID
            );
        }
    }

    @Nested
    class CreateNewMembership {

        @Test
        void should_delegate_to_legacy_service_with_mapped_reference_type() {
            var memberEntity = new MemberEntity();
            when(
                membershipService.createNewMembership(
                    EXECUTION_CONTEXT,
                    io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION,
                    "app-id",
                    USER_ID,
                    null,
                    "USER"
                )
            ).thenReturn(memberEntity);

            var result = cut.createNewMembership(EXECUTION_CONTEXT, MembershipReferenceType.APPLICATION, "app-id", USER_ID, null, "USER");

            assertThat(result).isSameAs(memberEntity);
        }
    }

    @Nested
    class AddGroupMemberships {

        @Test
        void should_add_api_application_and_group_roles() {
            var defaultGroupRole = RoleEntity.builder().name("MEMBER").build();
            when(roleService.findDefaultRoleByScopes("org-id", RoleScope.GROUP)).thenReturn(List.of(defaultGroupRole));

            cut.addGroupMemberships(EXECUTION_CONTEXT, GROUP_ID, USER_ID, "API_USER", "APP_USER");

            verify(membershipService).addRoleToMemberOnReference(
                EXECUTION_CONTEXT,
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USER_ID, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "API_USER")
            );
            verify(membershipService).addRoleToMemberOnReference(
                EXECUTION_CONTEXT,
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USER_ID, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "APP_USER")
            );
            verify(membershipService).addRoleToMemberOnReference(
                EXECUTION_CONTEXT,
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USER_ID, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.GROUP, "MEMBER")
            );
        }

        @Test
        void should_skip_group_role_when_no_default_role_exists() {
            when(roleService.findDefaultRoleByScopes("org-id", RoleScope.GROUP)).thenReturn(List.of());

            cut.addGroupMemberships(EXECUTION_CONTEXT, GROUP_ID, USER_ID, "API_USER", "APP_USER");

            verify(membershipService).addRoleToMemberOnReference(
                EXECUTION_CONTEXT,
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USER_ID, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "API_USER")
            );
            verify(membershipService).addRoleToMemberOnReference(
                EXECUTION_CONTEXT,
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USER_ID, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "APP_USER")
            );
            verifyNoMoreInteractions(membershipService);
        }
    }

    @Nested
    class TransferOwnership_Tests {

        @BeforeEach
        void setUpGraviteeContext() {
            GraviteeContext.setCurrentOrganization(EXECUTION_CONTEXT.getOrganizationId());
            GraviteeContext.setCurrentEnvironment(EXECUTION_CONTEXT.getEnvironmentId());
        }

        @AfterEach
        void cleanGraviteeContext() {
            GraviteeContext.cleanContext();
        }

        @Test
        void should_transfer_ownership() {
            var transfer = TransferOwnership.builder().newPrimaryOwnerId(USER_ID).currentPrimaryOwnerNewRole("USER").build();
            var newRole = RoleEntity.builder().name("USER").build();
            when(roleService.findByScopeAndName(RoleScope.API, "USER", "org-id")).thenReturn(Optional.of(newRole));

            cut.transferOwnership(transfer, RoleScope.API, "api-id");

            verify(membershipService).transferOwnership(
                EXECUTION_CONTEXT,
                io.gravitee.rest.api.model.MembershipReferenceType.API,
                RoleScope.API,
                "api-id",
                new MembershipService.MembershipMember(USER_ID, null, MembershipMemberType.USER),
                List.of(newRole)
            );
        }

        @Test
        void should_throw_when_new_role_is_primary_owner() {
            var transfer = TransferOwnership.builder().newPrimaryOwnerId(USER_ID).currentPrimaryOwnerNewRole("PRIMARY_OWNER").build();

            assertThatThrownBy(() -> cut.transferOwnership(transfer, RoleScope.API, "api-id")).isInstanceOf(
                TransferOwnershipNotAllowedException.class
            );
        }

        @Test
        void should_throw_when_neither_owner_id_nor_user_ref_is_provided() {
            var transfer = TransferOwnership.builder().currentPrimaryOwnerNewRole("USER").build();

            assertThatThrownBy(() -> cut.transferOwnership(transfer, RoleScope.API, "api-id")).isInstanceOf(InvalidDataException.class);
        }
    }
}
