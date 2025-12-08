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
package io.gravitee.apim.infra.query_service.api;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiMetadataQueryServiceImpl implements ApiMetadataQueryService {

    private final MetadataRepository metadataRepository;

    public ApiMetadataQueryServiceImpl(@Lazy MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    @Override
    public Map<String, ApiMetadata> findApiMetadata(final String environmentId, final String apiId) {
        try {
            Map<String, ApiMetadata> apiMetadata = metadataRepository
                .findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, environmentId)
                .stream()
                .map(this::toEnvironmentApiMetadata)
                .collect(toMap(ApiMetadata::getKey, Function.identity()));

            metadataRepository
                .findByReferenceTypeAndReferenceId(MetadataReferenceType.API, apiId)
                .forEach(metadata ->
                    apiMetadata.compute(metadata.getKey(), (key, existing) ->
                        Optional.ofNullable(existing)
                            .map(envEntry -> envEntry.toBuilder().apiId(apiId).name(metadata.getName()).value(metadata.getValue()).build())
                            .orElse(toApiSpecificApiMetadata(metadata))
                    )
                );

            return apiMetadata;
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to find metadata for an API [apiId={}]", apiId, e);
            throw new TechnicalManagementException(e);
        }
    }

    @Override
    public Map<String, Map<String, ApiMetadata>> findApisMetadata(final String environmentId, final Set<String> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, ApiMetadata> environmentMetadata = metadataRepository
                .findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, environmentId)
                .stream()
                .map(this::toEnvironmentApiMetadata)
                .collect(toMap(ApiMetadata::getKey, Function.identity()));

            Map<String, Map<String, ApiMetadata>> apiSpecificMetadataByApiId = new HashMap<>();
            apiIds.forEach(apiId -> apiSpecificMetadataByApiId.put(apiId, new HashMap<>()));

            metadataRepository
                .findByReferenceTypeAndReferenceIdIn(MetadataReferenceType.API, apiIds)
                .forEach(metadata -> {
                    String apiId = metadata.getReferenceId();
                    Map<String, ApiMetadata> apiSpecificMetadata = apiSpecificMetadataByApiId.get(apiId);
                    if (apiSpecificMetadata != null) {
                        apiSpecificMetadata.put(metadata.getKey(), toApiSpecificApiMetadata(metadata));
                    }
                });

            Map<String, Map<String, ApiMetadata>> result = new HashMap<>();
            apiIds.forEach(apiId -> result.put(apiId, mergedMetadataView(environmentMetadata, apiSpecificMetadataByApiId.get(apiId))));
            return result;
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApiMetadata toEnvironmentApiMetadata(io.gravitee.repository.management.model.Metadata metadata) {
        return ApiMetadata.builder()
            .key(metadata.getKey())
            .defaultValue(metadata.getValue())
            .name(metadata.getName())
            .format(Metadata.MetadataFormat.valueOf(metadata.getFormat().name()))
            .build();
    }

    private ApiMetadata toApiSpecificApiMetadata(io.gravitee.repository.management.model.Metadata metadata) {
        return ApiMetadata.builder()
            .apiId(metadata.getReferenceId())
            .key(metadata.getKey())
            .value(metadata.getValue())
            .name(metadata.getName())
            .format(Metadata.MetadataFormat.valueOf(metadata.getFormat().name()))
            .build();
    }

    private static Map<String, ApiMetadata> mergedMetadataView(
        Map<String, ApiMetadata> environmentMetadata,
        Map<String, ApiMetadata> apiSpecificMetadata
    ) {
        return new AbstractMap<>() {
            @Override
            public ApiMetadata get(Object key) {
                ApiMetadata apiEntry = apiSpecificMetadata != null ? apiSpecificMetadata.get(key) : null;
                ApiMetadata envEntry = environmentMetadata.get(key);
                if (apiEntry != null) {
                    if (envEntry != null) {
                        return apiEntry.toBuilder().defaultValue(envEntry.getDefaultValue()).build();
                    }
                    return apiEntry;
                }
                return envEntry;
            }

            @Override
            public Set<Entry<String, ApiMetadata>> entrySet() {
                Set<String> metadataKeys = new HashSet<>(environmentMetadata.keySet());
                if (apiSpecificMetadata != null) metadataKeys.addAll(apiSpecificMetadata.keySet());
                return metadataKeys
                    .stream()
                    .map(metadataKey -> new SimpleEntry<>(metadataKey, get(metadataKey)))
                    .collect(toSet());
            }
        };
    }
}
