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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.invitation.model.CreateApplicationInvitations;
import io.gravitee.rest.api.portal.rest.model.InvitationCreateInput;
import io.gravitee.rest.api.portal.rest.model.InvitationRecipientInput;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationInvitationsCreateInputMapper {
    ApplicationInvitationsCreateInputMapper INSTANCE = Mappers.getMapper(ApplicationInvitationsCreateInputMapper.class);

    default CreateApplicationInvitations toCreateApplicationInvitations(InvitationCreateInput input) {
        return new CreateApplicationInvitations(
            toRecipientEmails(input.getRecipients()),
            normalizeRoleName(input.getRole()),
            input.getNotify() == null || input.getNotify(),
            input.getConfirmationPageUrl()
        );
    }

    private Set<String> toRecipientEmails(List<InvitationRecipientInput> recipients) {
        if (recipients == null) {
            return Set.of();
        }

        var emails = new LinkedHashSet<String>();
        recipients.forEach(recipient -> emails.add(normalizeEmail(recipient == null ? null : recipient.getEmail())));
        return emails;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRoleName(String roleName) {
        return roleName == null ? null : roleName.trim();
    }
}
