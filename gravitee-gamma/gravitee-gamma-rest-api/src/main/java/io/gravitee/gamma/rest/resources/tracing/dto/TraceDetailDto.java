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

import io.gravitee.gamma.rest.core.tracing.model.TraceDetail;
import java.util.List;

/**
 * Full trace detail response — summary fields plus the assembled span tree. See {@link TraceSummaryDto}
 * for the ms-since-epoch timestamp rationale.
 */
public record TraceDetailDto(
    String traceId,
    long startTimeEpochMs,
    long durationNanos,
    String rootServiceName,
    String rootOperationName,
    boolean hasError,
    List<SpanDto> spans
) {
    public static TraceDetailDto from(TraceDetail detail) {
        return new TraceDetailDto(
            detail.traceId(),
            detail.startTime() != null ? detail.startTime().toEpochMilli() : 0L,
            detail.durationNanos(),
            detail.rootServiceName(),
            detail.rootOperationName(),
            detail.hasError(),
            detail
                .spans()
                .stream()
                .map(s -> SpanDto.from(detail.traceId(), s))
                .toList()
        );
    }
}
