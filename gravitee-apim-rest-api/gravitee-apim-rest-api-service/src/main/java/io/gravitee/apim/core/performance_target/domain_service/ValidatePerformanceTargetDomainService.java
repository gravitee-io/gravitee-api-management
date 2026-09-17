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
package io.gravitee.apim.core.performance_target.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.performance_target.exception.InvalidPerformanceTargetException;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Checks a target against the analytics definition and against the APIs of its subject, so that every rule can be
 * evaluated from gateway telemetry: the metric, measure and filters exist for the API types the rule covers, the
 * threshold fits the metric's unit, and the subject only lists v4 APIs of the target's environment.
 *
 * <p>A rule may name API types the subject does not hold yet. Its metric is still checked against those types, but
 * the subject is not required to contain them: a subject grows and shrinks as its dependencies change, and such a
 * rule is simply NOT_EVALUABLE until an API of that type joins it.
 */
@DomainService
@RequiredArgsConstructor
public class ValidatePerformanceTargetDomainService {

    private static final Map<ApiType, ApiSpec.Name> ANALYTICS_API_NAMES = Map.of(
        ApiType.PROXY,
        ApiSpec.Name.HTTP_PROXY,
        ApiType.LLM_PROXY,
        ApiSpec.Name.LLM,
        ApiType.MCP_PROXY,
        ApiSpec.Name.MCP,
        ApiType.A2A_PROXY,
        ApiSpec.Name.A2A,
        ApiType.MESSAGE,
        ApiSpec.Name.MESSAGE,
        ApiType.NATIVE,
        ApiSpec.Name.NATIVE,
        ApiType.EDGE,
        ApiSpec.Name.EDGE,
        ApiType.AUTHZ,
        ApiSpec.Name.AUTHZ
    );

    private final ApiCrudService apiCrudService;
    private final AnalyticsDefinitionQueryService analyticsDefinition;

    /**
     * @return the target as it will be stored: every rule validated, and an unscoped rule narrowed to the API types
     *     its metric can be read on — see {@link #scopedToTheMetric}.
     */
    public PerformanceTarget validate(PerformanceTarget target) {
        validateSchedule(target);
        if (target.subject().reference() == null || target.subject().reference().isBlank()) {
            throw new InvalidPerformanceTargetException("A target needs a reference");
        }
        if (target.rules().isEmpty()) {
            throw new InvalidPerformanceTargetException("A target needs at least one rule");
        }

        var subjectApiTypes = subjectApiTypes(target);
        var rules = new ArrayList<PerformanceTarget.Rule>(target.rules().size());
        for (int ruleIndex = 0; ruleIndex < target.rules().size(); ruleIndex++) {
            try {
                rules.add(validateRule(target.rules().get(ruleIndex), subjectApiTypes));
            } catch (InvalidPerformanceTargetException e) {
                throw new InvalidPerformanceTargetException(e.getMessage(), ruleIndex);
            }
        }
        return target.toBuilder().rules(rules).build();
    }

    private static void validateSchedule(PerformanceTarget target) {
        if (target.interval() == null || target.interval().compareTo(Duration.ZERO) <= 0) {
            throw new InvalidPerformanceTargetException("The evaluation interval must be positive");
        }
        if (target.window() == null || target.window().compareTo(target.interval()) < 0) {
            throw new InvalidPerformanceTargetException("The window must be at least as long as the evaluation interval");
        }
        if (target.minSampleSize() < 1) {
            throw new InvalidPerformanceTargetException("minSampleSize must be at least 1");
        }
    }

    private Set<ApiType> subjectApiTypes(PerformanceTarget target) {
        var apiIds = target.subject().apiIds();
        var apis = apiCrudService.findByIds(apiIds).stream().collect(Collectors.toMap(Api::getId, Function.identity()));
        var apiTypes = new HashSet<ApiType>();
        for (var apiId : apiIds) {
            var api = apis.get(apiId);
            if (api == null || !target.environmentId().equals(api.getEnvironmentId())) {
                throw new InvalidPerformanceTargetException("API %s does not exist in this environment".formatted(apiId));
            }
            if (api.getDefinitionVersion() != DefinitionVersion.V4 || api.getType() == null) {
                throw new InvalidPerformanceTargetException("API %s must be a v4 API to be evaluated".formatted(apiId));
            }
            apiTypes.add(api.getType());
        }
        return apiTypes;
    }

