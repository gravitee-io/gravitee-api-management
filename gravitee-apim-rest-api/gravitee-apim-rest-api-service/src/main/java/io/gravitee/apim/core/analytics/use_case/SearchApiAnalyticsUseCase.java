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
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class SearchApiAnalyticsUseCase {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
        "status",
        "mapped-status",
        "application",
        "plan",
        "host",
        "uri",
        "gateway-latency-ms",
        "gateway-response-time-ms",
        "endpoint-response-time-ms",
        "request-content-length"
    );

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input.apiId(), input.environmentId());

        return switch (input.type()) {
            case COUNT -> new Output(
                Type.COUNT,
                analyticsQueryService
                    .searchRequestsCount(executionContext, input.apiId(), input.from(), input.to())
                    .map(requestsCount -> requestsCount.getTotal() == null ? 0L : requestsCount.getTotal())
                    .orElse(0L)
            );
            case STATS -> {
                validateStatsField(input.field());
                yield analyticsQueryService
                    .searchStats(executionContext, input.apiId(), input.from(), input.to(), input.field())
                    .map(stats -> new Output(Type.STATS, stats.getCount(), stats.getMin(), stats.getMax(), stats.getAvg(), stats.getSum()))
                    .orElse(new Output(Type.STATS, 0L, 0D, 0D, 0D, 0D));
            }
            case GROUP_BY -> {
                validateGroupByField(input.field());
                var size = input.size() == null ? 10 : input.size();
                var order = input.order() == null ? GroupByOrder.DESC : input.order();
                yield analyticsQueryService
                    .searchGroupBy(
                        executionContext,
                        input.apiId(),
                        input.from(),
                        input.to(),
                        input.field(),
                        size,
                        AnalyticsQueryService.GroupByOrder.valueOf(order.name())
                    )
                    .map(groupBy -> new Output(Type.GROUP_BY, null, null, null, null, null, groupBy.getValues(), groupBy.getMetadata()))
                    .orElse(new Output(Type.GROUP_BY, null, null, null, null, null, Map.of(), Map.of()));
            }
            case DATE_HISTO -> new Output(Type.DATE_HISTO);
        };
    }

    private static void validateStatsField(String field) {
        if (!SUPPORTED_FIELDS.contains(field)) {
            throw new ValidationDomainException("Unsupported stats field", Map.of("field", String.valueOf(field)));
        }
    }

    private static void validateGroupByField(String field) {
        if (!SUPPORTED_FIELDS.contains(field)) {
            throw new ValidationDomainException("Unsupported group by field", Map.of("field", String.valueOf(field)));
        }
    }

    private void validateApiRequirements(String apiId, String environmentId) {
        final Api api = apiCrudService.get(apiId);
        validateApiDefinitionVersion(api.getDefinitionVersion(), apiId);
        validateApiType(api);
        validateApiMultiTenancyAccess(api, environmentId);
        validateApiIsNotTcp(api);
    }

    private static void validateApiDefinitionVersion(DefinitionVersion definitionVersion, String apiId) {
        if (!DefinitionVersion.V4.equals(definitionVersion)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiType(Api api) {
        if (!ApiType.PROXY.equals(api.getType())) {
            throw new ApiInvalidTypeException(api.getId(), ApiType.PROXY);
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    private static void validateApiIsNotTcp(Api api) {
        if (api.getApiDefinitionHttpV4().isTcpProxy()) {
            throw new TcpProxyNotSupportedException(api.getId());
        }
    }

    public record Input(
        String apiId,
        String environmentId,
        Type type,
        Instant from,
        Instant to,
        String field,
        Integer size,
        GroupByOrder order
    ) {}

    public record Output(
        Type type,
        Long count,
        Double min,
        Double max,
        Double avg,
        Double sum,
        Map<String, Long> values,
        Map<String, Map<String, String>> metadata
    ) {
        Output(Type type) {
            this(type, null, null, null, null, null, null, null);
        }

        Output(Type type, Long count) {
            this(type, count, null, null, null, null, null, null);
        }

        Output(Type type, Long count, Double min, Double max, Double avg, Double sum) {
            this(type, count, min, max, avg, sum, null, null);
        }
    }

    public enum Type {
        COUNT,
        STATS,
        GROUP_BY,
        DATE_HISTO,
    }

    public enum GroupByOrder {
        ASC,
        DESC,
    }
}
