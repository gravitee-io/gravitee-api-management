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
import static org.junit.jupiter.api.Assertions.*;

import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.apim.core.membership.model.AddMember;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateClusterMemberUseCaseTest {

    private UpdateClusterMemberUseCase updateClusterMemberUseCase;
    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        updateClusterMemberUseCase = new UpdateClusterMemberUseCase(memberQueryService);
        initMembers();
    }

    @Test
    void should_update_cluster_member() {
        Member member = memberQueryService.storage().get(0);
        assertThat(member.getRoles().get(0).getName()).isEqualTo("OWNER");
        updateClusterMemberUseCase.execute(new UpdateClusterMemberUseCase.Input("USER", "member-1", "cluster-1"));
        member = memberQueryService.storage().get(0);
        assertThat(member.getRoles().get(0).getName()).isEqualTo("USER");
        assertThat(member.getRoles().get(0).getScope()).isEqualTo(RoleScope.CLUSTER);
    }

    @Test
    void should_throw_validation_error_when_role_is_primary_owner() {
        assertThrows(SinglePrimaryOwnerException.class, () ->
            updateClusterMemberUseCase.execute(new UpdateClusterMemberUseCase.Input("PRIMARY_OWNER", "member-1", "cluster-1"))
        );
    }

    private void initMembers() {
        Member.Role role = Member.Role.builder().name("OWNER").build();
        List<Member> members = List.of(
            Member.builder()
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .id("member-1")
                .roles(List.of(role))
                .build()
        );
        memberQueryService.initWith(members);
    }
}
