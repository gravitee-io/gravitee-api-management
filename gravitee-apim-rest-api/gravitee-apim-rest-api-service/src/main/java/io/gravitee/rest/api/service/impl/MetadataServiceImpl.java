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

import static io.gravitee.repository.management.model.Audit.AuditProperties.METADATA;
import static io.gravitee.repository.management.model.Metadata.AuditEvent.*;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewMetadataEntity;
import io.gravitee.rest.api.model.UpdateMetadataEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DuplicateMetadataNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MetadataServiceImpl extends TransactionalService implements MetadataService {

    private static final String DEFAULT_REFERENCE_ID = "_";
    private final Logger LOGGER = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    public static String getDefaultReferenceId() {
        return DEFAULT_REFERENCE_ID;
    }

    @Override
    public List<MetadataEntity> findAllDefault() {
        try {
            LOGGER.debug("Find all metadata");
            return metadataRepository
                .findByReferenceType(MetadataReferenceType.DEFAULT)
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .map(this::convert)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find all metadata", ex);
            throw new TechnicalManagementException("An error occurred while trying to find all metadata", ex);
        }
    }

    @Override
    public MetadataEntity create(final ExecutionContext executionContext, final NewMetadataEntity metadataEntity) {
        // if no format defined, we just set String format
        if (metadataEntity.getFormat() == null) {
            metadataEntity.setFormat(MetadataFormat.STRING);
        }

        try {
            // First we prevent the duplicate metadata name
            final Optional<MetadataEntity> optionalMetadata = findAllDefault()
                .stream()
                .filter(metadata -> metadataEntity.getName().equalsIgnoreCase(metadata.getName()))
                .findAny();

            if (optionalMetadata.isPresent()) {
                throw new DuplicateMetadataNameException(optionalMetadata.get().getName());
            }

            checkMetadataValue(metadataEntity.getValue());
            checkMetadataFormat(executionContext, metadataEntity.getFormat(), metadataEntity.getValue());
            final Metadata metadata = convert(metadataEntity);
            final Date now = new Date();
            metadata.setCreatedAt(now);
            metadata.setUpdatedAt(now);
            metadataRepository.create(metadata);
            // Audit
            // FIXME: Use OrganizationAuditLog?
            auditService.createAuditLog(
                executionContext,
                singletonMap(METADATA, metadata.getKey()),
                METADATA_CREATED,
                metadata.getCreatedAt(),
                null,
                metadata
            );
            return convert(metadata);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create metadata {}", metadataEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create metadata " + metadataEntity.getName(), ex);
        }
    }

    @Override
    public MetadataEntity update(final ExecutionContext executionContext, final UpdateMetadataEntity metadataEntity) {
        try {
            // First we prevent the duplicate metadata name
            final Optional<Metadata> optionalMetadata = metadataRepository
                .findByReferenceType(MetadataReferenceType.DEFAULT)
                .stream()
                .filter(
                    metadata ->
                        !metadataEntity.getKey().equals(metadata.getKey()) && metadataEntity.getName().equalsIgnoreCase(metadata.getName())
                )
                .findAny();

            if (optionalMetadata.isPresent()) {
                throw new DuplicateMetadataNameException(optionalMetadata.get().getName());
            }

            checkMetadataValue(metadataEntity.getValue());
            checkMetadataFormat(executionContext, metadataEntity.getFormat(), metadataEntity.getValue());

            final Metadata metadata = convert(metadataEntity);
            final Date now = new Date();
            metadata.setUpdatedAt(now);
            metadataRepository.update(metadata);
            // Audit
            // FIXME: Use OrganizationAuditLog?
            auditService.createAuditLog(
                executionContext,
                singletonMap(METADATA, metadata.getKey()),
                METADATA_UPDATED,
                metadata.getCreatedAt(),
                null,
                metadata
            );
            return convert(metadata);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to update metadata {}", metadataEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurred while trying to update metadata " + metadataEntity.getName(), ex);
        }
    }

    private void checkMetadataValue(String value) {
        if (value == null || isBlank(value)) {
            LOGGER.error("Error occurred while trying to validate null or empty value");
            throw new TechnicalManagementException("Metadata value is required");
        }
    }

    @Override
    public void delete(final ExecutionContext executionContext, final String key) {
        try {
            final Optional<Metadata> optMetadata = metadataRepository.findById(key, DEFAULT_REFERENCE_ID, MetadataReferenceType.DEFAULT);
            if (optMetadata.isPresent()) {
                metadataRepository.delete(key, DEFAULT_REFERENCE_ID, MetadataReferenceType.DEFAULT);
                // Audit
                // FIXME: Use OrganizationAuditLog?
                auditService.createAuditLog(
                    executionContext,
                    singletonMap(METADATA, key),
                    METADATA_DELETED,
                    new Date(),
                    optMetadata.get(),
                    null
                );
                // delete all overridden API metadata
                final List<Metadata> apiMetadata = metadataRepository.findByKeyAndReferenceType(key, MetadataReferenceType.API);

                for (final Metadata metadata : apiMetadata) {
                    metadataRepository.delete(key, metadata.getReferenceId(), metadata.getReferenceType());
                    // Audit
                    auditService.createApiAuditLog(
                        executionContext,
                        metadata.getReferenceId(),
                        singletonMap(METADATA, key),
                        METADATA_DELETED,
                        new Date(),
                        metadata,
                        null
                    );
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete metadata {}", key, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete metadata " + key, ex);
        }
    }

    @Override
    public MetadataEntity findDefaultByKey(final String key) {
        try {
            LOGGER.debug("Find default metadata by key");
            final Optional<Metadata> optMetadata = metadataRepository.findById(key, DEFAULT_REFERENCE_ID, MetadataReferenceType.DEFAULT);
            if (optMetadata.isPresent()) {
                return convert(optMetadata.get());
            } else {
                return null;
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find default metadata by key", ex);
            throw new TechnicalManagementException("An error occurred while trying to find default metadata by key", ex);
        }
    }

    @Override
    public void checkMetadataFormat(ExecutionContext executionContext, MetadataFormat format, String value) {
        checkMetadataFormat(executionContext, format, value, null, null);
    }

    @Override
    public void checkMetadataFormat(
        ExecutionContext executionContext,
        final MetadataFormat format,
        final String value,
        final MetadataReferenceType referenceType,
        final Object entity
    ) {
        try {
            String decodedValue = value;
            if (entity != null && !isBlank(value) && value.startsWith("${")) {
                decodedValue =
                    this.notificationTemplateService.resolveInlineTemplateWithParam(
                            value,
                            new StringReader(value),
                            singletonMap(referenceType.name().toLowerCase(), entity)
                        );
            }

            if (isBlank(decodedValue)) {
                return;
            }

            switch (format) {
                case BOOLEAN:
                    Boolean.valueOf(decodedValue);
                    break;
                case URL:
                    new URL(decodedValue);
                    break;
                case MAIL:
                    final InternetAddress email = new InternetAddress(decodedValue);
                    email.validate();
                    break;
                case DATE:
                    final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-mm-dd");
                    sdf.setLenient(false);
                    sdf.parse(decodedValue);
                    break;
                case NUMERIC:
                    Double.valueOf(decodedValue);
                    break;
            }
        } catch (final Exception e) {
            LOGGER.error("Error occurred while trying to validate format '{}' of value '{}'", format, value, e);
            throw new TechnicalManagementException("Error occurred while trying to validate format " + format + " of value " + value, e);
        }
    }

    private MetadataEntity convert(final Metadata metadata) {
        final MetadataEntity metadataEntity = new MetadataEntity();
        metadataEntity.setKey(metadata.getKey());
        metadataEntity.setName(metadata.getName());
        metadataEntity.setValue(metadata.getValue());
        metadataEntity.setFormat(MetadataFormat.valueOf(metadata.getFormat().name()));
        return metadataEntity;
    }

    private Metadata convert(final NewMetadataEntity metadataEntity) {
        final Metadata metadata = new Metadata();
        metadata.setKey(IdGenerator.generate(metadataEntity.getName()));
        metadata.setName(metadataEntity.getName());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (metadataEntity.getValue() != null) {
            if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
                metadata.setValue(metadataEntity.getValue().substring(0, 10));
            } else {
                metadata.setValue(metadataEntity.getValue());
            }
        }

        metadata.setReferenceId(DEFAULT_REFERENCE_ID);
        metadata.setReferenceType(MetadataReferenceType.DEFAULT);

        return metadata;
    }

    private Metadata convert(final UpdateMetadataEntity metadataEntity) {
        final Metadata metadata = new Metadata();
        metadata.setKey(metadataEntity.getKey());
        metadata.setName(metadataEntity.getName());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (metadataEntity.getValue() != null) {
            if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
                metadata.setValue(metadataEntity.getValue().substring(0, 10));
            } else {
                metadata.setValue(metadataEntity.getValue());
            }
        }

        metadata.setReferenceId(DEFAULT_REFERENCE_ID);
        metadata.setReferenceType(MetadataReferenceType.DEFAULT);

        return metadata;
    }
}
