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
 * The scale a {@link AgentJudge} scores on — the one thing that makes two otherwise identical judges different.
 *
 * <p>Declared on the judge rather than travelling with each set of criteria, so the scale is fixed at deploy time.
 * That is what lets the allowed labels reach the model as a real JSON-schema {@code enum} instead of a sentence
 * asking it nicely: the schema is built once, when the graph is built, and a scale that could change per call could
 * not be in it.</p>
 *
 * <p>Its vocabulary is the evaluation one — {@code numeric}/{@code boolean}/{@code categorical} — not the
 * {@code number}/{@code boolean}/{@code enum} of {@link AgentOutput#getType()} that it compiles down to. The two are
 * deliberately kept apart: an author writing a judge is choosing how a verdict is expressed, not what Java type a
 * scope key ends up holding.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JudgeScore {

    /** A continuous score between {@link #getMin()} and {@link #getMax()}. The default when no type is declared. */
    public static final String TYPE_NUMERIC = "numeric";
    /** A verdict with two sides and no middle: {@code true} or {@code false}. */
    public static final String TYPE_BOOLEAN = "boolean";
    /** One of {@link #getLabels()}, chosen verbatim. */
    public static final String TYPE_CATEGORICAL = "categorical";

    /**
     * {@code numeric} | {@code boolean} | {@code categorical}. Absent ⇒ {@link #TYPE_NUMERIC}, which is the scale a
     * judge written without thinking about it wants.
     *
     * <p>A type this runtime does not know is refused at deploy rather than silently scored on something else — a
     * judge that quietly changed scale would corrupt every comparison made against it.</p>
     */
    private String type;

    /** Lower bound for {@link #TYPE_NUMERIC}. Absent ⇒ {@code 0}. */
    private Double min;

    /** Upper bound for {@link #TYPE_NUMERIC}. Absent ⇒ {@code 1}. */
    private Double max;

    /**
     * The allowed verdicts for {@link #TYPE_CATEGORICAL}, in the spelling the judge must answer in.
     *
     * <p>Spelling matters because it is what downstream reads: the coercion matches a model's answer
     * case-insensitively but hands back the declared form, so {@code PASS} and {@code pass} both land in the scope as
     * whatever is written here. Required for a categorical scale — a category with no categories is not a scale.</p>
     */
    private List<String> labels;
}
