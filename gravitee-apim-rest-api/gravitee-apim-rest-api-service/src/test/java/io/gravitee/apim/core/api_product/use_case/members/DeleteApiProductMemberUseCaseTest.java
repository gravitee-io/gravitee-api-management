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
package io.gravitee.apim.core.api_product.use_case.members;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteApiProductMemberUseCaseTest {

    private DeleteApiProductMemberUseCase deleteApiProductMemberUseCase;
    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        deleteApiProductMemberUseCase = new DeleteApiProductMemberUseCase(memberQueryService);
        initMembers();
    }

    @Test
    void should_delete_member() {
        assertThat(memberQueryService.storage()).hasSize(3);
        deleteApiProductMemberUseCase.execute(new DeleteApiProductMemberUseCase.Input("ap-1", "member-1"));
        assertThat(memberQueryService.storage()).hasSize(2);
        assertThat(memberQueryService.storage()).map(Member::getId).containsExactlyInAnyOrder("member-2", "member-3");
    }

    private void initMembers() {
        memberQueryService.initWith(
            List.of(
                Member.builder()
                    .id("member-1")
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .type(MembershipMemberType.USER)
                    .roles(List.of())
                    .build(),
                Member.builder()
                    .id("member-2")
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .type(MembershipMemberType.USER)
                    .roles(List.of())
                    .build(),
                Member.builder()
                    .id("member-3")
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .type(MembershipMemberType.USER)
                    .roles(List.of())
                    .build()
            )
        );
    }
}
