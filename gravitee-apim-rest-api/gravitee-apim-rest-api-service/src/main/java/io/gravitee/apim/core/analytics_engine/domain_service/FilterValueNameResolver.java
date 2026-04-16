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
package io.gravitee.apim.core.analytics_engine.domain_service;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import java.util.List;
import java.util.Map;

public interface FilterValueNameResolver {
    /**
     * Resolve IDs to human-readable names for ID-based filters (API, APPLICATION, PLAN).
     *
     * @return a map from ID to display name
     */
    Map<String, String> resolveNames(String environmentId, FilterSpec.Name filterName, List<String> ids);

    /**
     * Search the management database for entities matching the query by name (case-insensitive contains).
     *
     * @return filter values with both id and display name
     */
    List<FilterValue> searchByName(String environmentId, FilterSpec.Name filterName, String query, int page, int perPage);
}
