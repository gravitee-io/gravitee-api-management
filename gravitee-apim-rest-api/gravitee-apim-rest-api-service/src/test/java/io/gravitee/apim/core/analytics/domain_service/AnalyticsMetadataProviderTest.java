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

import org.junit.jupiter.api.Test;

class AnalyticsMetadataProviderTest {

    @Test
    void fieldOf_shouldReturnCorrectField() {
        assertEquals(AnalyticsMetadataProvider.Field.API, AnalyticsMetadataProvider.Field.of("api-id"));
        assertEquals(AnalyticsMetadataProvider.Field.APPLICATION, AnalyticsMetadataProvider.Field.of("application-id"));
        assertEquals(AnalyticsMetadataProvider.Field.PLAN, AnalyticsMetadataProvider.Field.of("plan-id"));
        assertEquals(AnalyticsMetadataProvider.Field.GEOIP_COUNTRY_ISO_CODE, AnalyticsMetadataProvider.Field.of("geoip.country_iso_code"));
    }

    @Test
    void fieldOf_shouldReturnGenericForUnknownValue() {
        assertEquals(AnalyticsMetadataProvider.Field.GENERIC, AnalyticsMetadataProvider.Field.of("unknown-field"));
    }
}
