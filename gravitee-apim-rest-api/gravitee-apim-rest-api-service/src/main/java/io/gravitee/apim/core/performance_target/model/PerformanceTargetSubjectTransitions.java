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

import java.util.List;

/**
 * Every rule of one subject that changed verdict in one evaluation run, whichever of the subject's targets it belongs
 * to. This is what is published on the event bus under {@link PerformanceTargetTransitionEvent#RULES_CHANGED} for the
 * modules that own subjects core cannot name, an agent for instance: the module resolves {@code reference} and tells
 * the owners it knows of.
 *
 * @param reference the subject's lookup key, as filed by the module that created the targets
 */
public record PerformanceTargetSubjectTransitions(
    String organizationId,
    String environmentId,
    String reference,
    List<PerformanceTargetRuleTransition> transitions
) {
    public PerformanceTargetSubjectTransitions {
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
    }

    public List<PerformanceTargetRuleTransition> of(PerformanceTargetRuleTransition.Kind kind) {
        return transitions
            .stream()
            .filter(transition -> transition.kind() == kind)
            .toList();
    }
}
