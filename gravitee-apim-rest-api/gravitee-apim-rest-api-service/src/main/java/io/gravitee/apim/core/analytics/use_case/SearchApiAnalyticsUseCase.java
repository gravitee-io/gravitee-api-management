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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.model.AnalyticsType;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified analytics use case supporting COUNT, STATS, GROUP_BY and DATE_HISTO query types
 * via the single {@code GET /v2/apis/{apiId}/analytics} endpoint.
 *
 * <p>All four query types share the same validation pipeline:
 * <ol>
 *   <li>API must exist (throws {@link ApiNotFoundException})</li>
 *   <li>API must be V4 (throws {@link ApiInvalidDefinitionVersionException})</li>
 *   <li>API must not be a TCP proxy (throws {@link TcpProxyNotSupportedException})</li>
 *   <li>API must belong to the caller's environment (multi-tenancy guard)</li>
 * </ol>
 * Field and interval validation for STATS/GROUP_BY/DATE_HISTO is intentionally kept
 * in the REST layer ({@code ApiAnalyticsResource}) so the use case stays focused on
 * business rules, not HTTP concerns.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchApiAnalyticsUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    /**
     * Validates the target API and dispatches to the appropriate analytics query.
     *
     * <p>Validation always runs in full before any query is executed, so callers
     * receive a domain exception (not a partial result) when the API is invalid.</p>
     *
     * @param executionContext organisation/environment context for the Elasticsearch tenant
     * @param input           query parameters including type discriminator and optional field/interval
     * @return a sealed {@link Output} variant whose concrete type matches {@link Input#type()}
     * @throws ApiNotFoundException                if the API does not exist or belongs to a different environment
     * @throws ApiInvalidDefinitionVersionException if the API is not V4
     * @throws TcpProxyNotSupportedException        if the API is a TCP proxy (no HTTP metrics)
     */
    public Output execute(ExecutionContext executionContext, Input input) {
        var api = apiCrudService.get(input.apiId());
        validateApiDefinitionVersion(api, input.apiId());
        validateApiIsNotTcp(api);
        validateApiMultiTenancyAccess(api, input.environmentId());

        return switch (input.type()) {
            case COUNT -> {
                var result = analyticsQueryService.searchRequestsCount(
                    executionContext,
                    input.apiId(),
                    input.from().orElse(null),
                    input.to().orElse(null)
                );
                yield new Output.Count(result.map(rc -> rc.getTotal() != null ? rc.getTotal() : 0L).orElse(0L));
            }
            case STATS -> {
                var stats = analyticsQueryService.searchStats(
                    executionContext,
                    input.apiId(),
                    input.field().orElse(null),
                    input.from().orElse(null),
                    input.to().orElse(null)
                );
                yield stats
                    .map(s -> new Output.Stats(s.count(), s.min(), s.max(), s.avg(), s.sum()))
                    .orElseGet(() -> new Output.Stats(0L, null, null, null, null));
            }
            case GROUP_BY -> {
                var groupBy = analyticsQueryService.searchGroupBy(
                    executionContext,
                    input.apiId(),
                    input.field().orElse(null),
                    input.size(),
                    input.from().orElse(null),
                    input.to().orElse(null)
                );
                yield groupBy
                    .map(g -> new Output.GroupBy(g.values(), g.metadata()))
                    .orElseGet(() -> new Output.GroupBy(Map.of(), Map.of()));
            }
            case DATE_HISTO -> {
                var dateHisto = analyticsQueryService.searchDateHisto(
                    executionContext,
                    input.apiId(),
                    input.field().orElse(null),
                    Duration.ofMillis(input.interval().orElse(0L)),
                    input.from().orElse(null),
                    input.to().orElse(null)
                );
                yield dateHisto
                    .map(dh ->
                        new Output.DateHisto(
                            dh.timestamps(),
                            dh.buckets().stream().map(b -> new Output.DateHisto.Bucket(b.field(), b.counts(), b.metadata())).toList()
                        )
                    )
                    .orElseGet(() -> new Output.DateHisto(List.of(), List.of()));
            }
        };
    }

    // -------------------------------------------------------------------------
    // Validation helpers — mirrors the pattern in SearchRequestsCountAnalyticsUseCase
    //                       and SearchResponseStatusRangesUseCase
    // -------------------------------------------------------------------------

    private static void validateApiDefinitionVersion(Api api, String apiId) {
        if (!DefinitionVersion.V4.equals(api.getDefinitionVersion())) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiIsNotTcp(Api api) {
        // Null guard: getApiDefinitionHttpV4() returns null for non-V4 APIs.
        // The version check always runs first, so null here means a bug in the call order —
        // skip the TCP check rather than masking the real exception with the wrong type.
        var v4Definition = api.getApiDefinitionHttpV4();
        if (v4Definition != null && v4Definition.isTcpProxy()) {
            throw new TcpProxyNotSupportedException(api.getId());
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    // -------------------------------------------------------------------------
    // Input / Output types
    // -------------------------------------------------------------------------

    /**
     * @param apiId         target API identifier
     * @param environmentId current environment (multi-tenancy guard)
     * @param type          query type discriminator
     * @param from          start of the time window (epoch ms), empty → backend default
     * @param to            end of the time window (epoch ms), empty → now
     * @param field         metric field name — required for STATS and GROUP_BY
     * @param interval      bucket interval in ms — required for DATE_HISTO
     * @param size          top-N limit for GROUP_BY (default 10)
     */
    public record Input(
        String apiId,
        String environmentId,
        AnalyticsType type,
        Optional<Instant> from,
        Optional<Instant> to,
        Optional<String> field,
        Optional<Long> interval,
        int size
    ) {}

    /**
     * Sealed output hierarchy — one variant per AnalyticsType.
     * Field shapes match the PRD JSON contract; Stories 2-5 populate real values.
     */
    public sealed interface Output permits Output.Count, Output.Stats, Output.GroupBy, Output.DateHisto {
        /** COUNT response: total document hit count. */
        record Count(long count) implements Output {}

        /** STATS response: statistical aggregates for a numeric field. */
        record Stats(long count, Double min, Double max, Double avg, Double sum) implements Output {}

        /** GROUP_BY response: top-N document counts keyed by field value. */
        record GroupBy(Map<String, Long> values, Map<String, Map<String, String>> metadata) implements Output {}

        /** DATE_HISTO response: time-bucketed histogram. */
        record DateHisto(List<Long> timestamps, List<Bucket> buckets) implements Output {
            /** One series entry per distinct field value (e.g. per HTTP status code). */
            public record Bucket(String field, List<Long> buckets, Map<String, String> metadata) {}
        }
    }
}
