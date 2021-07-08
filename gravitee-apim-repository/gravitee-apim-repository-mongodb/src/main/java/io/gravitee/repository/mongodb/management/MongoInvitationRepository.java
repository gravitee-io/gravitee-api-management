/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.mongodb.management.internal.api.InvitationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.InvitationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoInvitationRepository implements InvitationRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoInvitationRepository.class);

    @Autowired
    private InvitationMongoRepository internalInvitationRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Invitation> findById(String invitationId) throws TechnicalException {
        LOGGER.debug("Find invitation by ID [{}]", invitationId);

        final InvitationMongo invitation = internalInvitationRepo.findById(invitationId).orElse(null);

        LOGGER.debug("Find invitation by ID [{}] - Done", invitationId);
        return Optional.ofNullable(mapper.map(invitation, Invitation.class));
    }

    @Override
    public Invitation create(Invitation invitation) throws TechnicalException {
        LOGGER.debug("Create invitation [{}]", invitation.getEmail());

        InvitationMongo invitationMongo = mapper.map(invitation, InvitationMongo.class);
        InvitationMongo createdInvitationMongo = internalInvitationRepo.insert(invitationMongo);

        Invitation res = mapper.map(createdInvitationMongo, Invitation.class);

        LOGGER.debug("Create invitation [{}] - Done", invitation.getEmail());

        return res;
    }

    @Override
    public Invitation update(Invitation invitation) throws TechnicalException {
        if (invitation == null || invitation.getEmail() == null) {
            throw new IllegalStateException("Invitation to update must have an email");
        }

        final InvitationMongo invitationMongo = internalInvitationRepo.findById(invitation.getId()).orElse(null);

        if (invitationMongo == null) {
            throw new IllegalStateException(String.format("No invitation found with id [%s]", invitation.getId()));
        }

        try {
            //Update
            invitationMongo.setReferenceType(invitation.getReferenceType());
            invitationMongo.setReferenceId(invitation.getReferenceId());
            invitationMongo.setEmail(invitation.getEmail());
            invitationMongo.setApiRole(invitation.getApiRole());
            invitationMongo.setApplicationRole(invitation.getApplicationRole());
            invitationMongo.setCreatedAt(invitation.getCreatedAt());
            invitationMongo.setUpdatedAt(invitation.getUpdatedAt());

            InvitationMongo invitationMongoUpdated = internalInvitationRepo.save(invitationMongo);
            return mapper.map(invitationMongoUpdated, Invitation.class);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating invitation", e);
            throw new TechnicalException("An error occured when updating invitation");
        }
    }

    @Override
    public void delete(String invitationId) throws TechnicalException {
        try {
            internalInvitationRepo.deleteById(invitationId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting invitation [{}]", invitationId, e);
            throw new TechnicalException("An error occured when deleting invitation");
        }
    }

    @Override
    public Set<Invitation> findAll() {
        final List<InvitationMongo> invitations = internalInvitationRepo.findAll();
        return invitations.stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public List<Invitation> findByReference(String referenceType, String referenceId) {
        LOGGER.debug("Find invitation by reference '{}' / '{}'", referenceType, referenceId);

        final List<InvitationMongo> invitations = internalInvitationRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);

        LOGGER.debug("Find invitation by reference '{}' / '{}' done", referenceType, referenceId);
        return invitations.stream().map(this::map).collect(Collectors.toList());
    }

    private Invitation map(final InvitationMongo invitationMongo) {
        if (invitationMongo == null) {
            return null;
        }
        final Invitation invitation = new Invitation();
        invitation.setId(invitationMongo.getId());
        invitation.setReferenceType(invitationMongo.getReferenceType());
        invitation.setReferenceId(invitationMongo.getReferenceId());
        invitation.setApiRole(invitationMongo.getApiRole());
        invitation.setApplicationRole(invitationMongo.getApplicationRole());
        invitation.setEmail(invitationMongo.getEmail());
        invitation.setCreatedAt(invitationMongo.getCreatedAt());
        invitation.setUpdatedAt(invitationMongo.getUpdatedAt());
        return invitation;
    }
}
