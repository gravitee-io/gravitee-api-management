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
package io.gravitee.apim.core.analytics_engine.query_service;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface FilterValuesQueryService {
    /** For a caller not scoped by API kind: every call of {@code authorizedApiIds}. */
    default FilterValuesPage searchFilterValues(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        int page,
        int size,
        Map<String, Object> afterKey,
        String searchPattern,
        Set<String> authorizedApiIds
    ) {
        return searchFilterValues(
            organizationId,
            environmentId,
            filterName,
            from,
            to,
            page,
            size,
            afterKey,
            searchPattern,
            authorizedApiIds,
            null
        );
    }

    /**
     * @param trafficScope narrows the calls of {@code authorizedApiIds} the values come from, {@code null} when the
     *                     caller is not scoped by API kind
     */
    default FilterValuesPage searchFilterValues(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        int page,
        int size,
        Map<String, Object> afterKey,
        String searchPattern,
        Set<String> authorizedApiIds,
        TrafficScope trafficScope
    ) {
        return searchFilterValues(
            organizationId,
            environmentId,
            filterName,
            from,
            to,
            page,
            size,
            afterKey,
            searchPattern,
            authorizedApiIds,
            trafficScope,
            null
        );
    }

    /**
     * @param trafficScope narrows the calls of {@code authorizedApiIds} the values come from, {@code null} when the
     *                     caller is not scoped by API kind
     * @param apiProductId keeps the calls made through that API product only, {@code null} when it does not matter
     */
    FilterValuesPage searchFilterValues(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        int page,
        int size,
        Map<String, Object> afterKey,
        String searchPattern,
        Set<String> authorizedApiIds,
        TrafficScope trafficScope,
        String apiProductId
    );

    /** Of {@code values}, those found in the calls of {@code authorizedApiIds}, within the period when one is given. */
    default Set<String> findValuesInCalls(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        Set<String> values,
        Set<String> authorizedApiIds
    ) {
        return findValuesInCalls(organizationId, environmentId, filterName, from, to, values, authorizedApiIds, null);
    }

    /** As above, among the calls made through {@code apiProductId} only when one is given. */
    Set<String> findValuesInCalls(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        Set<String> values,
        Set<String> authorizedApiIds,
        String apiProductId
    );
}
