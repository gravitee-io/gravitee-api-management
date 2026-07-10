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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * How a {@code human} item reaches the human: best-effort reuse of the inbound entrypoint when it supports
 * human-in-the-loop, else an explicit channel {@code ref}. {@code async} marks a non-blocking/durable gate.
 *
 * <p>{@code timeout} is an ISO-8601 duration (e.g. {@code "PT1H"}) after which the pending request expires;
 * {@code onTimeout} is the action then — {@code "fail"} (default), {@code "reject"} or {@code "continue"}.</p>
 *
 * <p>{@code resume} chooses how the run continues once the answer arrives:
 * <ul>
 *   <li>{@code "client"} (default) — the gateway stores the answer and marks the run <em>ready</em>; the asker's UI
 *       re-invokes to resume, so the continuation can be fully interactive (further tool approvals, sign-in, …);</li>
 *   <li>{@code "server"} — the gateway resumes the run itself (fire-and-forget); the continuation must be autonomous
 *       (no further interactive steps).</li>
 * </ul></p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HumanChannel {

    private Boolean reuseEntrypoint;
    private String ref;
    private Boolean async;
    private String timeout;
    private String onTimeout;

    /** How the run resumes when the answer arrives — {@code "client"} (default) or {@code "server"}. */
    private String resume;
}
