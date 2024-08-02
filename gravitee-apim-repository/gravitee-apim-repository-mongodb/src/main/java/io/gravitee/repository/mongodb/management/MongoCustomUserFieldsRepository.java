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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.mongodb.management.internal.model.CustomUserFieldMongo;
import io.gravitee.repository.mongodb.management.internal.model.CustomUserFieldPkMongo;
import io.gravitee.repository.mongodb.management.internal.user.CustomUserFieldsMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class MongoCustomUserFieldsRepository implements CustomUserFieldsRepository {

    private final Logger logger = LoggerFactory.getLogger(MongoCustomUserFieldsRepository.class);

    @Autowired
    private CustomUserFieldsMongoRepository internalMongoRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public CustomUserField create(CustomUserField field) throws TechnicalException {
        if (field == null || field.getKey() == null || field.getReferenceId() == null || field.getReferenceType() == null) {
            throw new IllegalStateException("CustomUserField to create must have an id");
        }

        logger.debug("Create CustomUserField [{}]", field);
        CustomUserFieldMongo createdField = internalMongoRepo.insert(mapper.map(field));
        logger.debug("Create CustomUserField [{}] - Done", field);
        return mapper.map(createdField);
    }

    @Override
    public CustomUserField update(CustomUserField field) throws TechnicalException {
        if (field == null || field.getKey() == null || field.getReferenceId() == null || field.getReferenceType() == null) {
            throw new IllegalStateException("CustomUserField to update must have an id");
        }

        final CustomUserFieldPkMongo id = new CustomUserFieldPkMongo(
            field.getKey(),
            field.getReferenceId(),
            field.getReferenceType().name()
        );
        final Optional<CustomUserFieldMongo> previousField = internalMongoRepo.findById(id);
        if (previousField.isEmpty()) {
            throw new IllegalStateException(String.format("No CustomUserField found with id [%s]", id));
        }

        logger.debug("Update CustomUserField [{}]", field);
        CustomUserFieldMongo updatedField = internalMongoRepo.save(mapper.map(field));
        logger.debug("Update CustomUserField [{}] - Done", field);
        return mapper.map(updatedField);
    }

    @Override
    public void delete(String key, String refId, CustomUserFieldReferenceType referenceType) throws TechnicalException {
        final CustomUserFieldPkMongo id = new CustomUserFieldPkMongo(key, refId, referenceType.name());
        logger.debug("Delete CustomUserField by ID [{}]", id);
        internalMongoRepo.deleteById(id);
        logger.debug("Delete CustomUserField by ID [{}] - Done", id);
    }

    @Override
    public Optional<CustomUserField> findById(String key, String refId, CustomUserFieldReferenceType referenceType)
        throws TechnicalException {
        final CustomUserFieldPkMongo id = new CustomUserFieldPkMongo(key, refId, referenceType.name());
        logger.debug("Find CustomUserField by ID [{}]", id);

        final CustomUserFieldMongo field = internalMongoRepo.findById(id).orElse(null);

        logger.debug("Find CustomUserField by ID [{}] - Done", id);
        return Optional.ofNullable(mapper.map(field));
    }

    @Override
    public List<CustomUserField> findByReferenceIdAndReferenceType(String refId, CustomUserFieldReferenceType referenceType)
        throws TechnicalException {
        logger.debug("Find CustomUserField by Reference [{}/{}]", refId, referenceType);

        final List<CustomUserFieldMongo> fields = internalMongoRepo.findByReference(refId, referenceType.name());

        logger.debug("Find CustomUserField by Reference [{}/{}] - Done", refId, referenceType);
        return fields.stream().map(f -> mapper.map(f)).collect(Collectors.toList());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, CustomUserFieldReferenceType referenceType)
        throws TechnicalException {
        logger.debug("Delete custom user fields by reference [{}/{}]", referenceType, referenceId);
        try {
            final var fields = internalMongoRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(field -> field.getId().getKey())
                .toList();
            logger.debug("Delete custom user fields by reference [{}/{}] - Done", referenceType, referenceId);
            return fields;
        } catch (Exception ex) {
            logger.error("Failed to delete custom user fields by ref: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete custom user fields by ref");
        }
    }

    @Override
    public Set<CustomUserField> findAll() throws TechnicalException {
        return internalMongoRepo
            .findAll()
            .stream()
            .map(customUserFieldMongo -> mapper.map(customUserFieldMongo))
            .collect(Collectors.toSet());
    }
}
