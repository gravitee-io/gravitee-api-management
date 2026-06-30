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
 * The working memory an agent uses — a <b>reference</b> ({@code ref}) to a chat-memory <i>store</i> resource (e.g. a
 * Redis-backed store) that provides persistence, plus per-agent usage {@code configuration} (message-window size,
 * summarization …). The store resource itself is defined and deployed separately; this only declares which store the
 * agent picks up and how its working memory behaves.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkingMemory {

    /** Id/name of the chat-memory store resource this agent uses for persistence. */
    @JsonProperty(required = true)
    @NotBlank
    private String ref;

    /** Per-agent working-memory settings (e.g. {@code messageWindow.max}, {@code summarization}). */
    private Object configuration;
}
