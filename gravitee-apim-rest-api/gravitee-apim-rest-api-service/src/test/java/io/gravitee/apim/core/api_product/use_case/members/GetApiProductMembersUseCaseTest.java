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

class GetApiProductMembersUseCaseTest {

    private GetApiProductMembersUseCase getApiProductMembersUseCase;
    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        getApiProductMembersUseCase = new GetApiProductMembersUseCase(memberQueryService);
        initMembers();
    }

    @Test
    void should_return_api_product_members_sorted_by_id_with_nulls_last() {
        var result = getApiProductMembersUseCase.execute(new GetApiProductMembersUseCase.Input("ap-1"));

        assertThat(result.members()).map(Member::getId).containsExactly("member-1", "member-2", "member-5", null);
    }

    @Test
    void should_return_empty_list_when_no_members_for_product() {
        var result = getApiProductMembersUseCase.execute(new GetApiProductMembersUseCase.Input("ap-unknown"));

        assertThat(result.members()).isEmpty();
    }

    @Test
    void should_exclude_non_user_members() {
        var result = getApiProductMembersUseCase.execute(new GetApiProductMembersUseCase.Input("ap-1"));

        assertThat(result.members()).allMatch(m -> m.getType() == MembershipMemberType.USER);
    }

    private void initMembers() {
        List<Member> members = List.of(
            Member.builder()
                .id("member-5")
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.USER)
                .build(),
            Member.builder()
                .id("member-2")
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.USER)
                .build(),
            Member.builder()
                .id(null)
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.USER)
                .build(),
            Member.builder()
                .id("member-3")
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-2")
                .type(MembershipMemberType.USER)
                .build(),
            Member.builder()
                .id("member-1")
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.USER)
                .build(),
            Member.builder()
                .id("group-1")
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.GROUP)
                .build()
        );
        memberQueryService.initWith(members);
    }
}
