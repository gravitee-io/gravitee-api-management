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
import io.gravitee.apim.core.analytics.domain_service.ApiAnalyticsSpecification;
import io.gravitee.apim.core.analytics.model.StatsAnalytics;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchStatsUseCase {

    private final ApiCrudService apiCrudService;
    private final AnalyticsQueryService analyticsQueryService;

    public Output execute(ExecutionContext executionContext, Input input) {
        ApiAnalyticsSpecification
            .forSearchStatsAnalytics()
            .throwIfNotSatisfied(apiCrudService.get(input.api()), executionContext, input.from(), input.to());

        var statsQuery = new AnalyticsQueryService.StatsQuery(
            input.api(),
            input.field(),
            Instant.ofEpochMilli(input.from()),
            Instant.ofEpochMilli(input.to()),
            input.query()
        );
        var result = analyticsQueryService.searchStatsAnalytics(executionContext, statsQuery).orElse(null);

        return new Output(result);
    }

    public record Input(String api, long from, long to, String field, Optional<String> query) {}

    public record Output(StatsAnalytics analytics) {}
}
