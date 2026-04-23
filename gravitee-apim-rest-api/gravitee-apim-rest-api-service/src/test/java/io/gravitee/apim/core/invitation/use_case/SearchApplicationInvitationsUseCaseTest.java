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
package io.gravitee.apim.core.invitation.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import io.gravitee.apim.core.invitation.model.SearchApplicationInvitationsCriteria;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchApplicationInvitationsUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private InvitationQueryService invitationQueryService;

    private SearchApplicationInvitationsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchApplicationInvitationsUseCase(applicationCrudService, invitationQueryService);
    }

    @Test
    void should_validate_application_existence_before_searching() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenThrow(new ApplicationNotFoundException(APPLICATION_ID));

        Throwable throwable = catchThrowable(() ->
            cut.execute(
                new SearchApplicationInvitationsUseCase.Input(
                    executionContext,
                    APPLICATION_ID,
                    new SearchApplicationInvitationsCriteria(Optional.empty()),
                    new PageableImpl(1, 10)
                )
            )
        );

        assertThat(throwable).isInstanceOf(ApplicationNotFoundException.class).hasMessageContaining(APPLICATION_ID);
        verifyNoInteractions(invitationQueryService);
    }

    @Test
    void should_filter_sort_and_paginate_invitations() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var application = BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build();
        var criteria = new SearchApplicationInvitationsCriteria(Optional.of("JOHN"));
        var pageable = new PageableImpl(1, 1);
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(application);
        when(invitationQueryService.findByApplicationId(APPLICATION_ID, criteria, pageable)).thenReturn(
            new Page<>(List.of(anInvitation("john.alpha@example.com")), 1, 1, 1)
        );

        var result = cut.execute(new SearchApplicationInvitationsUseCase.Input(executionContext, APPLICATION_ID, criteria, pageable));

        assertThat(result.invitations().getTotalElements()).isEqualTo(1);
        assertThat(result.invitations().getPageElements()).isEqualTo(1);
        assertThat(result.invitations().getContent())
            .extracting(ApplicationInvitationItem::email)
            .containsExactly("john.alpha@example.com");
        verify(invitationQueryService).findByApplicationId(APPLICATION_ID, criteria, pageable);
    }

    @Test
    void should_return_empty_page_when_no_invitation_matches() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var application = BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build();
        var criteria = new SearchApplicationInvitationsCriteria(Optional.of("john"));
        var pageable = new PageableImpl(1, 10);
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(application);
        when(invitationQueryService.findByApplicationId(APPLICATION_ID, criteria, pageable)).thenReturn(new Page<>(List.of(), 1, 0, 0));

        var result = cut.execute(new SearchApplicationInvitationsUseCase.Input(executionContext, APPLICATION_ID, criteria, pageable));

        assertThat(result.invitations().getContent()).isEmpty();
        assertThat(result.invitations().getPageElements()).isZero();
        assertThat(result.invitations().getTotalElements()).isZero();
    }

    @Test
    void should_throw_when_page_is_invalid() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var application = BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build();
        var criteria = new SearchApplicationInvitationsCriteria(Optional.empty());
        var pageable = new PageableImpl(2, 10);
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(application);
        when(invitationQueryService.findByApplicationId(APPLICATION_ID, criteria, pageable)).thenThrow(new PaginationInvalidException());

        Throwable throwable = catchThrowable(() ->
            cut.execute(new SearchApplicationInvitationsUseCase.Input(executionContext, APPLICATION_ID, criteria, pageable))
        );

        assertThat(throwable).isInstanceOf(PaginationInvalidException.class);
    }

    private ApplicationInvitationItem anInvitation(String email) {
        return ApplicationInvitationItem.create(email, "USER");
    }
}
