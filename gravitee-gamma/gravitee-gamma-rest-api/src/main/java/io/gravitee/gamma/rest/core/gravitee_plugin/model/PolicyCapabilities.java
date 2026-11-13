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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What an installed policy declares about where it can run: the flows of its manifest, projected onto the Guardian
 * phases.
 */
public record PolicyCapabilities(boolean guardrail, DeclaredFlows flows) {
    /**
     * The Guardian phases the policy runs in, for each API type a guardrail can be scoped to that its manifest declares.
     * An API type may be declared with no Guardian phase, when the manifest only lists flow phases a Guardian never runs
     * in: those are dropped rather than refused, so one plugin declaring them does not hide the others.
     */
    private Map<ApiType, Set<Phase>> phasesByApiType() {
        return flows
            .phases()
            .entrySet()
            .stream()
            .filter(flow -> flow.getKey().guardrailApiType().isPresent())
            .collect(toMap(flow -> flow.getKey().guardrailApiType().orElseThrow(), flow -> toGuardianPhases(flow.getValue())));
    }

    /** The Guardian phases the policy declares in its {@code guardian} flow. */
    public Set<Phase> guardianPhases() {
        return toGuardianPhases(flows.phases().getOrDefault(PolicyFlowType.GUARDIAN, List.of()));
    }

    /** Whether a Guardian of this phase can run the policy, guardrail or not. */
    public boolean runsInGuardian(Phase phase) {
        return guardianPhases().contains(phase);
    }

    public boolean supports(Set<ApiType> apiTypes) {
        return phasesByApiType().keySet().containsAll(apiTypes);
    }

    public boolean runsIn(Set<ApiType> apiTypes, Phase phase) {
        var phases = phasesByApiType();
        return apiTypes.stream().allMatch(apiType -> phases.getOrDefault(apiType, Set.of()).contains(phase));
    }

    private static Set<Phase> toGuardianPhases(List<String> flowPhases) {
        return Arrays.stream(Phase.values())
            .filter(phase -> flowPhases.contains(phase.name()))
            .collect(toSet());
    }
}
