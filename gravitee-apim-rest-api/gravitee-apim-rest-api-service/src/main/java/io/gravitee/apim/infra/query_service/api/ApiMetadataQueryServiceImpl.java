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
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        return findApiMetadataForApis(environmentId, List.of(apiId)).getOrDefault(apiId, Map.of());
    }

    @Override
    public Map<String, Map<String, ApiMetadata>> findApiMetadataForApis(final String environmentId, final Collection<String> apiIds) {
        List<String> distinctApiIds = normalizeApiIds(apiIds);
        if (distinctApiIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, ApiMetadata> environmentDefaults = loadEnvironmentMetadata(environmentId);
            return mergeEnvironmentWithEachApi(distinctApiIds, environmentDefaults);
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to find metadata for APIs in environment [{}]", environmentId, e);
            throw new TechnicalManagementException(e);
        }
    }

    private static List<String> normalizeApiIds(Collection<String> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return List.of();
        }
        return apiIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    private Map<String, Map<String, ApiMetadata>> mergeEnvironmentWithEachApi(
        List<String> apiIds,
        Map<String, ApiMetadata> environmentDefaults
    ) throws TechnicalException {
        Map<String, Map<String, ApiMetadata>> result = new LinkedHashMap<>(apiIds.size());
        for (String apiId : apiIds) {
            result.put(apiId, mergedMetadataForApi(environmentDefaults, apiId));
        }
        return result;
    }

    private Map<String, ApiMetadata> mergedMetadataForApi(Map<String, ApiMetadata> environmentDefaults, String apiId)
        throws TechnicalException {
        Map<String, ApiMetadata> merged = new HashMap<>(environmentDefaults);
        applyApiScopedMetadata(merged, apiId);
        return merged;
    }

    private Map<String, ApiMetadata> loadEnvironmentMetadata(String environmentId) throws TechnicalException {
        return metadataRepository
            .findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, environmentId)
            .stream()
            .map(ApiMetadataQueryServiceImpl::apiMetadataFromEnvironmentRow)
            .collect(toMap(ApiMetadata::getKey, Function.identity()));
    }

    private void applyApiScopedMetadata(Map<String, ApiMetadata> target, String apiId) throws TechnicalException {
        for (var row : loadApiMetadataRows(apiId)) {
            mergeApiMetadataRowInto(target, apiId, row);
        }
    }

    private Collection<io.gravitee.repository.management.model.Metadata> loadApiMetadataRows(String apiId) throws TechnicalException {
        return metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.API, apiId);
    }

    private static void mergeApiMetadataRowInto(
        Map<String, ApiMetadata> target,
        String apiId,
        io.gravitee.repository.management.model.Metadata row
    ) {
        target.merge(row.getKey(), apiMetadataFromApiRow(row), (existing, ignoredDuplicateFromMerge) ->
            mergeApiRowOntoExisting(existing, apiId, row)
        );
    }

    private static ApiMetadata apiMetadataFromEnvironmentRow(io.gravitee.repository.management.model.Metadata m) {
        return ApiMetadata.builder().key(m.getKey()).defaultValue(m.getValue()).name(m.getName()).format(toCoreFormat(m)).build();
    }

    private static ApiMetadata apiMetadataFromApiRow(io.gravitee.repository.management.model.Metadata m) {
        return ApiMetadata.builder()
            .apiId(m.getReferenceId())
            .key(m.getKey())
            .value(m.getValue())
            .name(m.getName())
            .format(toCoreFormat(m))
            .build();
    }

    private static ApiMetadata mergeApiRowOntoExisting(
        ApiMetadata existing,
        String apiId,
        io.gravitee.repository.management.model.Metadata row
    ) {
        return existing.toBuilder().apiId(apiId).name(row.getName()).value(row.getValue()).build();
    }

    private static Metadata.MetadataFormat toCoreFormat(io.gravitee.repository.management.model.Metadata m) {
        return Metadata.MetadataFormat.valueOf(m.getFormat().name());
    }
}
