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
package io.gravitee.apim.core.tracing.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TraceSpan(
    String traceId,
    String spanId,
    String parentSpanId,
    String operationName,
    String serviceName,
    Instant startTime,
    long durationNanos,
    Map<String, String> attributes,
    List<TraceEvent> events,
    List<TraceSpan> children
) {
    public TraceSpan {
        if (children == null) {
            children = List.of();
        }
        if (attributes == null) {
            attributes = Map.of();
        }
        if (events == null) {
            events = List.of();
        }
    }

    public static TraceSpan of(
        String traceId,
        String spanId,
        String parentSpanId,
        String operationName,
        String serviceName,
        Instant startTime,
        long durationNanos,
        Map<String, String> attributes,
        List<TraceEvent> events
    ) {
        return new TraceSpan(traceId, spanId, parentSpanId, operationName, serviceName, startTime, durationNanos, attributes, events, List.of());
    }
}
