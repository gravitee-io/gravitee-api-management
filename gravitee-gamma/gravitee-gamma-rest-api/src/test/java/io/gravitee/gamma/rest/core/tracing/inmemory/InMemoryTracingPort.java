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
package io.gravitee.gamma.rest.core.tracing.inmemory;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TracingPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

/**
 * In-memory {@link TracingPort} for domain tests. Seed with {@link #givenTrace(String, String, Map, Trace, List)}
 * — the seeded traces are filtered on org / env / resource-attribute scope just like the real adapter
 * would scope ES queries, so the use case's filter envelope is exercised end-to-end rather than mocked.
 * Captured invocation args are exposed as getters so tests can assert plumbing (default lookback,
 * pagination) without a separate mock framework.
 */
public class InMemoryTracingPort implements TracingPort {

    private final List<TraceFixture> traces = new ArrayList<>();
    private final Map<String, List<Span>> spansByTraceId = new HashMap<>();

    @Getter
    private Map<String, String> lastResourceAttributeFilters;

    @Getter
    private Map<String, String> lastAttributeFilters;

    @Getter
    private Instant lastStart;

    @Getter
    private Instant lastEnd;

    @Getter
    private Integer lastPage;

    @Getter
    private Integer lastPerPage;

    public record TraceFixture(String orgId, String envId, Map<String, String> scope, Trace trace) {}

    public void givenTrace(String orgId, String envId, Map<String, String> scope, Trace trace, List<Span> spans) {
        traces.add(new TraceFixture(orgId, envId, scope, trace));
        if (spans != null) {
            spansByTraceId.put(trace.traceId(), spans);
        }
    }

    public void reset() {
        traces.clear();
        spansByTraceId.clear();
        lastResourceAttributeFilters = null;
        lastAttributeFilters = null;
        lastStart = null;
        lastEnd = null;
        lastPage = null;
        lastPerPage = null;
    }

    @Override
    public Page<Trace> searchTraces(
        String orgId,
        String envId,
        Map<String, String> resourceAttributeFilters,
        Map<String, String> attributeFilters,
        Instant start,
        Instant end,
        int page,
        int perPage
    ) {
        this.lastResourceAttributeFilters = resourceAttributeFilters;
        this.lastAttributeFilters = attributeFilters;
        this.lastStart = start;
        this.lastEnd = end;
        this.lastPage = page;
        this.lastPerPage = perPage;

        List<Trace> matching = traces
            .stream()
            .filter(t -> Objects.equals(t.orgId(), orgId) && Objects.equals(t.envId(), envId))
            .filter(t ->
                resourceAttributeFilters
                    .entrySet()
                    .stream()
                    .allMatch(e -> Objects.equals(t.scope().get(e.getKey()), e.getValue()))
            )
            .filter(t -> (start == null || !t.trace().startTime().isBefore(start)))
            .filter(t -> (end == null || !t.trace().startTime().isAfter(end)))
            .map(TraceFixture::trace)
            .toList();

        int from = Math.max(0, page) * Math.max(1, perPage);
        int to = Math.min(from + Math.max(1, perPage), matching.size());
        List<Trace> pageContent = from >= matching.size() ? List.of() : matching.subList(from, to);
        return new Page<>(pageContent, page, pageContent.size(), matching.size());
    }

    @Override
    public Optional<List<Span>> getTraceSpans(String orgId, String envId, String traceId, Map<String, String> resourceAttributeFilters) {
        this.lastResourceAttributeFilters = resourceAttributeFilters;
        return Optional.ofNullable(spansByTraceId.get(traceId));
    }
}
