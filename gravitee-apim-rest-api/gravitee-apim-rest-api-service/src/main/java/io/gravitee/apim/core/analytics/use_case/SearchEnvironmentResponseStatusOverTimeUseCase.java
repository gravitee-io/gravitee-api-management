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
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.utils.DurationUtils;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
@UseCase
public class SearchEnvironmentResponseStatusOverTimeUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;

    public Output execute(ExecutionContext executionContext, Input input) {
        Duration interval = DurationUtils.buildIntervalFromTimePeriod(input.from(), input.to());
        var apiIdsByDefinitionVersion = environmentAnalyticsApisDomainService.findReadableApiIdsByDefinitionVersion(
            executionContext,
            input.userId()
        );
        var apiIds = apiIdsByDefinitionVersion.values().stream().flatMap(Collection::stream).toList();
        if (apiIds.isEmpty()) {
            return new Output(
                new ResponseStatusOvertime(new ResponseStatusOvertime.TimeRange(input.from(), input.to(), interval), Map.of())
            );
        }

        var result = analyticsQueryService.searchResponseStatusOvertime(
            executionContext,
            new AnalyticsQueryService.ResponseStatusOverTimeQuery(
                apiIds,
                input.from(),
                input.to(),
                interval,
                apiIdsByDefinitionVersion.keySet()
            )
        );

        return new Output(result);
    }

    public record Input(Instant from, Instant to, String userId) {}

    public record Output(ResponseStatusOvertime responseStatusOvertime) {}
}
