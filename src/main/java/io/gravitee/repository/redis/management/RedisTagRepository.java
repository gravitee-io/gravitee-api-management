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
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.redis.management.internal.TagRedisRepository;
import io.gravitee.repository.redis.management.model.RedisTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisTagRepository implements TagRepository {

    @Autowired
    private TagRedisRepository tagRedisRepository;

    @Override
    public Optional<Tag> findById(final String tagId) throws TechnicalException {
        final RedisTag redisTag = tagRedisRepository.findById(tagId);
        return Optional.ofNullable(convert(redisTag));
    }

    @Override
    public Tag create(final Tag tag) throws TechnicalException {
        final RedisTag redisTag = tagRedisRepository.saveOrUpdate(convert(tag));
        return convert(redisTag);
    }

    @Override
    public Tag update(final Tag tag) throws TechnicalException {
        if (tag == null || tag.getName() == null) {
            throw new IllegalStateException("Tag to update must have a name");
        }

        final RedisTag redisTag = tagRedisRepository.findById(tag.getId());

        if (redisTag == null) {
            throw new IllegalStateException(String.format("No tag found with name [%s]", tag.getId()));
        }

        final RedisTag redisTagUpdated = tagRedisRepository.saveOrUpdate(convert(tag));
        return convert(redisTagUpdated);
    }

    @Override
    public Set<Tag> findAll() throws TechnicalException {
        final Set<RedisTag> tags = tagRedisRepository.findAll();

        return tags.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(final String tagId) throws TechnicalException {
        tagRedisRepository.delete(tagId);
    }

    private Tag convert(final RedisTag redisTag) {
        final Tag tag = new Tag();
        tag.setId(redisTag.getId());
        tag.setName(redisTag.getName());
        tag.setDescription(redisTag.getDescription());
        return tag;
    }

    private RedisTag convert(final Tag tag) {
        final RedisTag redisTag = new RedisTag();
        redisTag.setId(tag.getId());
        redisTag.setName(tag.getName());
        redisTag.setDescription(tag.getDescription());
        return redisTag;
    }
}
