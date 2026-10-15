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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.utils.DurationUtils;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
@UseCase
public class SearchEnvironmentResponseTimeOverTimeUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;

    public Single<Output> execute(ExecutionContext executionContext, Input input) {
        Instant to = input.to();
        Instant from = input.from();
        Duration interval = DurationUtils.buildIntervalFromTimePeriod(from, to);

        var apiIds = environmentAnalyticsApisDomainService.findReadableApiIdsByDefinitionVersion(executionContext, input.userId());
        var apis = apiIds.values().stream().flatMap(List::stream).toList();
        if (apis.isEmpty()) {
            return Single.just(new Output(from, to, interval, List.of()));
        }

        return analyticsQueryService
            .searchAvgResponseTimeOverTime(executionContext, apis, from, to, interval, apiIds.keySet())
            .map(statsData -> new Output(from, to, interval, statsData.values().stream().map(Math::round).toList()))
            .defaultIfEmpty(new Output(from, to, interval, List.of()));
    }

    public record Input(Instant from, Instant to, String userId) {}

    public record Output(Instant from, Instant to, Duration interval, List<Long> data) {}
}
