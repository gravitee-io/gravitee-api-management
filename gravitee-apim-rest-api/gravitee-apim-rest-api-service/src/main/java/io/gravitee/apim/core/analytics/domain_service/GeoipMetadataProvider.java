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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@DomainService
public class GeoipMetadataProvider implements AnalyticsMetadataProvider {

    private static final String METADATA_NAME = "name";

    @Override
    public boolean appliesTo(Field field) {
        return field == Field.GEOIP_COUNTRY_ISO_CODE;
    }

    @Override
    public Map<String, String> provide(String key, String environmentId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_NAME, (new Locale("", key)).getDisplayCountry(Locale.UK));
        return metadata;
    }
}
