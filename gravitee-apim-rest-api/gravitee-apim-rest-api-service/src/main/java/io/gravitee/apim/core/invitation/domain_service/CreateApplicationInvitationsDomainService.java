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

import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationReference;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import jakarta.annotation.Nonnull;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CreateApplicationInvitationsDomainService {

    private final InvitationQueryService invitationQueryService;
    private final InvitationCrudService invitationCrudService;
    private final ValidateApplicationInvitationRoleDomainService validateApplicationInvitationRoleDomainService;
    private final ApplicationInvitationNotificationDomainService applicationInvitationNotificationDomainService;
    private final UserCrudService userCrudService;
    private final MembershipQueryService membershipQueryService;
    private final MembershipDomainService membershipDomainService;

    public List<ApplicationInvitation> create(
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String applicationId,
        @Nonnull Set<String> recipientEmails,
        @Nonnull String roleName,
        boolean notifyUsers,
        URI confirmationPageUrl
    ) {
        validateApplicationInvitationRoleDomainService.validate(organizationId, roleName);
        var emails = validateRecipients(recipientEmails);

        validateNoPendingInvitation(applicationId, emails);
        var resolvedRecipients = resolveRecipients(organizationId, emails);
        validateDirectMemberAdditionRole(roleName, resolvedRecipients);
        addExistingUsersAsApplicationMembers(organizationId, environmentId, applicationId, roleName, resolvedRecipients);

        var createdInvitations = resolvedRecipients
            .invitationRecipients()
            .stream()
            .map(email -> ApplicationInvitation.create(applicationId, email, roleName))
            .map(invitationCrudService::create)
            .toList();

        if (notifyUsers && !createdInvitations.isEmpty()) {
            applicationInvitationNotificationDomainService.dispatchAsync(
                organizationId,
                environmentId,
                applicationId,
                createdInvitations,
                confirmationPageUrl
            );
        }

        return createdInvitations;
    }

    private Set<String> validateRecipients(Set<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new ValidationDomainException("At least one application invitation recipient is required.");
        }

        for (var email : recipients) {
            if (email == null || email.isBlank()) {
                throw new ValidationDomainException("Application invitation email must not be blank.");
            }
            if (!isValidEmail(email)) {
                throw new ValidationDomainException("Application invitation email is invalid: " + email);
            }
        }

        return recipients;
    }

    private ResolvedRecipients resolveRecipients(String organizationId, Set<String> emails) {
        var existingUserRecipients = new ArrayList<ExistingUserRecipient>();
        var invitationRecipients = new ArrayList<String>();

        emails.forEach(email -> {
            var users = userCrudService.findBaseUsersByEmail(organizationId, email.toLowerCase(Locale.ROOT));
            if (users.size() > 1) {
                throw new ConflictDomainException("Application invitation email [" + email + "] matches multiple users.", email);
            }
            if (users.size() == 1) {
                existingUserRecipients.add(new ExistingUserRecipient(users.getFirst().getId()));
            } else {
                invitationRecipients.add(email);
            }
        });

        return new ResolvedRecipients(existingUserRecipients, invitationRecipients);
    }

    private void validateDirectMemberAdditionRole(String roleName, ResolvedRecipients resolvedRecipients) {
        if (!resolvedRecipients.existingUserRecipients().isEmpty() && PRIMARY_OWNER.name().equals(roleName)) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
    }

    private void addExistingUsersAsApplicationMembers(
        String organizationId,
        String environmentId,
        String applicationId,
        String roleName,
        ResolvedRecipients resolvedRecipients
    ) {
        var executionContext = new ExecutionContext(organizationId, environmentId);
        filterAlreadyApplicationMembers(applicationId, resolvedRecipients.existingUserRecipients()).forEach(recipient ->
            membershipDomainService.createNewMembership(
                executionContext,
                MembershipReferenceType.APPLICATION,
                applicationId,
                recipient.userId(),
                null,
                roleName
            )
        );
    }

    private List<ExistingUserRecipient> filterAlreadyApplicationMembers(String applicationId, List<ExistingUserRecipient> recipients) {
        if (recipients.isEmpty()) {
            return recipients;
        }

        var userIds = recipients.stream().map(ExistingUserRecipient::userId).collect(Collectors.toSet());
        var existingApplicationMemberIds = membershipQueryService
            .findByMemberIdsAndMemberTypeAndReferenceType(userIds, Membership.Type.USER, Membership.ReferenceType.APPLICATION)
            .stream()
            .filter(membership -> applicationId.equals(membership.getReferenceId()))
            .map(Membership::getMemberId)
            .collect(Collectors.toSet());

        return recipients
            .stream()
            .filter(recipient -> !existingApplicationMemberIds.contains(recipient.userId()))
            .toList();
    }

    private boolean isValidEmail(String email) {
        try {
            var internetAddress = new InternetAddress(email);
            internetAddress.validate();
            return email.equals(internetAddress.getAddress());
        } catch (AddressException e) {
            return false;
        }
    }

    private void validateNoPendingInvitation(String applicationId, Set<String> emails) {
        invitationQueryService
            .findByReference(InvitationReference.application(applicationId))
            .stream()
            .map(ApplicationInvitation::email)
            .filter(emails::contains)
            .findFirst()
            .ifPresent(email -> {
                throw new ConflictDomainException("A pending application invitation already exists for email [" + email + "].", email);
            });
    }

    private record ExistingUserRecipient(String userId) {}

    private record ResolvedRecipients(List<ExistingUserRecipient> existingUserRecipients, List<String> invitationRecipients) {}
}
