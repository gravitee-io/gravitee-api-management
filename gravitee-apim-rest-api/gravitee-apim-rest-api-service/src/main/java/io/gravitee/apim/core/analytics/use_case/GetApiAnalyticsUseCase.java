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
import io.gravitee.apim.core.analytics.model.AnalyticsType;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified analytics use case — dispatches to the appropriate Elasticsearch
 * aggregation strategy based on {@link AnalyticsType}.
 *
 * <p>US-01 implements COUNT. STATS, GROUP_BY and DATE_HISTO are added in US-03/04.</p>
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class GetApiAnalyticsUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);

        return switch (input.type()) {
            case COUNT -> executeCount(executionContext, input);
            default -> throw new IllegalArgumentException("Unsupported analytics type for this endpoint: " + input.type());
        };
    }

    // ── COUNT ────────────────────────────────────────────────────────────────

    private CountOutput executeCount(ExecutionContext ctx, Input input) {
        return analyticsQueryService
            .searchRequestsCount(ctx, input.apiId(), input.from(), input.to())
            .map(rc -> new CountOutput(rc.getTotal() != null ? rc.getTotal() : 0L))
            .orElse(new CountOutput(0L));
    }

    // ── API validation ───────────────────────────────────────────────────────

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId());
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId());
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, input.environmentId());
    }

    private static void validateApiDefinitionVersion(DefinitionVersion version, String apiId) {
        if (!DefinitionVersion.V4.equals(version)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiIsNotTcp(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4 != null && apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    // ── I/O types ────────────────────────────────────────────────────────────

    /**
     * @param from epoch-millisecond lower bound; null means no lower bound (use-case default applies)
     * @param to   epoch-millisecond upper bound; null means now
     */
    public record Input(String apiId, String environmentId, AnalyticsType type, Instant from, Instant to) {}

    /**
     * Sealed output hierarchy — one subtype per {@link AnalyticsType}.
     * US-03/04 will add StatsOutput, GroupByOutput, DateHistoOutput.
     */
    public sealed interface Output permits GetApiAnalyticsUseCase.CountOutput {}

    public record CountOutput(long count) implements Output {}
}
