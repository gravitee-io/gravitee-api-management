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

import com.fasterxml.jackson.core.type.TypeReference;
import io.gravitee.repository.redis.management.internal.MemberRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMembership;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class MemberRedisRepositoryImpl extends AbstractRedisRepository implements MemberRedisRepository {

    private final static String REDIS_KEY = "members";

    @Override
    public Set<RedisMembership> getMemberships(String username) {
        Object membershipsObj = redisTemplate.opsForHash().get(REDIS_KEY, username);

        Set<RedisMembership> memberships = convert(membershipsObj, new TypeReference<Set<RedisMembership>>() {});

        if (memberships == null) {
            memberships = new HashSet<>();
        }

        return memberships;
    }

    @Override
    public void save(String username, Set<RedisMembership> memberships) {
        redisTemplate.opsForSet().add(REDIS_KEY + ":username:" + username, memberships);
    }
}
