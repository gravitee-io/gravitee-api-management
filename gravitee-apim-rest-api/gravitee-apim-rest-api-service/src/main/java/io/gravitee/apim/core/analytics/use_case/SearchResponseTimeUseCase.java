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

import static io.gravitee.apim.core.analytics.utils.AnalyticsUtils.validateHttpV4Api;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class SearchResponseTimeUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Single<Output> execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);

        ZonedDateTime now = TimeProvider.now();
        return analyticsQueryService
            .searchAvgResponseTimeOverTime(executionContext, input.apiId, now.minusDays(1), now, Duration.ofMinutes(30))
            .defaultIfEmpty(new AverageAggregate(0D, Map.of()))
            .map(Output::new);
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId);
        validateHttpV4Api(api, input.environmentId);
    }

    public record Input(String apiId, String environmentId) {}

    public record Output(AverageAggregate obj) {}
}
