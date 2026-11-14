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
package io.gravitee.gamma.rest.core.guardian.model;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotEnabledException;
import io.gravitee.gamma.rest.core.guardian.exception.InvalidGuardianOrderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Guardians an API runs: the {@code guardian-policy} steps of its internal flow, request ones before response
 * ones, each phase in execution order.
 *
 * <p>The internal flow belongs to the platform, not to Guardians alone: any other step it holds is left where it is.
 *
 * @author GraviteeSource Team
 */
public final class ApiGuardians {

    private final Flow internalFlow;

    private ApiGuardians(Flow internalFlow) {
        this.internalFlow = internalFlow == null ? Flow.builder().build() : internalFlow;
    }

    public static ApiGuardians of(Flow internalFlow) {
        return new ApiGuardians(internalFlow);
    }

    public Flow internalFlow() {
        return internalFlow;
    }

    public boolean isEnabled(String guardianId) {
        return list()
            .stream()
            .anyMatch(enabled -> enabled.guardianId().equals(guardianId));
    }

    /** The Guardians with this one enabled after those of its phase. */
    public ApiGuardians enable(String guardianId, String name, Phase phase) {
        var steps = new ArrayList<>(steps(phase));
        steps.add(GuardianSteps.of(guardianId, name));
        return withSteps(phase, steps);
    }

    /**
     * The Guardians with those of a phase run in this order. The other steps of the phase keep their position: the
     * Guardians swap the places they already held.
     *
     * @throws InvalidGuardianOrderException unless the order lists each Guardian of the phase exactly once.
     */
    public ApiGuardians reorder(Phase phase, List<String> guardianIds) {
        var steps = steps(phase);
        var enabled = guardianIdsOf(steps);
        if (guardianIds.size() != enabled.size() || !Set.copyOf(guardianIds).equals(Set.copyOf(enabled))) {
            throw new InvalidGuardianOrderException(phase, enabled);
        }
        var byGuardian = steps
            .stream()
            .filter(step -> GuardianSteps.guardianIdOf(step).isPresent())
            .collect(Collectors.toMap(step -> GuardianSteps.guardianIdOf(step).orElseThrow(), Function.identity()));
        var next = guardianIds.iterator();
        var reordered = steps
            .stream()
            .map(step -> GuardianSteps.guardianIdOf(step).isPresent() ? byGuardian.get(next.next()) : step)
            .toList();
        return withSteps(phase, reordered);
    }

    /**
     * The Guardians without this one.
     *
     * @throws GuardianNotEnabledException when the API does not run it.
     */
    public ApiGuardians disable(String guardianId) {
        if (!isEnabled(guardianId)) {
            throw new GuardianNotEnabledException(guardianId);
        }
        var others = new ApiGuardians(internalFlow);
        for (var phase : Phase.values()) {
            others = others.withSteps(
                phase,
                others
                    .steps(phase)
                    .stream()
                    .filter(step -> GuardianSteps.guardianIdOf(step).filter(guardianId::equals).isEmpty())
                    .toList()
            );
        }
        return others;
    }

    public List<Enabled> list() {
        var enabled = new ArrayList<Enabled>();
        enabled.addAll(list(Phase.REQUEST));
        enabled.addAll(list(Phase.RESPONSE));
        return enabled;
    }

    private List<Enabled> list(Phase phase) {
        var guardianIds = guardianIdsOf(steps(phase));
        var enabled = new ArrayList<Enabled>();
        for (int i = 0; i < guardianIds.size(); i++) {
            enabled.add(new Enabled(guardianIds.get(i), phase, i + 1));
        }
        return enabled;
    }

    private static List<String> guardianIdsOf(List<Step> steps) {
        return steps.stream().map(GuardianSteps::guardianIdOf).flatMap(Optional::stream).toList();
    }

    private ApiGuardians withSteps(Phase phase, List<Step> steps) {
        return new ApiGuardians(
            phase == Phase.REQUEST ? internalFlow.toBuilder().request(steps).build() : internalFlow.toBuilder().response(steps).build()
        );
    }

    private List<Step> steps(Phase phase) {
        var steps = phase == Phase.REQUEST ? internalFlow.getRequest() : internalFlow.getResponse();
        return steps == null ? List.of() : steps;
    }

    /** @param order the 1-based position of the Guardian among those of its phase. */
    public record Enabled(String guardianId, Phase phase, int order) {}
}
