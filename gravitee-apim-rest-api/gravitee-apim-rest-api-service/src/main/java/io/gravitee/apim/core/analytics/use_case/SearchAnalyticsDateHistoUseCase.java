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
import io.gravitee.apim.core.analytics.model.AnalyticsDateHistoResponse;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchAnalyticsDateHistoUseCase {

    static final Set<String> SUPPORTED_FIELDS = Set.of("status", "gateway-response-time-ms", "endpoint-response-time-ms");
    static final long MIN_INTERVAL_MS = 1_000L;
    static final long MAX_INTERVAL_MS = 1_000_000_000L;

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);
        validateInterval(input.intervalMs());
        validateField(input.field());

        var result = analyticsQueryService.searchDateHisto(
            executionContext,
            input.apiId(),
            input.field(),
            input.intervalMs(),
            input.from().orElse(null),
            input.to().orElse(null)
        );

        return new Output(result);
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId());
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId());
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, input.environmentId());
    }

    private static void validateField(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field is required for DATE_HISTO analytics");
        }
        if (!SUPPORTED_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                "Unsupported field for DATE_HISTO analytics: " + field + ". Supported fields: " + SUPPORTED_FIELDS
            );
        }
    }

    private static void validateInterval(long intervalMs) {
        if (intervalMs < MIN_INTERVAL_MS || intervalMs > MAX_INTERVAL_MS) {
            throw new IllegalArgumentException(
                "interval must be >= " + MIN_INTERVAL_MS + " and <= " + MAX_INTERVAL_MS + " but was: " + intervalMs
            );
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

    private static void validateApiIsNotTcp(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }

    public record Input(String apiId, String environmentId, String field, long intervalMs, Optional<Instant> from, Optional<Instant> to) {}

    public record Output(AnalyticsDateHistoResponse dateHisto) {}
}
