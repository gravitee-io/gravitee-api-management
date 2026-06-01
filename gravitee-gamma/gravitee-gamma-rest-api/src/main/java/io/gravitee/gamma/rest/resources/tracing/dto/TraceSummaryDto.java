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

import io.gravitee.gamma.rest.core.tracing.model.Trace;

/**
 * Response shape for one row of the trace list. Mirrors the core {@link Trace} 1:1 — kept as a separate
 * type so the wire format can evolve without changing the core model.
 *
 * <p>{@code startTimeEpochMs} is emitted as a plain JSON number (ms since epoch) so the consumer can
 * pass it directly to {@code new Date(value)} without parsing. The {@code long} field type bypasses
 * Jackson's {@code Instant} handling entirely — the parent rest-api ObjectMapper has
 * {@code WRITE_DATES_AS_TIMESTAMPS} enabled, which would otherwise serialise Instant as a fractional
 * {@code <epoch_seconds>.<nanos>} value that JS code misinterprets as milliseconds.
 *
 * <p>{@code status} is emitted lowercase ({@code "unset" | "ok" | "error"}) to match the
 * {@code @gravitee/gamma-lib-observability} {@code WireTraceSummary.status} contract — the lib renders
 * a status badge per row from this exact vocabulary.
 */
public record TraceSummaryDto(
    String traceId,
    long startTimeEpochMs,
    long durationNanos,
    String rootServiceName,
    String rootOperationName,
    String status,
    int spanCount
) {
    public static TraceSummaryDto from(Trace trace) {
        return new TraceSummaryDto(
            trace.traceId(),
            trace.startTime() != null ? trace.startTime().toEpochMilli() : 0L,
            trace.durationNanos(),
            trace.rootServiceName(),
            trace.rootOperationName(),
            trace.status().name().toLowerCase(),
            trace.spanCount()
        );
    }
}
