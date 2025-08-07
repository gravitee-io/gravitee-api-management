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
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@DomainService
public class ApplicationMetadataProvider implements AnalyticsMetadataProvider {

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_UNKNOWN_APPLICATION_NAME = "Unknown application (keyless)";
    private static final String METADATA_DELETED_APPLICATION_NAME = "Deleted application";
    private static final String UNKNOWN_SERVICE = "1";
    private static final String UNKNOWN_SERVICE_MAPPED = "?";
    private static final String ARCHIVED = "ARCHIVED";

    private final ApplicationCrudService applicationCrudService;

    public ApplicationMetadataProvider(ApplicationCrudService applicationCrudService) {
        this.applicationCrudService = applicationCrudService;
    }

    @Override
    public boolean appliesTo(Field field) {
        return field == Field.APPLICATION;
    }

    @Override
    public Map<String, String> provide(String key, String environmentId) {
        return provide(List.of(key), environmentId).getOrDefault(key, Map.of());
    }

    record ApplicationMetadata(String name, boolean unknown, boolean deleted) {
        Map<String, String> toMap() {
            var result = new HashMap<String, String>();
            result.put(METADATA_NAME, name);
            if (unknown) {
                result.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            }
            if (deleted) {
                result.put(METADATA_DELETED, Boolean.TRUE.toString());
            }
            return result;
        }
    }

    private static final ApplicationMetadata UNKNOWN_APPLICATION = new ApplicationMetadata(METADATA_UNKNOWN_APPLICATION_NAME, true, false);
    private static final ApplicationMetadata NOT_FOUND = new ApplicationMetadata(METADATA_DELETED_APPLICATION_NAME, false, true);

    private static ApplicationMetadata ofApplication(BaseApplicationEntity app) {
        return new ApplicationMetadata(app.getName(), false, ARCHIVED.equals(app.getStatus()));
    }

    @Override
    public Map<String, Map<String, String>> provide(List<String> keys, String environmentId) {
        // Build Map<String, ApplicationMetadata>
        Map<String, ApplicationMetadata> metaMap = new HashMap<>();

        // Unknown applications
        keys
            .stream()
            .filter(key -> UNKNOWN_SERVICE.equals(key) || UNKNOWN_SERVICE_MAPPED.equals(key))
            .forEach(key -> metaMap.put(key, UNKNOWN_APPLICATION));

        // Batch for real application ids
        List<String> appIds = keys.stream().filter(key -> !UNKNOWN_SERVICE.equals(key) && !UNKNOWN_SERVICE_MAPPED.equals(key)).toList();

        if (!appIds.isEmpty()) {
            var applications = applicationCrudService
                .findByIds(appIds, environmentId)
                .stream()
                .collect(Collectors.toMap(BaseApplicationEntity::getId, Function.identity()));

            // Found applications
            applications.forEach((id, app) -> metaMap.put(id, ofApplication(app)));

            // Not found applications
            appIds.stream().filter(id -> !applications.containsKey(id)).forEach(id -> metaMap.put(id, NOT_FOUND));
        }

        // Build output Map<String, Map<String, String>>
        return metaMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toMap()));
    }
}
