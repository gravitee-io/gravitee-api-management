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
import static fixtures.core.model.RoleFixtures.anApplicationRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationReference;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateApplicationInvitationsDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String ROLE_NAME = "USER";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";
    private static final String EXISTING_INVITATION_ID = "00000000-0000-0000-0000-000000000003";
    private static final URI CONFIRMATION_PAGE_URL = URI.create("https://portal.example.com/user/registration/confirm");

    @Mock
    private InvitationQueryService invitationQueryService;

    @Mock
    private InvitationCrudService invitationCrudService;

    @Mock
    private RoleQueryService roleQueryService;

    @Mock
    private ApplicationInvitationNotificationDomainService applicationInvitationNotificationDomainService;

    private CreateApplicationInvitationsDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new CreateApplicationInvitationsDomainService(
            invitationQueryService,
            invitationCrudService,
            new ValidateApplicationInvitationRoleDomainService(roleQueryService),
            applicationInvitationNotificationDomainService
        );
    }

    @Test
    void should_create_invitations_with_normalized_input_when_notify_is_false() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(invitationCrudService.create(any(ApplicationInvitation.class))).thenReturn(
            anApplicationInvitation(INVITATION_ID_1, "alice@example.com"),
            anApplicationInvitation(INVITATION_ID_2, "bob@example.com")
        );

        var result = cut.create(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            APPLICATION_ID,
            recipients("alice@example.com", "bob@example.com"),
            ROLE_NAME,
            false,
            null
        );

        assertThat(result).extracting(ApplicationInvitation::email).containsExactly("alice@example.com", "bob@example.com");

        var invitationCaptor = ArgumentCaptor.forClass(ApplicationInvitation.class);
        verify(invitationCrudService, times(2)).create(invitationCaptor.capture());
        assertThat(invitationCaptor.getAllValues()).extracting(ApplicationInvitation::applicationId).containsOnly(APPLICATION_ID);
        assertThat(invitationCaptor.getAllValues())
            .extracting(ApplicationInvitation::email)
            .containsExactly("alice@example.com", "bob@example.com");
        assertThat(invitationCaptor.getAllValues()).extracting(ApplicationInvitation::roleName).containsOnly(ROLE_NAME);
        verifyNoInteractions(applicationInvitationNotificationDomainService);
    }

    @Test
    void should_reject_invalid_email_before_creating_any_invitation() {
        givenExistingRole();

        var throwable = catchThrowable(() ->
            cut.create(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, Set.of("not-an-email"), ROLE_NAME, false, null)
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class).hasMessageContaining("email is invalid");
        verifyNoInteractions(invitationCrudService);
        verify(invitationQueryService, never()).findByReference(any());
    }

    @Test
    void should_create_invitations_and_dispatch_notification_when_notify_is_true() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(invitationCrudService.create(any(ApplicationInvitation.class))).thenReturn(
            anApplicationInvitation(INVITATION_ID_1, "alice@example.com")
        );

        var result = cut.create(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            APPLICATION_ID,
            Set.of("alice@example.com"),
            ROLE_NAME,
            true,
            CONFIRMATION_PAGE_URL
        );

        assertThat(result).extracting(ApplicationInvitation::email).containsExactly("alice@example.com");
        verify(applicationInvitationNotificationDomainService).dispatchAsync(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            APPLICATION_ID,
            result,
            CONFIRMATION_PAGE_URL
        );
    }

    @Test
    void should_reject_pending_invitation_conflict_before_creating_any_invitation() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(
            List.of(anApplicationInvitation(EXISTING_INVITATION_ID, "alice@example.com"))
        );

        var throwable = catchThrowable(() ->
            cut.create(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, Set.of("alice@example.com"), ROLE_NAME, false, null)
        );

        assertThat(throwable).isInstanceOf(ConflictDomainException.class).hasMessageContaining("pending application invitation");
        verifyNoInteractions(invitationCrudService);
    }

    @Test
    void should_reject_unknown_application_role() {
        when(roleQueryService.findApplicationRole(eq(ROLE_NAME), any(ReferenceContext.class))).thenReturn(Optional.empty());

        var throwable = catchThrowable(() ->
            cut.create(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, Set.of("alice@example.com"), ROLE_NAME, false, null)
        );

        assertThat(throwable).isInstanceOf(RoleNotFoundException.class);
        verifyNoInteractions(invitationQueryService, invitationCrudService);
    }

    private void givenExistingRole() {
        when(roleQueryService.findApplicationRole(eq(ROLE_NAME), any(ReferenceContext.class))).thenReturn(
            Optional.of(anApplicationRole("role-id", ROLE_NAME))
        );
    }

    private Set<String> recipients(String... emails) {
        return new LinkedHashSet<>(List.of(emails));
    }
}
