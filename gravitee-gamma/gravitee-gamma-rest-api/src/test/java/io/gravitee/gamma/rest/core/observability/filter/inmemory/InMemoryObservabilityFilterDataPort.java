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
package io.gravitee.gamma.rest.core.observability.filter.inmemory;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory {@link ObservabilityFilterDataPort}: the distinct values a KEYWORD filter holds in the
 * store, seeded per filter name, served with the same query narrowing and 1-based pagination the
 * real store applies. Records the last listing call so a test can assert what the use case asked for.
 */
public class InMemoryObservabilityFilterDataPort implements ObservabilityFilterDataPort {

    private final Map<String, List<FilterValue>> valuesByFilter = new HashMap<>();
    private final Map<String, Map<String, String>> labelsByFilter = new HashMap<>();
    private Call lastCall;

    /** One {@link #listKeywordValues} invocation, as received. */
    public record Call(String filterName, String query, Long from, Long to, int page, int perPage, Set<ApiType> apiTypes) {}

    public InMemoryObservabilityFilterDataPort givenKeywordValues(String filterName, List<FilterValue> values) {
        valuesByFilter.put(filterName, List.copyOf(values));
        return this;
    }

    public InMemoryObservabilityFilterDataPort givenLabels(String filterName, Map<String, String> labels) {
        labelsByFilter.put(filterName, Map.copyOf(labels));
        return this;
    }

    public Optional<Call> lastCall() {
        return Optional.ofNullable(lastCall);
    }

    public void reset() {
        valuesByFilter.clear();
        labelsByFilter.clear();
        lastCall = null;
    }

    @Override
    public FilterValuesPage listKeywordValues(
        String filterName,
        String query,
        Long from,
        Long to,
        int page,
        int perPage,
        Set<ApiType> apiTypes
    ) {
        lastCall = new Call(filterName, query, from, to, page, perPage, apiTypes);
        String needle = query == null || query.isBlank() ? null : query.toLowerCase();
        List<FilterValue> matching = valuesByFilter
            .getOrDefault(filterName, List.of())
            .stream()
            .filter(value -> needle == null || matches(value, needle))
            .toList();
        int fromIndex = (int) Math.min((long) (page - 1) * perPage, matching.size());
        int toIndex = Math.min(fromIndex + perPage, matching.size());
        return new FilterValuesPage(matching.subList(fromIndex, toIndex), matching.size());
    }

    @Override
    public List<ResolvedLabels> resolveLabels(List<ResolveRequest> requests) {
        return requests
            .stream()
            .map(request -> {
                Map<String, String> known = labelsByFilter.getOrDefault(request.filterName(), Map.of());
                Map<String, String> resolved = new LinkedHashMap<>();
                request
                    .ids()
                    .stream()
                    .filter(known::containsKey)
                    .forEach(id -> resolved.put(id, known.get(id)));
                return new ResolvedLabels(request.filterName(), resolved);
            })
            .toList();
    }

    private static boolean matches(FilterValue value, String needle) {
        return (value.value().toLowerCase().contains(needle) || (value.label() != null && value.label().toLowerCase().contains(needle)));
    }
}
