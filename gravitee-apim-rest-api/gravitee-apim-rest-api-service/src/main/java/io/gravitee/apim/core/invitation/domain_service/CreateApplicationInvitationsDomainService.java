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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationReference;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import jakarta.annotation.Nonnull;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CreateApplicationInvitationsDomainService {

    private final InvitationQueryService invitationQueryService;
    private final InvitationCrudService invitationCrudService;
    private final ValidateApplicationInvitationRoleDomainService validateApplicationInvitationRoleDomainService;

    public List<ApplicationInvitation> create(
        @Nonnull String organizationId,
        @Nonnull String applicationId,
        @Nonnull Set<String> recipientEmails,
        @Nonnull String roleName,
        boolean notifyUsers
    ) {
        if (notifyUsers) {
            throw new UnsupportedOperationException("Application invitation notifications are not implemented yet.");
        }

        validateApplicationInvitationRoleDomainService.validate(organizationId, roleName);
        var emails = validateRecipients(recipientEmails);

        validateNoPendingInvitation(applicationId, emails);
        return emails
            .stream()
            .map(email -> ApplicationInvitation.create(applicationId, email, roleName))
            .map(invitationCrudService::create)
            .toList();
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
}
