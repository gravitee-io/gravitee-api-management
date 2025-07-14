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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> metadata = new HashMap<>();
        try {
            if (UNKNOWN_SERVICE.equals(key) || UNKNOWN_SERVICE_MAPPED.equals(key)) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else {
                var api = apiCrudService.get(key);
                metadata.put(METADATA_NAME, api.getName());
                metadata.put(METADATA_VERSION, api.getVersion());
                if (Api.ApiLifecycleState.ARCHIVED == api.getApiLifecycleState()) {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                }
            }
        } catch (ApiNotFoundException e) {
            metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            metadata.put(METADATA_NAME, METADATA_DELETED_API_NAME);
        }
        return metadata;
    }
}
