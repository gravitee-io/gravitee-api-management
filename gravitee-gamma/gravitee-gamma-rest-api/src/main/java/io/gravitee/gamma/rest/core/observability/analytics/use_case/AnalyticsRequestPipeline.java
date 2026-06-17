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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Shared pipeline for the three analytics use cases: validates incoming filter conditions against
 * the {@link Signal#ANALYTICS} catalog, computes the RBAC-scoped API set, applies default
 * entrypoint scoping, and returns a prepared scope using only Gamma-native types. All APIM
 * analytics-engine type translation is deferred to the infra adapter.
 *
 * @author GraviteeSource Team
 */
@DomainService
@AllArgsConstructor
public class AnalyticsRequestPipeline {

    static final Set<ApiType> ANALYTICS_SUPPORTED_API_TYPES = ApiType.ALL;

    static final Set<String> DEFAULT_ENTRYPOINT_IDS = Set.of("http-get", "http-post", "http-proxy", "llm-proxy", "mcp-proxy");

    private final ObservabilityFilterValidator filterValidator;
    private final AccessibleApiScopeDomainService accessibleApiScope;

    /**
     * Validated and RBAC-scoped request data, expressed entirely in Gamma-native types. The infra
     * adapter is responsible for translating {@link #filters()} to the analytics-engine model.
     */
    public record PreparedScope(Instant from, Instant to, List<FilterCondition> filters, Set<String> apiIds) {
        static final PreparedScope EMPTY = new PreparedScope(null, null, List.of(), Set.of());

        public boolean isEmpty() {
            return this == EMPTY;
        }
    }

    public PreparedScope prepare(
        String organizationId,
        String environmentId,
        List<FilterCondition> rawFilters,
        Instant from,
        Instant to,
        ObservabilityAnalyticsDataPort analyticsDataPort
    ) {
        var conditions = rawFilters != null ? rawFilters : List.<FilterCondition>of();

        filterValidator.validate(conditions, Signal.ANALYTICS);
        validateTimeRange(from, to);

        var accessibleApis = analyticsDataPort.loadAccessibleApis(organizationId, environmentId);
        var userApiFilter = extractApiFilter(conditions);
        var scope = accessibleApiScope.computeScope(accessibleApis, ANALYTICS_SUPPORTED_API_TYPES, userApiFilter);

        if (scope.apiIds().isEmpty() && !userApiFilter.isEmpty()) {
            return PreparedScope.EMPTY;
        }

        var effectiveConditions = applyDefaultEntrypointScoping(removeApiConditions(conditions));

        var allFilters = new ArrayList<>(effectiveConditions);
        if (!scope.apiIds().isEmpty()) {
            allFilters.add(new FilterCondition("API", FilterOperator.IN, List.copyOf(scope.apiIds())));
        }

        return new PreparedScope(from, to, List.copyOf(allFilters), scope.apiIds());
    }

    private static void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationDomainException("Invalid time range: 'from' must be before 'to'.");
        }
    }

    private static Set<String> extractApiFilter(List<FilterCondition> conditions) {
        return conditions
            .stream()
            .filter(c -> "API".equals(c.name()))
            .flatMap(c -> c.values().stream())
            .collect(Collectors.toSet());
    }

    private static List<FilterCondition> removeApiConditions(List<FilterCondition> conditions) {
        return conditions
            .stream()
            .filter(c -> !"API".equals(c.name()))
            .toList();
    }

    private static List<FilterCondition> applyDefaultEntrypointScoping(List<FilterCondition> conditions) {
        boolean hasEntrypoint = conditions.stream().anyMatch(c -> "ENTRYPOINT".equals(c.name()));
        if (hasEntrypoint) {
            return conditions;
        }
        var result = new ArrayList<>(conditions);
        result.add(new FilterCondition("ENTRYPOINT", FilterOperator.IN, List.copyOf(DEFAULT_ENTRYPOINT_IDS)));
        return List.copyOf(result);
    }
}
