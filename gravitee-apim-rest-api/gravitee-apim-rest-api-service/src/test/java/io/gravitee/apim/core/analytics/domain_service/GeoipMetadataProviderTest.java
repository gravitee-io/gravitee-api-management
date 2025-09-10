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

class GeoipMetadataProviderTest {

    @Test
    void appliesTo_shouldReturnTrueForGeoipCountryIsoCodeField() {
        GeoipMetadataProvider provider = new GeoipMetadataProvider();
        assertTrue(provider.appliesTo(AnalyticsMetadataProvider.Field.GEOIP_COUNTRY_ISO_CODE));
    }

    @Test
    void appliesTo_shouldReturnFalseForOtherFields() {
        GeoipMetadataProvider provider = new GeoipMetadataProvider();
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.API));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.PLAN));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.APPLICATION));
        assertFalse(provider.appliesTo(AnalyticsMetadataProvider.Field.GENERIC));
    }

    @Test
    void provide_shouldReturnCountryNameForValidIsoCode() {
        GeoipMetadataProvider provider = new GeoipMetadataProvider();
        Map<String, String> metadata = provider.provide("FR", "env-id");
        assertEquals("France", metadata.get("name"));
    }

    @Test
    void provide_shouldReturnEmptyStringForInvalidIsoCode() {
        GeoipMetadataProvider provider = new GeoipMetadataProvider();
        Map<String, String> metadata = provider.provide("ZZ", "env-id");
        assertEquals("Unknown Region", metadata.get("name"));
    }
}