    private PerformanceTarget.Rule validateRule(PerformanceTarget.Rule rule, Set<ApiType> subjectApiTypes) {
        var metric = analyticsDefinition
            .findMetric(rule.metric())
            .orElseThrow(() -> new InvalidPerformanceTargetException("Unknown metric " + rule.metric()));
        if (!metric.measures().contains(rule.measure())) {
            throw new InvalidPerformanceTargetException(
                "Measure %s is not available for metric %s".formatted(rule.measure(), metric.name())
            );
        }

        var scoped = rule.apiTypes().isEmpty() ? scopedToTheMetric(rule, metric, subjectApiTypes) : rule;
        // A rule scoped to API types the subject does not hold right now is accepted: the metric is checked against
        // the rule's own types, and the evaluator reports it NOT_EVALUABLE until the subject grows an API of that
        // type. A subject changes as its dependencies come and go, so a scope must not pin it.
        for (var apiType : scoped.apiTypes().isEmpty() ? subjectApiTypes : scoped.apiTypes()) {
            if (!metric.apis().contains(ANALYTICS_API_NAMES.get(apiType))) {
                throw new InvalidPerformanceTargetException("Metric %s is not available for API type %s".formatted(metric.name(), apiType));
            }
        }
        validateThreshold(scoped.threshold(), metric);

        for (var filter : scoped.filters()) {
            validateFilter(filter, metric, scoped.apiTypes().isEmpty() ? subjectApiTypes : scoped.apiTypes());
        }
        return scoped;
    }

    /**
     * The scope an unscoped rule gets when its metric cannot be read on every API family the subject holds.
     *
     * <p>Most metrics belong to one family — of the analytics definition's metrics, only a handful are common to every
     * API type — while a subject routinely spans several: an agent holds the proxy fronting it and the LLM and MCP
     * proxies it calls. Reading an unscoped rule as a claim about every API in the subject therefore refused most
     * metrics on most agents, which is not what the author of such a rule means. They mean the APIs that can answer it.
     *
     * <p>The scope taken is the metric's own API types, not the subject's: the subject changes as dependencies come
     * and go, and a rule about conversations should cover an LLM proxy that joins tomorrow. A rule whose metric no API
     * in the subject can answer is refused instead — nothing would ever read it, so it is a mistake rather than a
     * scope. A rule every subject type can answer is left open, so it keeps covering whatever the subject grows into.
     */
    private static PerformanceTarget.Rule scopedToTheMetric(PerformanceTarget.Rule rule, MetricSpec metric, Set<ApiType> subjectApiTypes) {
        if (subjectApiTypes.stream().allMatch(apiType -> metric.apis().contains(ANALYTICS_API_NAMES.get(apiType)))) {
            return rule;
        }
        if (subjectApiTypes.stream().noneMatch(apiType -> metric.apis().contains(ANALYTICS_API_NAMES.get(apiType)))) {
            throw new InvalidPerformanceTargetException(
                "Metric %s can be read on %s, and this subject holds only %s".formatted(
                    metric.name(),
                    metric.apis().stream().map(Enum::name).sorted().collect(Collectors.joining(", ")),
                    subjectApiTypes.stream().map(Enum::name).sorted().collect(Collectors.joining(", "))
                )
            );
        }
        return rule
            .toBuilder()
            .apiTypes(
                ANALYTICS_API_NAMES.entrySet()
                    .stream()
                    .filter(entry -> metric.apis().contains(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet())
            )
            .build();
    }

    private static void validateThreshold(double threshold, MetricSpec metric) {
        boolean fitsUnit = metric.unit() == MetricSpec.Unit.PERCENT ? threshold >= 0 && threshold <= 100 : threshold >= 0;
        if (!fitsUnit) {
            throw new InvalidPerformanceTargetException(
                "Threshold %s is not a valid %s value for metric %s".formatted(threshold, metric.unit(), metric.name())
            );
        }
    }

    private void validateFilter(Filter filter, MetricSpec metric, Set<ApiType> ruleApiTypes) {
        if (filter.value() == null) {
            throw new InvalidPerformanceTargetException("Filter %s has no value".formatted(filter.name()));
        }
        var filterSpec = analyticsDefinition
            .findFilter(filter.name())
            .orElseThrow(() -> new InvalidPerformanceTargetException("Unknown filter " + filter.name()));
        if (!metric.filters().contains(filter.name())) {
            throw new InvalidPerformanceTargetException("Filter %s is not available for metric %s".formatted(filter.name(), metric.name()));
        }
        if (!filterSpec.operators().contains(filter.operator())) {
            throw new InvalidPerformanceTargetException(
                "Operator %s is not supported by filter %s".formatted(filter.operator(), filter.name())
            );
        }
        for (var apiType : ruleApiTypes) {
            if (!filterSpec.apis().contains(ANALYTICS_API_NAMES.get(apiType))) {
                throw new InvalidPerformanceTargetException("Filter %s is not available for API type %s".formatted(filter.name(), apiType));
            }
        }
    }
}
