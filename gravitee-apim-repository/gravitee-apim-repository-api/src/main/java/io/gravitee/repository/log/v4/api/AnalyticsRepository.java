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
package io.gravitee.repository.log.v4.api;

import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.model.analytics.*;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.Optional;

public interface AnalyticsRepository {
    Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery requestsCountQuery);

    Optional<AverageAggregate> searchAverageMessagesPerRequest(QueryContext queryContext, AverageMessagesPerRequestQuery query);

    Optional<AverageAggregate> searchAverageConnectionDuration(QueryContext queryContext, AverageConnectionDurationQuery query);

    @NonNull
    Optional<ResponseStatusRangesAggregate> searchResponseStatusRanges(QueryContext queryContext, ResponseStatusQueryCriteria query);

    Optional<TopHitsAggregate> searchTopHitsApi(QueryContext queryContext, TopHitsQueryCriteria criteria);

    @NonNull
    Maybe<AverageAggregate> searchResponseTimeOverTime(QueryContext queryContext, ResponseTimeRangeQuery query);

    ResponseStatusOverTimeAggregate searchResponseStatusOvertime(QueryContext queryContext, ResponseStatusOverTimeQuery query);

    RequestResponseTimeAggregate searchRequestResponseTimes(QueryContext queryContext, RequestResponseTimeQueryCriteria query);

    Optional<TopHitsAggregate> searchTopApps(QueryContext queryContext, TopHitsQueryCriteria criteria);

    Optional<TopFailedAggregate> searchTopFailedApis(QueryContext queryContext, TopFailedQueryCriteria criteria);

    List<HistogramAggregate> searchHistogram(QueryContext queryContext, HistogramQuery query);

    Optional<StatsAggregate> searchStats(QueryContext queryContext, StatsQuery query);

    Optional<CountByAggregate> searchRequestsCountByEvent(QueryContext queryContext, RequestsCountByEventQuery requestsCountQuery);

    Optional<GroupByAggregate> searchGroupBy(QueryContext queryContext, GroupByQuery query);

    Optional<ApiMetricsDetail> findApiMetricsDetail(QueryContext queryContext, ApiMetricsDetailQuery query);

    Optional<EventAnalyticsAggregate> searchEventAnalytics(QueryContext queryContext, HistogramQuery query);

    MeasuresResult searchHTTPMeasures(QueryContext queryContext, MeasuresQuery query);

    FacetsResult searchHTTPFacets(QueryContext queryContext, FacetsQuery query);

    FacetsResult searchEdgeFacets(QueryContext queryContext, FacetsQuery query);

    FacetsResult searchNativeApiFacets(QueryContext queryContext, FacetsQuery query);

    TimeSeriesResult searchHTTPTimeSeries(QueryContext queryContext, TimeSeriesQuery query);

    TimeSeriesResult searchNativeApiTimeSeries(QueryContext queryContext, TimeSeriesQuery query);

    MeasuresResult searchMessageMeasures(QueryContext queryContext, MeasuresQuery query);

    /**
     * Message measures broken down by a dimension — operation, connector, API, application.
     *
     * <p>Like {@link #searchMessageMeasures}, resolves the matching connection documents first: the
     * message index carries none of the connection dimensions, so a query filtered on plan or
     * application can only be answered by joining on request id.
     */
    FacetsResult searchMessageFacets(QueryContext queryContext, FacetsQuery query);

    /** Message measures over time, optionally split by a dimension. Same two-phase join. */
    TimeSeriesResult searchMessageTimeSeries(QueryContext queryContext, TimeSeriesQuery query);

    /**
     * Native Kafka event metrics, read from the {@code event-metrics} data stream (throughput per
     * topic, active connections, authentications, per-operation counters and durations) — as opposed
     * to {@code searchNativeApi*}, which reads connection documents from {@code v4-metrics}.
     */
    MeasuresResult searchEventMetricsMeasures(QueryContext queryContext, MeasuresQuery query);

    FacetsResult searchEventMetricsFacets(QueryContext queryContext, FacetsQuery query);

    TimeSeriesResult searchEventMetricsTimeSeries(QueryContext queryContext, TimeSeriesQuery query);

    MeasuresResult searchAuthzMeasures(QueryContext queryContext, MeasuresQuery query);

    FacetsResult searchAuthzFacets(QueryContext queryContext, FacetsQuery query);

    TimeSeriesResult searchAuthzTimeSeries(QueryContext queryContext, TimeSeriesQuery query);

    FilterValuesResult searchFilterValues(QueryContext queryContext, FilterValuesQuery query);
}
