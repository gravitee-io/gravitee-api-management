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
import io.gravitee.apim.core.analytics.model.Bucket;
import io.gravitee.apim.core.analytics.model.HistogramAnalytics;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.infra.adapter.ResponseStatusQueryCriteriaAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
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
import java.util.stream.Collectors;
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
    public Optional<RequestsCount> searchRequestsCount(ExecutionContext executionContext, String apiId, Instant from, Instant to) {
        return analyticsRepository
            .searchRequestsCount(executionContext.getQueryContext(), new RequestsCountQuery(apiId, from, to))
            .map(countAggregate ->
                RequestsCount.builder().total(countAggregate.getTotal()).countsByEntrypoint(countAggregate.getCountBy()).build()
            );
    }

    @Override
    public Optional<AverageMessagesPerRequest> searchAverageMessagesPerRequest(
        ExecutionContext executionContext,
        String apiId,
        Instant from,
        Instant to
    ) {
        return analyticsRepository
            .searchAverageMessagesPerRequest(executionContext.getQueryContext(), new AverageMessagesPerRequestQuery(apiId, from, to))
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
        List<String> apiIds,
        Instant startTime,
        Instant endTime,
        Duration interval,
        Collection<DefinitionVersion> versions
    ) {
        return analyticsRepository
            .searchResponseTimeOverTime(
                executionContext.getQueryContext(),
                new ResponseTimeRangeQuery(apiIds, startTime, endTime, interval, versions)
            )
            .map(AverageAggregate::getAverageBy);
    }

    @Override
    public ResponseStatusOvertime searchResponseStatusOvertime(ExecutionContext executionContext, ResponseStatusOverTimeQuery query) {
        var result = analyticsRepository.searchResponseStatusOvertime(
            executionContext.getQueryContext(),
            new io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery(
                query.apiIds(),
                query.from(),
                query.to(),
                query.interval(),
                query.versions()
            )
        );
        return ResponseStatusOvertime
            .builder()
            .data(result.getStatusCount())
            .timeRange(new ResponseStatusOvertime.TimeRange(query.from(), query.to(), query.interval()))
            .build();
    }

    @Override
    public Optional<TopHitsApps> searchTopHitsApps(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return analyticsRepository
            .searchTopApps(
                executionContext.getQueryContext(),
                new TopHitsQueryCriteria(parameters.getApiIds(), parameters.getFrom(), parameters.getTo())
            )
            .map(TopHitsAggregate::getTopHitsCounts)
            .map(topHitCounts ->
                topHitCounts
                    .entrySet()
                    .stream()
                    .map(entry -> TopHitsApps.TopHitApp.builder().id(entry.getKey()).count(entry.getValue()).build())
                    .toList()
            )
            .map(topHitApp -> TopHitsApps.builder().data(topHitApp).build());
    }

    @Override
    public RequestResponseTime searchRequestResponseTime(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        var result = analyticsRepository.searchRequestResponseTimes(
            executionContext.getQueryContext(),
            new RequestResponseTimeQueryCriteria(
                parameters.getApiIds(),
                parameters.getFrom(),
                parameters.getTo(),
                parameters.getDefinitionVersions()
            )
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

    @Override
    public Optional<TopFailedApis> searchTopFailedApis(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {
        return analyticsRepository
            .searchTopFailedApis(
                executionContext.getQueryContext(),
                new TopFailedQueryCriteria(parameters.getApiIds(), parameters.getFrom(), parameters.getTo())
            )
            .map(TopFailedAggregate::failedApis)
            .map(topFailedApis ->
                topFailedApis
                    .entrySet()
                    .stream()
                    .map(entry ->
                        TopFailedApis.TopFailedApi
                            .builder()
                            .id(entry.getKey())
                            .failedCalls(entry.getValue().failedCalls())
                            .failedCallsRatio(entry.getValue().failedCallsRatio())
                            .build()
                    )
                    .toList()
            )
            .map(topFailedApi -> TopFailedApis.builder().data(topFailedApi).build());
    }

    @Override
    public Optional<HistogramAnalytics> searchHistogramAnalytics(ExecutionContext executionContext, HistogramQuery histogramParameters) {
        List<io.gravitee.repository.log.v4.model.analytics.Aggregation> repoAggregations = null;
        if (histogramParameters.aggregations() != null) {
            repoAggregations =
                histogramParameters
                    .aggregations()
                    .stream()
                    .map(agg ->
                        new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                            agg.getField(),
                            io.gravitee.repository.log.v4.model.analytics.AggregationType.valueOf(agg.getAggregationType().name())
                        )
                    )
                    .collect(Collectors.toList());
        }

        List<HistogramAggregate<?>> repoResult = analyticsRepository.searchHistogram(
            executionContext.getQueryContext(),
            new io.gravitee.repository.log.v4.model.analytics.HistogramQuery(
                histogramParameters.apiId(),
                histogramParameters.from(),
                histogramParameters.to(),
                histogramParameters.interval(),
                repoAggregations
            )
        );

        if (repoResult == null || repoResult.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapHistogramAggregatesToHistogramAnalytics(repoResult, histogramParameters));
    }

    private HistogramAnalytics mapHistogramAggregatesToHistogramAnalytics(
        List<HistogramAggregate<?>> aggregates,
        HistogramQuery histogramParameters
    ) {
        Timestamp timestamp = new Timestamp(histogramParameters.from(), histogramParameters.to(), histogramParameters.interval());
        List<Bucket> values = mapBuckets(aggregates);
        return HistogramAnalytics.builder().timestamp(timestamp).values(values).build();
    }

    private List<Bucket> mapBuckets(List<HistogramAggregate<?>> aggregates) {
        if (aggregates == null) {
            return null;
        }
        return aggregates.stream().map(this::mapHistogramAggregateToBucket).collect(Collectors.toList());
    }

    private Bucket mapHistogramAggregateToBucket(HistogramAggregate<?> aggregate) {
        if (aggregate == null) {
            return null;
        }

        return Bucket
            .builder()
            .field(aggregate.getField())
            .name(aggregate.getName())
            .buckets(mapValuesToBuckets(aggregate.getBuckets()))
            .build();
    }

    private List<Bucket> mapValuesToBuckets(Map<String, ? extends List<?>> buckets) {
        return buckets
            .entrySet()
            .stream()
            .map(entry -> Bucket.builder().field(entry.getKey()).name(entry.getKey()).data((List<Number>) entry.getValue()).build())
            .toList();
    }
}
