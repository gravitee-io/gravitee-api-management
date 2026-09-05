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
package io.gravitee.apim.infra.performance_target;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.GroupedMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.GroupedMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetEvaluator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Evaluates targets through the analytics engine query services the dashboards use, with the same
 * {@code API IN [...]} filter a single-API dashboard applies, so a breach shows the number the chart shows.
 *
 * <p>One target: rules sharing a scope (resolved API ids plus rule filters) are answered by one measures request
 * that also carries the request-count metric of the subject's API family; that count is the sample size every rule
 * of the scope is judged on.
 *
 * <p>Many targets: those whose subject belongs to the HTTP API family are answered per environment and window by
 * grouped measures requests, one group per distinct scope shared by every rule that reads it, so a thousand targets
 * cost a handful of requests whatever the number of APIs a subject holds. A cheap request-count request comes first
 * and the measures request only covers the scopes with enough samples. A target of another API family is evaluated
 * on its own. Analytics calls from the scheduler and from evaluate-on-demand share a cap on the queries in flight.
 *
 * <p>The engine is called without a caller context on purpose: a target is validated against its environment when
 * it is written, and the scheduler evaluates it with no user at hand.
 *
 * <p>Known limit: MCP and A2A APIs expose HTTP-level metrics only. A JSON-RPC error carried by an HTTP 200, or an
 * A2A task that failed, is invisible to a target until protocol-level analytics exist.
 */
@Service
@CustomLog
public class PerformanceTargetEvaluatorImpl implements PerformanceTargetEvaluator {

    /** Scopes per grouped request, which bounds the number of buckets a single response carries. */
    static final int MAX_GROUPS_PER_REQUEST = 500;

    private static final int DEFAULT_CONCURRENCY = 2;

    private static final MetricMeasuresRequest HTTP_REQUEST_COUNT = new MetricMeasuresRequest(
        MetricSpec.Name.HTTP_REQUESTS,
        List.of(MetricSpec.Measure.COUNT)
    );

    /** The metric counting the samples a rule is judged on, per API type of the subject. */
    private static final Map<ApiType, MetricMeasuresRequest> SAMPLE_COUNT_METRICS = Map.of(
        ApiType.PROXY,
        HTTP_REQUEST_COUNT,
        ApiType.LLM_PROXY,
        HTTP_REQUEST_COUNT,
        ApiType.MCP_PROXY,
        HTTP_REQUEST_COUNT,
        ApiType.A2A_PROXY,
        HTTP_REQUEST_COUNT,
        ApiType.MESSAGE,
        HTTP_REQUEST_COUNT,
        ApiType.NATIVE,
        new MetricMeasuresRequest(MetricSpec.Name.NATIVE_OPERATIONS_RECEIVED, List.of(MetricSpec.Measure.SUM)),
        ApiType.EDGE,
        new MetricMeasuresRequest(MetricSpec.Name.EDGE_DETECTION_COUNT, List.of(MetricSpec.Measure.COUNT)),
        ApiType.AUTHZ,
        new MetricMeasuresRequest(MetricSpec.Name.AUTHZ_OPERATIONS, List.of(MetricSpec.Measure.COUNT))
    );

    /**
     * The API types whose documents share the HTTP analytics indices and can be measured per group; Edge lives
     * behind another entrypoint filter and the other families have engines of their own.
     */
    private static final Set<ApiType> GROUPABLE_API_TYPES = Set.of(
        ApiType.PROXY,
        ApiType.LLM_PROXY,
        ApiType.MCP_PROXY,
        ApiType.A2A_PROXY,
        ApiType.MESSAGE
    );

    private final ApiCrudService apiCrudService;
    private final EnvironmentCrudService environmentCrudService;
    private final AnalyticsQueryContextProvider queryContextProvider;
    private final Semaphore queriesInFlight;

