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

/**
 * Query for this agent's activity — hops on {@code v4-metrics} and decisions on {@code decisions},
 * joined by {@code request-id} and grouped by {@code conversation-id} when the header was present.
 */
public record AgentActivityQuery(
    /** A2A proxy API id — inbound calls land against this api. */
    String a2aApiId,
    /** Application ids the agent acts as — outbound LLM / MCP calls carry this application-id. */
    List<String> applicationIds,
    /** Actor id used on the {@code decisions} index; an agent id or a generated identity. */
    String actorId,
    Long from,
    Long to,
    int page,
    int size
) {}
