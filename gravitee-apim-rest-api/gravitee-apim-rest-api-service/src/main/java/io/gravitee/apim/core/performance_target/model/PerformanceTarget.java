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
package io.gravitee.apim.core.performance_target.model;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.definition.model.v4.ApiType;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * What "good" looks like for a subject: the API ids whose gateway telemetry is evaluated, and the rules it must meet.
 *
 * <p>{@code subject.reference} is an opaque lookup key owned by the module that created the target (an API id, an
 * agent catalog id). Core stores and indexes it and never interprets it.
 */
@Builder(toBuilder = true)
public record PerformanceTarget(
    String id,
    String environmentId,
    Subject subject,
    Duration window,
    Duration interval,
    int minSampleSize,
    List<Rule> rules,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {
    public PerformanceTarget {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * The subject ids a rule applies to: those of the rule's api types, or the whole subject when it names none.
     */
    public List<String> apiIdsFor(Rule rule, Map<String, ApiType> apiTypesById) {
        if (rule.apiTypes().isEmpty()) {
            return subject.apiIds();
        }
        return subject
            .apiIds()
            .stream()
            .filter(apiId -> apiTypesById.get(apiId) != null && rule.apiTypes().contains(apiTypesById.get(apiId)))
            .toList();
    }

    public record Subject(List<String> apiIds, String reference) {
        public Subject {
            apiIds = apiIds == null ? List.of() : List.copyOf(apiIds);
        }
    }

    /**
     * {@code apiTypes} restricts the rule to the subject ids of those types; empty means the whole subject.
     * {@code filters} narrow the measure further using the analytics engine filter vocabulary.
     */
    @Builder(toBuilder = true)
    public record Rule(
        MetricSpec.Name metric,
        MetricSpec.Measure measure,
        Operator operator,
        double threshold,
        Set<ApiType> apiTypes,
        List<Filter> filters
    ) {
        public Rule {
            apiTypes = apiTypes == null ? Set.of() : Set.copyOf(apiTypes);
            filters = filters == null ? List.of() : List.copyOf(filters);
        }

        /**
         * Judges {@code observed} against the threshold. Without a value, or with fewer samples than the target's
         * minimum, the rule is NOT_EVALUABLE and reports no observed value, never a PASS.
         */
        public PerformanceTargetEvaluation.RuleResult evaluate(Double observed, long sampleCount, int minSampleSize) {
            if (observed == null || sampleCount < minSampleSize) {
                return new PerformanceTargetEvaluation.RuleResult(
                    metric,
                    measure,
                    operator,
                    threshold,
                    null,
                    null,
                    sampleCount,
                    PerformanceTargetEvaluation.Status.NOT_EVALUABLE
                );
            }
            var status = operator.holds(observed, threshold)
                ? PerformanceTargetEvaluation.Status.PASS
                : PerformanceTargetEvaluation.Status.BREACH;
            return new PerformanceTargetEvaluation.RuleResult(
                metric,
                measure,
                operator,
                threshold,
                observed,
                PerformanceTargetEvaluation.Deviation.of(observed, threshold),
                sampleCount,
                status
            );
        }
    }

    public enum Operator {
        LT,
        LTE,
        GT,
        GTE;

        public boolean holds(double observed, double threshold) {
            return switch (this) {
                case LT -> observed < threshold;
                case LTE -> observed <= threshold;
                case GT -> observed > threshold;
                case GTE -> observed >= threshold;
            };
        }
    }
}
