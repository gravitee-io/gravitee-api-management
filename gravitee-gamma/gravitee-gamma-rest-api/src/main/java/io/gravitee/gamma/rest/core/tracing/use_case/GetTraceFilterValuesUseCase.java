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
package io.gravitee.gamma.rest.core.tracing.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.tracing.exception.TraceFilterNotFoundException;
import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterValue;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterValuesPage;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterRegistry;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Returns a paginated list of allowed values for a given filter. Behaviour is type-driven (mirrors
 * the apim management API's {@code GetFilterValuesUseCase}):
 *
 * <ul>
 *   <li><b>{@code ENUM}</b> — returns the static {@code enumValues} from the spec, optionally
 *       substring-filtered by {@code query} (case-insensitive), then paginated.</li>
 *   <li><b>{@code KEYWORD}</b> — not implemented in the slim cut (no keyword filter is exposed
 *       today). Throws {@link UnsupportedFilterException}. The follow-up PR wires this through a
 *       terms aggregation on the tracing repository.</li>
 *   <li><b>{@code NUMBER} / {@code STRING} / {@code BOOLEAN}</b> — no value pool to paginate;
 *       throws {@link UnsupportedFilterException} with
 *       {@code tracing.filter.value_listing_not_supported}.</li>
 * </ul>
 *
 * <p>An unknown {@code filterName} (not present in the registry for the given {@code moduleId})
 * throws {@link TraceFilterNotFoundException} → HTTP 404. Different from the search endpoint's
 * translator throwing {@code UnsupportedFilterException.unknownName} (HTTP 400): the values
 * endpoint addresses the filter via the URL path, so missing-path-segment is the natural 404.
 *
 * <p>Pagination is <b>1-based</b> to match the apim values endpoint convention. {@code perPage}
 * has a hard upper cap to keep the slice cheap (the ENUM path operates on small in-memory lists
 * anyway, but the cap stays in place once the KEYWORD path queries ES in the follow-up).
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetTraceFilterValuesUseCase {

    private static final int DEFAULT_PER_PAGE = 10;

    /** Hard cap matching apim's values endpoint — keeps ES round-trips bounded once KEYWORD ships. */
    private static final int MAX_PER_PAGE = 100;

    private final TraceFilterRegistry filterRegistry;

    public record Input(
        String moduleId,
        String filterName,
        /** Case-insensitive substring filter applied to {@code enumValues}. {@code null} / blank means no filter. */
        String query,
        /** 1-based page number. {@code null} means page 1. */
        Integer page,
        /** Page size. {@code null} means {@value #DEFAULT_PER_PAGE}. Clamped to {@value #MAX_PER_PAGE}. */
        Integer perPage
    ) {}

    public record Output(TraceFilterValuesPage page) {}

    public Output execute(Input input) {
        TraceFilterSpec spec = lookupFilter(input.moduleId(), input.filterName());
        return switch (spec.type()) {
            case ENUM -> new Output(handleEnum(spec, input));
            case KEYWORD -> throw UnsupportedFilterException.valueListingNotSupported(spec.name(), spec.type().name());
            // No keyword filter exists in the slim cut — once one lands (e.g. AIM's LLM_MODEL) the
            // case above gets a real handler that queries the tracing repository for distinct values.
            case NUMBER, STRING, BOOLEAN -> throw UnsupportedFilterException.valueListingNotSupported(spec.name(), spec.type().name());
        };
    }

    private TraceFilterSpec lookupFilter(String moduleId, String filterName) {
        return filterRegistry
            .getFiltersForModule(moduleId)
            .stream()
            .filter(s -> s.name().equals(filterName))
            .findFirst()
            .orElseThrow(() -> new TraceFilterNotFoundException(filterName));
    }

    private static TraceFilterValuesPage handleEnum(TraceFilterSpec spec, Input input) {
        List<String> enumValues = spec.enumValues() == null ? List.of() : spec.enumValues();
        if (enumValues.isEmpty()) {
            return new TraceFilterValuesPage(List.of(), 0L);
        }

        String queryLower = input.query() == null || input.query().isBlank() ? null : input.query().toLowerCase();
        List<TraceFilterValue> matching = enumValues
            .stream()
            .filter(v -> queryLower == null || v.toLowerCase().contains(queryLower))
            .map(v -> new TraceFilterValue(v, null))
            .toList();

        int page = (input.page() != null && input.page() > 0) ? input.page() : 1;
        int perPage = (input.perPage() != null && input.perPage() > 0) ? Math.min(input.perPage(), MAX_PER_PAGE) : DEFAULT_PER_PAGE;
        int fromIndex = Math.min((page - 1) * perPage, matching.size());
        int toIndex = Math.min(fromIndex + perPage, matching.size());

        return new TraceFilterValuesPage(matching.subList(fromIndex, toIndex), (long) matching.size());
    }
}
