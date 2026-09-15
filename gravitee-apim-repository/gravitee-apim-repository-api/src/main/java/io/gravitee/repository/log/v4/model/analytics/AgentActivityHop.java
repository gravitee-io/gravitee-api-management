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
 * One hop inside a run — a single gateway call (A2A inbound, LLM model call, or MCP tool call) plus
 * the decisions (authz, HITL, guardian) that were recorded with the same {@code request-id}.
 */
@Data
@Builder
public class AgentActivityHop {

    /** Gateway request id — joins this hop to its decisions. */
    String requestId;

    /** {@code inbound}, {@code llm-call}, or {@code mcp-call}. Derived from the entrypoint. */
    String kind;

    /** Epoch millis of this hop. */
    long timestamp;

    /** Human-readable label — "Asked to settle claim CLM-1234" or "LLM call: gpt-4o". */
    String label;

    /** Extra detail — endpoint URL, model name, tool name. */
    String detail;

    /** HTTP or gateway status code. */
    int status;

    /** Total cost from additional metrics (LLM sent+received, MCP tool cost), centi-units. */
    long cost;

    /** Decisions recorded with this hop's request-id — authz, HITL, guardian. */
    List<AgentActivityDecision> decisions;

    /** Conversation id from the shared metric when present. */
    String conversationId;

    /** Trace id when available (for View technical details). */
    String traceId;

    /** Entrypoint id — for deriving the hop kind. */
    String entrypointId;
}
