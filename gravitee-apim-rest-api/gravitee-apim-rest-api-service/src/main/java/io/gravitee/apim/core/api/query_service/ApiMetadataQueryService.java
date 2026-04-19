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
package io.gravitee.apim.core.api.query_service;

import io.gravitee.apim.core.api.model.ApiMetadata;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public interface ApiMetadataQueryService {
    /**
     * Find all metadata with their default value for an API.
     * @param environmentId The environment id.
     * @param apiId The API id.
     * @return A map of metadata key and metadata.
     */
    Map<String, ApiMetadata> findApiMetadata(String environmentId, String apiId);

    /**
     * Resolves metadata for several APIs. Default implementation delegates to {@link #findApiMetadata(String, String)}
     * per id; production code overrides this to load environment-level metadata only once.
     *
     * @param environmentId environment id
     * @param apiIds API ids (null ids skipped; duplicates resolved once)
     * @return map of apiId → merged metadata (same shape as {@link #findApiMetadata(String, String)})
     */
    default Map<String, Map<String, ApiMetadata>> findApiMetadataForApis(String environmentId, Collection<String> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, ApiMetadata>> result = new LinkedHashMap<>();
        for (String apiId : apiIds.stream().filter(Objects::nonNull).distinct().toList()) {
            result.put(apiId, findApiMetadata(environmentId, apiId));
        }
        return result;
    }
}
