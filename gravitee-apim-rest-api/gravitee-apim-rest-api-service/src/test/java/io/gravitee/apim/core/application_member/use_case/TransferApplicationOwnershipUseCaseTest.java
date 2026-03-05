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
package io.gravitee.apim.core.application_member.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.MembershipDomainServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferApplicationOwnershipUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ORGANIZATION_ID = "organization-id";

    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    private TransferApplicationOwnershipUseCase transferApplicationOwnershipUseCase;

    @BeforeEach
    void setUp() {
        membershipDomainService.reset();
        roleQueryService.reset();
        transferApplicationOwnershipUseCase = new TransferApplicationOwnershipUseCase(membershipDomainService, roleQueryService);
    }

    @Test
    void should_transfer_ownership_to_existing_member() {
        roleQueryService.initWith(List.of(aRole("USER", ORGANIZATION_ID)));
        membershipDomainService.initWith(List.of(aMembership("member-po", "PRIMARY_OWNER"), aMembership("member-1", "USER")));

        transferApplicationOwnershipUseCase.execute(
            new TransferApplicationOwnershipUseCase.Input(APPLICATION_ID, "member-1", "ref-1", "USER", ORGANIZATION_ID)
        );

        assertThat(
            membershipDomainService
                .storage()
                .stream()
                .filter(m -> m.getId().equals("member-po"))
                .findFirst()
                .orElseThrow()
                .getRoles()
                .get(0)
                .getName()
        ).isEqualTo("USER");
        assertThat(
            membershipDomainService
                .storage()
                .stream()
                .filter(m -> m.getId().equals("member-1"))
                .findFirst()
                .orElseThrow()
                .getRoles()
                .get(0)
                .getName()
        ).isEqualTo("PRIMARY_OWNER");
    }

    @Test
    void should_throw_when_role_not_found() {
        assertThrows(RoleNotFoundException.class, () ->
            transferApplicationOwnershipUseCase.execute(
                new TransferApplicationOwnershipUseCase.Input(APPLICATION_ID, "member-1", "ref-1", "UNKNOWN", ORGANIZATION_ID)
            )
        );
    }

    @Test
    void should_throw_when_previous_owner_new_role_is_primary_owner() {
        assertThrows(SinglePrimaryOwnerException.class, () ->
            transferApplicationOwnershipUseCase.execute(
                new TransferApplicationOwnershipUseCase.Input(APPLICATION_ID, "member-1", "ref-1", "PRIMARY_OWNER", ORGANIZATION_ID)
            )
        );
    }

    @Test
    void should_throw_when_target_user_not_found() {
        roleQueryService.initWith(List.of(aRole("USER", ORGANIZATION_ID)));
        var failingMembershipDomainService = new MembershipDomainService() {
            @Override
            public Map<String, char[]> getUserMemberPermissions(
                ExecutionContext executionContext,
                MembershipReferenceType referenceType,
                String referenceId,
                String userId
            ) {
                return Map.of();
            }

            @Override
            public void transferOwnership(TransferOwnership transferOwnership, RoleScope roleScope, String itemId) {
                throw new UserNotFoundException(transferOwnership.getNewPrimaryOwnerId());
            }

            @Override
            public MemberEntity createNewMembership(
                ExecutionContext executionContext,
                io.gravitee.apim.core.member.model.MembershipReferenceType referenceType,
                String referenceId,
                String userId,
                String externalReference,
                String roleName
            ) {
                return null;
            }
        };
        var useCase = new TransferApplicationOwnershipUseCase(failingMembershipDomainService, roleQueryService);

        assertThrows(UserNotFoundException.class, () ->
            useCase.execute(new TransferApplicationOwnershipUseCase.Input(APPLICATION_ID, "unknown-user", "ref-1", "USER", ORGANIZATION_ID))
        );
    }

    private static MemberEntity aMembership(String memberId, String roleName) {
        return MemberEntity.builder()
            .id(memberId)
            .referenceType(MembershipReferenceType.APPLICATION)
            .referenceId(APPLICATION_ID)
            .roles(List.of(RoleEntity.builder().name(roleName).scope(RoleScope.APPLICATION).build()))
            .build();
    }

    private static Role aRole(String roleName, String organizationId) {
        return Role.builder()
            .id("role-" + roleName.toLowerCase())
            .name(roleName)
            .scope(Role.Scope.APPLICATION)
            .referenceType(Role.ReferenceType.ORGANIZATION)
            .referenceId(organizationId)
            .build();
    }
}
