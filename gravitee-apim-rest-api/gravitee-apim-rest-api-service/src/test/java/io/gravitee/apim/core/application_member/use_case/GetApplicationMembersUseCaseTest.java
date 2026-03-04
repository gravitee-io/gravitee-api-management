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

import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApplicationMembersUseCaseTest {

    private GetApplicationMembersUseCase getApplicationMembersUseCase;
    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        getApplicationMembersUseCase = new GetApplicationMembersUseCase(memberQueryService);
        memberQueryService.reset();
        memberQueryService.initWith(
            List.of(
                aMember("member-2", "John Doe", "john@gravitee.io", "app-1"),
                aMember("member-1", "Jane Doe", "jane@gravitee.io", "app-1"),
                aMember("member-3", "Alice Smith", "alice@example.org", "app-2")
            )
        );
    }

    @Test
    void should_return_application_members() {
        var result = getApplicationMembersUseCase.execute(new GetApplicationMembersUseCase.Input("app-1", "env-1", null, 1, 10));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.members()).map(Member::getId).containsExactly("member-1", "member-2");
    }

    @Test
    void should_return_empty_list_when_application_id_not_found() {
        var result = getApplicationMembersUseCase.execute(new GetApplicationMembersUseCase.Input("unknown", "env-1", null, 1, 10));

        assertThat(result.totalElements()).isZero();
        assertThat(result.members()).isEmpty();
    }

    @Test
    void should_paginate_members() {
        var result = getApplicationMembersUseCase.execute(new GetApplicationMembersUseCase.Input("app-1", "env-1", null, 2, 1));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.members()).map(Member::getId).containsExactly("member-2");
    }

    @Test
    void should_filter_members_on_name_or_email() {
        var resultByName = getApplicationMembersUseCase.execute(new GetApplicationMembersUseCase.Input("app-1", "env-1", "jane", 1, 10));
        var resultByEmail = getApplicationMembersUseCase.execute(
            new GetApplicationMembersUseCase.Input("app-2", "env-1", "EXAMPLE.ORG", 1, 10)
        );

        assertThat(resultByName.totalElements()).isEqualTo(1);
        assertThat(resultByName.members()).map(Member::getId).containsExactly("member-1");
        assertThat(resultByEmail.totalElements()).isEqualTo(1);
        assertThat(resultByEmail.members()).map(Member::getId).containsExactly("member-3");
    }

    @Test
    void should_return_all_members_without_pagination_when_size_is_minus_one() {
        var result = getApplicationMembersUseCase.execute(new GetApplicationMembersUseCase.Input("app-1", "env-1", null, 1, -1));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.members()).map(Member::getId).containsExactly("member-1", "member-2");
    }

    @Test
    void should_propagate_member_permissions() {
        memberQueryService.reset();
        memberQueryService.initWith(
            List.of(
                Member.builder()
                    .id("member-with-permissions")
                    .displayName("Permission User")
                    .email("permissions@gravitee.io")
                    .type(MembershipMemberType.USER)
                    .referenceType(MembershipReferenceType.APPLICATION)
                    .referenceId("app-1")
                    .permissions(Map.of("APPLICATION_MEMBER", new char[] { 'R', 'U' }))
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .roles(List.of(Member.Role.builder().scope(RoleScope.APPLICATION).name("USER").build()))
                    .build()
            )
        );

        var result = getApplicationMembersUseCase.execute(new GetApplicationMembersUseCase.Input("app-1", "env-1", null, 1, 10));

        assertThat(result.members()).hasSize(1);
        assertThat(result.members().get(0).getPermissions()).containsKey("APPLICATION_MEMBER");
        assertThat(result.members().get(0).getPermissions().get("APPLICATION_MEMBER")).containsExactly('R', 'U');
    }

    private static Member aMember(String id, String displayName, String email, String applicationId) {
        return Member.builder()
            .id(id)
            .displayName(displayName)
            .email(email)
            .type(MembershipMemberType.USER)
            .referenceType(MembershipReferenceType.APPLICATION)
            .referenceId(applicationId)
            .createdAt(new Date())
            .updatedAt(new Date())
            .roles(List.of(Member.Role.builder().scope(RoleScope.APPLICATION).name("USER").build()))
            .build();
    }
}
