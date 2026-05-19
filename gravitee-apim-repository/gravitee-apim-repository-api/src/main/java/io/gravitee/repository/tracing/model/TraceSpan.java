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
package io.gravitee.repository.tracing.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single span inside a {@link Trace}. {@code children} is a tree-projection field reserved for callers that want to rebuild
 * the parent/child hierarchy locally — backends return spans flat and leave it empty (see {@link #of}).
 *
 * @author GraviteeSource Team
 */
public record TraceSpan(
    String traceId,
    String spanId,
    String parentSpanId,
    String operationName,
    String serviceName,
    Instant startTime,
    long durationNanos,
    Map<String, String> attributes,
    List<TraceSpanEvent> events,
    List<TraceSpan> children
) {
    public TraceSpan {
        if (children == null) {
            children = new ArrayList<>();
        }
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        if (events == null) {
            events = new ArrayList<>();
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
        List<TraceSpanEvent> events
    ) {
        return new TraceSpan(
            traceId,
            spanId,
            parentSpanId,
            operationName,
            serviceName,
            startTime,
            durationNanos,
            attributes,
            events,
            new ArrayList<>()
        );
    }
}
