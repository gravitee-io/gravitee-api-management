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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.SpanKind;
import io.gravitee.gamma.rest.core.tracing.model.SpanStatus;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TracingPort;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Wraps the platform-level {@code TracingRepository} SPI for the core layer. The SPI bean is always
 * present in the parent context — either a real impl (Elasticsearch / future Tempo) when the operator
 * configured a backend for the {@code OTEL_TRACES} scope, or the no-op fallback from
 * {@code gravitee-apim-repository-noop} when the operator set {@code repositories.otel-traces.type=none}.
 * No optional-injection fallback needed here.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class TracingPortAdapter implements TracingPort {

    private final TracingRepository tracingRepository;

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
        QueryContext queryContext = new QueryContext(orgId, envId);
        // ES terms aggs don't support from/size paging natively (only `size` = the first N buckets).
        // Until the tracing SPI grows composite-aggregation support, MVP paging is fetch-and-slice:
        // ask the SPI for the first (page+1)*perPage buckets, then slice client-side. Bounded by the
        // SPI's own MAX_SEARCH_RESULTS cap (200), so pages beyond the first ~10 with perPage=20 will
        // return an empty slice. Acceptable as a starting point; revisit when traces routinely exceed
        // 200 per query window.
        int fetchSize = (page + 1) * perPage;
        TraceSearchCriteria criteria = new TraceSearchCriteria(attributeFilters, fetchSize, start, end, resourceAttributeFilters);
        // The SPI is non-blocking (Single) but the use case wants a List — block here, at the boundary
        // between reactive infra and synchronous core. Acceptable trade-off: a REST request is already a
        // blocking unit of work and the trace search is bounded by the indexed window + the agg limit.
        List<io.gravitee.repository.tracing.model.Trace> fullPage = tracingRepository.searchTraces(queryContext, criteria).blockingGet();
        long total = fullPage.size();

        int fromIndex = Math.min(page * perPage, fullPage.size());
        int toIndex = Math.min(fromIndex + perPage, fullPage.size());
        List<Trace> slice = fullPage.subList(fromIndex, toIndex).stream().map(TracingPortAdapter::toCoreTrace).toList();
        return new Page<>(slice, page, perPage, total);
    }

    @Override
    public Optional<List<Span>> getTraceSpans(String orgId, String envId, String traceId, Map<String, String> resourceAttributeFilters) {
        QueryContext queryContext = new QueryContext(orgId, envId);
        // The SPI returns Maybe.empty() when the trace doesn't exist OR the env scope rejected it — both
        // collapse to Optional.empty here. The use case then short-circuits to a 404.
        return Optional.ofNullable(tracingRepository.getTrace(queryContext, traceId, resourceAttributeFilters).blockingGet()).map(trace ->
            trace.spans().stream().map(TracingPortAdapter::toCoreSpan).toList()
        );
    }

    private static Trace toCoreTrace(io.gravitee.repository.tracing.model.Trace source) {
        return new Trace(
            source.traceId(),
            source.startTime(),
            source.durationNanos(),
            source.rootService(),
            source.rootOperation(),
            Boolean.TRUE.equals(source.hasError())
        );
    }

    private static Span toCoreSpan(io.gravitee.repository.tracing.model.TraceSpan source) {
        Map<String, String> attrs = source.attributes();
        return new Span(
            source.spanId(),
            source.parentSpanId(),
            source.operationName(),
            source.serviceName(),
            source.startTime(),
            source.durationNanos(),
            // The ES adapter normalises status.code into otel.status_code (UNSET/OK/ERROR) on the
            // attribute map — see ElasticsearchTracingRepository.normalizeStatusCode. The promotion to
            // a first-class field happens here so consumers don't need to know the attribute key.
            SpanStatus.fromAttribute(attrs.get("otel.status_code")),
            // span.kind is not yet surfaced by the tracing repository SPI (TraceSpan record has no
            // `kind` field). Until that's threaded through, look for an optional `span.kind` attribute
            // — the OTel default is INTERNAL when missing, which fromAttribute returns for null.
            SpanKind.fromAttribute(attrs.get("span.kind")),
            attrs,
            // Span events on the spans returned by the SPI are usually empty in the OTel-collector path
            // (events get extracted into the logs data stream — fetched separately via OtelLogPort).
            // Keep the list shape consistent so the use case can stitch events onto it without nulls.
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
}
