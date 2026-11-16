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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import static java.util.stream.Collectors.toSet;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * What an installed policy declares about where it can run: the Guardian phases of its {@code guardian} flow, and the
 * API types of its guardrail flows.
 */
public record PolicyCapabilities(boolean guardrail, DeclaredFlows flows) {
    /** The Guardian phases the policy declares in its {@code guardian} flow. */
    public Set<Phase> guardianPhases() {
        return toGuardianPhases(flows.phases().getOrDefault(PolicyFlowType.GUARDIAN, List.of()));
    }

    /** Whether a Guardian of this phase can run the policy, guardrail or not. */
    public boolean runsInGuardian(Phase phase) {
        return guardianPhases().contains(phase);
    }

    /** Whether the manifest declares a guardrail flow for each of these API types, whatever the phases it lists. */
    public boolean supports(Set<ApiType> apiTypes) {
        return flows
            .phases()
            .keySet()
            .stream()
            .flatMap(flow -> flow.guardrailApiType().stream())
            .collect(toSet())
            .containsAll(apiTypes);
    }

    private static Set<Phase> toGuardianPhases(List<String> flowPhases) {
        return Arrays.stream(Phase.values())
            .filter(phase -> flowPhases.contains(phase.name()))
            .collect(toSet());
    }
}
