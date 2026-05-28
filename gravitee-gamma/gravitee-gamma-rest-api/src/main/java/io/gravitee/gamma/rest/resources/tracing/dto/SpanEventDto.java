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
package io.gravitee.gamma.rest.resources.tracing.dto;

import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import java.util.Map;

/**
 * Span event in the detail view. {@code spanId} is intentionally omitted from the wire payload — the
 * event is already nested under its parent span in the response shape, so the join key is implicit.
 * See {@link TraceSummaryDto} for the ms-since-epoch timestamp rationale.
 */
public record SpanEventDto(String name, long timestampEpochMs, Map<String, String> attributes) {
    public static SpanEventDto from(SpanEvent event) {
        return new SpanEventDto(event.name(), event.timestamp() != null ? event.timestamp().toEpochMilli() : 0L, event.attributes());
    }
}
