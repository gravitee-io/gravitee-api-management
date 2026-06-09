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
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.exception.ApplicationInvitationNotFoundException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import jakarta.annotation.Nonnull;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ResendApplicationInvitationDomainService {

    private final InvitationCrudService invitationCrudService;
    private final ApplicationInvitationNotificationDomainService applicationInvitationNotificationDomainService;

    public ApplicationInvitation resend(
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String applicationId,
        @Nonnull InvitationId invitationId,
        URI confirmationPageUrl
    ) {
        var invitation = invitationCrudService
            .findApplicationInvitationById(invitationId)
            .filter(applicationInvitation -> applicationId.equals(applicationInvitation.applicationId()))
            .orElseThrow(() -> new ApplicationInvitationNotFoundException(invitationId.toString()));

        invitation.markResendAttempted();

        var resentInvitation = invitationCrudService.update(invitation);
        applicationInvitationNotificationDomainService.dispatchAsync(
            organizationId,
            environmentId,
            applicationId,
            List.of(resentInvitation),
            confirmationPageUrl
        );

        return resentInvitation;
    }
}
