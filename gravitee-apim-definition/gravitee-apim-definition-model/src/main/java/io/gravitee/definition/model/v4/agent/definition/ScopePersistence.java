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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * How a {@code kind:workflow} agent persists its <b>agentic scope</b> (the shared blackboard state and agent-invocation
 * history carried across turns) — a <b>reference</b> ({@code ref}) to a store resource (e.g. a Redis-backed store) that
 * provides persistence. It is the workflow counterpart of a standalone agent's {@link WorkingMemory}: same "pick your
 * store by ref" shape, but a distinct type because scope persistence has no chat-memory notions (no message window /
 * summarization) — {@code configuration} is reserved for scope-relevant options (e.g. a future TTL / eviction policy).
 *
 * <p>The store resource is defined and deployed separately (a top-level {@code resources[]} entry); this only declares
 * which store the workflow picks up. Absent {@code scopePersistence} ⇒ the scope stays ephemeral (per-turn), unchanged.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScopePersistence {

    /** Id/name of the store resource this workflow uses to persist its agentic scope. */
    @JsonProperty(required = true)
    @NotBlank
    private String ref;

    /** Scope-persistence settings (reserved; e.g. a TTL / eviction policy). May be {@code null}. */
    private Object configuration;
}
