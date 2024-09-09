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

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
                .map(m ->
                    ApiMetadata
                        .builder()
                        .key(m.getKey())
                        .defaultValue(m.getValue())
                        .name(m.getName())
                        .format(Metadata.MetadataFormat.valueOf(m.getFormat().name()))
                        .build()
                )
                .collect(toMap(ApiMetadata::getKey, Function.identity()));

            metadataRepository
                .findByReferenceTypeAndReferenceId(MetadataReferenceType.API, apiId)
                .forEach(m ->
                    apiMetadata.compute(
                        m.getKey(),
                        (key, existing) ->
                            Optional
                                .ofNullable(existing)
                                .map(value -> value.toBuilder().apiId(apiId).name(m.getName()).value(m.getValue()).build())
                                .orElse(
                                    ApiMetadata
                                        .builder()
                                        .apiId(m.getReferenceId())
                                        .key(m.getKey())
                                        .value(m.getValue())
                                        .name(m.getName())
                                        .format(Metadata.MetadataFormat.valueOf(m.getFormat().name()))
                                        .build()
                                )
                    )
                );

            return apiMetadata;
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to find metadata for an API [apiId={}]", apiId, e);
            throw new TechnicalManagementException(e);
        }
    }
}
