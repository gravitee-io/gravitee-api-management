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
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.Invitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.apim.infra.adapter.InvitationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.rest.api.service.impl.TransactionalService;
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
    public ApplicationInvitation create(ApplicationInvitation invitation) {
        try {
            return InvitationAdapter.INSTANCE.toApplicationInvitation(
                invitationRepository.create(InvitationAdapter.INSTANCE.toRepository(invitation))
            );
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to create application invitation", e);
        }
    }

    @Override
    public List<Invitation> findByEmail(String email) {
        try {
            return invitationRepository.findByEmail(email).stream().map(InvitationAdapter.INSTANCE::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find invitations by email", e);
        }
    }

    @Override
    public void delete(InvitationId invitationId) {
        try {
            invitationRepository.delete(invitationId.toString());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to delete invitation", e);
        }
    }
}
