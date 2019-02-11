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
package io.gravitee.repository.redis.management.internal.impl;

import io.gravitee.repository.redis.management.internal.InvitationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisInvitation;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class InvitationRedisRepositoryImpl extends AbstractRedisRepository implements InvitationRedisRepository {

    private final static String REDIS_KEY = "invitation";

    @Override
    public Set<RedisInvitation> findAll() {
        final Map<Object, Object> invitations = redisTemplate.opsForHash().entries(REDIS_KEY);

        return invitations.values()
                .stream()
                .map(object -> convert(object, RedisInvitation.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisInvitation findById(final String invitationId) {
        Object invitation = redisTemplate.opsForHash().get(REDIS_KEY, invitationId);
        if (invitation == null) {
            return null;
        }

        return convert(invitation, RedisInvitation.class);
    }

    @Override
    public RedisInvitation saveOrUpdate(final RedisInvitation invitation) {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            final String refKey = getRefKey(invitation.getReferenceType(), invitation.getReferenceId());
            redisTemplate.opsForHash().put(REDIS_KEY, invitation.getId(), invitation);
            redisTemplate.opsForSet().add(refKey, invitation.getId());
            return null;
        });
        return invitation;
    }

    @Override
    public void delete(final String invitation) {
        redisTemplate.opsForHash().delete(REDIS_KEY, invitation);
    }

    @Override
    public List<RedisInvitation> findByReference(String referenceType, String referenceId) {
        final Set<Object> keys = redisTemplate.opsForSet().members(getRefKey(referenceType, referenceId));
        final List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(invitation -> convert(invitation, RedisInvitation.class))
                .collect(toList());
    }

    private String getRefKey(String referenceType, String referenceId) {
        return referenceType + ':' + referenceId;
    }
}
