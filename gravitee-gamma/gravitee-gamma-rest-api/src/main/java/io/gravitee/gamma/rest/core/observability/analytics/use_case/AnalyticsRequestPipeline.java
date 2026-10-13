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
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.RecordType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.EntrypointScopeProvider;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.model.TrafficScope;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Shared pipeline for the three analytics use cases: validates incoming filter conditions against
 * the {@link Signal#ANALYTICS} catalog, loads the caller's readable APIs once, and builds the scope of
 * each engine query using only Gamma-native types. Every condition is ANDed, the same field included:
 * the scope-only conditions ({@code API}, {@code API_TYPE}) each narrow the readable APIs, or for an
 * {@code API_TYPE} whose kind an entrypoint adds to APIs of another type, their calls (see
 * {@link PreparedRequest#scopeWith}), and the others reach the engine one clause each. The entrypoint scope is the engine's: it applies its
 * fail-open default to a query without an {@code ENTRYPOINT} condition and honours an explicit one
 * exactly. All APIM analytics-engine type translation is deferred to the infra adapter.
 *
 * @author GraviteeSource Team
 */
@DomainService
@AllArgsConstructor
public class AnalyticsRequestPipeline {

    private static final String API = "API";
    private static final String API_TYPE = "API_TYPE";
    private static final String RECORD_TYPE = "RECORD_TYPE";
    private static final Set<String> SCOPE_ONLY = Set.of(API, API_TYPE, RECORD_TYPE);

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
    private final EntrypointScopeProvider entrypointScopeProvider;

    /**
     * The scope of one engine query, expressed entirely in Gamma-native types. The infra adapter is
     * responsible for translating {@link #filters()} to the analytics-engine model.
     */
    public record PreparedScope(Instant from, Instant to, List<FilterCondition> filters, Set<String> apiIds, TrafficScope trafficScope) {
        /** A scope the API types alone decide. */
        public PreparedScope(Instant from, Instant to, List<FilterCondition> filters, Set<String> apiIds) {
            this(from, to, filters, apiIds, null);
        }

        /** Nothing the caller can read. Keeps the time range, so the query can still be validated. */
        public static PreparedScope empty(Instant from, Instant to) {
            return new PreparedScope(from, to, List.of(), Set.of());
        }

        public boolean isEmpty() {
            return apiIds.isEmpty();
        }
    }

    /**
     * A validated request and the APIs its caller can read, from which each engine query takes its scope.
     * The request's own conditions are never re-validated once a group's are appended.
     *
     * @param entrypointsAddingKind per API kind, the entrypoints whose calls belong to it whatever the API's type
     */
    public record PreparedRequest(
        Instant from,
        Instant to,
        List<FilterCondition> conditions,
        List<AccessibleApi> readableApis,
        Map<ApiType, Set<String>> entrypointsAddingKind
    ) {
        /**
         * The scope of a query filtered by the request's conditions and the given ones. The API scope is
         * the readable APIs narrowed by every {@code API} condition and by the kinds every {@code API_TYPE}
         * condition allows: disjoint conditions answer the empty scope, never every readable API. When an
         * entrypoint adds one of those kinds to an API of another type, the readable APIs all stay in and the
         * {@link TrafficScope} keeps, beyond the APIs of those types, only their calls through it.
         */
        public PreparedScope scopeWith(List<FilterCondition> groupConditions) {
            var all = new ArrayList<>(conditions);
            all.addAll(groupConditions);

            var candidateIds = readableApis.stream().map(AccessibleApi::id).collect(Collectors.toCollection(HashSet::new));
            Set<String> wantedKinds = null;
            for (var condition : all) {
                if (API.equals(condition.name())) {
                    candidateIds.retainAll(condition.values());
                } else if (API_TYPE.equals(condition.name())) {
                    var kinds = new HashSet<>(condition.values());
                    if (wantedKinds != null) {
                        kinds.retainAll(wantedKinds);
                    }
                    wantedKinds = kinds;
                }
            }
            var candidates = readableApis
                .stream()
                .filter(api -> candidateIds.contains(api.id()))
                .toList();
            var scoped = wantedKinds == null
                ? AccessibleApiScopeDomainService.scopeOf(candidates, ApiType.ALL, Set.of())
                : AccessibleApiScopeDomainService.scopeOf(candidates, kindsNamed(wantedKinds), entrypointsAdding(wantedKinds));
            if (scoped.apiIds().isEmpty()) {
                return PreparedScope.empty(from, to);
            }

            var filters = all
                .stream()
                .filter(condition -> !isScopeOnly(condition))
                .collect(Collectors.toCollection(ArrayList::new));
            filters.add(new FilterCondition(API, FilterOperator.IN, List.copyOf(scoped.apiIds())));
            return new PreparedScope(from, to, List.copyOf(filters), Set.copyOf(scoped.apiIds()), scoped.trafficScope());
        }

        private static Set<ApiType> kindsNamed(Set<String> names) {
            return Arrays.stream(ApiType.values())
                .filter(kind -> names.contains(kind.name()))
                .collect(Collectors.toSet());
        }

        private Set<String> entrypointsAdding(Set<String> kinds) {
            return entrypointsAddingKind
                .entrySet()
                .stream()
                .filter(entry -> kinds.contains(entry.getKey().name()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
        }

        private static boolean isScopeOnly(FilterCondition condition) {
            return SCOPE_ONLY.contains(condition.name());
        }
    }

    public PreparedRequest prepare(
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
        var readable = accessibleApiScope.computeScope(accessibleApis, ApiType.ALL, null).apiIds();
        var readableApis = accessibleApis
            .stream()
            .filter(api -> readable.contains(api.id()))
            .toList();

        var entrypointsAddingKind = Arrays.stream(ApiType.values()).collect(
            Collectors.toMap(kind -> kind, kind -> entrypointScopeProvider.entrypointsAdding(Set.of(kind)))
        );
        return new PreparedRequest(from, to, List.copyOf(conditions), readableApis, entrypointsAddingKind);
    }

    /**
     * Validates each metric's own conditions on their own, never combined with the request's. A
     * {@code RECORD_TYPE} is refused there: a metric already fixes the kind of record it counts.
     */
    public void validateMetricConditions(List<List<FilterCondition>> metricConditions) {
        for (var conditions : metricConditions) {
            filterValidator.validate(conditions, Signal.ANALYTICS);
            if (conditions.stream().anyMatch(condition -> RECORD_TYPE.equals(condition.name()))) {
                throw UnsupportedObservabilityFilterException.recordTypeOnMetric();
            }
        }
    }

    /**
     * The metric-level conditions of a facets or time-series request, which runs as one query: every
     * metric must carry the same set, in the sense of {@link #conditionSet}.
     */
    public List<FilterCondition> sharedMetricConditions(List<List<FilterCondition>> metricConditions) {
        var shared = metricConditions.isEmpty() ? List.<FilterCondition>of() : metricConditions.getFirst();
        var sharedSet = conditionSet(shared);
        if (metricConditions.stream().anyMatch(conditions -> !conditionSet(conditions).equals(sharedSet))) {
            throw InvalidObservabilityQueryException.differingMetricConditions();
        }
        return shared;
    }

    /**
     * The identity of a set of conditions: their order does not matter, nor the order of an {@code IN}'s
     * values, since every condition is ANDed and an {@code IN} reads as a set.
     */
    public static Set<FilterCondition> conditionSet(List<FilterCondition> conditions) {
        return conditions
            .stream()
            .map(condition -> new FilterCondition(condition.name(), condition.operator(), condition.values().stream().sorted().toList()))
            .collect(Collectors.toSet());
    }

    /**
     * Per metric name, the {@code RECORD_TYPE} conditions that do not select the metric's kind of record:
     * {@link PreparedRequest#scopeWith} strips them whatever their value, so the response must name them as
     * not applied. Metrics with nothing to report are left out.
     */
    public Map<String, List<String>> conditionsNotApplied(List<FilterCondition> rawFilters, List<String> metrics) {
        var notApplied = new LinkedHashMap<String, List<String>>();
        for (var metric : metrics) {
            var names = recordTypesNotApplied(rawFilters, metric);
            if (!names.isEmpty()) {
                notApplied.merge(metric, names, AnalyticsRequestPipeline::union);
            }
        }
        return notApplied;
    }

    /** The names of the {@code RECORD_TYPE} conditions that do not select the given metric's kind of record. */
    public List<String> recordTypesNotApplied(List<FilterCondition> rawFilters, String metric) {
        return (rawFilters != null ? rawFilters : List.<FilterCondition>of()).stream()
            .filter(condition -> RECORD_TYPE.equals(condition.name()))
            .filter(condition -> !selects(condition, recordTypeOf(metric)))
            .map(FilterCondition::name)
            .distinct()
            .toList();
    }

    /** The kind of record a metric counts: authorization decisions for the decision metrics, requests otherwise. */
    static RecordType recordTypeOf(String metric) {
        return metric != null && AUTHZ_DECISION_METRICS.contains(metric) ? RecordType.AUTHZ_DECISION : RecordType.REQUEST;
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
}
