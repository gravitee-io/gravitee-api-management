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

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * The outcome of evaluating one target over one window. The target status is the worst of its rules; a rule with
 * fewer samples than the target's minimum, or with no data at all, is NOT_EVALUABLE rather than PASS.
 */
@Builder(toBuilder = true)
public record PerformanceTargetEvaluation(
    String id,
    String targetId,
    String environmentId,
    String reference,
    Status status,
    List<RuleResult> rules,
    Instant windowFrom,
    Instant windowTo,
    List<String> coveredApiIds,
    Instant evaluatedAt,
    boolean latest
) {
    public PerformanceTargetEvaluation {
        rules = rules == null ? List.of() : List.copyOf(rules);
        coveredApiIds = coveredApiIds == null ? List.of() : List.copyOf(coveredApiIds);
    }

    public enum Status {
        PASS,
        BREACH,
        NOT_EVALUABLE,
    }

    @Builder(toBuilder = true)
    public record RuleResult(
        MetricSpec.Name metric,
        MetricSpec.Measure measure,
        PerformanceTarget.Operator operator,
        double threshold,
        Double observed,
        Deviation deviation,
        long sampleCount,
        Status status
    ) {}

    /**
     * How far {@code observed} is from the threshold: {@code absolute} in the metric's unit, {@code ratio} relative to
     * the threshold.
     */
    public record Deviation(double absolute, double ratio) {}
}
