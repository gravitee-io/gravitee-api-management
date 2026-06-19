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
package io.gravitee.gamma.rest.core.observability.filter.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.filter.exception.ObservabilityFilterNotFoundException;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Returns a paginated list of selectable values for one filter, addressed by name. Behaviour is
 * type-driven (mirrors the apim management API's {@code GetFilterValuesUseCase}):
 *
 * <ul>
 *   <li><b>{@code ENUM}</b> — returns the spec's static {@code enumValues} (value + label),
 *       optionally case-insensitive substring-filtered by {@code query} (on value or label), then
 *       paginated in memory.</li>
 *   <li><b>{@code KEYWORD}</b> — distinct values from the data store via
 *       {@link ObservabilityFilterDataPort} (id-based listing for
 *       {@code API}/{@code APPLICATION}/{@code PLAN}/{@code API_PRODUCT}, direct values otherwise).
 *       The port surfaces {@link UnsupportedObservabilityFilterException#valueListingNotSupported}
 *       for filters the backing store cannot list yet.</li>
 *   <li><b>{@code NUMBER} / {@code STRING} / {@code BOOLEAN}</b> — no enumerable value pool, so
 *       value listing is unsupported (HTTP 400).</li>
 * </ul>
 *
 * <p>An unknown {@code filterName} yields {@link ObservabilityFilterNotFoundException} → HTTP 404
 * (the filter is addressed via the URL path). Pagination is <b>1-based</b>; {@code perPage} is
 * clamped to {@value #MAX_PER_PAGE}.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityFilterValuesUseCase {

    private static final int DEFAULT_PER_PAGE = 10;

    /** Hard cap matching apim's values endpoint — keeps the slice (and future ES round-trips) bounded. */
    private static final int MAX_PER_PAGE = 100;

    private final FilterRegistry filterRegistry;
    private final ObservabilityFilterDataPort filterDataPort;

    public record Input(String filterName, String query, Long from, Long to, Integer page, Integer perPage, Set<ApiType> apiTypes) {
        public Input(String filterName, String query, Long from, Long to, Integer page, Integer perPage) {
            this(filterName, query, from, to, page, perPage, Set.of());
        }
    }

    public record Output(FilterValuesPage values, int page, int perPage) {}

    public Output execute(Input input) {
        Set<ApiType> apiTypes = input.apiTypes() != null ? input.apiTypes() : Set.of();
        FilterSpec spec = lookupFilter(input.filterName(), apiTypes);
        int page = resolvePage(input);
        int perPage = resolvePerPage(input);
        FilterValuesPage values = switch (spec.type()) {
            case ENUM -> handleEnum(spec, input, apiTypes, page, perPage);
            case KEYWORD -> filterDataPort.listKeywordValues(spec.name(), input.query(), input.from(), input.to(), page, perPage, apiTypes);
            case NUMBER, STRING, BOOLEAN -> throw UnsupportedObservabilityFilterException.valueListingNotSupported(
                spec.name(),
                spec.type().name()
            );
        };
        return new Output(values, page, perPage);
    }

    private static int resolvePage(Input input) {
        return (input.page() != null && input.page() > 0) ? input.page() : 1;
    }

    private static int resolvePerPage(Input input) {
        return (input.perPage() != null && input.perPage() > 0) ? Math.min(input.perPage(), MAX_PER_PAGE) : DEFAULT_PER_PAGE;
    }

    private FilterSpec lookupFilter(String filterName, Set<ApiType> apiTypes) {
        return filterRegistry
            .getFilters(null, apiTypes.isEmpty() ? null : apiTypes)
            .stream()
            .filter(s -> s.name().equals(filterName))
            .findFirst()
            .orElseThrow(() -> new ObservabilityFilterNotFoundException(filterName));
    }

    private static FilterValuesPage handleEnum(FilterSpec spec, Input input, Set<ApiType> apiTypes, int page, int perPage) {
        List<FilterSpec.EnumValue> enumValues = spec.enumValues() == null ? List.of() : spec.enumValues();
        if (!apiTypes.isEmpty() && "API_TYPE".equals(spec.name())) {
            Set<String> allowedValues = apiTypes.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
            enumValues = enumValues
                .stream()
                .filter(v -> allowedValues.contains(v.value()))
                .toList();
        }
        if (enumValues.isEmpty()) {
            return new FilterValuesPage(List.of(), 0L);
        }

        String queryLower = input.query() == null || input.query().isBlank() ? null : input.query().toLowerCase();
        List<FilterValue> matching = enumValues
            .stream()
            .filter(v -> queryLower == null || matches(v, queryLower))
            .map(v -> new FilterValue(v.value(), v.label()))
            .toList();

        int fromIndex = (int) Math.min((long) (page - 1) * perPage, matching.size());
        int toIndex = Math.min(fromIndex + perPage, matching.size());

        return new FilterValuesPage(matching.subList(fromIndex, toIndex), (long) matching.size());
    }

    private static boolean matches(FilterSpec.EnumValue value, String queryLower) {
        return (
            value.value().toLowerCase().contains(queryLower) || (value.label() != null && value.label().toLowerCase().contains(queryLower))
        );
    }
}
