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
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerRemovalException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApiProductMemberUseCaseTest {

    private UpdateApiProductMemberUseCase updateApiProductMemberUseCase;
    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        updateApiProductMemberUseCase = new UpdateApiProductMemberUseCase(memberQueryService);
        initMembers();
    }

    @Test
    void should_update_api_product_member() {
        Member member = memberQueryService.storage().get(0);
        assertThat(member.getRoles().get(0).getName()).isEqualTo("OWNER");

        updateApiProductMemberUseCase.execute(new UpdateApiProductMemberUseCase.Input("API_PRODUCT_USER", "member-1", "ap-1"));

        member = memberQueryService.storage().get(0);
        assertThat(member.getRoles().get(0).getName()).isEqualTo("API_PRODUCT_USER");
        assertThat(member.getRoles().get(0).getScope()).isEqualTo(RoleScope.API_PRODUCT);
    }

    @Test
    void should_throw_validation_error_when_role_is_primary_owner() {
        assertThatThrownBy(() ->
            updateApiProductMemberUseCase.execute(new UpdateApiProductMemberUseCase.Input("PRIMARY_OWNER", "member-1", "ap-1"))
        ).isInstanceOf(SinglePrimaryOwnerException.class);
    }

    @Test
    void should_throw_exception_when_downgrading_primary_owner() {
        assertThatThrownBy(() ->
            updateApiProductMemberUseCase.execute(new UpdateApiProductMemberUseCase.Input("API_PRODUCT_USER", "primary-owner", "ap-1"))
        ).isInstanceOf(PrimaryOwnerRemovalException.class);
    }

    @Test
    void should_throw_exception_when_member_not_found() {
        assertThatThrownBy(() ->
            updateApiProductMemberUseCase.execute(new UpdateApiProductMemberUseCase.Input("API_PRODUCT_USER", "member-unknown", "ap-1"))
        ).isInstanceOf(MemberNotFoundException.class);
    }

    private void initMembers() {
        List<Member> members = List.of(
            Member.builder()
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.USER)
                .id("member-1")
                .roles(List.of(Member.Role.builder().name("OWNER").build()))
                .build(),
            Member.builder()
                .referenceType(MembershipReferenceType.API_PRODUCT)
                .referenceId("ap-1")
                .type(MembershipMemberType.USER)
                .id("primary-owner")
                .roles(List.of(Member.Role.builder().name(PRIMARY_OWNER.name()).build()))
                .build()
        );
        memberQueryService.initWith(members);
    }
}