    public PerformanceTargetEvaluatorImpl(
        ApiCrudService apiCrudService,
        EnvironmentCrudService environmentCrudService,
        AnalyticsQueryContextProvider queryContextProvider
    ) {
        this(apiCrudService, environmentCrudService, queryContextProvider, DEFAULT_CONCURRENCY);
    }

    @Autowired
    public PerformanceTargetEvaluatorImpl(
        ApiCrudService apiCrudService,
        EnvironmentCrudService environmentCrudService,
        AnalyticsQueryContextProvider queryContextProvider,
        @Value("${services.performance-targets.concurrency:2}") int concurrency
    ) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("services.performance-targets.concurrency must allow at least 1 query in flight");
        }
        this.apiCrudService = apiCrudService;
        this.environmentCrudService = environmentCrudService;
        this.queryContextProvider = queryContextProvider;
        this.queriesInFlight = new Semaphore(concurrency);
    }

    @Override
    public PerformanceTargetEvaluation evaluate(PerformanceTarget target, Instant now) {
        return evaluate(target, now, apiTypesById(target.subject().apiIds()));
    }

    @Override
    public List<PerformanceTargetEvaluation> evaluateAll(List<PerformanceTarget> targets, Instant now) {
        var apiTypesById = apiTypesById(
            targets
                .stream()
                .flatMap(target -> target.subject().apiIds().stream())
                .distinct()
                .toList()
        );
        var results = new PerformanceTargetEvaluation[targets.size()];
        var batches = new LinkedHashMap<Batch, List<Integer>>();
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get(i);
            if (isBatchable(target, apiTypesById)) {
                batches.computeIfAbsent(new Batch(target.environmentId(), target.window()), batch -> new ArrayList<>()).add(i);
            } else {
                results[i] = evaluateAlone(target, now, apiTypesById);
            }
        }
        batches.forEach((batch, indexes) -> {
            var evaluations = evaluateBatch(batch, indexes.stream().map(targets::get).toList(), now, apiTypesById);
            for (int k = 0; k < indexes.size(); k++) {
                results[indexes.get(k)] = evaluations.get(k);
            }
        });
        return Arrays.stream(results).filter(Objects::nonNull).toList();
    }

    private PerformanceTargetEvaluation evaluateAlone(PerformanceTarget target, Instant now, Map<String, ApiType> apiTypesById) {
        try {
            return evaluate(target, now, apiTypesById);
        } catch (RuntimeException e) {
            log.warn(
                "Performance target [{}] of environment [{}] could not be evaluated, it is retried at its next slot",
                target.id(),
                target.environmentId(),
                e
            );
            return null;
        }
    }

    private PerformanceTargetEvaluation evaluate(PerformanceTarget target, Instant now, Map<String, ApiType> knownApiTypes) {
        var window = new TimeRange(now.minus(target.window()), now);
        var executionContext = executionContext(target.environmentId());
        var apiTypesById = subjectApiTypes(target, knownApiTypes);

        var rules = target.rules();
        var results = new PerformanceTargetEvaluation.RuleResult[rules.size()];
        var ruleIndexesByScope = new LinkedHashMap<Scope, List<Integer>>();
        for (int i = 0; i < rules.size(); i++) {
            var scope = scopeOf(target, rules.get(i), apiTypesById);
            if (scope == null) {
                results[i] = rules.get(i).evaluate(null, 0, target.minSampleSize());
            } else {
                ruleIndexesByScope.computeIfAbsent(scope, s -> new ArrayList<>()).add(i);
            }
        }

        ruleIndexesByScope.forEach((scope, ruleIndexes) -> {
            var sampleCountMetric = sampleCountMetric(scope.apiIds(), apiTypesById);
            var measures = searchMeasures(
                executionContext,
                window,
                scope,
                sampleCountMetric,
                ruleIndexes.stream().map(rules::get).toList()
            );
            var sampleCount = sampleCount(measures, sampleCountMetric);
            for (var i : ruleIndexes) {
                var rule = rules.get(i);
                results[i] = rule.evaluate(
                    value(measures, rule.metric(), rule.measure()).orElse(null),
                    sampleCount,
                    target.minSampleSize()
                );
            }
        });

        return evaluation(target, window, Arrays.asList(results), apiTypesById, now);
    }

    /**
     * A target is batchable when its subject belongs to the HTTP family and every rule metric is served by an engine
     * that computes measures per group.
     */
    private boolean isBatchable(PerformanceTarget target, Map<String, ApiType> knownApiTypes) {
        var apiTypesById = subjectApiTypes(target, knownApiTypes);
        return (
            GROUPABLE_API_TYPES.containsAll(apiTypesById.values()) &&
            target
                .rules()
                .stream()
                .allMatch(rule -> queryContextProvider.resolve(rule.metric()).supportsGroupedMeasures())
        );
    }

    private List<PerformanceTargetEvaluation> evaluateBatch(
        Batch batch,
        List<PerformanceTarget> targets,
        Instant now,
        Map<String, ApiType> knownApiTypes
    ) {
        var window = new TimeRange(now.minus(batch.window()), now);
        var executionContext = executionContext(batch.environmentId());

        var subjects = targets
            .stream()
            .map(target -> subjectApiTypes(target, knownApiTypes))
            .toList();
        // one group per distinct scope, shared by every rule that reads it; null when the rule has no API
        var groupKeys = new LinkedHashMap<Scope, String>();
        var ruleScopes = new ArrayList<List<Scope>>();
        for (int t = 0; t < targets.size(); t++) {
            var target = targets.get(t);
            var scopes = new ArrayList<Scope>();
            for (var rule : target.rules()) {
                var scope = scopeOf(target, rule, subjects.get(t));
                if (scope != null) {
                    groupKeys.computeIfAbsent(scope, s -> "scope-" + groupKeys.size());
                }
                scopes.add(scope);
            }
            ruleScopes.add(scopes);
        }

        var scopesByCountMetric = new LinkedHashMap<MetricMeasuresRequest, List<Scope>>();
        for (var scope : groupKeys.keySet()) {
            scopesByCountMetric.computeIfAbsent(sampleCountMetric(scope.apiIds(), knownApiTypes), metric -> new ArrayList<>()).add(scope);
        }
        var sampleCounts = new HashMap<Scope, Long>();
        scopesByCountMetric.forEach((countMetric, scopes) ->
            searchGroupedMeasures(executionContext, window, scopes, groupKeys, List.of(countMetric)).forEach((scope, measures) ->
                sampleCounts.put(scope, sampleCount(measures, countMetric))
            )
        );

        var measuredScopes = new LinkedHashSet<Scope>();
        var measuresByMetric = new LinkedHashMap<MetricSpec.Name, LinkedHashSet<MetricSpec.Measure>>();
        for (int t = 0; t < targets.size(); t++) {
            var target = targets.get(t);
            for (int i = 0; i < target.rules().size(); i++) {
                var scope = ruleScopes.get(t).get(i);
                if (scope != null && sampleCounts.get(scope) >= target.minSampleSize()) {
                    var rule = target.rules().get(i);
                    measuredScopes.add(scope);
                    measuresByMetric.computeIfAbsent(rule.metric(), metric -> new LinkedHashSet<>()).add(rule.measure());
                }
            }
        }
        var measures = measuredScopes.isEmpty()
            ? Map.<Scope, MeasuresResponse>of()
            : searchGroupedMeasures(executionContext, window, List.copyOf(measuredScopes), groupKeys, metrics(measuresByMetric));

        var evaluations = new ArrayList<PerformanceTargetEvaluation>();
        for (int t = 0; t < targets.size(); t++) {
            var target = targets.get(t);
            var results = new ArrayList<PerformanceTargetEvaluation.RuleResult>();
            for (int i = 0; i < target.rules().size(); i++) {
                var rule = target.rules().get(i);
                var scope = ruleScopes.get(t).get(i);
                if (scope == null) {
                    results.add(rule.evaluate(null, 0, target.minSampleSize()));
                } else {
                    var observed = Optional.ofNullable(measures.get(scope))
                        .flatMap(m -> value(m, rule.metric(), rule.measure()))
                        .orElse(null);
                    results.add(rule.evaluate(observed, sampleCounts.get(scope), target.minSampleSize()));
                }
            }
            evaluations.add(evaluation(target, window, results, subjects.get(t), now));
        }
        return evaluations;
    }

    private static PerformanceTargetEvaluation evaluation(
        PerformanceTarget target,
        TimeRange window,
        List<PerformanceTargetEvaluation.RuleResult> results,
        Map<String, ApiType> apiTypesById,
        Instant now
    ) {
        return PerformanceTargetEvaluation.builder()
            .targetId(target.id())
            .environmentId(target.environmentId())
            .reference(target.subject().reference())
            .status(PerformanceTargetEvaluation.Status.of(results))
            .rules(results)
            .windowFrom(window.from())
            .windowTo(window.to())
            .coveredApiIds(List.copyOf(apiTypesById.keySet()))
            .evaluatedAt(now)
            .build();
    }

    private ExecutionContext executionContext(String environmentId) {
        return new ExecutionContext(environmentCrudService.get(environmentId).getOrganizationId(), environmentId);
    }

    /** The typed APIs that still exist among {@code apiIds}; a deleted or untyped API is not covered. */
    private Map<String, ApiType> apiTypesById(List<String> apiIds) {
        if (apiIds.isEmpty()) {
            return Map.of();
        }
        return apiCrudService
            .findByIds(apiIds)
            .stream()
            .filter(api -> api.getType() != null)
            .collect(Collectors.toMap(Api::getId, Api::getType, (first, second) -> first, LinkedHashMap::new));
    }

    /** The subject APIs that are known, in subject order. */
    private static Map<String, ApiType> subjectApiTypes(PerformanceTarget target, Map<String, ApiType> knownApiTypes) {
        var apiTypesById = new LinkedHashMap<String, ApiType>();
        for (var apiId : target.subject().apiIds()) {
            if (knownApiTypes.containsKey(apiId)) {
                apiTypesById.put(apiId, knownApiTypes.get(apiId));
            }
        }
        return apiTypesById;
    }

    /** The documents a rule is measured on, or {@code null} when none of its API types is in the subject. */
    private static Scope scopeOf(PerformanceTarget target, PerformanceTarget.Rule rule, Map<String, ApiType> apiTypesById) {
        var apiIds = target.apiIdsFor(rule, apiTypesById).stream().filter(apiTypesById::containsKey).toList();
        return apiIds.isEmpty() ? null : new Scope(apiIds, rule.filters());
    }

    private static MetricMeasuresRequest sampleCountMetric(List<String> apiIds, Map<String, ApiType> apiTypesById) {
        var metrics = apiIds
            .stream()
            .map(apiTypesById::get)
            .map(apiType ->
                Objects.requireNonNull(SAMPLE_COUNT_METRICS.get(apiType), () -> "No request-count metric for API type " + apiType)
            )
            .distinct()
            .toList();
        if (metrics.size() != 1) {
            throw new IllegalStateException("A rule cannot span API types counted by different metrics: " + apiIds);
        }
        return metrics.getFirst();
    }

    private static long sampleCount(MeasuresResponse measures, MetricMeasuresRequest sampleCountMetric) {
        return value(measures, sampleCountMetric.name(), sampleCountMetric.measures().getFirst()).map(Math::round).orElse(0L);
    }

    private static List<MetricMeasuresRequest> metrics(Map<MetricSpec.Name, LinkedHashSet<MetricSpec.Measure>> measuresByMetric) {
        return measuresByMetric
            .entrySet()
            .stream()
            .map(entry -> new MetricMeasuresRequest(entry.getKey(), List.copyOf(entry.getValue())))
            .toList();
    }

    private MeasuresResponse searchMeasures(
        ExecutionContext executionContext,
        TimeRange window,
        Scope scope,
        MetricMeasuresRequest sampleCountMetric,
        List<PerformanceTarget.Rule> rules
    ) {
        var measuresByMetric = new LinkedHashMap<MetricSpec.Name, LinkedHashSet<MetricSpec.Measure>>();
        measuresByMetric.put(sampleCountMetric.name(), new LinkedHashSet<>(sampleCountMetric.measures()));
        for (var rule : rules) {
            measuresByMetric.computeIfAbsent(rule.metric(), metric -> new LinkedHashSet<>()).add(rule.measure());
        }

        var request = new MeasuresRequest(window, scope.filters(), metrics(measuresByMetric));
        return MeasuresResponse.merge(
            queryContextProvider
                .resolve(request)
                .entrySet()
                .stream()
                .map(entry -> withPermit(() -> entry.getKey().searchMeasures(executionContext, entry.getValue())))
                .toList()
        );
    }

    /**
     * The measures of every scope, {@value #MAX_GROUPS_PER_REQUEST} scopes per request. The request is pre-filtered
     * on the union of the scopes' API ids so a group only has to narrow it down.
     */
    private Map<Scope, MeasuresResponse> searchGroupedMeasures(
        ExecutionContext executionContext,
        TimeRange window,
        List<Scope> scopes,
        Map<Scope, String> groupKeys,
        List<MetricMeasuresRequest> metrics
    ) {
        var byScope = new HashMap<Scope, MeasuresResponse>();
        for (int from = 0; from < scopes.size(); from += MAX_GROUPS_PER_REQUEST) {
            var chunk = scopes.subList(from, Math.min(from + MAX_GROUPS_PER_REQUEST, scopes.size()));
            var groups = new LinkedHashMap<String, List<Filter>>();
            var apiIds = new LinkedHashSet<String>();
            for (var scope : chunk) {
                groups.put(groupKeys.get(scope), scope.filters());
                apiIds.addAll(scope.apiIds());
            }
            var request = new GroupedMeasuresRequest(window, List.of(apiIn(List.copyOf(apiIds))), metrics, groups);
            var response = GroupedMeasuresResponse.merge(
                queryContextProvider
                    .resolve(request)
                    .entrySet()
                    .stream()
                    .map(entry -> withPermit(() -> entry.getKey().searchGroupedMeasures(executionContext, entry.getValue())))
                    .toList()
            );
            for (var scope : chunk) {
                byScope.put(scope, response.groups().getOrDefault(groupKeys.get(scope), new MeasuresResponse(List.of())));
            }
        }
        return byScope;
    }

    private <T> T withPermit(Supplier<T> query) {
        try {
            queriesInFlight.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an analytics query slot", e);
        }
        try {
            return query.get();
        } finally {
            queriesInFlight.release();
        }
    }

    private static Filter apiIn(List<String> apiIds) {
        return new Filter(FilterSpec.Name.API, FilterOperator.IN, apiIds);
    }

    private static Optional<Double> value(MeasuresResponse response, MetricSpec.Name metric, MetricSpec.Measure measure) {
        return response
            .metrics()
            .stream()
            .filter(metricResponse -> metricResponse.name() == metric)
            .flatMap(metricResponse -> metricResponse.measures().stream())
            .filter(measureResponse -> measureResponse.name() == measure)
            .map(Measure::value)
            .filter(Objects::nonNull)
            .map(Number::doubleValue)
            .findFirst();
    }

    /** The documents a set of rules is measured on: the API ids they resolve to, narrowed by their own filters. */
    private record Scope(List<String> apiIds, List<Filter> ruleFilters) {
        /** The API filter first, then the rule filters, the order the single-target requests have always used. */
        List<Filter> filters() {
            var filters = new ArrayList<Filter>();
            filters.add(apiIn(apiIds));
            filters.addAll(ruleFilters);
            return filters;
        }
    }

    /** Targets answered by the same grouped requests: same environment, hence same indices, and same window. */
    private record Batch(String environmentId, Duration window) {}
}
