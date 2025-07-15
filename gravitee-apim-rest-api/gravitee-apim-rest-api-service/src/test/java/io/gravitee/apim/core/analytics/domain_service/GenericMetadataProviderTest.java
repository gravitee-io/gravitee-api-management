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

import java.util.Map;
import org.junit.jupiter.api.Test;

class GenericMetadataProviderTest {

    @Test
    void appliesTo_shouldReturnTrueForGenericField() {
        var provider = new GenericMetadataProvider();
        assertTrue(provider.appliesTo(AnalyticsMetadataProvider.Field.GENERIC));
    }

    @Test
    void appliesTo_shouldReturnFalseForOtherFields() {
        var provider = new GenericMetadataProvider();
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.API));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.PLAN));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.APPLICATION));
    }

    @Test
    void provide_shouldReturnKeyAsName() {
        var provider = new GenericMetadataProvider();
        Map<String, String> metadata = provider.provide("custom-key", "env-id");
        assertEquals("custom-key", metadata.get("name"));
        assertEquals(1, metadata.size());
    }
}
