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
package io.gravitee.gamma.rest.core.tracing.model;

import java.time.Instant;
import java.util.Map;

/**
 * A distinct value of a span attribute for one API, with rollups — the core model behind grouped views such as the
 * Agent Control Tower "Conversations" list (distinct {@code gen_ai.conversation.id} per API).
 *
 * @param value         the attribute value (e.g. a conversation id)
 * @param traceCount    number of distinct traces (turns) carrying this value in the queried window
 * @param firstActivity earliest span start time observed for this value, for a "started" / start-date column
 * @param lastActivity  latest span start time observed for this value, for ordering / "last activity"
 * @param attributes    top value of each caller-requested correlated span attribute (key → value), e.g.
 *                      {@code gravitee.entrypoint.id} per conversation; empty when none were requested
 */
public record TraceAttributeValue(
    String value,
    long traceCount,
    Instant firstActivity,
    Instant lastActivity,
    Map<String, String> attributes
) {}
