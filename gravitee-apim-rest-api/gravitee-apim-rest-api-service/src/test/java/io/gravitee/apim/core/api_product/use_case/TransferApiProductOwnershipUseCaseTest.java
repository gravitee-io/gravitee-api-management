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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.AbstractUseCaseTest;
import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferApiProductOwnershipUseCaseTest extends AbstractUseCaseTest {

    private static final String API_PRODUCT_ID = "my-api-product";

    private TransferApiProductOwnershipUseCase transferApiProductOwnershipUseCase;
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @BeforeEach
    void setUp() {
        transferApiProductOwnershipUseCase = new TransferApiProductOwnershipUseCase(membershipDomainService);
        initData();
    }

    @Test
    void should_transfer_ownership_to_existing_api_product_member() {
        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("PRIMARY_OWNER"),
            () -> assertThat(currentRoleOf("user-2")).isEqualTo("USER")
        );

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("USER").newPrimaryOwnerId("user-2").build(),
                API_PRODUCT_ID
            )
        );

        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("USER"),
            () -> assertThat(currentRoleOf("user-2")).isEqualTo("PRIMARY_OWNER")
        );
    }

    @Test
    void should_transfer_ownership_to_new_user_not_yet_a_member() {
        assertThat(currentRoleOf("user-1")).isEqualTo("PRIMARY_OWNER");

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("OWNER").newPrimaryOwnerId("user-new").build(),
                API_PRODUCT_ID
            )
        );

        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("OWNER"),
            () -> assertThat(currentRoleOf("user-new")).isEqualTo("PRIMARY_OWNER")
        );
    }

    @Test
    void should_transfer_ownership_to_group() {
        assertThat(currentRoleOf("user-1")).isEqualTo("PRIMARY_OWNER");

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder()
                    .currentPrimaryOwnerNewRole("USER")
                    .newPrimaryOwnerId("group-1")
                    .memberType(MembershipMemberType.GROUP)
                    .build(),
                API_PRODUCT_ID
            )
        );

        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("USER"),
            () -> assertThat(currentRoleOf("group-1")).isEqualTo("PRIMARY_OWNER"),
            () -> assertThat(memberTypeOf("group-1")).isEqualTo(io.gravitee.rest.api.model.MembershipMemberType.GROUP)
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String currentRoleOf(String userId) {
        var member = membershipDomainService
            .storage()
            .stream()
            .filter(m -> m.getId().equals(userId))
            .findFirst();
        assertThat(member).as("No member found with id: " + userId).isPresent();
        var role = member.get().getRoles().stream().findFirst();
        assertThat(role).as("Member '" + userId + "' has no roles assigned").isPresent();
        return role.get().getName();
    }

    private io.gravitee.rest.api.model.MembershipMemberType memberTypeOf(String memberId) {
        var member = membershipDomainService
            .storage()
            .stream()
            .filter(m -> m.getId().equals(memberId))
            .findFirst();
        assertThat(member).as("No member found with id: " + memberId).isPresent();
        return member.get().getType();
    }

    private void initData() {
        membershipDomainService.initWith(
            List.of(
                MemberEntity.builder()
                    .id("user-1")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .roles(List.of(RoleEntity.builder().name("PRIMARY_OWNER").scope(RoleScope.API_PRODUCT).build()))
                    .build(),
                MemberEntity.builder()
                    .id("user-2")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .roles(List.of(RoleEntity.builder().name("USER").scope(RoleScope.API_PRODUCT).build()))
                    .build()
            )
        );
    }
}
