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
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for the unified COUNT analytics query.
 * Returns total request count for a V4 API within a time range.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchAnalyticsCountUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsApiValidator analyticsApiValidator;

    public Output execute(ExecutionContext executionContext, Input input) {
        analyticsApiValidator.validate(input.apiId(), input.environmentId());

        var count = analyticsQueryService
            .searchRequestsCount(executionContext, input.apiId(), input.from(), input.to())
            .map(requestsCount -> requestsCount.getTotal() != null ? requestsCount.getTotal() : 0L)
            .orElse(0L);

        return new Output(count);
    }

    public record Input(String apiId, String environmentId, Instant from, Instant to) {}

    public record Output(long count) {}
}
