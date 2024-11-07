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
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AvailabilityUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiHealthQueryService apiHealthQueryService;

    public Maybe<Output> execute(Input input) {
        return validateApiRequirements(input)
            .flatMapMaybe(api -> apiHealthQueryService.availability(input.toApiFieldPeriodQuery()))
            .map(e -> new Output(e.global(), e.byField()));
    }

    private Single<Api> validateApiRequirements(AvailabilityUseCase.Input input) {
        return Single
            .fromCallable(() -> apiCrudService.get(input.api()))
            .flatMap(api -> validateApiMultiTenancyAccess(api, input.ctx().getEnvironmentId()))
            .flatMap(this::validateApiIsNotTcp);
    }

    private Single<Api> validateApiIsNotTcp(Api api) {
        return api.getApiDefinitionHttpV4().isTcpProxy() ? Single.error(new TcpProxyNotSupportedException(api.getId())) : Single.just(api);
    }

    private static Single<Api> validateApiMultiTenancyAccess(Api api, String environmentId) {
        return !api.belongsToEnvironment(environmentId) ? Single.error(new ApiNotFoundException(api.getId())) : Single.just(api);
    }

    public record Input(ExecutionContext ctx, Instant since, Instant until, String api, String field) {
        public ApiHealthQueryService.ApiFieldPeriodQuery toApiFieldPeriodQuery() {
            return new ApiHealthQueryService.ApiFieldPeriodQuery(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                api(),
                field(),
                since(),
                until()
            );
        }
    }

    public record Output(float global, Map<String, Float> byField) {}
}
