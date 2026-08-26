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
package io.gravitee.definition.model.v4.agent.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Declares that a standalone agent's job is to <b>judge</b> — to read some material against criteria it is given and
 * answer with a score and, by default, why.
 *
 * <p>Nothing here is a new kind of agent. The block is sugar: at deploy it is compiled into the {@code inputs} and
 * {@code outputs} the agent would otherwise have to spell out by hand, and from that point on a judge runs on exactly
 * the same graph, contract and guardrail as any other standalone agent. What it buys is that the scale is declared in
 * one place instead of retyped as an output triple per definition, that the prompt scaffolding — impartial framing,
 * what the scale means, where the material is — is written once rather than left to each author's prose, and that
 * something pointing at "a judge" by id can rely on the answer's shape.</p>
 *
 * <p>The criteria are deliberately <b>not</b> here. They arrive as an input on every call, so one deployed judge
 * serves every set of criteria anyone wants to score against; a judge that baked its criteria in would need
 * redeploying to ask a different question.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentJudge {

    /** The scale this judge scores on. Absent ⇒ a numeric score between 0 and 1. */
    private JudgeScore score;

    /**
     * Whether the judge must also say why. Absent ⇒ {@code true}.
     *
     * <p>Left on by default for two reasons. A score with no reasoning is unauditable — the whole point of reading an
     * evaluation is to disagree with it. And the two-field answer is the <i>constrained</i> one: two declared outputs
     * put the model under a JSON schema and an output guardrail, where a lone score falls back to a looser path that
     * asks for a bare value and, for a categorical scale, checks nothing at all. Turning this off buys a slightly
     * cheaper call and gives up both.</p>
     */
    private Boolean explanation;

    /**
     * What the judge is given to look at, beside the criteria — the names it reads and its caller must supply.
     * Absent ⇒ {@code ["input", "output"]}, the pair that covers judging one answer to one question.
     *
     * <p>Drawn from a fixed vocabulary so the names mean the same thing for every judge: {@code input},
     * {@code output}, {@code expected_output}, {@code context}, {@code tool_calls}, {@code metadata}. They are the
     * shape a recorded run exposes, chosen that way so whatever comes to select and feed runs to a judge later can
     * map one to the other without renaming anything.</p>
     */
    private List<String> variables;
}
