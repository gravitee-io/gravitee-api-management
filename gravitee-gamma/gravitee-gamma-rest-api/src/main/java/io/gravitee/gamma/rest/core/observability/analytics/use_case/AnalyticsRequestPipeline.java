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
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.RecordType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Shared pipeline for the three analytics use cases: validates incoming filter conditions against
 * the {@link Signal#ANALYTICS} catalog, computes the RBAC-scoped API set, strips the scope-only
 * conditions, and returns a prepared scope using only Gamma-native types. The entrypoint scope is the
 * engine's: it applies its fail-open default to a query without an {@code ENTRYPOINT} condition and
 * honours an explicit one exactly. All APIM analytics-engine type translation is deferred to the infra
 * adapter.
 *
 * @author GraviteeSource Team
 */
@DomainService
@AllArgsConstructor
public class AnalyticsRequestPipeline {

    static final Set<ApiType> ANALYTICS_SUPPORTED_API_TYPES = ApiType.ALL;

    private static final String RECORD_TYPE = "RECORD_TYPE";

    /** The metrics served from authorization decision records; pinned to the engine's routing by a test. */
    static final Set<String> AUTHZ_DECISION_METRICS = Set.of(
        "AUTHZ_DECISIONS",
        "AUTHZ_PERMITS",
        "AUTHZ_FORBIDS",
        "AUTHZ_NOT_APPLICABLE",
        "AUTHZ_FAILURES",
        "AUTHZ_EVAL_DURATION"
    );

    private final ObservabilityFilterValidator filterValidator;
    private final AccessibleApiScopeDomainService accessibleApiScope;

    /**
     * Validated and RBAC-scoped request data, expressed entirely in Gamma-native types. The infra
     * adapter is responsible for translating {@link #filters()} to the analytics-engine model.
     */
    public record PreparedScope(Instant from, Instant to, List<FilterCondition> filters, Set<String> apiIds) {
        /** Nothing the caller can read. Keeps the time range, so the query can still be validated. */
        public static PreparedScope empty(Instant from, Instant to) {
            return new PreparedScope(from, to, List.of(), Set.of());
        }

        public boolean isEmpty() {
            return apiIds.isEmpty();
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

        // Nothing readable means nothing to query, whether the caller named APIs it cannot read or none at
        // all: an analytics query never runs environment-wide.
        if (scope.apiIds().isEmpty()) {
            return PreparedScope.empty(from, to);
        }

        var effectiveConditions = removeRecordTypeConditions(removeApiConditions(conditions));

        var allFilters = new ArrayList<>(effectiveConditions);
        allFilters.add(new FilterCondition("API", FilterOperator.IN, List.copyOf(scope.apiIds())));

        return new PreparedScope(from, to, List.copyOf(allFilters), scope.apiIds());
    }

    /**
     * Per metric, the conditions Gamma never hands to the engine, which the response must still name as
     * not applied:
     *
     * <ul>
     *   <li>a {@code RECORD_TYPE} that does not select the metric's kind of record: {@link #prepare} strips
     *       it whatever its value;</li>
     *   <li>every metric-level condition: they are validated like the top-level ones, but not wired to the
     *       engine yet.</li>
     * </ul>
     *
     * Metrics with nothing to report are left out.
     */
    public Map<String, List<String>> conditionsNotApplied(List<FilterCondition> rawFilters, List<MetricConditions> metrics) {
        metrics.forEach(metric -> filterValidator.validate(metric.conditions(), Signal.ANALYTICS));
        var recordTypes = (rawFilters != null ? rawFilters : List.<FilterCondition>of()).stream()
            .filter(c -> RECORD_TYPE.equals(c.name()))
            .toList();

        var notApplied = new LinkedHashMap<String, List<String>>();
        for (var metric : metrics) {
            var names = new LinkedHashSet<String>();
            recordTypes
                .stream()
                .filter(condition -> !selects(condition, recordTypeOf(metric.metric())))
                .forEach(condition -> names.add(condition.name()));
            metric.conditions().forEach(condition -> names.add(condition.name()));
            if (!names.isEmpty()) {
                // A metric queried twice is one entry per name in the response: it carries both lists.
                notApplied.merge(metric.metric(), List.copyOf(names), AnalyticsRequestPipeline::union);
            }
        }
        return notApplied;
    }

    public record MetricConditions(String metric, List<FilterCondition> conditions) {}

    /** The kind of record a metric counts: authorization decisions for the decision metrics, requests otherwise. */
    static RecordType recordTypeOf(String metric) {
        return AUTHZ_DECISION_METRICS.contains(metric) ? RecordType.AUTHZ_DECISION : RecordType.REQUEST;
    }

    private static boolean selects(FilterCondition recordType, RecordType kind) {
        var values = recordType.values() != null ? recordType.values() : List.<String>of();
        var named = values.stream().anyMatch(value -> RecordType.fromNameOrDefault(value) == kind);
        return recordType.operator() == FilterOperator.NOT_IN || recordType.operator() == FilterOperator.NEQ ? !named : named;
    }

    private static List<String> union(List<String> first, List<String> second) {
        var names = new LinkedHashSet<>(first);
        names.addAll(second);
        return List.copyOf(names);
    }

    private static void validateTimeRange(Instant from, Instant to) {
        if (from == null) {
            throw InvalidObservabilityQueryException.missingTimeRangeBound("from");
        }
        if (to == null) {
            throw InvalidObservabilityQueryException.missingTimeRangeBound("to");
        }
        if (from.isAfter(to)) {
            throw InvalidObservabilityQueryException.invalidTimeRange();
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

    private static List<FilterCondition> removeRecordTypeConditions(List<FilterCondition> conditions) {
        return conditions
            .stream()
            .filter(c -> !RECORD_TYPE.equals(c.name()))
            .toList();
    }
}
