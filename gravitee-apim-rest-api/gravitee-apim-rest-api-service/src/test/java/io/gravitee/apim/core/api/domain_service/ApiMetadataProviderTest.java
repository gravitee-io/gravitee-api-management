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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiMetadataProviderTest {

    @Test
    void appliesTo_shouldReturnTrueForApiField() {
        var provider = new ApiMetadataProvider(mock(ApiCrudService.class));
        assertTrue(provider.appliesTo(AnalyticsMetadataProvider.Field.API));
    }

    @Test
    void appliesTo_shouldReturnFalseForOtherFields() {
        var provider = new ApiMetadataProvider(mock(ApiCrudService.class));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.PLAN));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.APPLICATION));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.GENERIC));
    }

    @Test
    void provide_shouldReturnApiMetadata() {
        var apiCrudService = mock(ApiCrudService.class);
        var api = mock(Api.class);
        when(api.getName()).thenReturn("Test API");
        when(api.getVersion()).thenReturn("v1");
        when(api.getApiLifecycleState()).thenReturn(Api.ApiLifecycleState.PUBLISHED);
        when(apiCrudService.get("api-id")).thenReturn(api);

        var provider = new ApiMetadataProvider(apiCrudService);
        Map<String, String> metadata = provider.provide("api-id", "env-id");

        assertEquals("Test API", metadata.get("name"));
        assertEquals("v1", metadata.get("version"));
        assertFalse(metadata.containsKey("deleted"));
        assertFalse(metadata.containsKey("unknown"));
    }

    @Test
    void provide_shouldReturnUnknownApiMetadataForUnknownKey() {
        var provider = new ApiMetadataProvider(mock(ApiCrudService.class));
        Map<String, String> metadata1 = provider.provide("1", "env-id");
        Map<String, String> metadata2 = provider.provide("?", "env-id");

        assertEquals("Unknown API (not found)", metadata1.get("name"));
        assertEquals("true", metadata1.get("unknown"));
        assertEquals("Unknown API (not found)", metadata2.get("name"));
        assertEquals("true", metadata2.get("unknown"));
    }

    @Test
    void provide_shouldReturnDeletedApiMetadataWhenNotFound() {
        var apiCrudService = mock(ApiCrudService.class);
        when(apiCrudService.get("deleted-id")).thenThrow(new ApiNotFoundException("deleted-id"));

        var provider = new ApiMetadataProvider(apiCrudService);
        Map<String, String> metadata = provider.provide("deleted-id", "env-id");

        assertEquals("Deleted API", metadata.get("name"));
        assertEquals("true", metadata.get("deleted"));
    }

    @Test
    void provide_shouldReturnDeletedApiMetadataWhenArchived() {
        var apiCrudService = mock(ApiCrudService.class);
        var api = mock(Api.class);
        when(api.getName()).thenReturn("Archived API");
        when(api.getVersion()).thenReturn("v2");
        when(api.getApiLifecycleState()).thenReturn(Api.ApiLifecycleState.ARCHIVED);
        when(apiCrudService.get("archived-id")).thenReturn(api);

        var provider = new ApiMetadataProvider(apiCrudService);
        Map<String, String> metadata = provider.provide("archived-id", "env-id");

        assertEquals("Archived API", metadata.get("name"));
        assertEquals("v2", metadata.get("version"));
        assertEquals("true", metadata.get("deleted"));
    }
}
