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
package io.gravitee.definition.model.v4.agent.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.definition.model.Plugin;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@code human} — a human-in-the-loop gate: renders {@code ask} from the scope ({@code {{key}}}), reads the reply
 * and writes it to {@code outputs} (inherited). It implements {@link Workflow} so a workflow may be a single human gate.
 *
 * <p>{@code channel} chooses which of two gates this is, and they behave differently enough to be worth naming:</p>
 * <ul>
 *   <li><b>in-conversation</b> ({@code channel} absent) — whoever is already talking to the agent is asked, and
 *       answers in place, the way a tool approval does. Bounded by the turn that opened it, so it takes no timeout
 *       and no resume mode;</li>
 *   <li><b>delegated</b> ({@code channel} present, with a {@code ref}) — the ask is pushed out-of-band so that
 *       <em>somebody else</em> decides. The asker may disconnect, so the run is resumable and the wait is bounded.
 *       See {@link HumanChannel} for what that entails.</li>
 * </ul>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HumanItem extends WorkflowItem implements Workflow {

    /** The request rendered from the scope ({@code {{key}}}) and shown to the human. */
    private String ask;

    private HumanChannel channel;

    @Override
    public List<Plugin> collectPlugins() {
        return List.of();
    }
}
