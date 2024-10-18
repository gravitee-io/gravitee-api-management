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

import io.gravitee.apim.core.analytics.model.StatusRangesQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.infra.adapter.ResponseStatusQueryCriteriaAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
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
            .searchRequestsCount(executionContext.getQueryContext(), RequestsCountQuery.builder().apiId(apiId).build())
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
        StatusRangesQueryParameters queryParameters
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
    public Optional<AverageConnectionDuration> searchAverageConnectionDuration(ExecutionContext executionContext, String apiId) {
        return analyticsRepository
            .searchAverageConnectionDuration(
                executionContext.getQueryContext(),
                AverageConnectionDurationQuery.builder().apiId(apiId).build()
            )
            .map(averageAggregate ->
                AverageConnectionDuration
                    .builder()
                    .globalAverage(averageAggregate.getAverage())
                    .averagesByEntrypoint(averageAggregate.getAverageBy())
                    .build()
            );
    }
}
