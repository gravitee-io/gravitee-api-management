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

/**
 * A rule whose verdict changed with the evaluation just stored: what it went to, and the target, rule and result it
 * is about. Transitions are what owners are told about; the evaluations themselves are not.
 *
 * @param evaluation the stored evaluation the transition was detected on
 */
public record PerformanceTargetRuleTransition(
    Kind kind,
    PerformanceTarget target,
    PerformanceTarget.Rule rule,
    PerformanceTargetEvaluation.RuleResult result,
    PerformanceTargetEvaluation evaluation
) {
    public String environmentId() {
        return target.environmentId();
    }

    public String reference() {
        return target.subject().reference();
    }

    public enum Kind {
        /** From PASS, or from a NOT_EVALUABLE state the owners were told about, to BREACH. */
        RULE_MISSED,
        /** From BREACH to PASS. */
        RULE_RECOVERED,
        /** From PASS or BREACH to NOT_EVALUABLE, after the configured number of consecutive not-evaluable windows. */
        RULE_NOT_EVALUABLE,
        /** From a NOT_EVALUABLE state the owners were told about to PASS; back to BREACH reads as RULE_MISSED. */
        RULE_EVALUABLE_AGAIN,
    }
}
