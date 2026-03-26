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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.analytics.GroupByResult;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for the unified GROUP_BY analytics query.
 * Returns a map of field values to their occurrence count, bucketed using ES terms aggregation.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchAnalyticsGroupByUseCase {

    private static final int DEFAULT_SIZE = 10;

    private static final Set<String> ALLOWED_FIELDS = Set.of("status", "mapped-status", "application", "plan", "host", "uri");

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateField(input.field());
        validateApiRequirements(input);

        int size = input.size() != null ? input.size() : DEFAULT_SIZE;

        var result = analyticsQueryService
            .searchGroupBy(executionContext, input.apiId(), input.field(), size, input.from(), input.to())
            .orElse(GroupByResult.builder().values(Collections.emptyMap()).build());

        return new Output(result);
    }

    private void validateField(String field) {
        if (field == null || !ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Invalid field: " + field + ". Allowed fields: " + ALLOWED_FIELDS);
        }
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId);
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId);
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, input.environmentId);
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

    private void validateApiIsNotTcp(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4 != null && apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }

    public record Input(String apiId, String environmentId, String field, Integer size, Instant from, Instant to) {}

    public record Output(GroupByResult groupBy) {}
}
