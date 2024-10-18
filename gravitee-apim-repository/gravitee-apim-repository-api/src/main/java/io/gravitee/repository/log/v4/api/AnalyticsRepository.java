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
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.Optional;

public interface AnalyticsRepository {
    Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery requestsCountQuery);

    Optional<AverageAggregate> searchAverageMessagesPerRequest(QueryContext queryContext, AverageMessagesPerRequestQuery query);

    Optional<AverageAggregate> searchAverageConnectionDuration(QueryContext queryContext, AverageConnectionDurationQuery query);

    @NonNull
    Optional<ResponseStatusRangesAggregate> searchResponseStatusRanges(QueryContext queryContext, ResponseStatusQueryCriteria query);
}
