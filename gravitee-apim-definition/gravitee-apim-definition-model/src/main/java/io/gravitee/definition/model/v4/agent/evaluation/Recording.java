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
package io.gravitee.definition.model.v4.agent.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Keeps a durable record of every run this agent finishes, so it can be evaluated later.
 *
 * <p>Opt-in per agent and declared here rather than switched on in gateway configuration, because it is a
 * data-protection boundary before it is a feature: recording persists what people asked and what the model answered,
 * for as long as the store keeps it. Whoever owns the agent is the one who can say that is acceptable, and the
 * definition is where they say it.</p>
 *
 * <p>An evaluation reads what recording writes. A dataset over an agent that never recorded selects nothing — not an
 * error, just no runs, which is why the runner reports how many it found.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Recording {

    /**
     * The {@code name} of a run-store resource in the agent's own {@code resources}.
     *
     * <p>A store rather than a fixed destination so recording can go somewhere the deployment already trusts, and so
     * an agent whose runs must not leave a given system can be pointed at one that keeps them there.</p>
     */
    private String ref;

    /** Absent ⇒ {@code true}: declaring the block is the opt-in, and this exists to suspend it without deleting it. */
    private Boolean enabled;
}
