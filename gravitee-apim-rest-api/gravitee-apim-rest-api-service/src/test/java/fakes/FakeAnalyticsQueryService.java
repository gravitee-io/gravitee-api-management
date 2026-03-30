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
package fakes;

import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FakeAnalyticsQueryService implements AnalyticsQueryService {

    public RequestsCount requestsCount;
    public AverageMessagesPerRequest averageMessagesPerRequest;
    public AverageConnectionDuration averageConnectionDuration;
    public ResponseStatusRanges responseStatusRanges;
    public TopHitsApis topHitsApis;
    public RequestResponseTime requestResponseTime;
    public LinkedHashMap<String, Double> averageAggregate = new LinkedHashMap<>();
    public ResponseStatusOvertime responseStatusOvertime;
    public TopHitsApps topHitsApps;
    public TopFailedApis topFailedApis;
    public ApiAnalyticsCountAggregate apiAnalyticsCount;
    public ApiAnalyticsStatsAggregate apiAnalyticsStats;
    public ApiAnalyticsGroupByAggregate apiAnalyticsGroupBy;
    public ApiAnalyticsDateHistoAggregate apiAnalyticsDateHisto;

    @Override
    public Optional<ApiAnalyticsCountAggregate> searchApiAnalyticsCount(ExecutionContext executionContext, ApiAnalyticsCountQuery query) {
        return Optional.ofNullable(apiAnalyticsCount);
    }

    @Override
    public Optional<ApiAnalyticsStatsAggregate> searchApiAnalyticsStats(ExecutionContext executionContext, ApiAnalyticsStatsQuery query) {
        return Optional.ofNullable(apiAnalyticsStats);
    }

    @Override
    public Optional<ApiAnalyticsGroupByAggregate> searchApiAnalyticsGroupBy(
        ExecutionContext executionContext,
        ApiAnalyticsGroupByQuery query
    ) {
        return Optional.ofNullable(apiAnalyticsGroupBy);
    }

    @Override
    public Optional<ApiAnalyticsDateHistoAggregate> searchApiAnalyticsDateHisto(
        ExecutionContext executionContext,
        ApiAnalyticsDateHistoQuery query
    ) {
        return Optional.ofNullable(apiAnalyticsDateHisto);
    }

    @Override
    public Optional<RequestsCount> searchRequestsCount(ExecutionContext executionContext, String apiId, Instant from, Instant to) {
        return Optional.ofNullable(requestsCount);
    }

    @Override
    public Optional<AverageMessagesPerRequest> searchAverageMessagesPerRequest(
        ExecutionContext executionContext,
        String apiId,
        Instant from,
        Instant to
    ) {
        return Optional.ofNullable(averageMessagesPerRequest);
    }

    @Override
    public Optional<AverageConnectionDuration> searchAverageConnectionDuration(
        ExecutionContext executionContext,
        String apiId,
        Instant from,
        Instant to
    ) {
        return Optional.ofNullable(averageConnectionDuration);
    }

    public void reset() {
        requestsCount = null;
        averageMessagesPerRequest = null;
        averageConnectionDuration = null;
        averageAggregate = new LinkedHashMap<>();
        responseStatusRanges = null;
        responseStatusOvertime = null;
        requestResponseTime = null;
        topFailedApis = null;
        apiAnalyticsCount = null;
        apiAnalyticsStats = null;
        apiAnalyticsGroupBy = null;
        apiAnalyticsDateHisto = null;
    }

    @Override
    public Optional<ResponseStatusRanges> searchResponseStatusRanges(
        ExecutionContext executionContext,
        AnalyticsQueryParameters queryParameters
    ) {
        return Optional.ofNullable(responseStatusRanges);
    }

    @Override
    public Optional<TopHitsApis> searchTopHitsApis(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return Optional.ofNullable(topHitsApis);
    }

    @Override
    public Maybe<Map<String, Double>> searchAvgResponseTimeOverTime(
        ExecutionContext executionContext,
        List<String> apiIds,
        Instant startTime,
        Instant endTime,
        Duration interval,
        Collection<DefinitionVersion> versions
    ) {
        return averageAggregate != null ? Maybe.just(averageAggregate) : Maybe.empty();
    }

    @Override
    public ResponseStatusOvertime searchResponseStatusOvertime(ExecutionContext executionContext, ResponseStatusOverTimeQuery query) {
        return responseStatusOvertime;
    }

    @Override
    public Optional<TopHitsApps> searchTopHitsApps(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return Optional.ofNullable(topHitsApps);
    }

    @Override
    public RequestResponseTime searchRequestResponseTime(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return requestResponseTime;
    }

    @Override
    public Optional<TopFailedApis> searchTopFailedApis(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return Optional.ofNullable(topFailedApis);
    }
}
