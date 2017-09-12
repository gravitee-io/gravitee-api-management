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
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.redis.management.internal.MetadataRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisMetadataRepository implements MetadataRepository {

    @Autowired
    private MetadataRedisRepository metadataRedisRepository;

    @Override
    public Metadata create(Metadata metadata) throws TechnicalException {
        return convert(metadataRedisRepository.create(convert(metadata)));
    }

    @Override
    public Metadata update(Metadata metadata) throws TechnicalException {
        if (metadata == null || metadata.getName() == null) {
            throw new IllegalStateException("Metadata to update must have a name");
        }

        RedisMetadata redisMetadata = metadataRedisRepository.findById(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType());

        if (redisMetadata == null) {
            throw new IllegalStateException(String.format("No metadata found with key [%s], reference id [%s] and type [%s]",
                    metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType()));
        }

        return convert(metadataRedisRepository.update(convert(metadata)));
    }

    @Override
    public void delete(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        metadataRedisRepository.delete(key, referenceId, referenceType);
    }

    @Override
    public Optional<Metadata> findById(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        return Optional.ofNullable(convert(metadataRedisRepository.findById(key, referenceId, referenceType)));
    }

    @Override
    public List<Metadata> findByKeyAndReferenceType(String key, MetadataReferenceType referenceType) throws TechnicalException {
        final List<RedisMetadata> metadata = metadataRedisRepository.findByKeyAndReferenceType(key, referenceType);
        return metadata.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public List<Metadata> findByReferenceType(MetadataReferenceType referenceType) throws TechnicalException {
        final List<RedisMetadata> metadata = metadataRedisRepository.findByReferenceType(referenceType);
        return metadata.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public List<Metadata> findByReferenceTypeAndReferenceId(MetadataReferenceType referenceType, String referenceId) throws TechnicalException {
        final List<RedisMetadata> metadata = metadataRedisRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
        return metadata.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private Metadata convert(final RedisMetadata redisMetadata) {
        if (redisMetadata == null) {
            return null;
        }
        final Metadata metadata = new Metadata();
        metadata.setKey(redisMetadata.getKey());
        metadata.setName(redisMetadata.getName());
        metadata.setFormat(redisMetadata.getFormat());
        metadata.setValue(redisMetadata.getValue());
        metadata.setReferenceId(redisMetadata.getReferenceId());
        metadata.setReferenceType(redisMetadata.getReferenceType());
        metadata.setCreatedAt(redisMetadata.getCreatedAt());
        metadata.setUpdatedAt(redisMetadata.getUpdatedAt());
        return metadata;
    }

    private RedisMetadata convert(final Metadata metadata) {
        final RedisMetadata redisMetadata = new RedisMetadata();
        redisMetadata.setKey(metadata.getKey());
        redisMetadata.setName(metadata.getName());
        redisMetadata.setFormat(metadata.getFormat());
        redisMetadata.setValue(metadata.getValue());
        redisMetadata.setReferenceId(metadata.getReferenceId());
        redisMetadata.setReferenceType(metadata.getReferenceType());
        redisMetadata.setCreatedAt(metadata.getCreatedAt());
        redisMetadata.setUpdatedAt(metadata.getUpdatedAt());
        return redisMetadata;
    }
}
