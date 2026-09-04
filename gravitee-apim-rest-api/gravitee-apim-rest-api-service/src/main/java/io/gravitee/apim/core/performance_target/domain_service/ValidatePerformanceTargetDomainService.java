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

    public void validate(PerformanceTarget target) {
        validateSchedule(target);
        if (target.subject().reference() == null || target.subject().reference().isBlank()) {
            throw new InvalidPerformanceTargetException("A target needs a reference");
        }
        if (target.rules().isEmpty()) {
            throw new InvalidPerformanceTargetException("A target needs at least one rule");
        }

        var subjectApiTypes = subjectApiTypes(target);
        for (int ruleIndex = 0; ruleIndex < target.rules().size(); ruleIndex++) {
            try {
                validateRule(target.rules().get(ruleIndex), subjectApiTypes);
            } catch (InvalidPerformanceTargetException e) {
                throw new InvalidPerformanceTargetException(e.getMessage(), ruleIndex);
            }
        }
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

    private void validateRule(PerformanceTarget.Rule rule, Set<ApiType> subjectApiTypes) {
        for (var apiType : rule.apiTypes()) {
            if (!subjectApiTypes.contains(apiType)) {
                throw new InvalidPerformanceTargetException("API type %s is not part of the subject".formatted(apiType));
            }
        }
        var ruleApiTypes = rule.apiTypes().isEmpty() ? subjectApiTypes : rule.apiTypes();

        var metric = analyticsDefinition
            .findMetric(rule.metric())
            .orElseThrow(() -> new InvalidPerformanceTargetException("Unknown metric " + rule.metric()));
        if (!metric.measures().contains(rule.measure())) {
            throw new InvalidPerformanceTargetException(
                "Measure %s is not available for metric %s".formatted(rule.measure(), metric.name())
            );
        }
        for (var apiType : ruleApiTypes) {
            if (!metric.apis().contains(ANALYTICS_API_NAMES.get(apiType))) {
                throw new InvalidPerformanceTargetException("Metric %s is not available for API type %s".formatted(metric.name(), apiType));
            }
        }
        validateThreshold(rule.threshold(), metric);

        for (var filter : rule.filters()) {
            validateFilter(filter, metric, ruleApiTypes);
        }
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
