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
import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.analytics.model.StatsAnalytics;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchStatsUseCase {

    private final ApiCrudService apiCrudService;
    private final AnalyticsQueryService analyticsQueryService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateInput(input, executionContext);

        var statsQuery = new AnalyticsQueryService.StatsQuery(
            input.api(),
            input.field(),
            Instant.ofEpochMilli(input.from()),
            Instant.ofEpochMilli(input.to())
        );
        var result = analyticsQueryService.searchStatsAnalytics(executionContext, statsQuery).orElse(null);

        return new Output(result);
    }

    private void validateInput(Input input, ExecutionContext executionContext) {
        validateApi(input.api, executionContext);
        validateTimeRange(input.from, input.to);
    }

    private void validateTimeRange(long from, long to) {
        if (from > to) {
            throw new IllegalTimeRangeException();
        }
    }

    private void validateApi(String apiId, ExecutionContext executionContext) {
        var api = apiCrudService.get(apiId);
        validateApiV4(apiId, api.getDefinitionVersion());
        validateApiProxy(api);
        validateApiMultiTenancyAccess(api, executionContext.getEnvironmentId());
    }

    private void validateApiV4(String apiId, DefinitionVersion apiDefinition) {
        if (!DefinitionVersion.V4.equals(apiDefinition)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private void validateApiProxy(Api api) {
        if (api.isTcpProxy()) {
            throw new TcpProxyNotSupportedException(api.getId());
        }
    }

    private void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    public record Input(String api, long from, long to, String field) {}

    public record Output(StatsAnalytics analytics) {}
}
