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

/**
 * Summary view of a trace — what the listing endpoint returns. Carries just enough to render a row
 * (trace id, root span operation / service, duration, status, span count, start time) without paying
 * for the full span tree. The detail endpoint switches to {@link TraceDetail} for the timeline view.
 *
 * <p>{@code status} is the trace-level rollup using the same {@link SpanStatus} vocabulary as per-span
 * status — any span reporting {@link SpanStatus#ERROR} promotes the trace to {@link SpanStatus#ERROR},
 * otherwise the root span's status drives the outcome. {@code spanCount} is the total number of spans
 * recorded for the trace within the search window.
 *
 * @author GraviteeSource Team
 */
public record Trace(
    String traceId,
    Instant startTime,
    long durationNanos,
    String rootServiceName,
    String rootOperationName,
    SpanStatus status,
    int spanCount
) {}
