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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Evaluates a target through the analytics engine query services the dashboards use, with the same
 * {@code API IN [...]} filter a single-API dashboard applies, so a breach shows the number the chart shows. Rules
 * sharing a filter set (resolved API ids plus rule filters) are answered by one measures request that also carries
 * the request-count metric of the subject's API family; that count is the sample size every rule of the set is
 * judged on.
 *
 * <p>The engine is called without a caller context on purpose: a target is validated against its environment when
 * it is written, and the scheduler evaluates it with no user at hand.
 *
 * <p>Known limit: MCP and A2A APIs expose HTTP-level metrics only. A JSON-RPC error carried by an HTTP 200, or an
 * A2A task that failed, is invisible to a target until protocol-level analytics exist.
 */
@Service
@RequiredArgsConstructor
public class PerformanceTargetEvaluatorImpl implements PerformanceTargetEvaluator {

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

    @Override
    public PerformanceTargetEvaluation evaluate(PerformanceTarget target, Instant now) {
        var window = new TimeRange(now.minus(target.window()), now);
        var organizationId = environmentCrudService.get(target.environmentId()).getOrganizationId();
        var executionContext = new ExecutionContext(organizationId, target.environmentId());
        var apiTypesById = apiTypesById(target);

        var rules = target.rules();
        var results = new PerformanceTargetEvaluation.RuleResult[rules.size()];
        var ruleIndexesByScope = new LinkedHashMap<Scope, List<Integer>>();
        for (int i = 0; i < rules.size(); i++) {
            var rule = rules.get(i);
            var apiIds = target.apiIdsFor(rule, apiTypesById).stream().filter(apiTypesById::containsKey).toList();
            if (apiIds.isEmpty()) {
                results[i] = rule.evaluate(null, 0, target.minSampleSize());
            } else {
                ruleIndexesByScope.computeIfAbsent(new Scope(apiIds, rule.filters()), scope -> new ArrayList<>()).add(i);
            }
        }

        ruleIndexesByScope.forEach((scope, ruleIndexes) -> {
            var sampleCountMetric = sampleCountMetric(scope, apiTypesById);
            var measures = search(executionContext, window, scope, sampleCountMetric, ruleIndexes.stream().map(rules::get).toList());
            var sampleCount = value(measures, sampleCountMetric.name(), sampleCountMetric.measures().getFirst())
                .map(Math::round)
                .orElse(0L);
            for (var i : ruleIndexes) {
                var rule = rules.get(i);
                results[i] = rule.evaluate(
                    value(measures, rule.metric(), rule.measure()).orElse(null),
                    sampleCount,
                    target.minSampleSize()
                );
            }
        });

        var ruleResults = Arrays.asList(results);
        return PerformanceTargetEvaluation.builder()
            .targetId(target.id())
            .environmentId(target.environmentId())
            .reference(target.subject().reference())
            .status(PerformanceTargetEvaluation.Status.of(ruleResults))
            .rules(ruleResults)
            .windowFrom(window.from())
            .windowTo(window.to())
            .coveredApiIds(List.copyOf(apiTypesById.keySet()))
            .evaluatedAt(now)
            .build();
    }

    /** The typed subject APIs that still exist, in subject order; a deleted or untyped API is not covered. */
    private Map<String, ApiType> apiTypesById(PerformanceTarget target) {
        var apis = apiCrudService
            .findByIds(target.subject().apiIds())
            .stream()
            .filter(api -> api.getType() != null)
            .collect(Collectors.toMap(Api::getId, Api::getType));
        var apiTypesById = new LinkedHashMap<String, ApiType>();
        for (var apiId : target.subject().apiIds()) {
            if (apis.containsKey(apiId)) {
                apiTypesById.put(apiId, apis.get(apiId));
            }
        }
        return apiTypesById;
    }

    private static MetricMeasuresRequest sampleCountMetric(Scope scope, Map<String, ApiType> apiTypesById) {
        var metrics = scope
            .apiIds()
            .stream()
            .map(apiTypesById::get)
            .map(apiType ->
                Objects.requireNonNull(SAMPLE_COUNT_METRICS.get(apiType), () -> "No request-count metric for API type " + apiType)
            )
            .distinct()
            .toList();
        if (metrics.size() != 1) {
            throw new IllegalStateException("A rule cannot span API types counted by different metrics: " + scope.apiIds());
        }
        return metrics.getFirst();
    }

    private MeasuresResponse search(
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
        var metrics = measuresByMetric
            .entrySet()
            .stream()
            .map(entry -> new MetricMeasuresRequest(entry.getKey(), List.copyOf(entry.getValue())))
            .toList();

        var filters = new ArrayList<Filter>();
        filters.add(new Filter(FilterSpec.Name.API, FilterOperator.IN, scope.apiIds()));
        filters.addAll(scope.filters());

        var request = new MeasuresRequest(window, filters, metrics);
        return MeasuresResponse.merge(
            queryContextProvider
                .resolve(request)
                .entrySet()
                .stream()
                .map(entry -> entry.getKey().searchMeasures(executionContext, entry.getValue()))
                .toList()
        );
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
}
