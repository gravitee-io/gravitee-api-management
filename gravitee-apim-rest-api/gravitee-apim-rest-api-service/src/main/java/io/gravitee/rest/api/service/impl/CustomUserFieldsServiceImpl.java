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

import static io.gravitee.repository.management.model.Audit.AuditProperties.USER_FIELD;
import static io.gravitee.repository.management.model.CustomUserField.AuditEvent.*;
import static io.gravitee.repository.management.model.CustomUserFieldReferenceType.ENVIRONMENT;
import static io.gravitee.repository.management.model.CustomUserFieldReferenceType.ORGANIZATION;
import static io.gravitee.rest.api.service.sanitizer.CustomFieldSanitizer.formatKeyValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.rest.api.model.CustomUserFieldEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldAlreadyExistException;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CustomUserFieldsServiceImpl extends TransactionalService implements CustomUserFieldService {

    private final Logger LOGGER = LoggerFactory.getLogger(CustomUserFieldsServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomUserFieldsRepository customUserFieldsRepository;

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserMetadataService userMetadataService;

    @Override
    public CustomUserFieldEntity create(final ExecutionContext executionContext, CustomUserFieldEntity newFieldEntity) {
        try {
            final String refId = executionContext.getOrganizationId();
            final CustomUserFieldReferenceType refType = ORGANIZATION;
            LOGGER.debug("Create custom user field [key={}, refId={}]", newFieldEntity.getKey(), refId);
            Optional<CustomUserField> existingRecord =
                this.customUserFieldsRepository.findById(formatKeyValue(newFieldEntity.getKey()), refId, refType);
            if (existingRecord.isPresent()) {
                throw new CustomUserFieldAlreadyExistException(newFieldEntity.getKey());
            } else {
                CustomUserField fieldToCreate = map(newFieldEntity);
                fieldToCreate.setReferenceId(refId);
                fieldToCreate.setReferenceType(refType);
                final Date now = new Date();
                fieldToCreate.setCreatedAt(now);
                fieldToCreate.setUpdatedAt(now);

                final CustomUserField recorded = customUserFieldsRepository.create(fieldToCreate);

                createAuditLog(executionContext, CUSTOM_USER_FIELD_CREATED, now, null, recorded);
                return map(recorded);
            }
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to create CustomUserField", e);
            throw new TechnicalManagementException("An error occurs while trying to create CustomUserField", e);
        }
    }

    @Override
    public CustomUserFieldEntity update(final ExecutionContext executionContext, CustomUserFieldEntity updateFieldEntity) {
        try {
            final String refId = executionContext.getOrganizationId();
            final CustomUserFieldReferenceType refType = ORGANIZATION;
            LOGGER.debug("Update custom user field [key={}, refId={}]", updateFieldEntity.getKey(), refId);
            Optional<CustomUserField> existingRecord =
                this.customUserFieldsRepository.findById(formatKeyValue(updateFieldEntity.getKey()), refId, refType);
            if (existingRecord.isPresent()) {
                CustomUserField fieldToUpdate = map(updateFieldEntity);
                fieldToUpdate.setKey(existingRecord.get().getKey());
                fieldToUpdate.setReferenceId(existingRecord.get().getReferenceId());
                fieldToUpdate.setReferenceType(existingRecord.get().getReferenceType());
                fieldToUpdate.setCreatedAt(existingRecord.get().getCreatedAt());
                final Date updatedAt = new Date();
                fieldToUpdate.setUpdatedAt(updatedAt);

                final CustomUserField updatedField = customUserFieldsRepository.update(fieldToUpdate);

                createAuditLog(executionContext, CUSTOM_USER_FIELD_UPDATED, updatedAt, existingRecord.get(), updatedField);
                return map(updatedField);
            } else {
                throw new CustomUserFieldNotFoundException(updateFieldEntity.getKey());
            }
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to update CustomUserField", e);
            throw new TechnicalManagementException("An error occurs while trying to update CustomUserField", e);
        }
    }

    @Override
    public void delete(final ExecutionContext executionContext, String key) {
        try {
            final String refId = executionContext.getOrganizationId();
            final CustomUserFieldReferenceType refType = ORGANIZATION;
            LOGGER.debug("Delete custom user field [key={}, refId={}]", key, refId);
            Optional<CustomUserField> existingRecord = this.customUserFieldsRepository.findById(formatKeyValue(key), refId, refType);
            if (existingRecord.isPresent()) {
                customUserFieldsRepository.delete(formatKeyValue(key), refId, refType);
                createAuditLog(executionContext, CUSTOM_USER_FIELD_DELETED, new Date(), existingRecord.get(), null);
                // remove all instance of this field from UserMetadata
                this.userMetadataService.deleteAllByCustomFieldId(
                        executionContext,
                        existingRecord.get().getKey(),
                        existingRecord.get().getReferenceId(),
                        existingRecord.get().getReferenceType()
                    );
            }
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to create CustomUserField", e);
            throw new TechnicalManagementException("An error occurs while trying to create CustomUserField", e);
        }
    }

    @Override
    public List<CustomUserFieldEntity> listAllFields(final ExecutionContext executionContext) {
        try {
            final String refId = executionContext.getOrganizationId();
            final CustomUserFieldReferenceType refType = ORGANIZATION;
            LOGGER.debug("List all custom user fields [refId={}/refType={}]", refId, refType);
            List<CustomUserField> records = this.customUserFieldsRepository.findByReferenceIdAndReferenceType(refId, refType);
            return records.stream().map(this::map).collect(Collectors.toList());
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to list all CustomUserField", e);
            throw new TechnicalManagementException("An error occurs while trying to list all CustomUserField", e);
        }
    }

    private void createAuditLog(
        final ExecutionContext executionContext,
        Audit.AuditEvent event,
        Date createdAt,
        CustomUserField oldValue,
        CustomUserField newValue
    ) {
        String key = oldValue != null ? oldValue.getKey() : newValue.getKey();
        CustomUserFieldReferenceType type = oldValue != null ? oldValue.getReferenceType() : newValue.getReferenceType();
        if (type == ORGANIZATION) {
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER_FIELD, key),
                event,
                createdAt,
                oldValue,
                newValue
            );
        } else if (type == ENVIRONMENT) {
            auditService.createAuditLog(executionContext, Collections.singletonMap(USER_FIELD, key), event, createdAt, oldValue, newValue);
        }
    }

    private CustomUserField map(CustomUserFieldEntity entity) {
        CustomUserField result = new CustomUserField();
        result.setKey(formatKeyValue(entity.getKey()));
        result.setLabel(entity.getLabel());
        result.setRequired(entity.isRequired());
        // Currently the type is restricted to String
        // We use the MetadataFormat Enum as commodity because these fields
        // will be stored into the Metadata repository with USER as referenceType
        result.setFormat(MetadataFormat.STRING);
        if (entity.getValues() != null) {
            result.setValues(entity.getValues().stream().distinct().collect(Collectors.toList()));
        }
        return result;
    }

    private CustomUserFieldEntity map(CustomUserField record) {
        CustomUserFieldEntity result = new CustomUserFieldEntity();
        result.setKey(formatKeyValue(record.getKey()));
        result.setLabel(record.getLabel());
        result.setRequired(record.isRequired());

        if (record.getValues() != null) {
            switch (record.getFormat()) {
                case STRING:
                    result.setValues(record.getValues());
                    break;
                default:
                    throw new TechnicalManagementException("Unable to read values of CustomUserField, format not supported");
            }
        }
        return result;
    }
}
