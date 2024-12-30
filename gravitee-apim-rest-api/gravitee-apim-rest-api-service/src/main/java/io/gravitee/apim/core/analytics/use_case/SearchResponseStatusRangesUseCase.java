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
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchResponseStatusRangesUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        long end = input.to() != null ? input.to() : TimeProvider.instantNow().toEpochMilli();
        long start = input.from() != null ? input.from() : Instant.ofEpochMilli(end).minus(Duration.ofDays(1)).toEpochMilli();

        validateDates(start, end);
        validateApiRequirements(input);
        var queryParameters = AnalyticsQueryParameters.builder().apiIds(List.of(input.apiId())).from(start).to(end).build();

        return analyticsQueryService.searchResponseStatusRanges(executionContext, queryParameters).map(Output::new).orElse(new Output());
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

    private static void validateDates(long start, long end) {
        if (start > end) {
            throw new IllegalArgumentException("Start date cannot be greater than end date");
        }
    }

    public record Input(String apiId, String environmentId, Long from, Long to) {}

    public record Output(Optional<ResponseStatusRanges> responseStatusRanges) {
        Output(ResponseStatusRanges responseStatusRanges) {
            this(Optional.of(responseStatusRanges));
        }

        Output() {
            this(new ResponseStatusRanges());
        }
    }
}
