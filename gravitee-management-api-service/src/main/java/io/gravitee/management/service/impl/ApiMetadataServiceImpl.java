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
package io.gravitee.management.service.impl;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.*;
import io.gravitee.management.service.ApiMetadataService;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MetadataService;
import io.gravitee.management.service.exceptions.ApiMetadataNotFoundException;
import io.gravitee.management.service.exceptions.DuplicateMetadataNameException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiMetadataServiceImpl implements ApiMetadataService {

    private final Logger LOGGER = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ApiService apiService;

    @Override
    public List<ApiMetadataEntity> findAllByApi(final String apiId) {
        try {
            LOGGER.debug("Find all metadata by api ID {}", apiId);
            final List<MetadataEntity> defaultMetadata = metadataService.findAllDefault();
            final List<String> defaultMetadataKeys = defaultMetadata.stream().map(MetadataEntity::getKey).collect(toList());

            final List<Metadata> apiMetadata = metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.API, apiId);

            final List<ApiMetadataEntity> allMetadata = new ArrayList<>();

            allMetadata.addAll(defaultMetadata.stream()
                    .map(metadata -> {
                        final Optional<Metadata> optApiMetadata = apiMetadata.stream()
                                .filter(metadataEntity -> metadata.getKey().equals(metadataEntity.getKey()))
                                .findAny();
                        return convert(optApiMetadata, metadata, null);
                    }).collect(toList()));


            allMetadata.addAll(apiMetadata.stream()
                    .filter(metadata -> !defaultMetadataKeys.contains(metadata.getKey()))
                    .map(metadata -> convert(metadata, apiId))
                    .collect(toList()));

            return allMetadata;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find all metadata by API", ex);
            throw new TechnicalManagementException("An error occurred while trying to find all metadata by API", ex);
        }
    }

    @Override
    public ApiMetadataEntity findByIdAndApi(final String metadataId, final String apiId) {
        LOGGER.debug("Find metadata by id {} and api {}", metadataId, apiId);
        try {
            final List<MetadataEntity> defaultMedatata = metadataService.findAllDefault();
            final Optional<Metadata> optMetadata = metadataRepository.findById(metadataId, apiId, MetadataReferenceType.API);

            if (optMetadata.isPresent()) {
                final Metadata metadata = optMetadata.get();
                final Optional<MetadataEntity> optDefaultMetadata =
                        defaultMedatata.stream().filter(metadataEntity -> metadata.getKey().equals(metadataEntity.getKey())).findAny();
                final ApiMetadataEntity apiMetadataEntity = convert(metadata, apiId);

                optDefaultMetadata.ifPresent(defMetadata -> apiMetadataEntity.setDefaultValue(defMetadata.getValue()));

                return apiMetadataEntity;
            }

            throw new ApiMetadataNotFoundException(apiId, metadataId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find metadata by id and API", ex);
            throw new TechnicalManagementException("An error occurred while trying to find metadata by id and API", ex);
        }
    }

    @Override
    public void delete(final String metadataId, final String apiId) {
        LOGGER.debug("Delete metadata by id {} and api {}", metadataId, apiId);
        // prevent deletion of a metadata not in the given api
        final ApiMetadataEntity apiMetadata = findByIdAndApi(metadataId, apiId);
        try {
            metadataRepository.delete(metadataId, apiMetadata.getApiId(), MetadataReferenceType.API);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete metadata {}", metadataId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete metadata " + metadataId, ex);
        }
    }

    @Override
    public ApiMetadataEntity create(final NewApiMetadataEntity metadataEntity) {
        final ApiEntity apiEntity = apiService.findById(metadataEntity.getApiId());

        // if no format defined, we just set String format
        if (metadataEntity.getFormat() == null) {
            metadataEntity.setFormat(MetadataFormat.STRING);
        }

        // First we prevent the duplicate metadata name
        final Optional<ApiMetadataEntity> optionalMetadata = findAllByApi(apiEntity.getId()).stream()
                .filter(metadata -> metadataEntity.getName().equalsIgnoreCase(metadata.getName()))
                .findAny();

        if (optionalMetadata.isPresent()) {
            throw new DuplicateMetadataNameException(optionalMetadata.get().getName());
        }

        metadataService.checkMetadataFormat(metadataEntity.getFormat(), metadataEntity.getValue());

        try {
            final Metadata metadata = convertForAPI(metadataEntity);
            final Date now = new Date();
            metadata.setCreatedAt(now);
            metadata.setUpdatedAt(now);
            return convert(metadataRepository.create(metadata), metadataEntity.getApiId());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create metadata {} on API {}", metadataEntity.getName(), metadataEntity.getApiId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create metadata " +
                    metadataEntity.getName() + " on API " + metadataEntity.getApiId(), ex);
        }
    }

    @Override
    public ApiMetadataEntity update(final UpdateApiMetadataEntity metadataEntity) {
        final ApiEntity apiEntity = apiService.findById(metadataEntity.getApiId());
        // First we prevent the duplicate metadata name
        final Optional<ApiMetadataEntity> optionalMetadata = findAllByApi(apiEntity.getId()).stream()
                .filter(metadata -> !metadataEntity.getKey().equals(metadata.getKey()) && metadataEntity.getName().equalsIgnoreCase(metadata.getName()))
                .findAny();

        if (optionalMetadata.isPresent()) {
            throw new DuplicateMetadataNameException(optionalMetadata.get().getName());
        }

        metadataService.checkMetadataFormat(metadataEntity.getFormat(), metadataEntity.getDefaultValue());
        try {
            final List<MetadataEntity> defaultMedatata = metadataService.findAllDefault();
            final Optional<MetadataEntity> optDefaultMetadata =
                    defaultMedatata.stream().filter(metadata -> metadata.getKey().equals(metadataEntity.getKey())).findAny();

            final Optional<Metadata> apiMetadata = metadataRepository.findById(metadataEntity.getKey(), metadataEntity.getApiId(), MetadataReferenceType.API);

            final Metadata savedMetadata;
            final Metadata metadata = convertForAPI(metadataEntity);
            final Date now = new Date();
            if (apiMetadata.isPresent()) {
                metadata.setUpdatedAt(now);
                savedMetadata = metadataRepository.update(metadata);
            } else {
                metadata.setCreatedAt(now);
                metadata.setUpdatedAt(now);
                savedMetadata = metadataRepository.create(metadata);
            }
            final ApiMetadataEntity apiMetadataEntity = convert(savedMetadata, null);
            optDefaultMetadata.ifPresent(defaultMetadata -> apiMetadataEntity.setDefaultValue(defaultMetadata.getValue()));

            return apiMetadataEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to update metadata {} on API {}", metadataEntity.getName(), metadataEntity.getApiId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to update metadata " +
                    metadataEntity.getName() + " on API " + metadataEntity.getApiId(), ex);
        }
    }

    private ApiMetadataEntity convert(final Optional<Metadata> optMetadata, final MetadataEntity defaultMetadata, final String apiId) {
        final Metadata metadata;
        if (optMetadata.isPresent()) {
            metadata = optMetadata.get();
        } else {
            metadata = convert(defaultMetadata);
            metadata.setValue(null);
        }

        final ApiMetadataEntity apiMetadataEntity = convert(metadata, apiId);

        apiMetadataEntity.setDefaultValue(defaultMetadata.getValue());

        return apiMetadataEntity;
    }

    private ApiMetadataEntity convert(final Metadata metadata, final String apiId) {
        final ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();

        apiMetadataEntity.setApiId(apiId);
        apiMetadataEntity.setValue(metadata.getValue());
        apiMetadataEntity.setKey(metadata.getKey());
        apiMetadataEntity.setName(metadata.getName());
        apiMetadataEntity.setHidden(metadata.isHidden());
        apiMetadataEntity.setFormat(MetadataFormat.valueOf(metadata.getFormat().name()));
        return apiMetadataEntity;
    }

    private Metadata convert(final MetadataEntity metadataEntity) {
        final Metadata metadata = new Metadata();
        metadata.setKey(metadataEntity.getKey());
        metadata.setName(metadataEntity.getName());
        metadata.setHidden(metadataEntity.isHidden());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
            metadata.setValue(metadataEntity.getValue().substring(0, 10));
        } else {
            metadata.setValue(metadataEntity.getValue());
        }
        return metadata;
    }

    private Metadata convertForAPI(final NewApiMetadataEntity metadataEntity) {
        final Metadata metadata = new Metadata();
        metadata.setKey(IdGenerator.generate(metadataEntity.getName()));
        metadata.setName(metadataEntity.getName());
        metadata.setHidden(metadataEntity.isHidden());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
            metadata.setValue(metadataEntity.getValue().substring(0, 10));
        } else {
            metadata.setValue(metadataEntity.getValue());
        }

        metadata.setReferenceId(metadataEntity.getApiId());
        metadata.setReferenceType(MetadataReferenceType.API);

        return metadata;
    }

    private Metadata convertForAPI(final UpdateApiMetadataEntity metadataEntity) {
        final Metadata metadata = new Metadata();
        metadata.setKey(metadataEntity.getKey());
        metadata.setName(metadataEntity.getName());
        metadata.setHidden(metadataEntity.isHidden());
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.valueOf(metadataEntity.getFormat().name()));

        if (MetadataFormat.DATE.equals(metadataEntity.getFormat())) {
            metadata.setValue(metadataEntity.getValue().substring(0, 10));
        } else {
            metadata.setValue(metadataEntity.getValue());
        }

        metadata.setReferenceId(metadataEntity.getApiId());
        metadata.setReferenceType(MetadataReferenceType.API);

        return metadata;
    }
}
