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
package inmemory;

import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetEvaluator;
import java.time.Instant;
import java.util.List;

/**
 * Gives every rule the configured status, PASS by default with an observed value at half the threshold, so tests can
 * focus on what happens around the evaluation rather than on the numbers.
 */
public class PerformanceTargetEvaluatorInMemory implements PerformanceTargetEvaluator {

    private PerformanceTargetEvaluation.Status status = PerformanceTargetEvaluation.Status.PASS;

    public void status(PerformanceTargetEvaluation.Status status) {
        this.status = status;
    }

    @Override
    public PerformanceTargetEvaluation evaluate(PerformanceTarget target, Instant now) {
        var evaluable = status != PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
        return PerformanceTargetEvaluation.builder()
            .targetId(target.id())
            .environmentId(target.environmentId())
            .reference(target.subject().reference())
            .status(status)
            .rules(
                target
                    .rules()
                    .stream()
                    .map(rule ->
                        new PerformanceTargetEvaluation.RuleResult(
                            rule.metric(),
                            rule.measure(),
                            rule.operator(),
                            rule.threshold(),
                            evaluable ? rule.threshold() / 2 : null,
                            evaluable ? new PerformanceTargetEvaluation.Deviation(-rule.threshold() / 2, -0.5) : null,
                            evaluable ? 42 : 0,
                            status
                        )
                    )
                    .toList()
            )
            .windowFrom(now.minus(target.window()))
            .windowTo(now)
            .coveredApiIds(target.subject().apiIds())
            .evaluatedAt(now)
            .build();
    }

    @Override
    public List<PerformanceTargetEvaluation> evaluateAll(List<PerformanceTarget> targets, Instant now) {
        return targets
            .stream()
            .map(target -> evaluate(target, now))
            .toList();
    }
}
