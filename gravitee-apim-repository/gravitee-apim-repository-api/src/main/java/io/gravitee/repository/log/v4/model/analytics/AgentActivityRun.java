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
package io.gravitee.repository.log.v4.model.analytics;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * One agent activity "run" — a row in the hub Activity table.
 *
 * <p>When the call-chain carried an {@code X-Gravitee-Conversation-Id} header, all hops with that id
 * are bundled into one run. Without it, each hop is its own run. A run always has at least one hop.
 */
@Data
@Builder
public class AgentActivityRun {

    /** The conversation id that ties hops together; absent when the header was not used. */
    String conversationId;

    /**
     * Shared outcome words the UI maps to a badge — one of {@code done}, {@code done-with-changes},
     * {@code stopped}, {@code handed-off}, {@code waiting}, {@code undecided}, {@code failed}.
     */
    String outcome;

    /** One sentence for the collapsed row, next to the outcome badge. */
    String outcomeSummary;

    /** Who asked — the subject/user on the inbound A2A call. */
    String askedBy;

    /** The metric and decision rows that belong to this run, sorted by time. */
    List<AgentActivityHop> hops;

    /** Earliest hop timestamp, epoch millis. */
    long startedAt;

    /** Latest hop timestamp, epoch millis. */
    long lastEventAt;
}
