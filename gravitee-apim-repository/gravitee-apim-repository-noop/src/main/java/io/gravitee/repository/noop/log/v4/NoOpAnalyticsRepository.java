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

import io.gravitee.repository.analytics.query.stats.EventAnalyticsAggregate;
import io.gravitee.repository.analytics.query.stats.EventAnalyticsQuery;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetailQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
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
    public Optional<EventAnalyticsAggregate> searchEventAnalytics(QueryContext queryContext, EventAnalyticsQuery query) {
        return Optional.empty();
    }
}
