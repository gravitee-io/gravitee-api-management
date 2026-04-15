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
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;

/**
 * Repository contract for analytics aggregations backed by API log data.
 *
 * <p>The API is intentionally query-oriented: each method maps to one aggregation strategy
 * (count, stats, terms/group-by, histogram, top hits, etc.) so use cases can request only
 * the data shape they need.</p>
 */
public interface AnalyticsRepository {
    /** Returns request count aggregates for a single API over a time range. */
    Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery requestsCountQuery);

    /** Returns average messages-per-request aggregates. */
    Optional<AverageAggregate> searchAverageMessagesPerRequest(QueryContext queryContext, AverageMessagesPerRequestQuery query);

    /** Returns average connection-duration aggregates. */
    Optional<AverageAggregate> searchAverageConnectionDuration(QueryContext queryContext, AverageConnectionDurationQuery query);

    @NonNull
    /** Returns response status range aggregates (1xx..5xx buckets). */
    Optional<ResponseStatusRangesAggregate> searchResponseStatusRanges(QueryContext queryContext, ResponseStatusQueryCriteria query);

    /** Returns top APIs by hit count. */
    Optional<TopHitsAggregate> searchTopHitsApi(QueryContext queryContext, TopHitsQueryCriteria criteria);

    @NonNull
    /** Returns response time over time-series points. */
    Maybe<AverageAggregate> searchResponseTimeOverTime(QueryContext queryContext, ResponseTimeRangeQuery query);

    /** Returns response-status evolution over time buckets. */
    ResponseStatusOverTimeAggregate searchResponseStatusOvertime(QueryContext queryContext, ResponseStatusOverTimeQuery query);

    /** Returns request/response latency aggregates. */
    RequestResponseTimeAggregate searchRequestResponseTimes(QueryContext queryContext, RequestResponseTimeQueryCriteria query);

    /** Returns top applications by hit count. */
    Optional<TopHitsAggregate> searchTopApps(QueryContext queryContext, TopHitsQueryCriteria criteria);

    /** Returns top failed APIs by error count. */
    Optional<TopFailedAggregate> searchTopFailedApis(QueryContext queryContext, TopFailedQueryCriteria criteria);

    /** ES {@code stats} aggregation on a single field (US-03). */
    Optional<StatsAggregate> searchStats(QueryContext queryContext, StatsQueryCriteria criteria);

    /** ES {@code terms} aggregation on a single field (US-03 GROUP_BY). */
    Optional<GroupByAggregate> searchGroupBy(QueryContext queryContext, GroupByQueryCriteria criteria);
}
