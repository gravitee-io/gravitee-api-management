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

import java.util.Map;

public interface AnalyticsMetadataProvider {
    enum Field {
        API("api-id"),
        APPLICATION("application-id"),
        PLAN("plan-id"),
        GEOIP_COUNTRY_ISO_CODE("geoip.country_iso_code"),
        GENERIC("");

        private final String value;

        Field(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Field of(String value) {
            for (Field field : Field.values()) {
                if (field.value.equalsIgnoreCase(value)) {
                    return field;
                }
            }
            return GENERIC;
        }
    }

    boolean appliesTo(Field field);

    Map<String, String> provide(String key, String environmentId);
}
