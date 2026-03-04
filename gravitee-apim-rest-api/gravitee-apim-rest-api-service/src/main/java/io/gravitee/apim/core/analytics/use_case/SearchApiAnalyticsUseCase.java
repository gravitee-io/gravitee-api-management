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
import io.gravitee.apim.core.analytics.model.DateHistoResult;
import io.gravitee.apim.core.analytics.model.GroupByResult;
import io.gravitee.apim.core.analytics.model.StatsResult;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchApiAnalyticsUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);

        return switch (input.type()) {
            case COUNT -> executeCount(executionContext, input);
            case STATS -> executeStats(executionContext, input);
            case GROUP_BY -> executeGroupBy(executionContext, input);
            case DATE_HISTO -> executeDateHistogram(executionContext, input);
        };
    }

    private Output executeCount(ExecutionContext executionContext, Input input) {
        var count = analyticsQueryService.searchCount(executionContext, input.apiId(), input.from(), input.to()).orElse(0L);
        return new Output(AnalyticsResult.count(count));
    }

    private Output executeStats(ExecutionContext executionContext, Input input) {
        var stats = analyticsQueryService
            .searchStats(executionContext, input.apiId(), input.from(), input.to(), input.field())
            .orElse(StatsResult.builder().count(0).min(0).max(0).avg(0).sum(0).build());
        return new Output(AnalyticsResult.stats(stats));
    }

    private Output executeGroupBy(ExecutionContext executionContext, Input input) {
        var groupBy = analyticsQueryService
            .searchGroupBy(executionContext, input.apiId(), input.from(), input.to(), input.field(), input.size())
            .orElse(GroupByResult.builder().values(java.util.Map.of()).metadata(java.util.Map.of()).build());
        return new Output(AnalyticsResult.groupBy(groupBy));
    }

    private Output executeDateHistogram(ExecutionContext executionContext, Input input) {
        var interval = input.interval() != null ? Duration.ofMillis(input.interval()) : Duration.ofHours(1);
        var dateHisto = analyticsQueryService
            .searchDateHistogram(executionContext, input.apiId(), input.from(), input.to(), input.field(), interval, input.size())
            .orElse(DateHistoResult.builder().timestamps(java.util.List.of()).values(java.util.List.of()).build());
        return new Output(AnalyticsResult.dateHisto(dateHisto));
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId());
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId());
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, input.environmentId());
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
        if (apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }

    public enum AnalyticsQueryType {
        COUNT,
        STATS,
        GROUP_BY,
        DATE_HISTO,
    }

    public record Input(
        String apiId,
        String environmentId,
        AnalyticsQueryType type,
        Instant from,
        Instant to,
        String field,
        Long interval,
        int size
    ) {}

    public record Output(AnalyticsResult result) {}

    public sealed interface AnalyticsResult {
        record CountResult(long count) implements AnalyticsResult {}

        record StatsResultResult(StatsResult stats) implements AnalyticsResult {}

        record GroupByResultResult(GroupByResult groupBy) implements AnalyticsResult {}

        record DateHistoResultResult(DateHistoResult dateHisto) implements AnalyticsResult {}

        static AnalyticsResult count(long count) {
            return new CountResult(count);
        }

        static AnalyticsResult stats(StatsResult stats) {
            return new StatsResultResult(stats);
        }

        static AnalyticsResult groupBy(GroupByResult groupBy) {
            return new GroupByResultResult(groupBy);
        }

        static AnalyticsResult dateHisto(DateHistoResult dateHisto) {
            return new DateHistoResultResult(dateHisto);
        }
    }
}
