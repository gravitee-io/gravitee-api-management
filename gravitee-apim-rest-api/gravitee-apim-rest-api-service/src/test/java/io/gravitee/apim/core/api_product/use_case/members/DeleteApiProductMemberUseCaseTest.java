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

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.membership.exception.MemberNotFoundException;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerRemovalException;
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
        assertThat(memberQueryService.storage()).hasSize(4);
        deleteApiProductMemberUseCase.execute(new DeleteApiProductMemberUseCase.Input("ap-1", "member-1"));
        assertThat(memberQueryService.storage()).hasSize(3);
        assertThat(memberQueryService.storage()).map(Member::getId).containsExactlyInAnyOrder("member-2", "member-3", "primary-owner");
    }

    @Test
    void should_throw_exception_when_member_not_found() {
        assertThatThrownBy(() ->
            deleteApiProductMemberUseCase.execute(new DeleteApiProductMemberUseCase.Input("ap-1", "member-unknown"))
        ).isInstanceOf(MemberNotFoundException.class);
        assertThat(memberQueryService.storage()).hasSize(4);
    }

    @Test
    void should_throw_exception_when_deleting_primary_owner() {
        assertThatThrownBy(() ->
            deleteApiProductMemberUseCase.execute(new DeleteApiProductMemberUseCase.Input("ap-1", "primary-owner"))
        ).isInstanceOf(PrimaryOwnerRemovalException.class);
        assertThat(memberQueryService.storage()).hasSize(4);
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
                    .build(),
                Member.builder()
                    .id("primary-owner")
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .type(MembershipMemberType.USER)
                    .roles(List.of(Member.Role.builder().name(PRIMARY_OWNER.name()).build()))
                    .build()
            )
        );
    }
}
