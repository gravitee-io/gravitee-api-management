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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationMetadataProviderTest {

    @Test
    void appliesTo_shouldReturnTrueForApplicationField() {
        var provider = new ApplicationMetadataProvider(mock(ApplicationCrudService.class));
        assertTrue(provider.appliesTo(AnalyticsMetadataProvider.Field.APPLICATION));
    }

    @Test
    void appliesTo_shouldReturnFalseForOtherFields() {
        var provider = new ApplicationMetadataProvider(mock(ApplicationCrudService.class));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.API));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.PLAN));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.GENERIC));
    }

    @Test
    void provide_shouldReturnApplicationMetadata() {
        var crudService = mock(ApplicationCrudService.class);
        var app = mock(BaseApplicationEntity.class);
        when(app.getName()).thenReturn("Test App");
        when(app.getStatus()).thenReturn("ACTIVE");
        when(app.getId()).thenReturn("app-id");
        when(crudService.findByIds(List.of("app-id"), "env-id")).thenReturn(List.of(app));

        var provider = new ApplicationMetadataProvider(crudService);
        Map<String, String> metadata = provider.provide("app-id", "env-id");

        assertEquals("Test App", metadata.get("name"));
        assertFalse(metadata.containsKey("deleted"));
        assertFalse(metadata.containsKey("unknown"));
    }

    @Test
    void provide_shouldReturnUnknownApplicationMetadataForUnknownKey() {
        var provider = new ApplicationMetadataProvider(mock(ApplicationCrudService.class));
        Map<String, String> metadata1 = provider.provide("1", "env-id");
        Map<String, String> metadata2 = provider.provide("?", "env-id");

        assertEquals("Unknown application (keyless)", metadata1.get("name"));
        assertEquals("true", metadata1.get("unknown"));
        assertEquals("Unknown application (keyless)", metadata2.get("name"));
        assertEquals("true", metadata2.get("unknown"));
    }

    @Test
    void provide_shouldReturnDeletedApplicationMetadataWhenNotFound() {
        var crudService = mock(ApplicationCrudService.class);
        when(crudService.findByIds(List.of("deleted-id"), "env-id")).thenReturn(List.of());

        var provider = new ApplicationMetadataProvider(crudService);
        Map<String, String> metadata = provider.provide("deleted-id", "env-id");

        assertEquals("Deleted application", metadata.get("name"));
        assertEquals("true", metadata.get("deleted"));
    }

    @Test
    void provide_shouldReturnDeletedApplicationMetadataWhenArchived() {
        var crudService = mock(ApplicationCrudService.class);
        var app = mock(BaseApplicationEntity.class);
        when(app.getName()).thenReturn("Archived App");
        when(app.getStatus()).thenReturn("ARCHIVED");
        when(app.getId()).thenReturn("archived-id");
        when(crudService.findByIds(List.of("archived-id"), "env-id")).thenReturn(List.of(app));

        var provider = new ApplicationMetadataProvider(crudService);
        Map<String, String> metadata = provider.provide("archived-id", "env-id");

        assertEquals("Archived App", metadata.get("name"));
        assertEquals("true", metadata.get("deleted"));
    }
}
