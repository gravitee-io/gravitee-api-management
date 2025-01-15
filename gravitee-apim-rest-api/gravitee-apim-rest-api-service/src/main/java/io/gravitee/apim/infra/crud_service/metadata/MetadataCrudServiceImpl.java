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
package io.gravitee.apim.infra.crud_service.metadata;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static io.gravitee.repository.management.model.MetadataReferenceType.API;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.metadata.model.MetadataId;
import io.gravitee.apim.infra.adapter.MetadataAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.MetadataReferenceType;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MetadataCrudServiceImpl implements MetadataCrudService {

    private final MetadataRepository metadataRepository;

    public MetadataCrudServiceImpl(@Lazy MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    public Metadata create(Metadata metadata) {
        try {
            var result = metadataRepository.create(MetadataAdapter.INSTANCE.toRepository(metadata));
            return MetadataAdapter.INSTANCE.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurs while trying to create the %s metadata of [%sId=%s]",
                    metadata.getKey(),
                    metadata.getReferenceType().name().toLowerCase(),
                    metadata.getReferenceId()
                ),
                e
            );
        }
    }

    @Override
    public Optional<Metadata> findById(MetadataId id) {
        try {
            return metadataRepository
                .findById(id.getKey(), id.getReferenceId(), MetadataReferenceType.valueOf(id.getReferenceType().name()))
                .map(MetadataAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while finding metadata by id [%s, %s, %s]",
                    id.getReferenceId(),
                    id.getReferenceType().name(),
                    id.getKey()
                ),
                e
            );
        }
    }

    @Override
    public Collection<Metadata> findByApiId(String id) {
        try {
            return stream(metadataRepository.findByReferenceTypeAndReferenceId(API, id)).map(MetadataAdapter.INSTANCE::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while finding metadata by API id [%s]".formatted(id), e);
        }
    }

    @Override
    public Metadata update(Metadata metadata) {
        try {
            var result = metadataRepository.update(MetadataAdapter.INSTANCE.toRepository(metadata));
            return MetadataAdapter.INSTANCE.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurs while trying to update the %s metadata of [%sId=%s]",
                    metadata.getKey(),
                    metadata.getReferenceType().name().toLowerCase(),
                    metadata.getReferenceId()
                ),
                e
            );
        }
    }

    @Override
    public void delete(MetadataId id) {
        try {
            metadataRepository.delete(id.getKey(), id.getReferenceId(), MetadataReferenceType.valueOf(id.getReferenceType().name()));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurs while trying to delete the metadata with key: %s", id.getKey()),
                e
            );
        }
    }
}
