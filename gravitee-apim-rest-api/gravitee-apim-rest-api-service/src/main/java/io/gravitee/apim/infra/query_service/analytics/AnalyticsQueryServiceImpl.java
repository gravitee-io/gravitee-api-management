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

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
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
    public Optional<RequestsCount> searchRequestsCount(String apiId) {
        return analyticsRepository
            .searchRequestsCount(RequestsCountQuery.builder().apiId(apiId).build())
            .map(countAggregate ->
                RequestsCount.builder().total(countAggregate.getTotal()).countsByEntrypoint(countAggregate.getCountBy()).build()
            );
    }

    @Override
    public Optional<AverageMessagesPerRequest> searchAverageMessagesPerRequest(String apiId) {
        return analyticsRepository
            .searchAverageMessagesPerRequest(AverageMessagesPerRequestQuery.builder().apiId(apiId).build())
            .map(averageAggregate ->
                AverageMessagesPerRequest
                    .builder()
                    .globalAverage(averageAggregate.getAverage())
                    .averagesByEntrypoint(averageAggregate.getAverageBy())
                    .build()
            );
    }

    @Override
    public Optional<AverageConnectionDuration> searchAverageConnectionDuration(String apiId) {
        return analyticsRepository
            .searchAverageConnectionDuration(AverageConnectionDurationQuery.builder().apiId(apiId).build())
            .map(averageAggregate ->
                AverageConnectionDuration
                    .builder()
                    .globalAverage(averageAggregate.getAverage())
                    .averagesByEntrypoint(averageAggregate.getAverageBy())
                    .build()
            );
    }
}
