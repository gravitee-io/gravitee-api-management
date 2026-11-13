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

    public List<Enabled> list() {
        var enabled = new ArrayList<Enabled>();
        enabled.addAll(list(Phase.REQUEST));
        enabled.addAll(list(Phase.RESPONSE));
        return enabled;
    }

    private List<Enabled> list(Phase phase) {
        var guardianIds = steps(phase).stream().map(GuardianSteps::guardianIdOf).flatMap(Optional::stream).toList();
        var enabled = new ArrayList<Enabled>();
        for (int i = 0; i < guardianIds.size(); i++) {
            enabled.add(new Enabled(guardianIds.get(i), phase, i + 1));
        }
        return enabled;
    }

    private List<Step> steps(Phase phase) {
        var steps = phase == Phase.REQUEST ? internalFlow.getRequest() : internalFlow.getResponse();
        return steps == null ? List.of() : steps;
    }

    /** @param order the 1-based position of the Guardian among those of its phase. */
    public record Enabled(String guardianId, Phase phase, int order) {}
}
