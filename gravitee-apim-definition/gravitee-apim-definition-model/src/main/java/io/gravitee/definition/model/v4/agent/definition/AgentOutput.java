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
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A named value an agent produces into the shared scope — the outbound half of the contract {@link AgentInput}
 * describes inbound.
 *
 * <p>The list replaces the single {@code output} key that came before it, so there is one concept rather than a
 * field beside a list that can disagree. What the list <i>means</i> follows from its length, mirroring the inbound
 * rule that a lone input takes the whole message and two or more must be named:</p>
 *
 * <ul>
 *   <li><b>one, untyped</b> — the agent's answer lands under that key as text, which is what every definition
 *       written before this field existed did;</li>
 *   <li><b>one, typed</b> — the answer is parsed to the declared type first, so a scope key holds a real number
 *       and a workflow guard compares numbers instead of parsing whatever prose the model wrote;</li>
 *   <li><b>two or more</b> — the model is held to a JSON object and each declared name lands as its own scope key,
 *       so a downstream step reads a field rather than being told how to pick one out of an answer.</li>
 * </ul>
 *
 * <p>Honoured where an agent <i>produces</i>: a standalone definition, whether it runs directly or as a workflow
 * leaf. On a workflow <b>root</b> the outputs are a projection instead — the names are read from the finished scope
 * and rendered for the caller, with no model asked to produce them a second time.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentOutput {

    /** The name the agent knows this value by — what a model is asked to produce, and what a caller sees. */
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    /**
     * The scope key this value is actually written to, when it differs from {@link #name}. Absent ⇒ it is written
     * under its own name, which is the behaviour of every definition written before this field existed.
     *
     * <p>The mirror of {@link AgentInput#getBinding()}, and there for the same reason: an agent keeps its own
     * vocabulary — it is asked for {@code score} and answers {@code score} — while the workflow says where that value
     * lands. Without it, composing two agents that both produce {@code score} means one silently overwriting the
     * other, and the only fix is editing an agent someone else owns.</p>
     *
     * <p>Honoured on workflow <b>leaf</b> items. On the workflow <b>root</b> it reads the other way round, as an
     * input's does: the value is read from the bound scope key and published to the caller under {@link #name}.</p>
     */
    private String binding;

    /**
     * Optional declared type: {@code string} | {@code number} | {@code boolean} | {@code enum}.
     *
     * <p>Same vocabulary as {@link AgentInput#getType()} and {@code Clause.valueType}, and read by the same coercion —
     * a value produced as a number is a value a guard can compare as one. Absent ⇒ the answer is passed through as
     * the model wrote it.</p>
     */
    private String type;

    /** Allowed values, for {@code type: enum}. */
    private List<Object> values;

    /** Bounds for {@code type: number}. Absent ⇒ unbounded on that side. */
    private Double min;

    private Double max;

    /**
     * What this value is, in words.
     *
     * <p>Not decoration: it becomes the description of the field in the JSON schema the model is held to, so it is
     * read by the model rather than only by a person.</p>
     */
    private String description;
}
