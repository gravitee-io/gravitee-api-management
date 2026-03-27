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
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.rest.api.model.v4.analytics.StatsResult;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for the unified STATS analytics query.
 * Returns statistical aggregations (min/max/avg/sum/count) for a given field on a V4 API.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchAnalyticsStatsUseCase {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "gateway-response-time-ms",
        "endpoint-response-time-ms",
        "request-content-length",
        "gateway-latency-ms"
    );

    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsApiValidator analyticsApiValidator;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateField(input.field());
        analyticsApiValidator.validate(input.apiId(), input.environmentId());

        var stats = analyticsQueryService
            .searchStats(executionContext, input.apiId(), input.field(), input.from(), input.to())
            .orElse(StatsResult.builder().count(0).min(0).max(0).avg(0).sum(0).build());

        return new Output(stats);
    }

    private void validateField(String field) {
        if (field == null || !ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Invalid field: " + field + ". Allowed fields: " + ALLOWED_FIELDS);
        }
    }

    public record Input(String apiId, String environmentId, String field, Instant from, Instant to) {}

    public record Output(StatsResult stats) {}
}
