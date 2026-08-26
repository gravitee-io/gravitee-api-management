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
package io.gravitee.definition.model.v4.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.definition.model.v4.agent.evaluation.Dataset;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The body of a {@code kind:evaluation} definition — a scheduled or on-demand scoring of an agent's recorded runs.
 *
 * <p>Deployed the same way an agent is, and for the same reasons: it wants a listener so someone can trigger it, a
 * plan so that trigger is protected, resources so its evaluators and its store are declared where everything else is
 * declared, and a deploy lifecycle so editing it takes effect. Making it a {@code kind} rather than a new sort of
 * thing means none of that had to be built twice.</p>
 *
 * <p>What it runs is one loop: resolve the {@link #dataset} to a set of recorded runs, put every run through every
 * evaluator, write what they answer. Both the schedule and the trigger run exactly that, so nothing is only reachable
 * one way.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluationDefinition {

    /** Which recorded runs to score. */
    private Dataset dataset;

    /**
     * How often to run unprompted, as a duration: {@code 1h}, {@code 24h}. Absent ⇒ never — the trigger is the only
     * way in, which is what you want while an evaluator is still being tuned.
     *
     * <p>Note this schedules per gateway node. Two nodes means two evaluations of the same runs, and twice the model
     * spend if a judge is attached.</p>
     */
    private String schedule;

    /**
     * The {@code name} of the resource holding recorded runs and receiving scores — the same store the agent under
     * test records into. Reading and writing through one resource is deliberate: a score is only meaningful beside
     * the run it scored, so the thing that can find the run is the thing that should keep the verdict.
     */
    private String store;

    /**
     * The {@code name}s of the evaluator resources to apply, in order.
     *
     * <p>Named rather than embedded so one evaluator serves several evaluations, and so a check can be corrected in
     * one place. Every evaluator sees every selected run; an evaluator that fails is skipped for that run and the
     * others still record — one broken check must not cost a whole night's scores.</p>
     */
    private List<String> evaluators;
}
