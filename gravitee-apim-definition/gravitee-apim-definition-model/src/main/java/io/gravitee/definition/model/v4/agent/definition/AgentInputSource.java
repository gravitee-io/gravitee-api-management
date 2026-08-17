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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Where a declared input takes its value from when the agent is called — its <em>external</em> half.
 *
 * <p>Distinct from {@link AgentInput#getBinding()} on purpose. {@code binding} answers "which scope key does this
 * input read while the workflow runs", which is the workflow author's business; a source answers "where does the value
 * come from when someone calls this agent", which is the caller's. Conflating them would mean a workflow could not
 * accept its own inputs without every sub-agent agreeing on how they arrive — the thing this whole contract exists to
 * avoid.</p>
 *
 * <p>Absent ⇒ the runtime decides: a lone declared input takes the request's query, which is how every definition
 * written before this field behaved.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentInputSource {

    /** The request's own message, verbatim. The default for a single declared input. */
    public static final String KIND_QUERY = "query";
    /** A value the agent's author fixes; the caller cannot override it. */
    public static final String KIND_VALUE = "value";
    /** An attribute of the request, once parsed as a JSON object. */
    public static final String KIND_ATTRIBUTE = "attribute";

    /** {@code query} | {@code value} | {@code attribute}. A kind this runtime does not know leaves the input unset. */
    private String kind;

    /** The fixed value, for {@link #KIND_VALUE}. Typed as the caller's JSON would be: text, number or boolean. */
    private Object value;

    /**
     * The attribute to read, for {@link #KIND_ATTRIBUTE}. Blank ⇒ the input's own name, so a caller whose JSON already
     * uses the agent's vocabulary declares nothing here.
     */
    private String attribute;
}
