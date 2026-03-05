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
package io.gravitee.apim.core.application_member.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteApplicationMemberUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String MEMBER_ID = "member-id";
    private static final String PRIMARY_OWNER_ID = "primary-owner-id";

    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();
    private DeleteApplicationMemberUseCase deleteApplicationMemberUseCase;

    @BeforeEach
    void setUp() {
        deleteApplicationMemberUseCase = new DeleteApplicationMemberUseCase(memberQueryService);
        memberQueryService.reset();
    }

    @Test
    void should_delete_application_member() {
        memberQueryService.initWith(List.of(aMember(MEMBER_ID, "USER"), aMember(PRIMARY_OWNER_ID, "PRIMARY_OWNER")));

        deleteApplicationMemberUseCase.execute(new DeleteApplicationMemberUseCase.Input(APPLICATION_ID, MEMBER_ID));

        assertThat(memberQueryService.storage()).hasSize(1);
        assertThat(memberQueryService.storage()).map(Member::getId).containsExactly(PRIMARY_OWNER_ID);
    }

    @Test
    void should_reject_primary_owner_deletion() {
        memberQueryService.initWith(List.of(aMember(PRIMARY_OWNER_ID, "PRIMARY_OWNER")));

        assertThrows(SinglePrimaryOwnerException.class, () ->
            deleteApplicationMemberUseCase.execute(new DeleteApplicationMemberUseCase.Input(APPLICATION_ID, PRIMARY_OWNER_ID))
        );
    }

    @Test
    void should_throw_when_member_not_found() {
        assertThrows(NotFoundDomainException.class, () ->
            deleteApplicationMemberUseCase.execute(new DeleteApplicationMemberUseCase.Input(APPLICATION_ID, MEMBER_ID))
        );
    }

    private static Member aMember(String id, String roleName) {
        return Member.builder()
            .id(id)
            .displayName("Member")
            .email("member@gravitee.io")
            .type(MembershipMemberType.USER)
            .referenceType(MembershipReferenceType.APPLICATION)
            .referenceId(APPLICATION_ID)
            .createdAt(new Date())
            .updatedAt(new Date())
            .roles(List.of(Member.Role.builder().scope(RoleScope.APPLICATION).name(roleName).build()))
            .build();
    }
}
