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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.exception.ApplicationInvitationNotFoundException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.apim.core.invitation.model.UpdateApplicationInvitation;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.ReferenceContext;
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
class UpdateApplicationInvitationDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String OTHER_APPLICATION_ID = "other-application-id";
    private static final String INVITATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final String CURRENT_ROLE_NAME = "USER";
    private static final String UPDATED_ROLE_NAME = "OWNER";
    private static final Instant UPDATE_TIME = Instant.parse("2026-04-23T10:15:00Z");

    @Mock
    private InvitationCrudService invitationCrudService;

    @Mock
    private RoleQueryService roleQueryService;

    private UpdateApplicationInvitationDomainService cut;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));
        cut = new UpdateApplicationInvitationDomainService(
            invitationCrudService,
            new ValidateApplicationInvitationRoleDomainService(roleQueryService)
        );
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_update_application_invitation_role() {
        var invitationId = InvitationId.of(INVITATION_ID);
        var invitation = anApplicationInvitation(INVITATION_ID, APPLICATION_ID, "alice@example.com", CURRENT_ROLE_NAME);
        givenInvitation(invitationId, invitation);
        givenExistingRole(UPDATED_ROLE_NAME);
        when(invitationCrudService.update(any(ApplicationInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cut.update(ORGANIZATION_ID, APPLICATION_ID, invitationId, new UpdateApplicationInvitation(UPDATED_ROLE_NAME));

        assertThat(result.id()).isEqualTo(invitation.id());
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.roleName()).isEqualTo(UPDATED_ROLE_NAME);
        assertThat(result.createdAt()).isEqualTo(invitation.createdAt());
        assertThat(result.updatedAt()).isEqualTo(UPDATE_TIME.atZone(ZoneId.systemDefault()));

        var invitationCaptor = ArgumentCaptor.forClass(ApplicationInvitation.class);
        verify(invitationCrudService).update(invitationCaptor.capture());
        assertThat(invitationCaptor.getValue().roleName()).isEqualTo(UPDATED_ROLE_NAME);
    }

    @Test
    void should_throw_when_invitation_does_not_exist() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            cut.update(ORGANIZATION_ID, APPLICATION_ID, invitationId, new UpdateApplicationInvitation(UPDATED_ROLE_NAME))
        ).isInstanceOf(ApplicationInvitationNotFoundException.class);

        verifyNoInteractions(roleQueryService);
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_throw_when_invitation_belongs_to_another_application() {
        var invitationId = InvitationId.of(INVITATION_ID);
        givenInvitation(invitationId, anApplicationInvitation(INVITATION_ID, OTHER_APPLICATION_ID, "alice@example.com", CURRENT_ROLE_NAME));

        assertThatThrownBy(() ->
            cut.update(ORGANIZATION_ID, APPLICATION_ID, invitationId, new UpdateApplicationInvitation(UPDATED_ROLE_NAME))
        ).isInstanceOf(ApplicationInvitationNotFoundException.class);

        verifyNoInteractions(roleQueryService);
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_reject_blank_role() {
        var invitationId = InvitationId.of(INVITATION_ID);
        givenInvitation(invitationId, anApplicationInvitation(INVITATION_ID, APPLICATION_ID, "alice@example.com", CURRENT_ROLE_NAME));

        assertThatThrownBy(() -> cut.update(ORGANIZATION_ID, APPLICATION_ID, invitationId, new UpdateApplicationInvitation(" ")))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("role must not be blank");

        verifyNoInteractions(roleQueryService);
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_reject_unknown_role() {
        var invitationId = InvitationId.of(INVITATION_ID);
        givenInvitation(invitationId, anApplicationInvitation(INVITATION_ID, APPLICATION_ID, "alice@example.com", CURRENT_ROLE_NAME));
        when(roleQueryService.findApplicationRole(eq(UPDATED_ROLE_NAME), any(ReferenceContext.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            cut.update(ORGANIZATION_ID, APPLICATION_ID, invitationId, new UpdateApplicationInvitation(UPDATED_ROLE_NAME))
        ).isInstanceOf(RoleNotFoundException.class);

        verify(invitationCrudService, never()).update(any());
    }

    private void givenInvitation(InvitationId invitationId, ApplicationInvitation invitation) {
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.of(invitation));
    }

    private void givenExistingRole(String roleName) {
        when(roleQueryService.findApplicationRole(eq(roleName), any(ReferenceContext.class))).thenReturn(
            Optional.of(anApplicationRole("role-id", roleName))
        );
    }
}
