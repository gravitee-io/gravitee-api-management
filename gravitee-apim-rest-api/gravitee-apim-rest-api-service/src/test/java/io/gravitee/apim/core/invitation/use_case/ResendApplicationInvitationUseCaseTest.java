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

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.invitation.domain_service.ResendApplicationInvitationDomainService;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResendApplicationInvitationUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String INVITATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final URI CONFIRMATION_PAGE_URL = URI.create("https://portal.example.com/user/registration/confirm");

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private ResendApplicationInvitationDomainService resendApplicationInvitationDomainService;

    private ResendApplicationInvitationUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new ResendApplicationInvitationUseCase(applicationCrudService, resendApplicationInvitationDomainService);
    }

    @Test
    void should_validate_application_existence_before_resending_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenThrow(new ApplicationNotFoundException(APPLICATION_ID));

        assertThatThrownBy(() ->
            cut.execute(
                new ResendApplicationInvitationUseCase.Input(
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    APPLICATION_ID,
                    invitationId,
                    CONFIRMATION_PAGE_URL
                )
            )
        ).isInstanceOf(ApplicationNotFoundException.class);

        verifyNoInteractions(resendApplicationInvitationDomainService);
    }

    @Test
    void should_resend_application_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID);
        var invitation = anApplicationInvitation(INVITATION_ID, APPLICATION_ID, "alice@example.com", "USER");
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(
            BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build()
        );
        when(
            resendApplicationInvitationDomainService.resend(
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                APPLICATION_ID,
                invitationId,
                CONFIRMATION_PAGE_URL
            )
        ).thenReturn(invitation);

        var output = cut.execute(
            new ResendApplicationInvitationUseCase.Input(
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                APPLICATION_ID,
                invitationId,
                CONFIRMATION_PAGE_URL
            )
        );

        assertThat(output.invitation()).isEqualTo(invitation);
        verify(resendApplicationInvitationDomainService).resend(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            APPLICATION_ID,
            invitationId,
            CONFIRMATION_PAGE_URL
        );
    }
}
