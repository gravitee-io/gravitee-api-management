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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.redis.management.internal.InvitationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisInvitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisInvitationRepository implements InvitationRepository {

    @Autowired
    private InvitationRedisRepository invitationRedisRepository;

    @Override
    public Optional<Invitation> findById(final String invitationId) throws TechnicalException {
        final RedisInvitation redisInvitation = invitationRedisRepository.findById(invitationId);
        return Optional.ofNullable(convert(redisInvitation));
    }

    @Override
    public Invitation create(final Invitation invitation) throws TechnicalException {
        final RedisInvitation redisInvitation = invitationRedisRepository.saveOrUpdate(convert(invitation));
        return convert(redisInvitation);
    }

    @Override
    public Invitation update(final Invitation invitation) throws TechnicalException {
        if (invitation == null || invitation.getEmail() == null) {
            throw new IllegalStateException("Invitation to update must have an email");
        }

        final RedisInvitation redisInvitation = invitationRedisRepository.findById(invitation.getId());

        if (redisInvitation == null) {
            throw new IllegalStateException(String.format("No invitation found with id [%s]", invitation.getId()));
        }

        final RedisInvitation redisInvitationUpdated = invitationRedisRepository.saveOrUpdate(convert(invitation));
        return convert(redisInvitationUpdated);
    }

    @Override
    public Set<Invitation> findAll() {
        final Set<RedisInvitation> invitations = invitationRedisRepository.findAll();

        return invitations.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Invitation> findByReference(String referenceType, String referenceId) {
        final List<RedisInvitation> invitations = invitationRedisRepository.findByReference(referenceType, referenceId);
        return invitations.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final String invitationId) throws TechnicalException {
        invitationRedisRepository.delete(invitationId);
    }

    private Invitation convert(final RedisInvitation redisInvitation) {
        final Invitation invitation = new Invitation();
        invitation.setId(redisInvitation.getId());
        invitation.setReferenceType(redisInvitation.getReferenceType());
        invitation.setReferenceId(redisInvitation.getReferenceId());
        invitation.setEmail(redisInvitation.getEmail());
        invitation.setApiRole(redisInvitation.getApiRole());
        invitation.setApplicationRole(redisInvitation.getApplicationRole());
        invitation.setCreatedAt(redisInvitation.getCreatedAt());
        invitation.setUpdatedAt(redisInvitation.getUpdatedAt());
        return invitation;
    }

    private RedisInvitation convert(final Invitation invitation) {
        final RedisInvitation redisInvitation = new RedisInvitation();
        redisInvitation.setId(invitation.getId());
        redisInvitation.setReferenceType(invitation.getReferenceType());
        redisInvitation.setReferenceId(invitation.getReferenceId());
        redisInvitation.setEmail(invitation.getEmail());
        redisInvitation.setApiRole(invitation.getApiRole());
        redisInvitation.setApplicationRole(invitation.getApplicationRole());
        redisInvitation.setCreatedAt(invitation.getCreatedAt());
        redisInvitation.setUpdatedAt(invitation.getUpdatedAt());
        return redisInvitation;
    }
}
