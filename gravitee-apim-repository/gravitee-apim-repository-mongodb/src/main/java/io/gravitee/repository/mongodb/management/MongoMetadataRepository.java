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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.mongodb.management.internal.api.MetadataMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.MetadataMongo;
import io.gravitee.repository.mongodb.management.internal.model.MetadataPkMongo;
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
public class MongoMetadataRepository implements MetadataRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoMetadataRepository.class);

    @Autowired
    private MetadataMongoRepository internalMetadataRepository;

    @Override
    public Optional<Metadata> findById(final String key, final String referenceId, final MetadataReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Find metadata by key '{}' ref type '{}' ref id '{}'", key, referenceType, referenceId);
        final MetadataMongo metadata = internalMetadataRepository
            .findById(new MetadataPkMongo(key, referenceId, referenceType.name()))
            .orElse(null);
        LOGGER.debug("Find metadata by key '{}' ref type '{}' ref id '{}' done", key, referenceType, referenceId);
        return Optional.ofNullable(map(metadata));
    }

    @Override
    public Metadata create(Metadata metadata) throws TechnicalException {
        LOGGER.debug("Create metadata [{}]", metadata.getName());
        try {
            Metadata res = map(internalMetadataRepository.insert(map(metadata)));
            LOGGER.debug("Create metadata [{}] - Done", metadata.getName());
            return res;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            LOGGER.error("An error occurred while creating metadata", e);
            throw new DuplicateKeyException("An error occurred while creating metadata", e);
        } catch (Exception e) {
            LOGGER.error("An error occurred while creating metadata", e);
            throw new TechnicalException("An error occurred while updating metadata", e);
        }
    }

    @Override
    public Metadata update(Metadata metadata) throws TechnicalException {
        if (metadata == null || metadata.getName() == null) {
            throw new IllegalStateException("Metadata to update must have a name");
        }

        final MetadataPkMongo metadataId = new MetadataPkMongo(
            metadata.getKey(),
            metadata.getReferenceId(),
            metadata.getReferenceType().name()
        );
        MetadataMongo metadataMongo = internalMetadataRepository.findById(metadataId).orElse(null);

        if (metadataMongo == null) {
            throw new IllegalStateException(String.format("No metadata found with id [%s]", metadataId));
        }

        try {
            metadataMongo.setName(metadata.getName());
            metadataMongo.setValue(metadata.getValue());
            metadataMongo.setFormat(metadata.getFormat().name());

            return map(internalMetadataRepository.save(metadataMongo));
        } catch (Exception e) {
            LOGGER.error("An error occurred while updating metadata", e);
            throw new TechnicalException("An error occurred while updating metadata");
        }
    }

    @Override
    public void delete(final String key, final String referenceId, final MetadataReferenceType referenceType) throws TechnicalException {
        final MetadataPkMongo id = new MetadataPkMongo(key, referenceId, referenceType.name());
        try {
            internalMetadataRepository.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occurred while deleting metadata [{}]", id, e);
            throw new TechnicalException("An error occurred while deleting metadata");
        }
    }

    @Override
    public List<Metadata> findByReferenceType(MetadataReferenceType referenceType) {
        LOGGER.debug("Find metadata by ref type '{}'", referenceType);

        final List<MetadataMongo> metadata = internalMetadataRepository.findByIdReferenceType(referenceType);

        LOGGER.debug("Find metadata by ref type '{}' done", referenceType);
        return metadata.stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<Metadata> findByReferenceTypeAndReferenceId(MetadataReferenceType referenceType, String referenceId) {
        LOGGER.debug("Find metadata by ref type '{}'", referenceType);

        final List<MetadataMongo> metadata = internalMetadataRepository.findByIdReferenceTypeAndIdReferenceId(referenceType, referenceId);

        LOGGER.debug("Find metadata by ref type '{}' done", referenceType);
        return metadata.stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, MetadataReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete metadata by refId: {}/{}", referenceId, referenceType);
        try {
            final var metadata = internalMetadataRepository
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType)
                .stream()
                .map(metadataMongo -> metadataMongo.getId().getKey())
                .toList();
            LOGGER.debug("Delete metadata by refId: {}/{} - Done", referenceId, referenceType);
            return metadata;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete metadata by refId: {}/{}", referenceId, referenceId, ex);
            throw new TechnicalException("Failed to delete metadata by reference");
        }
    }

    @Override
    public List<Metadata> findByKeyAndReferenceType(final String key, final MetadataReferenceType referenceType) {
        LOGGER.debug("Find metadata by key '{}' and ref type '{}'", key, referenceType);

        final List<MetadataMongo> metadata = internalMetadataRepository.findByIdKeyAndIdReferenceType(key, referenceType);

        LOGGER.debug("Find metadata by key '{}' and ref type '{}' done", key, referenceType);
        return metadata.stream().map(this::map).collect(Collectors.toList());
    }

    private Metadata map(final MetadataMongo metadataMongo) {
        if (metadataMongo == null) {
            return null;
        }
        final Metadata metadata = new Metadata();
        metadata.setKey(metadataMongo.getId().getKey());
        metadata.setReferenceType(MetadataReferenceType.valueOf(metadataMongo.getId().getReferenceType()));
        metadata.setReferenceId(metadataMongo.getId().getReferenceId());
        metadata.setFormat(MetadataFormat.valueOf(metadataMongo.getFormat()));
        metadata.setName(metadataMongo.getName());
        metadata.setValue(metadataMongo.getValue());
        metadata.setCreatedAt(metadataMongo.getCreatedAt());
        metadata.setUpdatedAt(metadataMongo.getUpdatedAt());
        return metadata;
    }

    private MetadataMongo map(final Metadata metadata) {
        MetadataMongo metadataMongo = new MetadataMongo();
        metadataMongo.setId(new MetadataPkMongo(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType().name()));
        metadataMongo.setFormat(metadata.getFormat().name());
        metadataMongo.setName(metadata.getName());
        metadataMongo.setValue(metadata.getValue());
        metadataMongo.setCreatedAt(metadata.getCreatedAt());
        metadataMongo.setUpdatedAt(metadata.getUpdatedAt());
        return metadataMongo;
    }

    @Override
    public Set<Metadata> findAll() throws TechnicalException {
        return internalMetadataRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
