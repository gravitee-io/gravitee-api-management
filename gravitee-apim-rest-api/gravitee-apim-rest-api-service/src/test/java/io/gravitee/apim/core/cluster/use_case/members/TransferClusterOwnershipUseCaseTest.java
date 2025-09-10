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
package io.gravitee.apim.core.cluster.use_case.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.AbstractUseCaseTest;
import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferClusterOwnershipUseCaseTest extends AbstractUseCaseTest {

    private final String CLUSTER_ID = "my-cluster";

    private TransferClusterOwnershipUseCase transferClusterOwnershipUseCase;
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @BeforeEach
    void setUp() {
        transferClusterOwnershipUseCase = new TransferClusterOwnershipUseCase(membershipDomainService);
        initData();
    }

    @Test
    public void should_transfer_ownership_to_cluster_member() {
        assertAll(
            () ->
                assertThat(
                    membershipDomainService
                        .storage()
                        .stream()
                        .filter(m -> m.getId().equals("user-1"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles()
                        .get(0)
                        .getName()
                )
                    .isEqualTo("PRIMARY_OWNER"),
            () ->
                assertThat(
                    membershipDomainService
                        .storage()
                        .stream()
                        .filter(m -> m.getId().equals("user-2"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles()
                        .get(0)
                        .getName()
                )
                    .isEqualTo("USER")
        );

        transferClusterOwnershipUseCase.execute(
            new TransferClusterOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("USER").newPrimaryOwnerId("user-2").build(),
                CLUSTER_ID
            )
        );

        assertAll(
            () ->
                assertThat(
                    membershipDomainService
                        .storage()
                        .stream()
                        .filter(m -> m.getId().equals("user-1"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles()
                        .get(0)
                        .getName()
                )
                    .isEqualTo("USER"),
            () ->
                assertThat(
                    membershipDomainService
                        .storage()
                        .stream()
                        .filter(m -> m.getId().equals("user-2"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles()
                        .get(0)
                        .getName()
                )
                    .isEqualTo("PRIMARY_OWNER")
        );
    }

    @Test
    public void should_transfer_ownership_to_other_user() {
        assertAll(() ->
            assertThat(
                membershipDomainService
                    .storage()
                    .stream()
                    .filter(m -> m.getId().equals("user-1"))
                    .findFirst()
                    .orElseThrow()
                    .getRoles()
                    .get(0)
                    .getName()
            )
                .isEqualTo("PRIMARY_OWNER")
        );

        transferClusterOwnershipUseCase.execute(
            new TransferClusterOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("USER").newPrimaryOwnerId("user-3").build(),
                CLUSTER_ID
            )
        );

        assertAll(
            () ->
                assertThat(
                    membershipDomainService
                        .storage()
                        .stream()
                        .filter(m -> m.getId().equals("user-1"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles()
                        .get(0)
                        .getName()
                )
                    .isEqualTo("USER"),
            () ->
                assertThat(
                    membershipDomainService
                        .storage()
                        .stream()
                        .filter(m -> m.getId().equals("user-3"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles()
                        .get(0)
                        .getName()
                )
                    .isEqualTo("PRIMARY_OWNER")
        );
    }

    private void initData() {
        List<MemberEntity> memberEntities = List.of(
            MemberEntity
                .builder()
                .id("user-1")
                .referenceId(CLUSTER_ID)
                .referenceType(MembershipReferenceType.CLUSTER)
                .roles(List.of(RoleEntity.builder().name("PRIMARY_OWNER").scope(RoleScope.CLUSTER).build()))
                .build(),
            MemberEntity
                .builder()
                .id("user-2")
                .referenceId(CLUSTER_ID)
                .referenceType(MembershipReferenceType.CLUSTER)
                .roles(List.of(RoleEntity.builder().name("USER").scope(RoleScope.CLUSTER).build()))
                .build()
        );
        membershipDomainService.initWith(memberEntities);
    }
}
