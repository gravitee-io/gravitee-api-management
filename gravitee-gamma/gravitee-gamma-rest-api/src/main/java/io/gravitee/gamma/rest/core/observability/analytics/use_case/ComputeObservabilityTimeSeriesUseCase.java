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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange;
import io.gravitee.gamma.rest.core.observability.analytics.model.SeriesRanking;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * Computes analytics time-series via the shared engine, applying the unified Gamma filter vocabulary
 * and RBAC scoping.
 *
 * <p>A {@code limit} on a faceted time-series keeps the same top values in every time bucket: a facets
 * query over the whole range ranks them first, then the time-series plots only those. The two queries run
 * one after the other on the same prepared scope, so the ranking covers exactly the population plotted.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class ComputeObservabilityTimeSeriesUseCase {

    /**
     * The engine returns a facet's values in its own order (by document count), so the facets query asks
     * for this many innermost values and Gamma ranks them: the top values by any measure are exact up to
     * this many distinct values over the range. An outer facet keeps the engine's 10 most frequent values.
     */
    static final int RANKING_CANDIDATES = 1000;

    /** A chart with more series is unreadable, and each one is a bucket in every time bucket. */
    static final int MAX_SERIES = 100;

    private final ObservabilityAnalyticsDataPort analyticsDataPort;
    private final AnalyticsRequestPipeline pipeline;

    public record Input(
        String organizationId,
        String environmentId,
        List<FilterCondition> filters,
        Instant from,
        Instant to,
        Long interval,
        List<AnalyticsFacetMetricQuery> metrics,
        List<String> facets,
        Integer limit,
        List<AnalyticsNumberRange> ranges
    ) {}

    public record Output(JsonNode response) {}

    public Output execute(Input input) {
        if (input.metrics == null || input.metrics.isEmpty()) {
            throw InvalidObservabilityQueryException.missingMetrics();
        }
        if (input.interval == null || input.interval <= 0) {
            throw InvalidObservabilityQueryException.invalidInterval(input.interval);
        }
        if (input.limit != null && input.limit > MAX_SERIES) {
            throw InvalidObservabilityQueryException.invalidLimit(input.limit, MAX_SERIES);
        }
        // Before the request is prepared: a refused condition never reaches the data.
        var metricConditions = input.metrics.stream().map(AnalyticsFacetMetricQuery::filters).toList();
        pipeline.validateMetricConditions(metricConditions);
        var shared = pipeline.sharedMetricConditions(metricConditions);
        var notApplied = pipeline.conditionsNotApplied(
            input.filters,
            input.metrics.stream().map(AnalyticsFacetMetricQuery::metricName).toList()
        );
        var kind = pipeline.dataKindOf(input.filters, metricConditions, input.facets);
        var scope = pipeline
            .prepare(
                input.organizationId,
                input.environmentId,
                input.filters,
                input.from,
                input.to,
                kind,
                input.metrics.stream().map(AnalyticsFacetMetricQuery::metricName).toList(),
                analyticsDataPort
            )
            .scopeWith(shared);
        var query = new ObservabilityAnalyticsDataPort.TimeSeriesQuery(
            input.organizationId,
            input.environmentId,
            scope,
            input.interval,
            input.facets,
            input.metrics,
            input.ranges,
            notApplied,
            null
        );
        analyticsDataPort.validate(query);

        if (scope.isEmpty()) {
            return new Output(analyticsDataPort.emptyTimeSeriesResponse());
        }
        if (!limitsSeries(input)) {
            return new Output(analyticsDataPort.computeTimeSeries(query));
        }
        var ranking = rank(input, scope);
        if (ranking.isEmpty()) {
            return new Output(analyticsDataPort.emptyTimeSeriesResponse());
        }
        return new Output(analyticsDataPort.computeTimeSeries(query.withRanking(ranking)));
    }

    private static boolean limitsSeries(Input input) {
        return input.limit != null && input.limit > 0 && input.facets != null && !input.facets.isEmpty();
    }

    private SeriesRanking rank(Input input, AnalyticsRequestPipeline.PreparedScope scope) {
        var unsortedMetrics = input.metrics
            .stream()
            .map(metric -> new AnalyticsFacetMetricQuery(metric.metricName(), metric.measures(), List.of(), metric.filters()))
            .toList();
        var facets = analyticsDataPort.computeFacets(
            new ObservabilityAnalyticsDataPort.FacetsQuery(
                input.organizationId,
                input.environmentId,
                scope,
                input.facets,
                RANKING_CANDIDATES,
                unsortedMetrics,
                input.ranges,
                Map.of()
            )
        );
        return SeriesRanking.rank(facets, input.metrics, input.facets.size(), input.limit);
    }
}
