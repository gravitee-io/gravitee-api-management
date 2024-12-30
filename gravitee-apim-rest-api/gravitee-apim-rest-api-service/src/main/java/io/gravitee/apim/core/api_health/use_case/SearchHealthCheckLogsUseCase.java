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
package io.gravitee.apim.core.api_health.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_health.model.HealthCheckLog;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchHealthCheckLogsUseCase {

    private final ApiHealthQueryService apiHealthQueryService;
    private final ApiCrudService apiCrudService;

    public Maybe<Output> execute(SearchHealthCheckLogsUseCase.Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        return validateApiRequirements(input)
            .flatMapMaybe(api ->
                apiHealthQueryService.searchLogs(
                    new ApiHealthQueryService.SearchLogsQuery(
                        input.organizationId,
                        input.environmentId,
                        input.apiId,
                        input.from,
                        input.to,
                        input.success,
                        pageable
                    )
                )
            )
            .map(Output::new);
    }

    private Single<Api> validateApiRequirements(Input input) {
        return Single
            .fromCallable(() -> apiCrudService.get(input.apiId()))
            .flatMap(api -> validateApiMultiTenancyAccess(api, input.environmentId()))
            .flatMap(this::validateApiIsNotTcp);
    }

    private Single<Api> validateApiIsNotTcp(Api api) {
        return api.getApiDefinitionHttpV4().isTcpProxy() ? Single.error(new TcpProxyNotSupportedException(api.getId())) : Single.just(api);
    }

    private static Single<Api> validateApiMultiTenancyAccess(Api api, String environmentId) {
        return !api.belongsToEnvironment(environmentId) ? Single.error(new ApiNotFoundException(api.getId())) : Single.just(api);
    }

    public record Input(
        String organizationId,
        String environmentId,
        String apiId,
        Instant from,
        Instant to,
        Optional<Boolean> success,
        Optional<Pageable> pageable
    ) {}

    public record Output(Page<HealthCheckLog> logs) {}
}
