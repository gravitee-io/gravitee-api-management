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
package io.gravitee.apim.infra.crud_service.invitation;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import io.gravitee.apim.infra.query_service.invitation.ApplicationInvitationItemMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.impl.TransactionalService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class InvitationCrudServiceImpl extends TransactionalService implements InvitationCrudService {

    private final InvitationRepository invitationRepository;

    public InvitationCrudServiceImpl(@Lazy InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    public List<ApplicationInvitationItem> createApplicationInvitations(String applicationId, String role, List<String> emails) {
        try {
            var createdInvitations = new ArrayList<ApplicationInvitationItem>();
            for (var email : emails) {
                createdInvitations.add(createApplicationInvitation(applicationId, role, email));
            }
            return createdInvitations;
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to create application invitations", e);
        }
    }

    private ApplicationInvitationItem createApplicationInvitation(String applicationId, String role, String email)
        throws TechnicalException {
        var invitation = new Invitation();
        invitation.setId(UuidString.generateRandom());
        invitation.setReferenceType(InvitationReferenceType.APPLICATION.name());
        invitation.setReferenceId(applicationId);
        invitation.setEmail(email);
        invitation.setApiRole(null);
        invitation.setApplicationRole(role);
        invitation.setCreatedAt(new Date());

        return ApplicationInvitationItemMapper.INSTANCE.toApplicationInvitationItem(invitationRepository.create(invitation));
    }
}
