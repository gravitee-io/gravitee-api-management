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
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.tracing.TracingResourceFilters;
import io.gravitee.gamma.rest.core.tracing.model.FilterCondition;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TracingPort;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * Lists traces for the trace explorer view. Scopes hard on the caller-supplied {@code module}
 * value + env via {@link TracingResourceFilters} so traces from other modules / envs never appear.
 *
 * <p>Filter input is the abstract {@link FilterCondition} shape the UI knows — the use case
 * delegates to {@link SearchTraceFilterTranslator} to turn each condition into the underlying OTel
 * attribute key the {@link TracingPort} accepts. Slim cut: only {@code eq} on a small set of
 * supported filter names; everything else throws {@code UnsupportedFilterException} (mapped to
 * HTTP 400). The follow-up PR extends the {@code TraceFilterContributor} SPI so each filter
 * carries its own translation logic and the per-filter switch in {@code SearchTraceFilterTranslator}
 * goes away.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class SearchTracesUseCase {

    /**
     * Default lookback when the caller didn't pin a window — 24h is long enough for typical debug
     * workflows without scanning months of indexed traces. Bounded explicitly here rather than in the
     * adapter so the policy lives next to the use case, where it can be tuned per consumer.
     */
    private static final Duration DEFAULT_LOOKBACK = Duration.ofHours(24);

    private static final int DEFAULT_PER_PAGE = 20;

    private final TracingPort tracingPort;

    public record Input(
        String organizationId,
        String environmentId,
        // Required — the explorer always scopes to one API, never to "every API I can see". Per-API
        // auth scope is enforced by the caller picking from APIs they can see (mandatory API picker
        // before any search); pinning to one apiId keeps each query bounded to a single
        // resource-attribute term, portable across backends (works on Tempo too, which doesn't
        // efficiently OR over hundreds of api-ids). Module isn't carried separately because every
        // apiId belongs to exactly one module — the apiId alone is sufficient to scope.
        String apiId,
        // Abstract filters from the UI — translated to attribute filters by SearchTraceFilterTranslator.
        // Empty / null means "no filters beyond the required scope envelope".
        List<FilterCondition> filters,
        Instant start,
        Instant end,
        // 1-based page number aligned with apim's v2 logs/analytics convention. Null defers to default.
        Integer page,
        Integer perPage
    ) {}

    public record Output(Page<Trace> traces) {}

    public Output execute(Input input) {
        Instant resolvedEnd = input.end != null ? input.end : Instant.now();
        // Last-24h fallback: anchor on `end` (now if also unset) so a caller pinning only `end` gets
        // "the 24h leading up to that endpoint" rather than "now-24h..end" which can yield empty windows.
        Instant resolvedStart = input.start != null ? input.start : resolvedEnd.minus(DEFAULT_LOOKBACK);
        int resolvedPage = (input.page != null && input.page >= 1) ? input.page : 1;
        int resolvedPerPage = (input.perPage != null && input.perPage > 0) ? input.perPage : DEFAULT_PER_PAGE;
        // Throws UnsupportedFilterException on unknown name / unsupported operator — mapped to HTTP 400
        // upstream by the apim ValidationDomainExceptionMapper already registered in GammaModuleApplication.
        Map<String, String> attributeFilters = SearchTraceFilterTranslator.toAttributeFilters(input.filters);

        // TracingPort still takes a 0-based page (internal pagination math); convert at the boundary.
        Page<Trace> traces = tracingPort.searchTraces(
            input.organizationId,
            input.environmentId,
            TracingResourceFilters.forApi(input.environmentId, input.apiId),
            attributeFilters,
            resolvedStart,
            resolvedEnd,
            resolvedPage - 1,
            resolvedPerPage
        );
        return new Output(traces);
    }
}
