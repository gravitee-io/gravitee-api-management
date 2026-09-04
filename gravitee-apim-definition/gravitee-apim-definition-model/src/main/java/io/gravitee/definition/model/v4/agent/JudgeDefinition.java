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
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.agent.definition.AgentModel;
import io.gravitee.definition.model.v4.agent.definition.JudgeScore;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The body of a {@code kind:judge} agent — one that reads some material against criteria it is handed and answers
 * with a score and, by default, why.
 *
 * <p>A judge is its own kind rather than a standalone agent with a flag, because the two have different bodies. A
 * judge has no {@code role} (the runtime's fixed framing is the role), no {@code tools} or {@code skills} (it reads
 * what it is handed and nothing else), no {@code workingMemory} (every call is its own conversation — a judge that
 * remembers its last verdict is a bug) and no hand-written {@code inputs}/{@code outputs}: the contract is what the
 * kind is for. What it does say is the scale, whether to explain, and which of the subject's material it is shown.</p>
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
public class JudgeDefinition {

    /** One call scores one turn of a conversation: one request in, one answer out. The default. */
    public static final String SUBJECT_TURN = "turn";
    /** One call scores the ordered steps a run took. Reserved — named so adding it later is a value, not a field. */
    public static final String SUBJECT_TRAJECTORY = "trajectory";

    /** The model the judge scores with. Same shape and same default as a standalone agent's. */
    private AgentModel model;

    /**
     * What one call scores: {@link #SUBJECT_TURN} (absent ⇒ this) or {@link #SUBJECT_TRAJECTORY}.
     *
     * <p>The subject fixes the vocabulary {@link #getVariables()} draws from. A runtime refuses at deploy a subject
     * it does not honour, exactly as it refuses a score type it does not know.</p>
     */
    private String subject;

    /**
     * The author's own instructions, appended after the runtime's fixed framing and the scale. Persona, house rules
     * and rubric detail go here. There is no {@code role}: the framing is the role, and two roles is a judge told
     * twice, in slightly different words, what it is.
     */
    private String instructions;

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
     * Absent ⇒ the subject's default; for a {@link #SUBJECT_TURN} judge, {@code ["input", "output"]}.
     *
     * <p>Drawn from the subject's fixed vocabulary so the names mean the same thing for every judge — for a turn:
     * {@code input}, {@code output}, {@code expected_output}, {@code context}, {@code tool_calls}, {@code metadata}.
     * They are the shape a recorded run exposes, chosen that way so whatever selects and feeds runs to a judge can
     * map one to the other without renaming anything.</p>
     */
    private List<String> variables;

    /** The capability plugins this judge references — its model, and nothing else. */
    public List<Plugin> collectPlugins() {
        return model != null && model.getType() != null ? List.of(new Plugin("model", model.getType())) : List.of();
    }
}
