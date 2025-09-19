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

import inmemory.MemberDomainServiceInMemory;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClusterMembersUseCaseTest {

    private GetClusterMembersUseCase getClusterMembersUseCase;
    private final MemberDomainServiceInMemory memberDomainService = new MemberDomainServiceInMemory();

    @BeforeEach
    void setUp() {
        getClusterMembersUseCase = new GetClusterMembersUseCase(memberDomainService);
        initMembers();
    }

    @Test
    void should_return_cluster_members() {
        // When
        var result = getClusterMembersUseCase.execute(new GetClusterMembersUseCase.Input("cluster-1"));
        // Then
        assertThat(result.members()).map(MemberEntity::getId).containsExactly("member-1", "member-2", "member-5");
    }

    @Test
    void should_return_empty_list_when_cluster_id_not_found() {
        // When
        var result = getClusterMembersUseCase.execute(new GetClusterMembersUseCase.Input("cluster-3"));
        // Then
        assertThat(result.members()).isEmpty();
    }

    private void initMembers() {
        List<MemberEntity> members = List.of(
            MemberEntity.builder()
                .id("member-5")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity.builder()
                .id("member-2")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity.builder()
                .id("member-3")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-2")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity.builder()
                .id("member-4")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-2")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity.builder()
                .id("member-1")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .build()
        );
        memberDomainService.initWith(members);
    }
}
