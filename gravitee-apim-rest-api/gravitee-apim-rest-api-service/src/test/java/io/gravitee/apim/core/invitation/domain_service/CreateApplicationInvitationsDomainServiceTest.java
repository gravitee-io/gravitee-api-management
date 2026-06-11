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
import static fixtures.core.model.BaseUserEntityFixtures.aBaseUserEntity;
import static fixtures.core.model.RoleFixtures.anApplicationRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.MembershipAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
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

    @Mock
    private UserCrudService userCrudService;

    @Mock
    private MembershipDomainService membershipDomainService;

    private CreateApplicationInvitationsDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new CreateApplicationInvitationsDomainService(
            invitationQueryService,
            invitationCrudService,
            new ValidateApplicationInvitationRoleDomainService(roleQueryService),
            applicationInvitationNotificationDomainService,
            userCrudService,
            membershipDomainService
        );
    }

    @Test
    void should_create_invitations_with_normalized_input_when_notify_is_false() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        givenNoExistingUsers("alice@example.com", "bob@example.com");
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
        givenNoExistingUsers("alice@example.com");
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
    void should_add_existing_user_as_application_member_without_creating_invitation() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(userCrudService.findBaseUsersByEmail(ORGANIZATION_ID, "alice@example.com")).thenReturn(
            List.of(aBaseUserEntity("user-id", ORGANIZATION_ID, "alice@example.com"))
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

        assertThat(result).isEmpty();
        verify(membershipDomainService).createNewMembership(
            new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
            MembershipReferenceType.APPLICATION,
            APPLICATION_ID,
            "user-id",
            null,
            ROLE_NAME
        );
        verifyNoInteractions(invitationCrudService, applicationInvitationNotificationDomainService);
    }

    @Test
    void should_create_invitation_for_unknown_email_and_add_member_for_existing_user_in_same_request() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(userCrudService.findBaseUsersByEmail(ORGANIZATION_ID, "alice@example.com")).thenReturn(
            List.of(aBaseUserEntity("user-id", ORGANIZATION_ID, "alice@example.com"))
        );
        givenNoExistingUsers("bob@example.com");
        when(invitationCrudService.create(any(ApplicationInvitation.class))).thenReturn(
            anApplicationInvitation(INVITATION_ID_1, "bob@example.com")
        );

        var result = cut.create(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            APPLICATION_ID,
            recipients("alice@example.com", "bob@example.com"),
            ROLE_NAME,
            true,
            CONFIRMATION_PAGE_URL
        );

        assertThat(result).extracting(ApplicationInvitation::email).containsExactly("bob@example.com");
        var inOrder = inOrder(membershipDomainService, invitationCrudService);
        inOrder
            .verify(membershipDomainService)
            .createNewMembership(
                new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
                MembershipReferenceType.APPLICATION,
                APPLICATION_ID,
                "user-id",
                null,
                ROLE_NAME
            );
        inOrder.verify(invitationCrudService).create(any(ApplicationInvitation.class));
        verify(applicationInvitationNotificationDomainService).dispatchAsync(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            APPLICATION_ID,
            result,
            CONFIRMATION_PAGE_URL
        );
    }

    @Test
    void should_reject_primary_owner_role_when_existing_user_would_be_added_as_member() {
        givenExistingRole("PRIMARY_OWNER");
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(userCrudService.findBaseUsersByEmail(ORGANIZATION_ID, "alice@example.com")).thenReturn(
            List.of(aBaseUserEntity("user-id", ORGANIZATION_ID, "alice@example.com"))
        );

        var throwable = catchThrowable(() ->
            cut.create(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, Set.of("alice@example.com"), "PRIMARY_OWNER", false, null)
        );

        assertThat(throwable).isInstanceOf(SinglePrimaryOwnerException.class);
        verifyNoInteractions(membershipDomainService, invitationCrudService, applicationInvitationNotificationDomainService);
    }

    @Test
    void should_keep_current_primary_owner_pending_invitation_behavior_when_all_recipients_are_unknown() {
        givenExistingRole("PRIMARY_OWNER");
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        givenNoExistingUsers("alice@example.com");
        when(invitationCrudService.create(any(ApplicationInvitation.class))).thenReturn(
            anApplicationInvitation(INVITATION_ID_1, "alice@example.com")
        );

        var result = cut.create(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, Set.of("alice@example.com"), "PRIMARY_OWNER", false, null);

        assertThat(result).extracting(ApplicationInvitation::email).containsExactly("alice@example.com");
        verifyNoInteractions(membershipDomainService, applicationInvitationNotificationDomainService);
    }

    @Test
    void should_propagate_membership_already_exists_when_existing_user_is_already_application_member() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(userCrudService.findBaseUsersByEmail(ORGANIZATION_ID, "alice@example.com")).thenReturn(
            List.of(aBaseUserEntity("user-id", ORGANIZATION_ID, "alice@example.com"))
        );
        givenNoExistingUsers("bob@example.com");
        var membershipAlreadyExists = new MembershipAlreadyExistsException(
            "user-id",
            MembershipMemberType.USER,
            APPLICATION_ID,
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
        );
        when(
            membershipDomainService.createNewMembership(
                new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
                MembershipReferenceType.APPLICATION,
                APPLICATION_ID,
                "user-id",
                null,
                ROLE_NAME
            )
        ).thenThrow(membershipAlreadyExists);

        var throwable = catchThrowable(() ->
            cut.create(
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                APPLICATION_ID,
                recipients("alice@example.com", "bob@example.com"),
                ROLE_NAME,
                true,
                CONFIRMATION_PAGE_URL
            )
        );

        assertThat(throwable).isSameAs(membershipAlreadyExists);
        verifyNoInteractions(invitationCrudService, applicationInvitationNotificationDomainService);
    }

    @Test
    void should_reject_existing_user_email_when_email_matches_multiple_users_before_any_write() {
        givenExistingRole();
        when(invitationQueryService.findByReference(InvitationReference.application(APPLICATION_ID))).thenReturn(List.of());
        when(userCrudService.findBaseUsersByEmail(ORGANIZATION_ID, "alice@example.com")).thenReturn(
            List.of(
                aBaseUserEntity("user-id-1", ORGANIZATION_ID, "alice@example.com"),
                aBaseUserEntity("user-id-2", ORGANIZATION_ID, "alice@example.com")
            )
        );

        var throwable = catchThrowable(() ->
            cut.create(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, Set.of("alice@example.com"), ROLE_NAME, false, null)
        );

        assertThat(throwable).isInstanceOf(ConflictDomainException.class).hasMessageContaining("matches multiple users");
        verifyNoInteractions(membershipDomainService, invitationCrudService, applicationInvitationNotificationDomainService);
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
        verifyNoInteractions(userCrudService, membershipDomainService);
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
        givenExistingRole(ROLE_NAME);
    }

    private void givenExistingRole(String roleName) {
        when(roleQueryService.findApplicationRole(eq(roleName), any(ReferenceContext.class))).thenReturn(
            Optional.of(anApplicationRole("role-id", roleName))
        );
    }

    private void givenNoExistingUsers(String... emails) {
        for (var email : emails) {
            when(userCrudService.findBaseUsersByEmail(ORGANIZATION_ID, email)).thenReturn(List.of());
        }
    }

    private Set<String> recipients(String... emails) {
        return new LinkedHashSet<>(List.of(emails));
    }
}
