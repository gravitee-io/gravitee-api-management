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

import io.gravitee.gamma.rest.core.tracing.model.TraceAttributeValue;

/**
 * A distinct span-attribute value with rollups (e.g. one conversation: id + turn count + last activity).
 *
 * <p>{@code firstActivityEpochMs} / {@code lastActivityEpochMs} are emitted as plain JSON numbers (ms since epoch) so
 * the consumer can pass them straight to {@code new Date(value)} — see {@link TraceSummaryDto} for the same convention
 * and the reason a {@code long} is used rather than an {@code Instant} (the parent ObjectMapper's
 * {@code WRITE_DATES_AS_TIMESTAMPS} would otherwise serialise an Instant as fractional {@code <epoch_seconds>.<nanos>},
 * which JS reads as ms → 1970).
 */
public record TraceAttributeValueDto(String value, long traceCount, long firstActivityEpochMs, long lastActivityEpochMs) {
    public static TraceAttributeValueDto from(TraceAttributeValue source) {
        return new TraceAttributeValueDto(
            source.value(),
            source.traceCount(),
            source.firstActivity() != null ? source.firstActivity().toEpochMilli() : 0L,
            source.lastActivity() != null ? source.lastActivity().toEpochMilli() : 0L
        );
    }
}
