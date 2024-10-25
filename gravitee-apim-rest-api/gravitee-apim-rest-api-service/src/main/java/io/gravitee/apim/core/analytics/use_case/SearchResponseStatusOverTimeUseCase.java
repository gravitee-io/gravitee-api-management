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
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchResponseStatusOverTimeUseCase {

    private static final Collection<IntervalData> INTERVAL = List.of(
        new IntervalData(Duration.ofMinutes(5), Duration.ofSeconds(10)),
        new IntervalData(Duration.ofMinutes(30), Duration.ofSeconds(15)),
        new IntervalData(Duration.ofHours(1), Duration.ofSeconds(30)),
        new IntervalData(Duration.ofHours(3), Duration.ofMinutes(1)),
        new IntervalData(Duration.ofHours(6), Duration.ofMinutes(2)),
        new IntervalData(Duration.ofHours(12), Duration.ofMinutes(5)),
        new IntervalData(Duration.ofDays(1), Duration.ofMinutes(10)),
        new IntervalData(Duration.ofDays(3), Duration.ofMinutes(30)),
        new IntervalData(Duration.ofDays(7), Duration.ofHours(1)),
        new IntervalData(Duration.ofDays(14), Duration.ofHours(3)),
        new IntervalData(Duration.ofDays(30), Duration.ofHours(6)),
        new IntervalData(Duration.ofDays(90), Duration.ofHours(12))
    );

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);
        Duration duration = Duration.between(input.from(), input.to());
        Duration interval = INTERVAL
            .stream()
            .filter(id -> id.dataDuration().compareTo(duration) <= 0)
            .max(Comparator.comparing(IntervalData::dataDuration))
            .map(IntervalData::interval)
            .orElse(Duration.ofDays(1));

        var result = analyticsQueryService.searchResponseStatusOvertime(
            executionContext,
            new AnalyticsQueryService.ResponseStatusOverTimeQuery(input.apiId, input.from(), input.to(), interval)
        );

        return new Output(result);
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId);
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId);
        validateApiMultiTenancyAccess(api, input.environmentId);
        validateApiIsNotTcp(api);
    }

    private void validateApiIsNotTcp(Api api) {
        if (api.getApiDefinitionHttpV4().isTcpProxy()) {
            throw new TcpProxyNotSupportedException(api.getId());
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    private static void validateApiDefinitionVersion(DefinitionVersion definitionVersion, String apiId) {
        if (!DefinitionVersion.V4.equals(definitionVersion)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    public record Input(String apiId, String environmentId, Instant from, Instant to) {}

    public record Output(ResponseStatusOvertime responseStatusOvertime) {}

    private record IntervalData(Duration dataDuration, Duration interval) {}
}
