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

import io.gravitee.repository.redis.management.internal.TagRedisRepository;
import io.gravitee.repository.redis.management.model.RedisTag;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TagRedisRepositoryImpl extends AbstractRedisRepository implements TagRedisRepository {

    private final static String REDIS_KEY = "tag";

    @Override
    public Set<RedisTag> findAll() {
        final Map<Object, Object> tags = redisTemplate.opsForHash().entries(REDIS_KEY);

        return tags.values()
                .stream()
                .map(object -> convert(object, RedisTag.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisTag findById(final String tagId) {
        Object tag = redisTemplate.opsForHash().get(REDIS_KEY, tagId);
        if (tag == null) {
            return null;
        }

        return convert(tag, RedisTag.class);
    }

    @Override
    public RedisTag saveOrUpdate(final RedisTag tag) {
        redisTemplate.opsForHash().put(REDIS_KEY, tag.getId(), tag);
        return tag;
    }

    @Override
    public void delete(final String tag) {
        redisTemplate.opsForHash().delete(REDIS_KEY, tag);
    }
}
