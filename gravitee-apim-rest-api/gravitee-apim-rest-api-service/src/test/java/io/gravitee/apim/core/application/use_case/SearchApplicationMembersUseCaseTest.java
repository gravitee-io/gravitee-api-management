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
package io.gravitee.apim.core.application.use_case;

import static fixtures.core.model.BaseUserEntityFixtures.aBaseUserEntity;
import static fixtures.core.model.MembershipFixtures.anApplicationMembership;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.model.SearchApplicationMembersCriteria;
import io.gravitee.apim.core.membership.domain_service.SearchApplicationMembersDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchApplicationMembersUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";

    @Mock
    private SearchApplicationMembersDomainService searchApplicationMembersDomainService;

    @Mock
    private UserCrudService userCrudService;

    private SearchApplicationMembersUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchApplicationMembersUseCase(searchApplicationMembersDomainService, userCrudService);
    }

    @Test
    void should_throw_when_application_is_not_found() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var pageable = new PageableImpl(1, 10);
        var criteria = SearchApplicationMembersCriteria.builder().build();

        when(searchApplicationMembersDomainService.searchApplicationMembers(ENVIRONMENT_ID, APPLICATION_ID)).thenThrow(
            new ApplicationNotFoundException(APPLICATION_ID)
        );

        Throwable throwable = catchThrowable(() ->
            cut.execute(new SearchApplicationMembersUseCase.Input(executionContext, APPLICATION_ID, criteria, pageable))
        );

        assertThat(throwable).isInstanceOf(ApplicationNotFoundException.class).hasMessageContaining(APPLICATION_ID);
    }

    @Test
    void should_search_filter_sort_paginate_and_return_users_for_page() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var pageable = new PageableImpl(1, 1);
        var criteria = SearchApplicationMembersCriteria.builder().displayName("a").build();
        var membership1 = anApplicationMembership("00000000-0000-0000-0000-000000000001", APPLICATION_ID, "user-1", "role-1");
        var membership2 = anApplicationMembership("00000000-0000-0000-0000-000000000002", APPLICATION_ID, "user-2", "role-2");
        var groupMembership = anApplicationMembership(
            "00000000-0000-0000-0000-000000000003",
            APPLICATION_ID,
            "group-1",
            "role-3"
        ).withMemberType(Membership.Type.GROUP);
        var user1 = aBaseUserEntity("user-1");
        user1.setFirstname("Alice");
        user1.setLastname("");
        var user2 = aBaseUserEntity("user-2");
        user2.setFirstname("Bob");
        user2.setLastname("");

        when(searchApplicationMembersDomainService.searchApplicationMembers(ENVIRONMENT_ID, APPLICATION_ID)).thenReturn(
            List.of(membership2, groupMembership, membership1)
        );
        when(userCrudService.findBaseUsersByIds(List.of("user-2", "user-1"))).thenReturn(Set.of(user1, user2));

        var result = cut.execute(new SearchApplicationMembersUseCase.Input(executionContext, APPLICATION_ID, criteria, pageable));

        assertThat(result.memberships().getTotalElements()).isEqualTo(1);
        assertThat(result.memberships().getContent()).containsExactly(membership1);
        assertThat(result.users()).containsExactly(user1);
        verify(searchApplicationMembersDomainService).searchApplicationMembers(eq(ENVIRONMENT_ID), eq(APPLICATION_ID));
        verify(userCrudService).findBaseUsersByIds(eq(List.of("user-2", "user-1")));
    }

    @Test
    void should_throw_when_page_is_invalid() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var pageable = new PageableImpl(2, 10);
        var membership = anApplicationMembership("00000000-0000-0000-0000-000000000001", APPLICATION_ID, "user-1", "role-1");
        var user = aBaseUserEntity("user-1");

        when(searchApplicationMembersDomainService.searchApplicationMembers(ENVIRONMENT_ID, APPLICATION_ID)).thenReturn(
            List.of(membership)
        );
        when(userCrudService.findBaseUsersByIds(List.of("user-1"))).thenReturn(Set.of(user));

        Throwable throwable = catchThrowable(() ->
            cut.execute(
                new SearchApplicationMembersUseCase.Input(
                    executionContext,
                    APPLICATION_ID,
                    SearchApplicationMembersCriteria.builder().build(),
                    pageable
                )
            )
        );

        assertThat(throwable).isInstanceOf(PaginationInvalidException.class);
    }
}
