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
package io.gravitee.apim.core.user.use_case;

import static fixtures.core.model.UserFixtures.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.model.User;
import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.apim.core.user.query_service.UserQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchUsersUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private MembershipQueryService membershipQueryService;

    @Mock
    private ApplicationCrudService applicationCrudService;

    private SearchUsersUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchUsersUseCase(userQueryService, membershipQueryService, applicationCrudService);
    }

    @Test
    void should_use_criteria_query_and_enrich_application_membership() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var user1 = aUser("user-1", "ref-1", "John", "Smith", "John Smith", "john.smith@example.com");
        var user2 = aUser("user-2", "ref-2", "Johnny", "Doe", "Johnny Doe", "johnny.doe@example.com");

        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(new BaseApplicationEntity());
        when(userQueryService.search(new UserSearchQuery("body-query"))).thenReturn(List.of(user1, user2));
        when(
            membershipQueryService.findByMemberIdsAndMemberTypeAndReferenceType(
                List.of("user-2", "user-1"),
                Membership.Type.USER,
                Membership.ReferenceType.APPLICATION
            )
        ).thenReturn(List.of(Membership.builder().memberId("user-2").referenceId(APPLICATION_ID).build()));

        var result = cut.execute(
            new SearchUsersUseCase.Input(executionContext, new UserSearchQuery("body-query"), Optional.of(APPLICATION_ID))
        );

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.data()).extracting(User::id).containsExactly("user-2", "user-1");
        assertThat(result.applicationMembership()).containsEntry("user-2", true);
        assertThat(result.applicationMembership()).containsEntry("user-1", false);
        verify(userQueryService).search(new UserSearchQuery("body-query"));
        verify(applicationCrudService).findById(APPLICATION_ID, ENVIRONMENT_ID);
    }

    @Test
    void should_ignore_users_without_id_when_enriching_application_membership() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var userWithoutId = aUser(null, "ref-1", "External", "Adams", "External Adams", "external.adams@example.com");
        var userWithId = aUser("user-1", "ref-2", "John", "Smith", "John Smith", "john.smith@example.com");

        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(new BaseApplicationEntity());
        when(userQueryService.search(new UserSearchQuery("body-query"))).thenReturn(List.of(userWithoutId, userWithId));
        when(
            membershipQueryService.findByMemberIdsAndMemberTypeAndReferenceType(
                List.of("user-1"),
                Membership.Type.USER,
                Membership.ReferenceType.APPLICATION
            )
        ).thenReturn(List.of());

        var result = cut.execute(
            new SearchUsersUseCase.Input(executionContext, new UserSearchQuery("body-query"), Optional.of(APPLICATION_ID))
        );

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.data()).extracting(User::id).containsExactly(null, "user-1");
        assertThat(result.applicationMembership()).hasSize(1).containsEntry("user-1", false);
        verify(userQueryService).search(new UserSearchQuery("body-query"));
        verify(applicationCrudService).findById(APPLICATION_ID, ENVIRONMENT_ID);
    }

    @Test
    void should_fallback_to_wildcard_query_when_no_query_is_provided() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var user = aUser("user-1", null, null, "Smith", "John Smith", null);

        when(userQueryService.search(new UserSearchQuery(null))).thenReturn(List.of(user));

        var result = cut.execute(new SearchUsersUseCase.Input(executionContext, new UserSearchQuery(null), Optional.empty()));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.data()).extracting(User::id).containsExactly("user-1");
        assertThat(result.applicationMembership()).isNull();
        verify(applicationCrudService, never()).findById(anyString(), anyString());
    }

    @Test
    void should_return_all_sorted_users_without_pagination() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

        when(userQueryService.search(new UserSearchQuery("john"))).thenReturn(
            List.of(aUser("user-1", null, null, "Smith", "John Smith", null), aUser("user-2", null, null, "Doe", "Johnny Doe", null))
        );

        var result = cut.execute(new SearchUsersUseCase.Input(executionContext, new UserSearchQuery("john"), Optional.empty()));

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.data()).extracting(User::id).containsExactly("user-2", "user-1");
    }

    @Test
    void should_throw_when_application_membership_is_requested_for_unknown_application() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenThrow(new ApplicationNotFoundException(APPLICATION_ID));

        Throwable throwable = catchThrowable(() ->
            cut.execute(new SearchUsersUseCase.Input(executionContext, new UserSearchQuery(null), Optional.of(APPLICATION_ID)))
        );

        assertThat(throwable).isInstanceOf(ApplicationNotFoundException.class).hasMessageContaining(APPLICATION_ID);
        verify(userQueryService, never()).search(any());
    }
}
