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
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetNotificationPolicy;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tells, from a target's stored evaluations, which of its rules changed verdict with the evaluation just stored. The
 * state a rule is compared against is read off the store, never off node memory, so several nodes and restarts
 * report each change once: whichever node stores an evaluation reads the same history.
 *
 * <p>A rule is compared to its last evaluable verdict, PASS or BREACH. Windows in which it could not be evaluated
 * are pending until there are {@code notEvaluableAfter} of them in a row, at which point the owners are told once
 * and the rule is in a reported not-evaluable state until it can be evaluated again. A rule never evaluated so far
 * has nothing to be compared against: its first verdict is news of nothing.
 */
@DomainService
public class PerformanceTargetTransitionDomainService {

    /**
     * @param history the target's stored evaluations before {@code current}, most recent first; when one of them
     *                holds a not-evaluable rule it must be at least {@code policy.notEvaluableAfter()} deep, else the
     *                latest one alone decides
     */
    public List<PerformanceTargetRuleTransition> detect(
        PerformanceTarget target,
        PerformanceTargetEvaluation current,
        List<PerformanceTargetEvaluation> history,
        PerformanceTargetNotificationPolicy policy
    ) {
        var transitions = new ArrayList<PerformanceTargetRuleTransition>();
        for (var rule : target.rules()) {
            var result = rule.id() == null ? null : resultOf(current, rule.id());
            if (result == null) {
                continue;
            }
            var previous = history
                .stream()
                .map(evaluation -> resultOf(evaluation, rule.id()))
                .filter(Objects::nonNull)
                .map(r -> r.status())
                .toList();
            transition(result.status(), previous, policy.notEvaluableAfter()).ifPresent(kind ->
                transitions.add(new PerformanceTargetRuleTransition(kind, target, rule, result, current))
            );
        }
        return transitions;
    }

    /**
     * @param previous the rule's earlier verdicts, most recent first
     */
    static Optional<Kind> transition(Status now, List<Status> previous, int notEvaluableAfter) {
        var pending = 0;
        while (pending < previous.size() && previous.get(pending) == Status.NOT_EVALUABLE) {
            pending++;
        }
        if (pending >= notEvaluableAfter) {
            return switch (now) {
                case PASS -> Optional.of(Kind.RULE_EVALUABLE_AGAIN);
                case BREACH -> Optional.of(Kind.RULE_MISSED);
                case NOT_EVALUABLE -> Optional.empty();
            };
        }
        if (pending == previous.size()) {
            return Optional.empty();
        }
        var baseline = previous.get(pending);
        return switch (now) {
            case NOT_EVALUABLE -> pending + 1 >= notEvaluableAfter ? Optional.of(Kind.RULE_NOT_EVALUABLE) : Optional.empty();
            case BREACH -> baseline == Status.PASS ? Optional.of(Kind.RULE_MISSED) : Optional.empty();
            case PASS -> baseline == Status.BREACH ? Optional.of(Kind.RULE_RECOVERED) : Optional.empty();
        };
    }

    private static PerformanceTargetEvaluation.RuleResult resultOf(PerformanceTargetEvaluation evaluation, String ruleId) {
        return evaluation
            .rules()
            .stream()
            .filter(result -> ruleId.equals(result.id()))
            .findFirst()
            .orElse(null);
    }
}
