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
package io.gravitee.gamma.rest.core.observability.logs.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsPage;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

/**
 * Environment-wide log search returning light rows from the {@code v4-metrics} index. Validates
 * every incoming {@link FilterCondition} against the unified filter registry for the
 * {@link Signal#LOGS} signal, computes the RBAC-scoped API set via
 * {@link AccessibleApiScopeDomainService}, applies default entrypoint scoping so table totals match
 * the dashboard, and delegates the actual search to the data port.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class SearchObservabilityLogsUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 20;
    private static final int MAX_PER_PAGE = 100;

    /**
     * API types the LOGS signal serves today. Extensible: adding MESSAGE or NATIVE later only
     * requires widening this set — the rest of the pipeline (AccessibleApiScope, query building)
     * adjusts automatically.
     */
    static final Set<ApiType> LOGS_SUPPORTED_API_TYPES = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.A2A);

    /**
     * Canonical HTTP entrypoints applied when no explicit entrypoint filter is set, so the logs
     * table total matches the analytics dashboard for the same time range. Mirrors
     * {@code FilterAdapter.httpFilter()} from the analytics ES adapter. The ES query builder
     * adds a field-missing fallback alongside these terms.
     */
    static final Set<String> DEFAULT_ENTRYPOINT_IDS = Set.of("http-get", "http-post", "http-proxy", "llm-proxy", "mcp-proxy", "a2a-proxy");

    private final ObservabilityLogsDataPort logsDataPort;
    private final ObservabilityFilterValidator filterValidator;
    private final AccessibleApiScopeDomainService accessibleApiScope;

    public record Input(
        String organizationId,
        String environmentId,
        List<FilterCondition> filters,
        Instant from,
        Instant to,
        Integer page,
        Integer perPage
    ) {}

    public record Output(LogsPage data, int page, int perPage) {}

    public Output execute(Input input) {
        int page = resolvePage(input);
        int perPage = resolvePerPage(input);

        var conditions = input.filters != null ? input.filters : List.<FilterCondition>of();

        filterValidator.validate(conditions, Signal.LOGS);
        validateTimeRange(input.from, input.to);

        var accessibleApis = logsDataPort.loadAccessibleApis(input.organizationId, input.environmentId);

        var userApiFilter = extractApiFilter(conditions);
        var effectiveApiTypes = narrowApiTypes(conditions);
        var scope = accessibleApiScope.computeScope(accessibleApis, effectiveApiTypes, userApiFilter);

        if (scope.apiIds().isEmpty()) {
            return new Output(LogsPage.EMPTY, page, perPage);
        }

        var effectiveConditions = applyDefaultEntrypointScoping(removeScopeConditions(conditions));

        var query = LogsSearchQuery.builder()
            .apiIds(scope.apiIds())
            .apisById(scope.apisById())
            .conditions(effectiveConditions)
            .from(input.from != null ? input.from.toEpochMilli() : null)
            .to(input.to != null ? input.to.toEpochMilli() : null)
            .page(page)
            .perPage(perPage)
            .build();

        var result = logsDataPort.searchLogs(input.organizationId, input.environmentId, query);
        return new Output(result, page, perPage);
    }

    private static int resolvePage(Input input) {
        return (input.page() != null && input.page() > 0) ? input.page() : DEFAULT_PAGE;
    }

    private static int resolvePerPage(Input input) {
        return (input.perPage() != null && input.perPage() > 0) ? Math.min(input.perPage(), MAX_PER_PAGE) : DEFAULT_PER_PAGE;
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

    /**
     * Narrows {@link #LOGS_SUPPORTED_API_TYPES} when the caller supplies an explicit
     * {@code API_TYPE} filter. Values are intersected with the supported set so that an
     * unsupported type (e.g. {@code MESSAGE}) simply yields an empty scope instead of an error.
     */
    private static Set<ApiType> narrowApiTypes(List<FilterCondition> conditions) {
        var requested = conditions
            .stream()
            .filter(c -> "API_TYPE".equals(c.name()))
            .flatMap(c -> c.values().stream())
            .flatMap(v -> {
                try {
                    return Stream.of(ApiType.valueOf(v));
                } catch (IllegalArgumentException e) {
                    return Stream.empty();
                }
            })
            .collect(Collectors.toSet());

        if (requested.isEmpty()) {
            return LOGS_SUPPORTED_API_TYPES;
        }
        requested.retainAll(LOGS_SUPPORTED_API_TYPES);
        return requested;
    }

    private static List<FilterCondition> removeScopeConditions(List<FilterCondition> conditions) {
        return conditions
            .stream()
            .filter(c -> !"API".equals(c.name()) && !"API_TYPE".equals(c.name()))
            .toList();
    }

    /**
     * If the caller didn't supply an explicit entrypoint filter, inject the canonical HTTP
     * entrypoint set so the logs table matches the dashboard totals.
     */
    private static List<FilterCondition> applyDefaultEntrypointScoping(List<FilterCondition> conditions) {
        boolean hasEntrypoint = conditions.stream().anyMatch(c -> "ENTRYPOINT".equals(c.name()));
        if (hasEntrypoint) {
            return conditions;
        }
        var result = new ArrayList<>(conditions);
        result.add(
            new FilterCondition(
                "ENTRYPOINT",
                io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator.IN,
                List.copyOf(DEFAULT_ENTRYPOINT_IDS)
            )
        );
        return List.copyOf(result);
    }
}
