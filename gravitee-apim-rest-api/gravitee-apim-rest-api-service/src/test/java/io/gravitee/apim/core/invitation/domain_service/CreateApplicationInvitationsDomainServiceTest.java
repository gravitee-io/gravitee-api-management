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

import static fixtures.core.model.ApplicationInvitationItemFixtures.anApplicationInvitationItem;
import static fixtures.core.model.RoleFixtures.anApplicationRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import io.gravitee.apim.core.invitation.model.InvitationReferenceType;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateApplicationInvitationsDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String ROLE = "USER";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";
    private static final String EXISTING_INVITATION_ID = "00000000-0000-0000-0000-000000000003";

    @Mock
    private InvitationQueryService invitationQueryService;

    @Mock
    private InvitationCrudService invitationCrudService;

    @Mock
    private RoleQueryService roleQueryService;

    private CreateApplicationInvitationsDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new CreateApplicationInvitationsDomainService(invitationQueryService, invitationCrudService, roleQueryService);
    }

    @Test
    void should_create_invitations_with_sanitized_input_when_notify_is_false() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(List.of());
        when(
            invitationCrudService.createApplicationInvitations(APPLICATION_ID, ROLE, List.of("alice@example.com", "bob@example.com"))
        ).thenReturn(
            List.of(
                anApplicationInvitationItem(INVITATION_ID_1, "alice@example.com"),
                anApplicationInvitationItem(INVITATION_ID_2, "bob@example.com")
            )
        );

        var result = cut.create(ORGANIZATION_ID, APPLICATION_ID, recipients(" Alice@Example.com ", "BOB@example.com"), " USER ", false);

        assertThat(result).extracting(ApplicationInvitationItem::email).containsExactly("alice@example.com", "bob@example.com");
        verify(invitationCrudService).createApplicationInvitations(APPLICATION_ID, ROLE, List.of("alice@example.com", "bob@example.com"));
    }

    @Test
    void should_reject_invalid_email_before_creating_any_invitation() {
        givenExistingRole();

        var throwable = catchThrowable(() -> cut.create(ORGANIZATION_ID, APPLICATION_ID, Set.of("not-an-email"), ROLE, false));

        assertThat(throwable).isInstanceOf(ValidationDomainException.class).hasMessageContaining("email is invalid");
        verifyNoInteractions(invitationCrudService);
        verify(invitationQueryService, never()).findByReference(any(), any());
    }

    @Test
    void should_reject_notify_true_before_creating_any_invitation() {
        var throwable = catchThrowable(() -> cut.create(ORGANIZATION_ID, APPLICATION_ID, Set.of("alice@example.com"), ROLE, true));

        assertThat(throwable).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("not implemented yet");
        verifyNoInteractions(roleQueryService, invitationQueryService, invitationCrudService);
    }

    @Test
    void should_reject_pending_invitation_conflict_before_creating_any_invitation() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(
            List.of(anApplicationInvitationItem(EXISTING_INVITATION_ID, "alice@example.com"))
        );

        var throwable = catchThrowable(() -> cut.create(ORGANIZATION_ID, APPLICATION_ID, Set.of("ALICE@example.com"), ROLE, false));

        assertThat(throwable).isInstanceOf(ConflictDomainException.class).hasMessageContaining("pending application invitation");
        verifyNoInteractions(invitationCrudService);
    }

    @Test
    void should_reject_unknown_application_role() {
        when(roleQueryService.findApplicationRole(eq(ROLE), any(ReferenceContext.class))).thenReturn(Optional.empty());

        var throwable = catchThrowable(() -> cut.create(ORGANIZATION_ID, APPLICATION_ID, Set.of("alice@example.com"), ROLE, false));

        assertThat(throwable).isInstanceOf(RoleNotFoundException.class);
        verifyNoInteractions(invitationQueryService, invitationCrudService);
    }

    private void givenExistingRole() {
        when(roleQueryService.findApplicationRole(eq(ROLE), any(ReferenceContext.class))).thenReturn(
            Optional.of(anApplicationRole("role-id", ROLE))
        );
    }

    private Set<String> recipients(String... emails) {
        return new LinkedHashSet<>(List.of(emails));
    }
}
