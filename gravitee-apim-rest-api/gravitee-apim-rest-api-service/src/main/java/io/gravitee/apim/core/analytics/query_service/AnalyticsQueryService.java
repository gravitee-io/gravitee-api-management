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

import io.gravitee.apim.core.analytics.model.EnvironmentAnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
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

public interface AnalyticsQueryService {
    Optional<RequestsCount> searchRequestsCount(ExecutionContext executionContext, String apiId);

    Optional<AverageMessagesPerRequest> searchAverageMessagesPerRequest(ExecutionContext executionContext, String apiId);

    Optional<AverageConnectionDuration> searchAverageConnectionDuration(ExecutionContext executionContext, String apiId);

    Optional<ResponseStatusRanges> searchResponseStatusRanges(
        ExecutionContext executionContext,
        EnvironmentAnalyticsQueryParameters queryParameters
    );

    Optional<TopHitsApis> searchTopHitsApis(ExecutionContext executionContext, EnvironmentAnalyticsQueryParameters parameters);

    Maybe<Map<String, Double>> searchAvgResponseTimeOverTime(
        ExecutionContext executionContext,
        String apiId,
        Instant startTime,
        Instant endTime,
        Duration interval
    );

    ResponseStatusOvertime searchResponseStatusOvertime(ExecutionContext executionContext, ResponseStatusOverTimeQuery query);

    RequestResponseTime searchRequestResponseTime(ExecutionContext executionContext, EnvironmentAnalyticsQueryParameters parameters);

    record ResponseStatusOverTimeQuery(String apiId, Instant from, Instant to, Duration interval) {}
}
