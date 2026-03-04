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

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;

public interface AnalyticsRepository {
    /**
     * Search stats (min, max, avg, sum, count) for a numeric field on v4 API metrics.
     * Data source: *-v4-metrics-* index only.
     */
    Optional<StatsAggregate> searchStats(QueryContext queryContext, ApiAnalyticsStatsQuery query);

    /**
     * Search group-by (terms aggregation) for a field on v4 API metrics.
     * Data source: *-v4-metrics-* index only.
     */
    Optional<GroupByAggregate> searchGroupBy(QueryContext queryContext, ApiAnalyticsGroupByQuery query);

    /**
     * Search date histogram with optional terms sub-aggregation for a field on v4 API metrics.
     * Data source: *-v4-metrics-* index only.
     */
    Optional<DateHistoAggregate> searchDateHisto(QueryContext queryContext, ApiAnalyticsDateHistoQuery query);
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
}
