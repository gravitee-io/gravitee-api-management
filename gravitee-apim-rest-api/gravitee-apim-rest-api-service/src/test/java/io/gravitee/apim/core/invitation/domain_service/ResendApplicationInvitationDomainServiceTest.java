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
package io.gravitee.apim.core.invitation.domain_service;

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.exception.ApplicationInvitationNotFoundException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.common.utils.TimeProvider;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResendApplicationInvitationDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String OTHER_APPLICATION_ID = "other-application-id";
    private static final String INVITATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final URI CONFIRMATION_PAGE_URL = URI.create("https://portal.example.com/user/registration/confirm");
    private static final Instant RESEND_TIME = Instant.parse("2026-06-09T09:15:00Z");

    @Mock
    private InvitationCrudService invitationCrudService;

    @Mock
    private ApplicationInvitationNotificationDomainService applicationInvitationNotificationDomainService;

    private ResendApplicationInvitationDomainService cut;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(RESEND_TIME, ZoneId.systemDefault()));
        cut = new ResendApplicationInvitationDomainService(invitationCrudService, applicationInvitationNotificationDomainService);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_resend_application_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID);
        var invitation = anApplicationInvitation(INVITATION_ID, APPLICATION_ID, "alice@example.com", "USER");
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationCrudService.update(any(ApplicationInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cut.resend(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, invitationId, CONFIRMATION_PAGE_URL);

        assertThat(result.id()).isEqualTo(invitation.id());
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.roleName()).isEqualTo("USER");
        assertThat(result.createdAt()).isEqualTo(invitation.createdAt());
        assertThat(result.updatedAt()).isEqualTo(RESEND_TIME.atZone(ZoneId.systemDefault()));

        var invitationCaptor = ArgumentCaptor.forClass(ApplicationInvitation.class);
        verify(invitationCrudService).update(invitationCaptor.capture());
        assertThat(invitationCaptor.getValue().updatedAt()).isEqualTo(RESEND_TIME.atZone(ZoneId.systemDefault()));
        verify(applicationInvitationNotificationDomainService).dispatchAsync(
            eq(ORGANIZATION_ID),
            eq(ENVIRONMENT_ID),
            eq(APPLICATION_ID),
            argThat(invitations -> invitations.size() == 1 && invitations.get(0).updatedAt().equals(result.updatedAt())),
            eq(CONFIRMATION_PAGE_URL)
        );
    }

    @Test
    void should_throw_when_invitation_does_not_exist() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            cut.resend(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, invitationId, CONFIRMATION_PAGE_URL)
        ).isInstanceOf(ApplicationInvitationNotFoundException.class);

        verify(invitationCrudService, never()).update(any());
        verifyNoInteractions(applicationInvitationNotificationDomainService);
    }

    @Test
    void should_throw_when_invitation_belongs_to_another_application() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anApplicationInvitation(INVITATION_ID, OTHER_APPLICATION_ID, "alice@example.com", "USER"))
        );

        assertThatThrownBy(() ->
            cut.resend(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, invitationId, CONFIRMATION_PAGE_URL)
        ).isInstanceOf(ApplicationInvitationNotFoundException.class);

        verify(invitationCrudService, never()).update(any());
        verifyNoInteractions(applicationInvitationNotificationDomainService);
    }
}
