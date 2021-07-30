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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.ENTRYPOINT;
import static io.gravitee.repository.management.model.Entrypoint.AuditEvent.*;
import static java.util.Arrays.sort;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.EntrypointReferenceType;
import io.gravitee.rest.api.model.NewEntryPointEntity;
import io.gravitee.rest.api.model.UpdateEntryPointEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.EntrypointNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EntrypointServiceImpl extends TransactionalService implements EntrypointService {

    private final Logger LOGGER = LoggerFactory.getLogger(EntrypointServiceImpl.class);
    private static final String SEPARATOR = ";";

    @Autowired
    private AuditService auditService;

    @Autowired
    private EntrypointRepository entrypointRepository;

    @Override
    public EntrypointEntity findByIdAndReference(final String entrypointId, String referenceId, EntrypointReferenceType referenceType) {
        try {
            LOGGER.debug("Find by id {}", entrypointId);
            final Optional<Entrypoint> optionalEntryPoint = entrypointRepository.findByIdAndReference(
                entrypointId,
                referenceId,
                repoEntrypointReferenceType(referenceType)
            );
            if (!optionalEntryPoint.isPresent()) {
                throw new EntrypointNotFoundException(entrypointId);
            }
            return convert(optionalEntryPoint.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all entrypoints", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all entrypoints", ex);
        }
    }

    @Override
    public List<EntrypointEntity> findByReference(String referenceId, EntrypointReferenceType referenceType) {
        try {
            LOGGER.debug("Find all APIs");
            return entrypointRepository
                .findByReference(referenceId, repoEntrypointReferenceType(referenceType))
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all entrypoints", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all entrypoints", ex);
        }
    }

    @Override
    public EntrypointEntity create(final NewEntryPointEntity entrypointEntity, String referenceId, EntrypointReferenceType referenceType) {
        try {
            final Entrypoint entrypoint = convert(entrypointEntity, referenceId, referenceType);
            final EntrypointEntity savedEntryPoint = convert(entrypointRepository.create(entrypoint));
            auditService.createEnvironmentAuditLog(
                Collections.singletonMap(ENTRYPOINT, entrypoint.getId()),
                ENTRYPOINT_CREATED,
                new Date(),
                null,
                entrypoint
            );
            return savedEntryPoint;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create entrypoint {}", entrypointEntity.getValue(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create entrypoint " + entrypointEntity.getValue(), ex);
        }
    }

    @Override
    public EntrypointEntity update(
        final UpdateEntryPointEntity entrypointEntity,
        String referenceId,
        EntrypointReferenceType referenceType
    ) {
        try {
            final Optional<Entrypoint> entrypointOptional = entrypointRepository.findByIdAndReference(
                entrypointEntity.getId(),
                referenceId,
                repoEntrypointReferenceType(referenceType)
            );
            if (entrypointOptional.isPresent()) {
                final Entrypoint entrypoint = convert(entrypointEntity);
                Entrypoint existingEntrypoint = entrypointOptional.get();
                entrypoint.setReferenceId(existingEntrypoint.getReferenceId());
                entrypoint.setReferenceType(existingEntrypoint.getReferenceType());
                final EntrypointEntity savedEntryPoint = convert(entrypointRepository.update(entrypoint));
                auditService.createEnvironmentAuditLog(
                    Collections.singletonMap(ENTRYPOINT, entrypoint.getId()),
                    ENTRYPOINT_UPDATED,
                    new Date(),
                    entrypointOptional.get(),
                    entrypoint
                );
                return savedEntryPoint;
            } else {
                throw new EntrypointNotFoundException(entrypointEntity.getId());
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update entrypoint {}", entrypointEntity.getValue(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update entrypoint " + entrypointEntity.getValue(), ex);
        }
    }

    @Override
    public void delete(final String entrypointId, String referenceId, EntrypointReferenceType referenceType) {
        try {
            Optional<Entrypoint> entrypointOptional = entrypointRepository.findByIdAndReference(
                entrypointId,
                referenceId,
                repoEntrypointReferenceType(referenceType)
            );
            if (entrypointOptional.isPresent()) {
                entrypointRepository.delete(entrypointId);
                auditService.createEnvironmentAuditLog(
                    Collections.singletonMap(ENTRYPOINT, entrypointId),
                    ENTRYPOINT_DELETED,
                    new Date(),
                    null,
                    entrypointOptional.get()
                );
            } else {
                throw new EntrypointNotFoundException(entrypointId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete entrypoint {}", entrypointId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete entrypoint " + entrypointId, ex);
        }
    }

    private Entrypoint convert(final NewEntryPointEntity entrypointEntity, String referenceId, EntrypointReferenceType referenceType) {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId(UuidString.generateRandom());
        entrypoint.setValue(entrypointEntity.getValue());
        entrypoint.setTags(String.join(SEPARATOR, entrypointEntity.getTags()));
        entrypoint.setReferenceId(referenceId);
        entrypoint.setReferenceType(repoEntrypointReferenceType(referenceType));
        return entrypoint;
    }

    private Entrypoint convert(final UpdateEntryPointEntity entrypointEntity) {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId(entrypointEntity.getId());
        entrypoint.setValue(entrypointEntity.getValue());
        entrypoint.setTags(String.join(SEPARATOR, entrypointEntity.getTags()));
        return entrypoint;
    }

    private EntrypointEntity convert(final Entrypoint entrypoint) {
        final EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setId(entrypoint.getId());
        entrypointEntity.setValue(entrypoint.getValue());
        entrypointEntity.setTags(entrypoint.getTags().split(SEPARATOR));
        return entrypointEntity;
    }

    private io.gravitee.repository.management.model.EntrypointReferenceType repoEntrypointReferenceType(
        EntrypointReferenceType referenceType
    ) {
        return io.gravitee.repository.management.model.EntrypointReferenceType.valueOf(referenceType.name());
    }
}
