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
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.AbstractUseCaseTest;
import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.model.AddMember;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddClusterMemberUseCaseTest extends AbstractUseCaseTest {

    private AddClusterMemberUseCase addClusterMemberUseCase;
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @BeforeEach
    void setUp() {
        addClusterMemberUseCase = new AddClusterMemberUseCase(membershipDomainService);
    }

    @Test
    void should_add_cluster_member() {
        AuditInfo audit = AuditInfo.builder().organizationId("org-1").build();
        AddMember addMember = AddMember.builder().roleName("USER").userId("user-1").build();
        // When
        addClusterMemberUseCase.execute(new AddClusterMemberUseCase.Input(audit, addMember, "cluster-1"));
        // Then
        var membership = membershipDomainService.storage().stream().findFirst().orElseThrow();
        assertAll(() ->
            assertThat(membership)
                .usingRecursiveComparison()
                .isEqualTo(
                    MemberEntity.builder()
                        .referenceType(MembershipReferenceType.CLUSTER)
                        .referenceId("cluster-1")
                        .roles(List.of(RoleEntity.builder().name("USER").build()))
                        .type(MembershipMemberType.USER)
                        .id("user-1")
                        .build()
                )
        );
    }

    @Test
    void should_throw_validation_error_when_role_is_primary_owner() {
        AuditInfo audit = AuditInfo.builder().build();
        AddMember addMember = AddMember.builder().roleName("PRIMARY_OWNER").build();

        assertThrows(SinglePrimaryOwnerException.class, () ->
            addClusterMemberUseCase.execute(new AddClusterMemberUseCase.Input(audit, addMember, "cluster-1"))
        );
    }
}
