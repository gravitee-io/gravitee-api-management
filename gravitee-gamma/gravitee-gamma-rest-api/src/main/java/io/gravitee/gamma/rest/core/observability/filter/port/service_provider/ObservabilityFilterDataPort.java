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
package io.gravitee.gamma.rest.core.observability.filter.port.service_provider;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core-side port onto the store-backed observability filter data: distinct values for KEYWORD
 * filters and bulk id → label resolution. The infra adapter delegates to the platform's analytics
 * services (which own the Elasticsearch translation, the management-DB name lookups, and the
 * caller-scoped authorized-API set), so the core stays unaware of those mechanics.
 *
 * <p>Tenancy / actor scoping is resolved by the adapter from the ambient request context (the same
 * source the rest of the rest-api uses) — callers do not thread organization / environment / user
 * ids through this port.
 *
 * @author GraviteeSource Team
 */
public interface ObservabilityFilterDataPort {
    /**
     * Distinct selectable values for a KEYWORD filter. For id-based filters
     * ({@code API}/{@code APPLICATION}/{@code PLAN}/{@code API_PRODUCT}) the returned
     * {@link io.gravitee.gamma.rest.core.observability.filter.model.FilterValue#value()} is the
     * entity id and {@code label} its display name; for direct-value filters the value is the raw
     * indexed value and {@code label} is {@code null}.
     *
     * @param apiTypes   optional API-kind constraint to narrow results to the calls those kinds select,
     *                   the entrypoints adding them included (empty = unconstrained)
     * @param apiProduct the single API product the request is scoped to, or {@code null}: only its calls count, and a
     *                   reader of that AI workspace gets the values its aggregates show them
     */
    FilterValuesPage listKeywordValues(
        String filterName,
        String query,
        Long from,
        Long to,
        int page,
        int perPage,
        Set<ApiType> apiTypes,
        String apiProduct
    );

    /**
     * Bulk id → label resolution, grouped per requested filter.
     *
     * @param apiProduct the single API product the request is scoped to, or {@code null}: a reader of that AI workspace
     *                   also gets the labels of what its aggregates show them
     */
    List<ResolvedLabels> resolveLabels(List<ResolveRequest> requests, String apiProduct);

    record ResolveRequest(String filterName, List<String> ids) {}

    record ResolvedLabels(String filterName, Map<String, String> labels) {}
}
