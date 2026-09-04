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

import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
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
 * <p>One target: rules sharing a filter set (resolved API ids plus rule filters) are answered by one measures request
 * that also carries the request-count metric of the subject's API family; that count is the sample size every rule
 * of the set is judged on.
 *
 * <p>Many targets: those whose every rule resolves to a single API and carries no filter are answered per
 * environment and window by facets requests grouped by API, one bucket per API, so a thousand targets cost a
 * handful of requests. A cheap request-count query comes first and the aggregation only covers the APIs with enough
 * samples. Any other target is evaluated on its own. Analytics calls from the scheduler and from evaluate-on-demand
 * share a cap on the queries in flight.
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

    /** Targets per facets request, which bounds the number of buckets a single response carries. */
    static final int MAX_TARGETS_PER_REQUEST = 500;

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
            for (int from = 0; from < indexes.size(); from += MAX_TARGETS_PER_REQUEST) {
                var chunk = indexes.subList(from, Math.min(from + MAX_TARGETS_PER_REQUEST, indexes.size()));
                var evaluations = evaluateBatch(batch, chunk.stream().map(targets::get).toList(), now, apiTypesById);
                for (int k = 0; k < chunk.size(); k++) {
                    results[chunk.get(k)] = evaluations.get(k);
                }
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
            var rule = rules.get(i);
            var apiIds = scopeApiIds(target, rule, apiTypesById);
            if (apiIds.isEmpty()) {
                results[i] = rule.evaluate(null, 0, target.minSampleSize());
            } else {
                ruleIndexesByScope.computeIfAbsent(new Scope(apiIds, rule.filters()), scope -> new ArrayList<>()).add(i);
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
     * A target is batchable when every rule can be read from an API bucket: one API, no rule filter, and an API
     * family whose analytics can be grouped by API, which excludes Edge.
     */
    private static boolean isBatchable(PerformanceTarget target, Map<String, ApiType> knownApiTypes) {
        var apiTypesById = subjectApiTypes(target, knownApiTypes);
        for (var rule : target.rules()) {
            if (!rule.filters().isEmpty()) {
                return false;
            }
            var apiIds = scopeApiIds(target, rule, apiTypesById);
            if (apiIds.size() > 1 || (apiIds.size() == 1 && apiTypesById.get(apiIds.getFirst()) == ApiType.EDGE)) {
                return false;
            }
        }
        return true;
    }

    private List<PerformanceTargetEvaluation> evaluateBatch(
        Batch batch,
        List<PerformanceTarget> targets,
        Instant now,
        Map<String, ApiType> knownApiTypes
    ) {
        var window = new TimeRange(now.minus(batch.window()), now);
        var executionContext = executionContext(batch.environmentId());

        var subjects = targets.stream().collect(Collectors.toMap(PerformanceTarget::id, target -> subjectApiTypes(target, knownApiTypes)));
        // the API each rule reads its bucket from, by target then rule position; null when the rule has no API
        var ruleApiIds = new HashMap<String, List<String>>();
        var apiIds = new LinkedHashSet<String>();
        for (var target : targets) {
            var perRule = new ArrayList<String>();
            for (var rule : target.rules()) {
                var scope = scopeApiIds(target, rule, subjects.get(target.id()));
                perRule.add(scope.isEmpty() ? null : scope.getFirst());
                apiIds.addAll(scope);
            }
            ruleApiIds.put(target.id(), perRule);
        }

        var countMetrics = apiIds
            .stream()
            .map(apiId -> SAMPLE_COUNT_METRICS.get(knownApiTypes.get(apiId)))
            .distinct()
            .toList();
        var counts = apiIds.isEmpty()
            ? Map.<String, MeasuresResponse>of()
            : searchFacetsByApi(executionContext, window, List.copyOf(apiIds), countMetrics);
        var sampleCounts = new HashMap<String, Long>();
        for (var apiId : apiIds) {
            var countMetric = SAMPLE_COUNT_METRICS.get(knownApiTypes.get(apiId));
            sampleCounts.put(apiId, counts.containsKey(apiId) ? sampleCount(counts.get(apiId), countMetric) : 0L);
        }

        var measuredApiIds = new LinkedHashSet<String>();
        var measuresByMetric = new LinkedHashMap<MetricSpec.Name, LinkedHashSet<MetricSpec.Measure>>();
        for (var target : targets) {
            var rules = target.rules();
            for (int i = 0; i < rules.size(); i++) {
                var apiId = ruleApiIds.get(target.id()).get(i);
                if (apiId != null && sampleCounts.get(apiId) >= target.minSampleSize()) {
                    measuredApiIds.add(apiId);
                    measuresByMetric.computeIfAbsent(rules.get(i).metric(), metric -> new LinkedHashSet<>()).add(rules.get(i).measure());
                }
            }
        }
        var measures = measuredApiIds.isEmpty()
            ? Map.<String, MeasuresResponse>of()
            : searchFacetsByApi(executionContext, window, List.copyOf(measuredApiIds), metrics(measuresByMetric));

        var evaluations = new ArrayList<PerformanceTargetEvaluation>();
        for (var target : targets) {
            var rules = target.rules();
            var results = new ArrayList<PerformanceTargetEvaluation.RuleResult>();
            for (int i = 0; i < rules.size(); i++) {
                var rule = rules.get(i);
                var apiId = ruleApiIds.get(target.id()).get(i);
                if (apiId == null) {
                    results.add(rule.evaluate(null, 0, target.minSampleSize()));
                } else {
                    var observed = Optional.ofNullable(measures.get(apiId))
                        .flatMap(m -> value(m, rule.metric(), rule.measure()))
                        .orElse(null);
                    results.add(rule.evaluate(observed, sampleCounts.get(apiId), target.minSampleSize()));
                }
            }
            evaluations.add(evaluation(target, window, results, subjects.get(target.id()), now));
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

    private static List<String> scopeApiIds(PerformanceTarget target, PerformanceTarget.Rule rule, Map<String, ApiType> apiTypesById) {
        return target.apiIdsFor(rule, apiTypesById).stream().filter(apiTypesById::containsKey).toList();
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

        var filters = new ArrayList<Filter>();
        filters.add(apiIn(scope.apiIds()));
        filters.addAll(scope.filters());

        var request = new MeasuresRequest(window, filters, metrics(measuresByMetric));
        return MeasuresResponse.merge(
            queryContextProvider
                .resolve(request)
                .entrySet()
                .stream()
                .map(entry -> withPermit(() -> entry.getKey().searchMeasures(executionContext, entry.getValue())))
                .toList()
        );
    }

    /** One facets request grouped by API; the answer is read back as one measures response per API id. */
    private Map<String, MeasuresResponse> searchFacetsByApi(
        ExecutionContext executionContext,
        TimeRange window,
        List<String> apiIds,
        List<MetricMeasuresRequest> metrics
    ) {
        var request = new FacetsRequest(
            window,
            List.of(apiIn(apiIds)),
            metrics
                .stream()
                .map(metric -> new FacetMetricMeasuresRequest(metric.name(), metric.measures(), List.of()))
                .toList(),
            List.of(FacetSpec.Name.API),
            apiIds.size(),
            List.of()
        );
        var response = FacetsResponse.merge(
            queryContextProvider
                .resolve(request)
                .entrySet()
                .stream()
                .map(entry -> withPermit(() -> entry.getKey().searchFacets(executionContext, entry.getValue())))
                .toList()
        );

        var metricsByApi = new HashMap<String, List<MetricMeasuresResponse>>();
        for (var metric : response.metrics()) {
            for (var bucket : metric.buckets()) {
                metricsByApi
                    .computeIfAbsent(bucket.key(), apiId -> new ArrayList<>())
                    .add(new MetricMeasuresResponse(metric.metric(), metric.unit(), bucket.measures()));
            }
        }
        var byApi = new HashMap<String, MeasuresResponse>();
        metricsByApi.forEach((apiId, apiMetrics) -> byApi.put(apiId, new MeasuresResponse(apiMetrics)));
        return byApi;
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
    private record Scope(List<String> apiIds, List<Filter> filters) {}

    /** Targets answered by one facets request: same environment, hence same indices, and same window. */
    private record Batch(String environmentId, Duration window) {}
}
