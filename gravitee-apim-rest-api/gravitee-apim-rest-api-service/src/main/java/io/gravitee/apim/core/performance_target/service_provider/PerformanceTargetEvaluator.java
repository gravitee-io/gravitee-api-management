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
package io.gravitee.apim.core.performance_target.service_provider;

import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import java.time.Instant;
import java.util.List;

/**
 * Evaluates a target against gateway telemetry. Shared by the scheduler and by evaluate-on-demand so that both produce
 * the same numbers as the analytics dashboards.
 */
public interface PerformanceTargetEvaluator {
    /**
     * Evaluates the target over its window ending at {@code now}. The result is not stored and carries no id: the
     * caller decides whether it becomes the target's latest evaluation.
     */
    PerformanceTargetEvaluation evaluate(PerformanceTarget target, Instant now);

    /**
     * Evaluates many targets over their windows ending at {@code now}, with as few analytics requests as their
     * shapes allow: the number of requests is bounded by the number of distinct environments and windows, not by
     * the number of targets. Results come in the order of the targets; a target that could not be evaluated on its
     * own is left out, so the list may be shorter than the input.
     */
    List<PerformanceTargetEvaluation> evaluateAll(List<PerformanceTarget> targets, Instant now);
}
