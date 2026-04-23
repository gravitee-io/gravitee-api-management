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

import static io.gravitee.rest.api.service.common.ReferenceContext.Type.ORGANIZATION;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import io.gravitee.apim.core.invitation.model.InvitationReferenceType;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CreateApplicationInvitationsDomainService {

    private final InvitationQueryService invitationQueryService;
    private final InvitationCrudService invitationCrudService;
    private final RoleQueryService roleQueryService;

    public List<ApplicationInvitationItem> create(
        String organizationId,
        String applicationId,
        Set<String> recipientEmails,
        String role,
        boolean notifyUsers
    ) {
        if (notifyUsers) {
            throw new UnsupportedOperationException("Application invitation notifications are not implemented yet.");
        }

        var sanitizedRole = validateAndSanitizeRole(organizationId, role);
        var emails = validateAndSanitizeRecipients(recipientEmails);

        validateNoPendingInvitation(applicationId, emails);
        return invitationCrudService.createApplicationInvitations(applicationId, sanitizedRole, emails);
    }

    private String validateAndSanitizeRole(String organizationId, String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationDomainException("Application invitation role must not be blank.");
        }

        var sanitizedRole = role.trim();
        var referenceContext = new ReferenceContext(ORGANIZATION, organizationId);
        roleQueryService
            .findApplicationRole(sanitizedRole, referenceContext)
            .orElseThrow(() -> new RoleNotFoundException(sanitizedRole, referenceContext));

        return sanitizedRole;
    }

    private List<String> validateAndSanitizeRecipients(Set<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new ValidationDomainException("At least one application invitation recipient is required.");
        }

        var sanitizedEmails = new ArrayList<String>();
        for (var recipient : recipients) {
            var email = sanitizeEmail(recipient);
            if (!isValidEmail(email)) {
                throw new ValidationDomainException("Application invitation email is invalid: " + email);
            }
            sanitizedEmails.add(email);
        }

        return sanitizedEmails;
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

    private void validateNoPendingInvitation(String applicationId, List<String> emails) {
        var requestedEmails = Set.copyOf(emails);
        invitationQueryService
            .findByReference(InvitationReferenceType.APPLICATION, applicationId)
            .stream()
            .map(ApplicationInvitationItem::email)
            .map(this::sanitizeEmail)
            .filter(requestedEmails::contains)
            .findFirst()
            .ifPresent(email -> {
                throw new ConflictDomainException("A pending application invitation already exists for email [" + email + "].", email);
            });
    }

    private String sanitizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
