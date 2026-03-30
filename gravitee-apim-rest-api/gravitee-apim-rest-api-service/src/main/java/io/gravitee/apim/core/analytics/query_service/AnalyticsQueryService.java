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
package io.gravitee.apim.core.analytics.query_service;

import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsCountAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsQuery;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.model.v4.analytics.TopFailedApis;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AnalyticsQueryService {
    Optional<ApiAnalyticsCountAggregate> searchApiAnalyticsCount(ExecutionContext executionContext, ApiAnalyticsCountQuery query);

    Optional<ApiAnalyticsStatsAggregate> searchApiAnalyticsStats(ExecutionContext executionContext, ApiAnalyticsStatsQuery query);

    Optional<ApiAnalyticsGroupByAggregate> searchApiAnalyticsGroupBy(ExecutionContext executionContext, ApiAnalyticsGroupByQuery query);

    Optional<ApiAnalyticsDateHistoAggregate> searchApiAnalyticsDateHisto(
        ExecutionContext executionContext,
        ApiAnalyticsDateHistoQuery query
    );

    Optional<RequestsCount> searchRequestsCount(ExecutionContext executionContext, String apiId, Instant from, Instant to);

    Optional<AverageMessagesPerRequest> searchAverageMessagesPerRequest(
        ExecutionContext executionContext,
        String apiId,
        Instant from,
        Instant to
    );

    Optional<AverageConnectionDuration> searchAverageConnectionDuration(
        ExecutionContext executionContext,
        String apiId,
        Instant from,
        Instant to
    );

    Optional<ResponseStatusRanges> searchResponseStatusRanges(ExecutionContext executionContext, AnalyticsQueryParameters queryParameters);

    Optional<TopHitsApis> searchTopHitsApis(ExecutionContext executionContext, AnalyticsQueryParameters parameters);

    Maybe<Map<String, Double>> searchAvgResponseTimeOverTime(
        ExecutionContext executionContext,
        List<String> apiIds,
        Instant startTime,
        Instant endTime,
        Duration interval,
        Collection<DefinitionVersion> versions
    );

    ResponseStatusOvertime searchResponseStatusOvertime(ExecutionContext executionContext, ResponseStatusOverTimeQuery query);

    Optional<TopHitsApps> searchTopHitsApps(ExecutionContext executionContext, AnalyticsQueryParameters parameters);

    RequestResponseTime searchRequestResponseTime(ExecutionContext executionContext, AnalyticsQueryParameters parameters);

    Optional<TopFailedApis> searchTopFailedApis(ExecutionContext executionContext, AnalyticsQueryParameters parameters);

    record ResponseStatusOverTimeQuery(
        List<String> apiIds,
        Instant from,
        Instant to,
        Duration interval,
        Collection<DefinitionVersion> versions
    ) {
        public ResponseStatusOverTimeQuery(
            List<String> apiIds,
            Instant from,
            Instant to,
            Duration interval,
            Collection<DefinitionVersion> versions
        ) {
            this.apiIds = apiIds;
            this.from = from;
            this.to = to;
            this.interval = interval;
            this.versions = versions == null || versions.isEmpty() ? List.of(DefinitionVersion.V4) : versions;
        }
    }
}
