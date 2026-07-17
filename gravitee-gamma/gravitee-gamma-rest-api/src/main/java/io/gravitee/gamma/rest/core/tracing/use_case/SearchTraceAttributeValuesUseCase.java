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
import io.gravitee.gamma.rest.core.tracing.TraceScopeFilters;
import io.gravitee.gamma.rest.core.tracing.model.TraceAttributeValue;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TracingPort;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Distinct values of a span attribute for one API, with rollups. Scoping mirrors
 * {@link SearchTracesUseCase}: pinned to one {@code apiId} (which defines the module), over a time window.
 */
@UseCase
@AllArgsConstructor
public class SearchTraceAttributeValuesUseCase {

    private static final Duration DEFAULT_LOOKBACK = Duration.ofHours(24);
    private static final int DEFAULT_LIMIT = 100;

    private final TracingPort tracingPort;

    public record Input(
        String organizationId,
        String environmentId,
        String apiId,
        // Abstract filter name (e.g. CONVERSATION_ID) — resolved to a span-attribute key by the translator.
        String filterName,
        // Dotted span-attribute keys to also return per value (e.g. gravitee.entrypoint.id). Null/empty for none.
        List<String> correlatedAttributeKeys,
        Instant start,
        Instant end,
        // Max distinct values to return; null defers to the default.
        Integer limit
    ) {}

    public record Output(List<TraceAttributeValue> values) {}

    public Output execute(Input input) {
        Instant resolvedEnd = input.end != null ? input.end : Instant.now();
        Instant resolvedStart = input.start != null ? input.start : resolvedEnd.minus(DEFAULT_LOOKBACK);
        int resolvedLimit = (input.limit != null && input.limit > 0) ? input.limit : DEFAULT_LIMIT;
        List<String> correlated = input.correlatedAttributeKeys != null ? input.correlatedAttributeKeys : List.of();
        // Throws UnsupportedFilterException on an unknown name — mapped to HTTP 400 by the registered mapper.
        String attributeKey = SearchTraceFilterTranslator.attributeKey(input.filterName);

        List<TraceAttributeValue> values = tracingPort.aggregateAttributeValues(
            input.organizationId,
            input.environmentId,
            TraceScopeFilters.forApi(input.environmentId, input.apiId),
            attributeKey,
            correlated,
            resolvedStart,
            resolvedEnd,
            resolvedLimit
        );
        return new Output(values);
    }
}
