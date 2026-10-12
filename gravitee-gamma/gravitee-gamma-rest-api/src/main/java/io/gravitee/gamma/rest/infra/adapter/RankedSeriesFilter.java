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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.gamma.rest.core.observability.analytics.model.SeriesRanking;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trims an engine time-series to the ranked series. The ranking conditions narrow what the engine fetches,
 * but a filter the engine ignores, or one matching more than its values, would let other series through:
 * the series kept are decided here, by their facet keys.
 *
 * <p>The ranking conditions are Gamma's, not the caller's: one the engine reports as ignored is left out
 * of {@code ignoredFilters} unless the caller's scope carries a condition of the same name.
 */
final class RankedSeriesFilter {

    private RankedSeriesFilter() {}

    static TimeSeriesResponse keepRankedSeries(
        TimeSeriesResponse response,
        SeriesRanking ranking,
        List<Filter> rankingConditions,
        AnalyticsRequestPipeline.PreparedScope scope
    ) {
        var callerNames = scope.filters().stream().map(FilterCondition::name).collect(Collectors.toSet());
        var gammaOnly = rankingConditions
            .stream()
            .map(Filter::name)
            .filter(name -> !callerNames.contains(name.name()))
            .collect(Collectors.toSet());
        return new TimeSeriesResponse(
            response
                .metrics()
                .stream()
                .map(metric -> keepRankedSeries(metric, ranking, gammaOnly))
                .toList()
        );
    }

    private static TimeSeriesMetricResponse keepRankedSeries(
        TimeSeriesMetricResponse metric,
        SeriesRanking ranking,
        Set<FilterSpec.Name> gammaOnly
    ) {
        var metricName = metric.name().name();
        var buckets = metric.buckets() == null
            ? null
            : metric
                .buckets()
                .stream()
                .map(bucket ->
                    new TimeSeriesBucketResponse(
                        bucket.key(),
                        bucket.name(),
                        bucket.timestamp(),
                        keep(bucket.buckets(), List.of(), metricName, ranking),
                        bucket.measures()
                    )
                )
                .toList();
        var ignoredFilters = metric
            .ignoredFilters()
            .stream()
            .filter(name -> !gammaOnly.contains(name))
            .toList();
        return new TimeSeriesMetricResponse(metric.name(), metric.unit(), buckets, ignoredFilters);
    }

    private static List<FacetBucketResponse> keep(
        List<FacetBucketResponse> buckets,
        List<String> outerKeys,
        String metric,
        SeriesRanking ranking
    ) {
        if (buckets == null) {
            return null;
        }
        var kept = new ArrayList<FacetBucketResponse>();
        for (var bucket : buckets) {
            var keys = new ArrayList<>(outerKeys);
            keys.add(bucket.key());
            if (!ranking.includes(metric, keys)) {
                continue;
            }
            var children = keep(bucket.buckets(), keys, metric, ranking);
            // An outer value whose ranked series have no data in this time bucket goes too, rather than
            // staying as an empty group the response would type as a leaf without measures.
            var emptiedGroup = bucket.buckets() != null && !bucket.buckets().isEmpty() && children.isEmpty();
            if (!emptiedGroup) {
                kept.add(new FacetBucketResponse(bucket.key(), bucket.name(), children, bucket.measures()));
            }
        }
        return kept;
    }
}
