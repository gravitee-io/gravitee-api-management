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
package io.gravitee.apim.infra.query_service.analytics;

import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.infra.adapter.ResponseStatusQueryCriteriaAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsQueryServiceImpl(@Lazy AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public Optional<RequestsCount> searchRequestsCount(ExecutionContext executionContext, String apiId) {
        return analyticsRepository
            .searchRequestsCount(executionContext.getQueryContext(), new RequestsCountQuery(apiId))
            .map(countAggregate ->
                RequestsCount.builder().total(countAggregate.getTotal()).countsByEntrypoint(countAggregate.getCountBy()).build()
            );
    }

    @Override
    public Optional<AverageMessagesPerRequest> searchAverageMessagesPerRequest(ExecutionContext executionContext, String apiId) {
        return analyticsRepository
            .searchAverageMessagesPerRequest(
                executionContext.getQueryContext(),
                AverageMessagesPerRequestQuery.builder().apiId(apiId).build()
            )
            .map(averageAggregate ->
                AverageMessagesPerRequest
                    .builder()
                    .globalAverage(averageAggregate.getAverage())
                    .averagesByEntrypoint(averageAggregate.getAverageBy())
                    .build()
            );
    }

    @Override
    public Optional<ResponseStatusRanges> searchResponseStatusRanges(
        ExecutionContext executionContext,
        AnalyticsQueryParameters queryParameters
    ) {
        var responseStatusQueryParameters = ResponseStatusQueryCriteriaAdapter.INSTANCE.map(queryParameters);

        final var queryResult = analyticsRepository.searchResponseStatusRanges(
            executionContext.getQueryContext(),
            responseStatusQueryParameters
        );
        return queryResult.map(analytics ->
            ResponseStatusRanges
                .builder()
                .ranges(analytics.getRanges())
                .statusRangesCountByEntrypoint(analytics.getStatusRangesCountByEntrypoint())
                .build()
        );
    }

    @Override
    public Optional<TopHitsApis> searchTopHitsApis(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return analyticsRepository
            .searchTopHitsApi(
                executionContext.getQueryContext(),
                new TopHitsQueryCriteria(parameters.getApiIds(), parameters.getFrom(), parameters.getTo())
            )
            .map(TopHitsAggregate::getTopHitsCounts)
            .map(topHitCounts ->
                topHitCounts
                    .entrySet()
                    .stream()
                    .map(entry -> TopHitsApis.TopHitApi.builder().id(entry.getKey()).count(entry.getValue()).build())
                    .toList()
            )
            .map(topHitApis -> TopHitsApis.builder().data(topHitApis).build());
    }

    @Override
    public Optional<AverageConnectionDuration> searchAverageConnectionDuration(
        ExecutionContext executionContext,
        String apiId,
        Instant from,
        Instant to
    ) {
        return analyticsRepository
            .searchAverageConnectionDuration(executionContext.getQueryContext(), new AverageConnectionDurationQuery(apiId, from, to))
            .map(averageAggregate ->
                AverageConnectionDuration
                    .builder()
                    .globalAverage(averageAggregate.getAverage())
                    .averagesByEntrypoint(averageAggregate.getAverageBy())
                    .build()
            );
    }

    @Override
    public Maybe<Map<String, Double>> searchAvgResponseTimeOverTime(
        ExecutionContext executionContext,
        String apiId,
        Instant startTime,
        Instant endTime,
        Duration interval
    ) {
        return analyticsRepository
            .searchResponseTimeOverTime(executionContext.getQueryContext(), new ResponseTimeRangeQuery(apiId, startTime, endTime, interval))
            .map(AverageAggregate::getAverageBy);
    }

    @Override
    public ResponseStatusOvertime searchResponseStatusOvertime(ExecutionContext executionContext, ResponseStatusOverTimeQuery query) {
        var result = analyticsRepository.searchResponseStatusOvertime(
            executionContext.getQueryContext(),
            new io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery(
                query.apiId(),
                query.from(),
                query.to(),
                query.interval()
            )
        );
        return ResponseStatusOvertime
            .builder()
            .data(result.getStatusCount())
            .timeRange(new ResponseStatusOvertime.TimeRange(query.from(), query.to(), query.interval()))
            .build();
    }

    @Override
    public RequestResponseTime searchRequestResponseTime(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        var result = analyticsRepository.searchRequestResponseTimes(
            executionContext.getQueryContext(),
            new RequestResponseTimeQueryCriteria(parameters.getApiIds(), parameters.getFrom(), parameters.getTo())
        );

        return RequestResponseTime
            .builder()
            .requestsPerSecond(result.getRequestsPerSecond())
            .requestsTotal(result.getRequestsTotal())
            .responseMinTime(result.getResponseMinTime())
            .responseMaxTime(result.getResponseMaxTime())
            .responseAvgTime(result.getResponseAvgTime())
            .build();
    }
}
