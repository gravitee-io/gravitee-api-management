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

import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.redis.management.internal.MembershipRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMembership;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MembershipRedisRepositoryImpl extends AbstractRedisRepository implements MembershipRedisRepository {

    private final static String REDIS_KEY = "membership";

    @Override
    public RedisMembership findById(String userId, String referenceType, String referenceId) {
        Object membershipsObj = redisTemplate.opsForHash().get(REDIS_KEY, getMembershipKey(userId, referenceType, referenceId));
        if (membershipsObj == null) {
            return null;
        }

        return convert(membershipsObj, RedisMembership.class);
    }

    @Override
    public RedisMembership saveOrUpdate(RedisMembership membership) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                redisTemplate.opsForHash().put(REDIS_KEY, getMembershipKey(membership), membership);
                redisTemplate.opsForSet().add(getMembershipByReferenceKey(membership), getMembershipKey(membership));
                redisTemplate.opsForSet().add(getMembershipByUserKey(membership), getMembershipKey(membership));
                return null;
            }
        });
        return membership;
    }

    @Override
    public void delete(RedisMembership membership) {
        redisTemplate.executePipelined( new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                redisTemplate.opsForHash().delete(REDIS_KEY, getMembershipKey(membership));
                redisTemplate.opsForSet().remove(getMembershipByReferenceKey(membership), getMembershipKey(membership));
                redisTemplate.opsForSet().remove(getMembershipByUserKey(membership), getMembershipKey(membership));
                return null;
            }
        });
    }

    public Set<RedisMembership> findByReferences(String referenceType, List<String> referenceIds) {
        Set<RedisMembership> memberships = new HashSet<>();
        referenceIds.forEach(id -> {
            Set<Object> keys = redisTemplate.opsForSet().members(getMembershipByReferenceKey(referenceType, id));
            List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
            memberships.addAll(
                    values.stream()
                            .filter(Objects::nonNull)
                            .map(membership -> convert(membership, RedisMembership.class))
                            .collect(Collectors.toSet()));
        });
        return memberships;

    }

    public Set<RedisMembership> findByUserAndReferenceType(String userId, String referenceType) {
        Set<Object> keys = redisTemplate.opsForSet().members(getMembershipByUserKey(userId, referenceType));
        List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(membership -> convert(membership, RedisMembership.class))
                .collect(Collectors.toSet());
    }


    private String getMembershipKey(RedisMembership membership) {
        return getMembershipKey(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
    }
    private String getMembershipKey(String userId, String referenceType, String referenceId) {
        return userId + ":" + referenceType + ":" + referenceId;
    }

    private String getMembershipByReferenceKey(RedisMembership membership) {
        return getMembershipByReferenceKey(membership.getReferenceType(), membership.getReferenceId());
    }

    private String getMembershipByReferenceKey(String referenceType, String referenceId) {
        return REDIS_KEY + ":" + referenceType + ":" + referenceId;
    }

    private String getMembershipByUserKey(RedisMembership membership) {
        return getMembershipByUserKey(membership.getUserId(), membership.getReferenceType());
    }

    private String getMembershipByUserKey(String userId, String referenceType) {
        return REDIS_KEY + ":user:" + userId + ":" + referenceType;
    }
}
