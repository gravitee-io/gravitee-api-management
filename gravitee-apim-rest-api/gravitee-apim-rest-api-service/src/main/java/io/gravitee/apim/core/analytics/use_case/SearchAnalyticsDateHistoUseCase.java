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
import io.gravitee.rest.api.model.v4.analytics.DateHistogramResult;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for the unified DATE_HISTO analytics query.
 * Returns time-bucketed histogram data with a nested terms sub-aggregation.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchAnalyticsDateHistoUseCase {

    private static final Set<String> ALLOWED_FIELDS = Set.of("status", "mapped-status", "application", "plan", "host", "uri");

    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsApiValidator analyticsApiValidator;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateField(input.field());
        validateInterval(input.interval());
        analyticsApiValidator.validate(input.apiId(), input.environmentId());

        var result = analyticsQueryService
            .searchDateHistogram(executionContext, input.apiId(), input.field(), input.interval(), input.from(), input.to())
            .orElse(DateHistogramResult.builder().timestamps(Collections.emptyList()).values(Collections.emptyMap()).build());

        return new Output(result);
    }

    private void validateField(String field) {
        if (field == null || !ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Invalid field: " + field + ". Allowed fields: " + ALLOWED_FIELDS);
        }
    }

    private void validateInterval(Duration interval) {
        if (interval == null || interval.toMillis() <= 0) {
            throw new IllegalArgumentException("Interval must be a positive duration, got: " + interval);
        }
    }

    public record Input(String apiId, String environmentId, String field, Duration interval, Instant from, Instant to) {}

    public record Output(DateHistogramResult dateHistogram) {}
}
