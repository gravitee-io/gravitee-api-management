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
package io.gravitee.apim.core.plan.domain_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.analytics.domain_service.PlanMetadataProvider;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.exception.PlanNotFoundException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlanMetadataProviderTest {

    @Test
    void appliesTo_shouldReturnTrueForPlanField() {
        var provider = new PlanMetadataProvider(mock(PlanCrudService.class));
        assertTrue(provider.appliesTo(AnalyticsMetadataProvider.Field.PLAN));
    }

    @Test
    void appliesTo_shouldReturnFalseForOtherFields() {
        var provider = new PlanMetadataProvider(mock(PlanCrudService.class));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.API));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.APPLICATION));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.GENERIC));
    }

    @Test
    void provide_shouldReturnPlanMetadata() {
        var crudService = mock(PlanCrudService.class);
        var plan = mock(io.gravitee.apim.core.plan.model.Plan.class);
        when(plan.getName()).thenReturn("Test Plan");
        when(crudService.getById("plan-id")).thenReturn(plan);

        var provider = new PlanMetadataProvider(crudService);
        Map<String, String> metadata = provider.provide("plan-id", "env-id");

        assertEquals("Test Plan", metadata.get("name"));
        assertFalse(metadata.containsKey("deleted"));
        assertFalse(metadata.containsKey("unknown"));
    }

    @Test
    void provide_shouldReturnUnknownPlanMetadataForUnknownKey() {
        var provider = new PlanMetadataProvider(mock(PlanCrudService.class));
        Map<String, String> metadata1 = provider.provide("1", "env-id");
        Map<String, String> metadata2 = provider.provide("?", "env-id");

        assertEquals("Unknown plan (keyless)", metadata1.get("name"));
        assertEquals("true", metadata1.get("unknown"));
        assertEquals("Unknown plan (keyless)", metadata2.get("name"));
        assertEquals("true", metadata2.get("unknown"));
    }

    @Test
    void provide_shouldReturnDeletedPlanMetadataWhenNotFound() {
        var crudService = mock(PlanCrudService.class);
        when(crudService.getById("deleted-id")).thenThrow(new PlanNotFoundException("deleted-id"));

        var provider = new PlanMetadataProvider(crudService);
        Map<String, String> metadata = provider.provide("deleted-id", "env-id");

        assertEquals("Deleted plan", metadata.get("name"));
        assertEquals("true", metadata.get("deleted"));
    }
}
