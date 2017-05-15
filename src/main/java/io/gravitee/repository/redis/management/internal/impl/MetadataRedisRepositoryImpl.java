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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.redis.management.internal.MetadataRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMetadata;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MetadataRedisRepositoryImpl extends AbstractRedisRepository implements MetadataRedisRepository {

    private final static String REDIS_KEY = "metadata";

    @Override
    public RedisMetadata create(final RedisMetadata metadata) throws TechnicalException {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            final String metadataKey =
                    getMetadataKey(metadata.getKey(), metadata.getReferenceType(), metadata.getReferenceId());
            redisTemplate.opsForHash().putIfAbsent(REDIS_KEY, metadataKey, metadata);
            redisTemplate.opsForSet().add(getMetadataKey(metadata.getKey(), metadata.getReferenceType(), ""), metadataKey);
            redisTemplate.opsForSet().add(getMetadataKey("", metadata.getReferenceType(), ""), metadataKey);
            redisTemplate.opsForSet().add(getMetadataKey("", metadata.getReferenceType(), metadata.getReferenceId()), metadataKey);
            return null;
        });
        return metadata;
    }

    @Override
    public RedisMetadata update(final RedisMetadata metadata) throws TechnicalException {
        redisTemplate.opsForHash().put(REDIS_KEY,
                getMetadataKey(metadata.getKey(), metadata.getReferenceType(), metadata.getReferenceId()), metadata);
        return metadata;
    }

    @Override
    public void delete(final String key, final String referenceId, final MetadataReferenceType referenceType) throws TechnicalException {
        redisTemplate.opsForHash().delete(REDIS_KEY, getMetadataKey(key, referenceType, referenceId));
    }

    @Override
    public RedisMetadata findById(final String key, final String referenceId, final MetadataReferenceType referenceType) throws TechnicalException {
        return convert(redisTemplate.opsForHash().get(REDIS_KEY, getMetadataKey(key, referenceType, referenceId)), RedisMetadata.class);
    }

    @Override
    public List<RedisMetadata> findByKeyAndReferenceType(final String key, final MetadataReferenceType referenceType) throws TechnicalException {
        return findByKeyAndReferenceTypeAndReferenceId(key, referenceType, "");
    }

    @Override
    public List<RedisMetadata> findByReferenceType(final MetadataReferenceType referenceType) throws TechnicalException {
        return findByKeyAndReferenceTypeAndReferenceId("", referenceType, "");
    }

    @Override
    public List<RedisMetadata> findByReferenceTypeAndReferenceId(final MetadataReferenceType referenceType, final String referenceId) throws TechnicalException {
        return findByKeyAndReferenceTypeAndReferenceId("", referenceType, referenceId);
    }

    private List<RedisMetadata> findByKeyAndReferenceTypeAndReferenceId(final String key, final MetadataReferenceType referenceType, final String referenceId) {
        final Set<Object> keys = redisTemplate.opsForSet().members(getMetadataKey(key, referenceType, referenceId));
        final List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(metadata -> convert(metadata, RedisMetadata.class))
                .collect(toList());
    }

    private String getMetadataKey(final String key, final MetadataReferenceType referenceType, final String referenceId) {
        return key + ":" + referenceType + ":" + referenceId;
    }
}
