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
package io.gravitee.apim.core.analytics.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@DomainService
public class ApiMetadataProvider implements AnalyticsMetadataProvider {

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_VERSION = "version";
    private static final String METADATA_UNKNOWN_API_NAME = "Unknown API (not found)";
    private static final String METADATA_DELETED_API_NAME = "Deleted API";
    private static final String UNKNOWN_SERVICE = "1";
    private static final String UNKNOWN_SERVICE_MAPPED = "?";

    private final ApiCrudService apiCrudService;

    public ApiMetadataProvider(ApiCrudService apiCrudService) {
        this.apiCrudService = apiCrudService;
    }

    @Override
    public boolean appliesTo(Field field) {
        return field == Field.API;
    }

    @Override
    public Map<String, String> provide(String key, String environmentId) {
        return provide(List.of(key), environmentId).getOrDefault(key, Map.of());
    }

    record ApiMetadata(String name, boolean unknown, String version, boolean deleted) {
        Map<String, String> toMap() {
            var result = new HashMap<String, String>();
            result.put(METADATA_NAME, name);
            if (unknown) {
                result.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            }
            if (version != null) {
                result.put(METADATA_VERSION, version);
            }
            if (deleted) {
                result.put(METADATA_DELETED, Boolean.TRUE.toString());
            }
            return result;
        }
    }

    private static final ApiMetadata UNKNOWN_API = new ApiMetadata(METADATA_UNKNOWN_API_NAME, true, null, false);
    private static final ApiMetadata NOT_FOUND = new ApiMetadata(METADATA_DELETED_API_NAME, false, null, true);

    private static ApiMetadata ofApi(Api api) {
        return new ApiMetadata(api.getName(), false, api.getVersion(), api.getApiLifecycleState() == Api.ApiLifecycleState.ARCHIVED);
    }

    @Override
    public Map<String, Map<String, String>> provide(List<String> keys, String environmentId) {
        // Build Map<String, ApiMetadata>
        Map<String, ApiMetadata> metaMap = new HashMap<>();

        // Unknown services
        keys
            .stream()
            .filter(key -> UNKNOWN_SERVICE.equals(key) || UNKNOWN_SERVICE_MAPPED.equals(key))
            .forEach(key -> metaMap.put(key, UNKNOWN_API));

        // Batch for real API ids
        List<String> apiIds = keys
            .stream()
            .filter(key -> !UNKNOWN_SERVICE.equals(key) && !UNKNOWN_SERVICE_MAPPED.equals(key))
            .toList();

        if (!apiIds.isEmpty()) {
            Map<String, Api> apis = apiCrudService.findByIds(apiIds).stream().collect(Collectors.toMap(Api::getId, Function.identity()));

            // Found APIs
            apis.forEach((id, api) -> metaMap.put(id, ofApi(api)));

            // Not found APIs
            apiIds
                .stream()
                .filter(id -> !apis.containsKey(id))
                .forEach(id -> metaMap.put(id, NOT_FOUND));
        }

        // Build output Map<String, Map<String, String>>
        return metaMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toMap()));
    }
}
