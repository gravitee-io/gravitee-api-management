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
 * The <b>delegated</b> half of a {@link HumanItem}: the ask is pushed out-of-band, to somebody other than whoever is
 * talking to the agent, through the channel resource named by {@code ref} — a webhook, a Slack channel. {@code ref} is
 * what makes a gate delegated, so it is required; a {@code human} item carrying <em>no</em> channel at all is the other
 * half, the in-conversation gate, where the caller answers in place and nothing here applies.
 *
 * <p>Every field below therefore governs a delegated gate only. An in-conversation gate is bounded by the turn that
 * opened it: there is no pending request to expire and no run to resume, so a channel written without a {@code ref}
 * configures nothing.</p>
 *
 * <p>{@code timeout} is an ISO-8601 duration (e.g. {@code "PT1H"}) after which the pending request expires;
 * {@code onTimeout} is the action then. Only {@code "fail"} (the default) is performed — the expiring run is marked
 * timed out and nothing else happens to it. {@code "reject"} and {@code "continue"} are reserved and <b>not
 * implemented</b>: the value reaches the pending entry and is never acted on, so declaring either one silently gets
 * {@code "fail"}. Do not offer them as choices.</p>
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

    /** The channel resource that carries the ask out-of-band. Required — see the class javadoc. */
    private String ref;

    private String timeout;
    private String onTimeout;

    /** How the run resumes when the answer arrives — {@code "client"} (default) or {@code "server"}. */
    private String resume;
}
