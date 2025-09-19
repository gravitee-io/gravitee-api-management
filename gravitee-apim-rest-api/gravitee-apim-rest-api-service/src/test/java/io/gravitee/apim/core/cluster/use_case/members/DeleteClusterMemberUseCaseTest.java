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
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.membership.model.Membership;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteClusterMemberUseCaseTest {

    private DeleteClusterMemberUseCase deleteClusterMemberUseCase;
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        deleteClusterMemberUseCase = new DeleteClusterMemberUseCase(membershipCrudService, membershipQueryService);
        initMemberships();
    }

    @Test
    public void should_delete_member() {
        assertThat(membershipCrudService.storage().size()).isEqualTo(3);
        deleteClusterMemberUseCase.execute(new DeleteClusterMemberUseCase.Input("cluster-1", "member-1"));
        assertThat(membershipCrudService.storage().size()).isEqualTo(2);
        assertThat(membershipCrudService.storage()).map(Membership::getId).containsExactlyInAnyOrder("m2", "m3");
    }

    @Test
    public void should_throw_exception_if_membership_not_found() {
        assertThrows(NoSuchElementException.class, () ->
            deleteClusterMemberUseCase.execute(new DeleteClusterMemberUseCase.Input("cluster-1", "member-unknown"))
        );
    }

    private void initMemberships() {
        List<Membership> memberships = List.of(
            Membership.builder()
                .id("m1")
                .referenceType(Membership.ReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .memberType(Membership.Type.USER)
                .memberId("member-1")
                .build(),
            Membership.builder()
                .id("m2")
                .referenceType(Membership.ReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .memberType(Membership.Type.USER)
                .memberId("member-2")
                .build(),
            Membership.builder()
                .id("m3")
                .referenceType(Membership.ReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .memberType(Membership.Type.USER)
                .memberId("member-3")
                .build()
        );
        membershipQueryService.initWith(memberships);
        membershipCrudService.initWith(memberships);
    }
}
