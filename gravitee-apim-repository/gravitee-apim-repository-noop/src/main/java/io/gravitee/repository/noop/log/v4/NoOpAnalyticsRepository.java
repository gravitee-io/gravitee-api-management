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
package io.gravitee.repository.noop.log.v4;

import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.GroupedMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.GroupedMeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.*;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NoOpAnalyticsRepository implements AnalyticsRepository {

    @Override
    public Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery requestsCountQuery) {
        return Optional.empty();
    }

    @Override
    public Optional<AverageAggregate> searchAverageMessagesPerRequest(QueryContext queryContext, AverageMessagesPerRequestQuery query) {
        return Optional.empty();
    }

    @Override
    public Optional<AverageAggregate> searchAverageConnectionDuration(QueryContext queryContext, AverageConnectionDurationQuery query) {
        return Optional.empty();
    }

    @Override
    public @NonNull Optional<ResponseStatusRangesAggregate> searchResponseStatusRanges(
        QueryContext queryContext,
        ResponseStatusQueryCriteria query
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<TopHitsAggregate> searchTopHitsApi(QueryContext queryContext, TopHitsQueryCriteria criteria) {
        return Optional.empty();
    }

    @Override
    public @NonNull Maybe<AverageAggregate> searchResponseTimeOverTime(QueryContext queryContext, ResponseTimeRangeQuery query) {
        return Maybe.empty();
    }

    @Override
    public ResponseStatusOverTimeAggregate searchResponseStatusOvertime(QueryContext queryContext, ResponseStatusOverTimeQuery query) {
        return new ResponseStatusOverTimeAggregate();
    }

    @Override
    public RequestResponseTimeAggregate searchRequestResponseTimes(QueryContext queryContext, RequestResponseTimeQueryCriteria query) {
        return RequestResponseTimeAggregate.builder().build();
    }

    @Override
    public Optional<TopHitsAggregate> searchTopApps(QueryContext queryContext, TopHitsQueryCriteria criteria) {
        return Optional.empty();
    }

    @Override
    public Optional<TopFailedAggregate> searchTopFailedApis(QueryContext queryContext, TopFailedQueryCriteria criteria) {
        return Optional.empty();
    }

    @Override
    public List<HistogramAggregate> searchHistogram(QueryContext queryContext, HistogramQuery query) {
        return Collections.emptyList();
    }

    @Override
    public Optional<StatsAggregate> searchStats(QueryContext queryContext, StatsQuery query) {
        return Optional.empty();
    }

    @Override
    public Optional<CountByAggregate> searchRequestsCountByEvent(QueryContext queryContext, RequestsCountByEventQuery requestsCountQuery) {
        return Optional.empty();
    }

    @Override
    public Optional<GroupByAggregate> searchGroupBy(QueryContext queryContext, GroupByQuery query) {
        return Optional.empty();
    }

    @Override
    public Optional<ApiMetricsDetail> findApiMetricsDetail(QueryContext queryContext, ApiMetricsDetailQuery query) {
        return Optional.empty();
    }

    @Override
    public Optional<EventAnalyticsAggregate> searchEventAnalytics(QueryContext queryContext, HistogramQuery query) {
        return Optional.empty();
    }

    @Override
    public MeasuresResult searchHTTPMeasures(QueryContext queryContext, MeasuresQuery query) {
        return null;
    }

    @Override
    public GroupedMeasuresResult searchHTTPGroupedMeasures(QueryContext queryContext, GroupedMeasuresQuery query) {
        return null;
    }

    @Override
    public FacetsResult searchHTTPFacets(QueryContext queryContext, FacetsQuery query) {
        return null;
    }

    @Override
    public FacetsResult searchEdgeFacets(QueryContext queryContext, FacetsQuery query) {
        return null;
    }

    @Override
    public FacetsResult searchNativeApiFacets(QueryContext queryContext, FacetsQuery query) {
        return new FacetsResult(Collections.emptyList());
    }

    @Override
    public TimeSeriesResult searchHTTPTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        return null;
    }

    @Override
    public TimeSeriesResult searchNativeApiTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        return new TimeSeriesResult(List.of());
    }

    @Override
    public MeasuresResult searchMessageMeasures(QueryContext queryContext, MeasuresQuery query) {
        return null;
    }

    @Override
    public FacetsResult searchMessageFacets(QueryContext queryContext, FacetsQuery query) {
        return new FacetsResult(List.of());
    }

    @Override
    public TimeSeriesResult searchMessageTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        return new TimeSeriesResult(List.of());
    }

    @Override
    public MeasuresResult searchEventMetricsMeasures(QueryContext queryContext, MeasuresQuery query) {
        return new MeasuresResult(List.of());
    }

    @Override
    public FacetsResult searchEventMetricsFacets(QueryContext queryContext, FacetsQuery query) {
        return new FacetsResult(List.of());
    }

    @Override
    public TimeSeriesResult searchEventMetricsTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        return new TimeSeriesResult(List.of());
    }

    @Override
    public MeasuresResult searchAuthzMeasures(QueryContext queryContext, MeasuresQuery query) {
        return new MeasuresResult(List.of());
    }

    @Override
    public FacetsResult searchAuthzFacets(QueryContext queryContext, FacetsQuery query) {
        return new FacetsResult(List.of());
    }

    @Override
    public TimeSeriesResult searchAuthzTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        return new TimeSeriesResult(List.of());
    }

    @Override
    public FilterValuesResult searchFilterValues(QueryContext queryContext, FilterValuesQuery query) {
        return new FilterValuesResult(Collections.emptyList(), null, 0);
    }
}
