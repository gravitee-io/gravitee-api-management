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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /**
     * The Guardians of a phase, run in this order and no other — those the API ran on that phase and the list leaves
     * out stop running. The Guardians take the place of the block they already held, the first position of the phase
     * when the API ran none, so the other steps of the internal flow keep their order around them.
     */
    public ApiGuardians replace(Phase phase, List<GuardianRef> guardians) {
        var steps = steps(phase);
        var others = new ArrayList<Step>();
        var runsAt = -1;
        for (var step : steps) {
            if (GuardianSteps.guardianIdOf(step).isPresent()) {
                if (runsAt < 0) {
                    runsAt = others.size();
                }
            } else {
                others.add(step);
            }
        }
        others.addAll(
            Math.max(runsAt, 0),
            guardians
                .stream()
                .map(ref -> GuardianSteps.of(ref.guardianId(), ref.name()))
                .toList()
        );
        return withSteps(phase, others);
    }

    public List<Enabled> list() {
        var enabled = new ArrayList<Enabled>();
        enabled.addAll(list(Phase.REQUEST));
        enabled.addAll(list(Phase.RESPONSE));
        return enabled;
    }

    /** The Guardians of a phase, in execution order. */
    public List<Enabled> list(Phase phase) {
        var guardianIds = idsOf(phase);
        var enabled = new ArrayList<Enabled>();
        for (int i = 0; i < guardianIds.size(); i++) {
            enabled.add(new Enabled(guardianIds.get(i), phase, i + 1));
        }
        return enabled;
    }

    /** The ids of the Guardians of a phase, in execution order. */
    public List<String> idsOf(Phase phase) {
        return guardianIdsOf(steps(phase));
    }

    private static List<String> guardianIdsOf(List<Step> steps) {
        return steps.stream().map(GuardianSteps::guardianIdOf).flatMap(Optional::stream).toList();
    }

    private ApiGuardians withSteps(Phase phase, List<Step> steps) {
        return new ApiGuardians(
            switch (phase) {
                case REQUEST -> internalFlow.toBuilder().request(steps).build();
                case RESPONSE -> internalFlow.toBuilder().response(steps).build();
            }
        );
    }

    private List<Step> steps(Phase phase) {
        var steps = switch (phase) {
            case REQUEST -> internalFlow.getRequest();
            case RESPONSE -> internalFlow.getResponse();
        };
        return steps == null ? List.of() : steps;
    }

    /** @param order the 1-based position of the Guardian among those of its phase. */
    public record Enabled(String guardianId, Phase phase, int order) {}

    /** A Guardian to run, named as the revision the gateways run names it. */
    public record GuardianRef(String guardianId, String name) {}
}
