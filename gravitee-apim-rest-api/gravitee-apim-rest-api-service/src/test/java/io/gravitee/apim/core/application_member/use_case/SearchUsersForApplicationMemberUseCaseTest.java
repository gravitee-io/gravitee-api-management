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

import inmemory.ApplicationMemberUserQueryServiceInMemory;
import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.application_member.model.ApplicationMemberSearchUser;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchUsersForApplicationMemberUseCaseTest {

    private static final String APPLICATION_ID = "application-id";

    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();
    private final ApplicationMemberUserQueryServiceInMemory applicationMemberUserQueryService =
        new ApplicationMemberUserQueryServiceInMemory();
    private SearchUsersForApplicationMemberUseCase searchUsersForApplicationMemberUseCase;

    @BeforeEach
    void setUp() {
        searchUsersForApplicationMemberUseCase = new SearchUsersForApplicationMemberUseCase(
            applicationMemberUserQueryService,
            memberQueryService
        );
        memberQueryService.reset();
        applicationMemberUserQueryService.reset();

        memberQueryService.initWith(List.of(aMember("member-1", APPLICATION_ID), aMember("member-2", "another-app")));
        applicationMemberUserQueryService.initWith(
            List.of(
                aUser("member-1", "ref-1", "Jane", "Doe", "Jane Doe", "jane@gravitee.io"),
                aUser("member-2", "ref-2", "John", "Black", "John Black", "john@gravitee.io"),
                aUser("member-3", "ref-3", "Mary", "Clark", "Mary Clark", "mary@gravitee.io")
            )
        );
    }

    @Test
    void should_return_matching_users() {
        var result = searchUsersForApplicationMemberUseCase.execute(
            new SearchUsersForApplicationMemberUseCase.Input(APPLICATION_ID, "mary", "env-id")
        );

        assertThat(result.users()).extracting(ApplicationMemberSearchUser::id).containsExactly("member-3");
    }

    @Test
    void should_exclude_existing_application_members() {
        var result = searchUsersForApplicationMemberUseCase.execute(
            new SearchUsersForApplicationMemberUseCase.Input(APPLICATION_ID, "*", "env-id")
        );

        assertThat(result.users()).extracting(ApplicationMemberSearchUser::id).containsExactly("member-2", "member-3");
    }

    @Test
    void should_return_all_users_when_query_is_empty() {
        var result = searchUsersForApplicationMemberUseCase.execute(
            new SearchUsersForApplicationMemberUseCase.Input(APPLICATION_ID, "   ", "env-id")
        );

        assertThat(result.users()).extracting(ApplicationMemberSearchUser::id).containsExactly("member-2", "member-3");
    }

    private static Member aMember(String id, String applicationId) {
        return Member.builder()
            .id(id)
            .displayName("Member")
            .email("member@gravitee.io")
            .type(MembershipMemberType.USER)
            .referenceType(MembershipReferenceType.APPLICATION)
            .referenceId(applicationId)
            .createdAt(new Date())
            .updatedAt(new Date())
            .roles(List.of(Member.Role.builder().scope(RoleScope.APPLICATION).name("USER").build()))
            .build();
    }

    private static ApplicationMemberSearchUser aUser(
        String id,
        String reference,
        String firstName,
        String lastName,
        String displayName,
        String email
    ) {
        return new ApplicationMemberSearchUser(id, reference, firstName, lastName, displayName, email);
    }
}
