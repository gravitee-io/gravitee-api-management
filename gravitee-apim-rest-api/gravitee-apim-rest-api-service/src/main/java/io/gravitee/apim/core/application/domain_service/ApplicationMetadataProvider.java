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
package io.gravitee.apim.core.application.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> metadata = new HashMap<>();
        try {
            if (UNKNOWN_SERVICE.equals(key) || UNKNOWN_SERVICE_MAPPED.equals(key)) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_APPLICATION_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else {
                var app = applicationCrudService.findById(key, environmentId);
                metadata.put(METADATA_NAME, app.getName());
                if (ARCHIVED.equals(app.getStatus())) {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                }
            }
        } catch (ApplicationNotFoundException e) {
            metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            metadata.put(METADATA_NAME, METADATA_DELETED_APPLICATION_NAME);
        }
        return metadata;
    }
}
