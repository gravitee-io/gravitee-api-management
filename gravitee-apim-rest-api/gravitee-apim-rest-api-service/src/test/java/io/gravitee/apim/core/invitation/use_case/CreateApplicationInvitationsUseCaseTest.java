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
import io.gravitee.apim.core.invitation.domain_service.CreateApplicationInvitationsDomainService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.CreateApplicationInvitations;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateApplicationInvitationsUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ROLE_NAME = "USER";

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private CreateApplicationInvitationsDomainService createApplicationInvitationsDomainService;

    private CreateApplicationInvitationsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new CreateApplicationInvitationsUseCase(applicationCrudService, createApplicationInvitationsDomainService);
    }

    @Test
    void should_validate_application_existence_before_creating_invitations() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenThrow(new ApplicationNotFoundException(APPLICATION_ID));

        Throwable throwable = catchThrowable(() ->
            cut.execute(
                new CreateApplicationInvitationsUseCase.Input(
                    executionContext,
                    APPLICATION_ID,
                    new CreateApplicationInvitations(Set.of("alice@example.com"), ROLE_NAME, false)
                )
            )
        );

        assertThat(throwable).isInstanceOf(ApplicationNotFoundException.class).hasMessageContaining(APPLICATION_ID);
        verifyNoInteractions(createApplicationInvitationsDomainService);
    }

    @Test
    void should_create_application_invitations() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var recipients = Set.of("alice@example.com", "bob@example.com");
        var application = BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build();
        var invitations = List.of(anInvitation("alice@example.com"), anInvitation("bob@example.com"));
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(application);
        when(createApplicationInvitationsDomainService.create(ORGANIZATION_ID, APPLICATION_ID, recipients, ROLE_NAME, false)).thenReturn(
            invitations
        );

        var result = cut.execute(
            new CreateApplicationInvitationsUseCase.Input(
                executionContext,
                APPLICATION_ID,
                new CreateApplicationInvitations(recipients, ROLE_NAME, false)
            )
        );

        assertThat(result.invitations()).isEqualTo(invitations);
        verify(createApplicationInvitationsDomainService).create(ORGANIZATION_ID, APPLICATION_ID, recipients, ROLE_NAME, false);
    }

    @Test
    void should_propagate_notify_not_implemented_exception() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var recipients = Set.of("alice@example.com");
        var application = BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build();
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(application);
        when(createApplicationInvitationsDomainService.create(ORGANIZATION_ID, APPLICATION_ID, recipients, ROLE_NAME, true)).thenThrow(
            new UnsupportedOperationException("Application invitation notifications are not implemented yet.")
        );

        Throwable throwable = catchThrowable(() ->
            cut.execute(
                new CreateApplicationInvitationsUseCase.Input(
                    executionContext,
                    APPLICATION_ID,
                    new CreateApplicationInvitations(recipients, ROLE_NAME, true)
                )
            )
        );

        assertThat(throwable).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("not implemented yet");
    }

    private ApplicationInvitation anInvitation(String email) {
        return ApplicationInvitation.create(APPLICATION_ID, email, ROLE_NAME);
    }
}
