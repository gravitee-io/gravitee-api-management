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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.utils.DurationUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class SearchResponseTimeUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Single<Output> execute(ExecutionContext executionContext, Input input) {
        Instant to = input.to();
        Instant from = input.from();
        Duration interval = DurationUtils.buildIntervalFromTimePeriod(from, to);

        return validateApiRequirements(input)
            .flatMapMaybe(api -> analyticsQueryService.searchAvgResponseTimeOverTime(executionContext, api.getId(), from, to, interval))
            .map(statsData -> new Output(from, to, interval, statsData.values().stream().map(Math::round).toList()))
            .defaultIfEmpty(new Output(from, to, interval, List.of()));
    }

    private Single<Api> validateApiRequirements(Input input) {
        return Maybe
            .fromOptional(apiCrudService.findById(input.apiId))
            .switchIfEmpty(Single.error(new ApiNotFoundException(input.apiId)))
            .flatMap(SearchResponseTimeUseCase::validateApiDefinitionVersion)
            .flatMap(api -> validateApiMultiTenancyAccess(api, input.environmentId))
            .flatMap(SearchResponseTimeUseCase::validateApiIsNotTcp);
    }

    private static Single<Api> validateApiIsNotTcp(Api api) {
        return api.getApiDefinitionHttpV4().isTcpProxy() ? Single.error(new TcpProxyNotSupportedException(api.getId())) : Single.just(api);
    }

    private static Single<Api> validateApiMultiTenancyAccess(Api api, String environmentId) {
        return api.belongsToEnvironment(environmentId) ? Single.just(api) : Single.error(new ApiNotFoundException(api.getId()));
    }

    private static Single<Api> validateApiDefinitionVersion(Api api) {
        return DefinitionVersion.V4.equals(api.getDefinitionVersion())
            ? Single.just(api)
            : Single.error(new ApiInvalidDefinitionVersionException(api.getId()));
    }

    public record Input(String apiId, String environmentId, Instant from, Instant to) {}

    public record Output(Instant from, Instant to, Duration interval, List<Long> data) {}
}
