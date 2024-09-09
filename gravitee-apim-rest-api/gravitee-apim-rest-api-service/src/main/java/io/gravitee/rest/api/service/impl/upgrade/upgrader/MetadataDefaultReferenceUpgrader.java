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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.DEFAULT_METADATA_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MetadataDefaultReferenceUpgrader implements Upgrader {

    EnvironmentRepository environmentRepository;

    MetadataRepository metadataRepository;

    public MetadataDefaultReferenceUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy MetadataRepository metadataRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.metadataRepository = metadataRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            Set<Environment> environments = environmentRepository.findAll();
            List<Metadata> metadataList = metadataRepository
                .findAll()
                .stream()
                .filter(metadata -> metadata.getReferenceId().equals("_"))
                .flatMap((Function<Metadata, Stream<Metadata>>) metadata -> migrateDefaultMetadataToEnvironments(environments, metadata))
                .toList();
            log.info("Migrating metadata: {} for environments {}", metadataList.size(), environments);
        } catch (Exception e) {
            log.error("Failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }
        return true;
    }

    private Stream<Metadata> migrateDefaultMetadataToEnvironments(Set<Environment> environments, Metadata metadata) {
        Stream<Metadata> metadataStream = environments
            .stream()
            .map(environment -> {
                Metadata metadataToCreate = duplicateMetadata(environment, metadata);
                try {
                    return metadataRepository.create(metadataToCreate);
                } catch (DuplicateKeyException e) {
                    log.warn("Failed to duplicate metadata {} to {}", metadata, environment, e);
                    return metadataToCreate;
                } catch (TechnicalException e) {
                    log.error("Failed to duplicate metadata {} to {}", metadata, environment, e);
                    throw new TechnicalManagementException(e);
                }
            });
        try {
            metadataRepository.delete(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType());
        } catch (TechnicalException e) {
            log.error("Failed to delete metadata {}", metadata, e);
            throw new TechnicalManagementException(e);
        }
        return metadataStream;
    }

    private Metadata duplicateMetadata(Environment environment, Metadata metadata) {
        return Metadata
            .builder()
            .referenceId(environment.getId())
            .referenceType(MetadataReferenceType.ENVIRONMENT)
            .name(metadata.getName())
            .key(metadata.getKey())
            .value(metadata.getValue())
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .format(metadata.getFormat())
            .build();
    }

    @Override
    public int getOrder() {
        return DEFAULT_METADATA_UPGRADER;
    }
}
