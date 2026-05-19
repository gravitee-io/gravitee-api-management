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
import java.util.List;

/**
 * Aggregated view of a distributed trace returned by a {@link io.gravitee.repository.tracing.api.TracingRepository}.
 * <p>
 * In list responses {@code spans} is empty (the backend only carries summary attributes); {@code spans} is populated when the
 * trace is fetched by id. {@code hasError} may be {@code null} when the backend cannot determine the error status.
 *
 * @author GraviteeSource Team
 */
public record Trace(
    String traceId,
    Instant startTime,
    long durationNanos,
    String rootService,
    String rootOperation,
    Boolean hasError,
    List<TraceSpan> spans
) {
    public Trace {
        if (spans == null) {
            spans = new ArrayList<>();
        }
    }
}
