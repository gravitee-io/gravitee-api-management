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
package io.gravitee.gamma.rest.core.guardrail.model;

import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import java.util.ArrayList;
import java.util.List;

/**
 * The Guardrails an API runs: the {@code guardrail-run} steps of its internal flow, request ones before response
 * ones, each phase in execution order.
 *
 * <p>The internal flow belongs to the platform, not to Guardrails alone: any other step it holds is left where it is.
 *
 * @author GraviteeSource Team
 */
public final class ApiGuardrails {

    private final Flow internalFlow;

    private ApiGuardrails(Flow internalFlow) {
        this.internalFlow = internalFlow == null ? Flow.builder().build() : internalFlow;
    }

    public static ApiGuardrails of(Flow internalFlow) {
        return new ApiGuardrails(internalFlow);
    }

    public Flow internalFlow() {
        return internalFlow;
    }

    /**
     * The Guardrails of a phase, run in this order and no other, each under its condition — those the API ran on that
     * phase and the list leaves out stop running. The Guardrails take the place of the block they already held, the
     * first position of the phase when the API ran none, so the other steps of the internal flow keep their order
     * around them.
     *
     * <p>Once one of them carries a condition, the block runs after the other steps of the phase instead: those set the
     * attributes a condition reads, and a condition evaluated before them reads nothing and holds on no call.
     */
    public ApiGuardrails replace(Phase phase, List<GuardrailRef> guardrails) {
        var steps = steps(phase);
        var others = new ArrayList<Step>();
        var runsAt = -1;
        for (var step : steps) {
            if (GuardrailSteps.guardrailIdOf(step).isPresent()) {
                if (runsAt < 0) {
                    runsAt = others.size();
                }
            } else {
                others.add(step);
            }
        }
        var conditional = guardrails.stream().anyMatch(ref -> ref.condition() != null);
        others.addAll(
            conditional ? others.size() : Math.max(runsAt, 0),
            guardrails
                .stream()
                .map(ref -> GuardrailSteps.of(ref.guardrailId(), ref.name(), ref.condition()))
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

    /** The Guardrails of a phase, in execution order. */
    public List<Enabled> list(Phase phase) {
        var attachments = attachmentsOf(phase);
        var enabled = new ArrayList<Enabled>();
        for (int i = 0; i < attachments.size(); i++) {
            enabled.add(new Enabled(attachments.get(i).guardrailId(), phase, i + 1, attachments.get(i).condition()));
        }
        return enabled;
    }

    /** The ids of the Guardrails of a phase, in execution order. */
    public List<String> idsOf(Phase phase) {
        return attachmentsOf(phase).stream().map(ApiGuardrailAttachment::guardrailId).toList();
    }

    /** The Guardrails of a phase and the condition each runs under, in execution order. */
    public List<ApiGuardrailAttachment> attachmentsOf(Phase phase) {
        return steps(phase)
            .stream()
            .flatMap(step ->
                GuardrailSteps.guardrailIdOf(step)
                    .map(guardrailId -> new ApiGuardrailAttachment(guardrailId, step.getCondition()))
                    .stream()
            )
            .toList();
    }

    private ApiGuardrails withSteps(Phase phase, List<Step> steps) {
        return new ApiGuardrails(
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

    /**
     * @param order the 1-based position of the Guardrail among those of its phase.
     * @param condition what the step runs the Guardrail under, {@code null} on every call.
     */
    public record Enabled(String guardrailId, Phase phase, int order, String condition) {}

    /**
     * A Guardrail to run, named as the revision the gateways run names it.
     *
     * @param condition what its step runs it under, {@code null} on every call.
     */
    public record GuardrailRef(String guardrailId, String name, String condition) {
        /** A Guardrail to run on every call. */
        public GuardrailRef(String guardrailId, String name) {
            this(guardrailId, name, null);
        }
    }
}
