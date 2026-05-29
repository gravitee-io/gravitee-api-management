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
package io.gravitee.repository.elasticsearch.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import io.gravitee.repository.tracing.model.TraceSpan;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ElasticsearchTracingRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("test-org", "test-env");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Client client;
    private ElasticsearchTracingRepository repository;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(Client.class);
        repository = new ElasticsearchTracingRepository("traces-apim.otel-{orgId}", client);
    }

    private void stubBackends(SearchResponse traceResponse) {
        when(client.search(any(), any(), any())).thenReturn(Single.just(traceResponse));
    }

    @Test
    void searchTraces_should_substitute_orgId_in_index_and_build_aggregation_query() throws Exception {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        // Pin the wire shape: attributeFilters go to attributes.* unconditionally, resourceAttributeFilters
        // go to resource.attributes.* — no key-prefix heuristics. Service identity (a resource concept) must
        // be declared explicitly by the caller via resourceAttributeFilters.
        TraceSearchCriteria criteria = new TraceSearchCriteria(
            Map.of("http.status_code", "200"),
            5,
            Instant.parse("2026-04-30T09:00:00Z"),
            Instant.parse("2026-04-30T11:00:00Z"),
            Map.of("service.name", "test-service", "gravitee.environment.id", "test-env")
        );

        repository.searchTraces(QUERY_CONTEXT, criteria).blockingGet();

        ArgumentCaptor<String> indexCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(indexCaptor.capture(), eq(null), bodyCaptor.capture());

        // Hyphen in orgId converted to underscore by the OTel-mode dataset normalisation — matches
        // the data stream the collector wrote (see OtelDataStreamIndexUtils javadoc).
        assertThat(indexCaptor.getValue()).isEqualTo("traces-apim.otel-test_org");

        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        JsonNode filter = body.path("query").path("bool").path("filter");
        // Clause order: time range, then attribute filters, then resource-attribute filters.
        assertThat(filter.get(0).path("range").path("@timestamp").path("gte").asText()).isEqualTo("2026-04-30T09:00:00Z");
        assertThat(filter.get(1).path("term").path("attributes.http.status_code").asText()).isEqualTo("200");
        // service.name + gravitee.environment.id both land on resource.attributes.* — gather both into a set
        // since the resource-filter iteration order is the criteria map's iteration order (unspecified for
        // Map.of) and we only care about contents, not order.
        java.util.Map<String, String> resourceClauses = new java.util.HashMap<>();
        for (int i = 2; i < 4; i++) {
            JsonNode term = filter.get(i).path("term");
            term.fields().forEachRemaining(e -> resourceClauses.put(e.getKey(), e.getValue().asText()));
        }
        assertThat(resourceClauses)
            .containsEntry("resource.attributes.service.name", "test-service")
            .containsEntry("resource.attributes.gravitee.environment.id", "test-env");
        assertThat(body.path("aggs").path("traces").path("terms").path("field").asText()).isEqualTo("trace_id");
        assertThat(body.path("aggs").path("traces").path("terms").path("size").asInt()).isEqualTo(5);
    }

    @Test
    void searchTraces_should_cap_aggregation_size_when_caller_requests_more_than_max_search_results() throws Exception {
        // Defensive cap on the terms-agg size: ES coordinator's search.max_buckets defaults to 10 000 and a
        // larger value is rejected with circuit_breaking_exception. 200 mirrors the impl constant.
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        TraceSearchCriteria criteria = new TraceSearchCriteria(Map.of(), 100_000, null, null, Map.of());

        repository.searchTraces(QUERY_CONTEXT, criteria).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        assertThat(body.path("aggs").path("traces").path("terms").path("size").asInt()).isEqualTo(200);
    }

    @Test
    void searchTraces_should_map_aggregation_buckets_to_traces() {
        SearchResponse response = new SearchResponse();
        response.setAggregations(Map.of("traces", buildTracesAggregation()));
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        TraceSearchCriteria criteria = new TraceSearchCriteria(Map.of(), 20, null, null, Map.of());
        List<Trace> traces = repository.searchTraces(QUERY_CONTEXT, criteria).blockingGet();

        assertThat(traces).hasSize(1);
        Trace trace = traces.get(0);
        assertThat(trace.traceId()).isEqualTo("a1b2c3d4");
        assertThat(trace.rootService()).isEqualTo("test-service");
        assertThat(trace.rootOperation()).isEqualTo("GET /ok");
        assertThat(trace.durationNanos()).isEqualTo(5_000_000L);
        // Root span's status drives the trace-level rollup when no span errored — buildTracesAggregation seeds it
        // with STATUS_CODE_OK so the bucket should resolve to OK (not UNSET).
        assertThat(trace.status()).isEqualTo("OK");
        // doc_count on the bucket = number of spans for that trace_id. Fixture sets it to 3.
        assertThat(trace.spanCount()).isEqualTo(3);
        assertThat(trace.startTime()).isEqualTo(Instant.parse("2026-04-30T10:00:00Z"));
        assertThat(trace.spans()).isEmpty();
    }

    @Test
    void getTrace_should_wrap_query_in_bool_filter_when_resource_filters_present() throws Exception {
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        SearchHit hit = new SearchHit();
        hit.setSource(buildSpanSource());
        hits.setHits(List.of(hit));
        response.setSearchHits(hits);
        stubBackends(response);

        Trace trace = repository.getTrace(QUERY_CONTEXT, "a1b2c3d4", Map.of("gravitee.environment.id", "test-env")).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        // With resource filters set, getTrace switches from a top-level term to a bool.filter so both the trace_id
        // and the resource clauses land in the same query.
        JsonNode filter = body.path("query").path("bool").path("filter");
        assertThat(filter.get(0).path("term").path("trace_id").asText()).isEqualTo("a1b2c3d4");
        assertThat(filter.get(1).path("term").path("resource.attributes.gravitee.environment.id").asText()).isEqualTo("test-env");

        assertThat(trace).isNotNull();
        assertThat(trace.traceId()).isEqualTo("a1b2c3d4");
        assertThat(trace.spans()).hasSize(1);
        TraceSpan span = trace.spans().get(0);
        assertThat(span.spanId()).isEqualTo("span-1");
        assertThat(span.serviceName()).isEqualTo("test-service");
        assertThat(span.attributes()).containsEntry("http.status_code", "200");
        // OTel status normalised to UNSET/OK/ERROR and injected as a span attribute (mirrors the Tempo impl), so
        // a UI / contract caller sees the same attribute regardless of which backend serves the request.
        assertThat(span.attributes()).containsEntry("otel.status_code", "OK");
        assertThat(span.events()).hasSize(1);
        assertThat(span.events().get(0).name()).isEqualTo("exception");
    }

    @Test
    void getTrace_should_promote_trace_status_to_ERROR_when_any_span_has_error_short_form_status() {
        // The OTel ES exporter (otel mapping mode, recent versions) writes status.code as the short form ("Error"),
        // not the proto-enum-suffix form ("STATUS_CODE_ERROR"). Both must work.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        SearchHit hit = new SearchHit();
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", "a1b2c3d4");
        source.put("span_id", "span-err");
        source.put("name", "errored");
        source.put("@timestamp", "2026-04-30T10:00:00Z");
        source.put("duration", 1L);
        source.put("status", Map.of("code", "Error"));
        hit.setSource(MAPPER.valueToTree(source));
        hits.setHits(List.of(hit));
        response.setSearchHits(hits);
        stubBackends(response);

        Trace trace = repository.getTrace(QUERY_CONTEXT, "a1b2c3d4", Map.of()).blockingGet();

        assertThat(trace).isNotNull();
        // Trace-level status rolls up to ERROR because at least one span reported ERROR. Wire vocabulary matches
        // per-span status (UNSET / OK / ERROR) so the UI can reuse the same lookup.
        assertThat(trace.status()).isEqualTo("ERROR");
        assertThat(trace.spanCount()).isEqualTo(1);
        assertThat(trace.spans().get(0).attributes()).containsEntry("otel.status_code", "ERROR");
    }

    @Test
    void parseInstant_should_handle_otel_epoch_millis_decimal_string() {
        // OTel ES exporter (otel mapping mode) writes @timestamp as "<epoch-ms>.<sub-ms-digits>" — a decimal-shaped
        // string of epoch ms with sub-millisecond precision. The parser must extract both pieces to produce a
        // correct Instant.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        SearchHit hit = new SearchHit();
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", "a1b2c3d4");
        source.put("span_id", "span-1");
        source.put("name", "POST /proxy");
        source.put("@timestamp", "1778515640168.675208");
        source.put("duration", 11107458L);
        hit.setSource(MAPPER.valueToTree(source));
        hits.setHits(List.of(hit));
        response.setSearchHits(hits);
        stubBackends(response);

        Trace trace = repository.getTrace(QUERY_CONTEXT, "a1b2c3d4", Map.of()).blockingGet();

        assertThat(trace).isNotNull();
        assertThat(trace.startTime()).isEqualTo(Instant.ofEpochSecond(1778515640L, 168_675_208L));
        assertThat(trace.spans().get(0).startTime()).isEqualTo(Instant.ofEpochSecond(1778515640L, 168_675_208L));
    }

    @Test
    void getTrace_should_complete_empty_when_no_hits() {
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        hits.setHits(List.of());
        response.setSearchHits(hits);
        stubBackends(response);

        Trace trace = repository.getTrace(QUERY_CONTEXT, "missing", Map.of()).blockingGet();

        assertThat(trace).isNull();
    }

    @Test
    void resolveIndex_should_lowercase_placeholder_values_to_match_data_stream_convention() {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));
        // QueryContext orgId comes through as the caller-provided casing — typically "DEFAULT" for the default org.
        // OTel collectors write to lowercase data-stream names (ES data streams require lowercase), so the index
        // must resolve to traces-apim.otel-default regardless of input casing.
        QueryContext upperCase = new QueryContext("DEFAULT", "PROD");
        ElasticsearchTracingRepository repo = new ElasticsearchTracingRepository("traces-apim.otel-{orgId}-{envId}", client);

        repo.searchTraces(upperCase, new TraceSearchCriteria(Map.of(), 20, null, null, Map.of())).blockingGet();

        ArgumentCaptor<String> indexCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(indexCaptor.capture(), eq(null), any());
        assertThat(indexCaptor.getValue()).isEqualTo("traces-apim.otel-default-prod");
    }

    @Test
    void searchTraces_should_emit_resource_attribute_filters_on_resource_side() throws Exception {
        // resourceAttributeFilters are an explicit caller signal — the impl emits them on resource.attributes.*
        // without any prefix heuristic or hardcoded key list. Span tags stay on attributes.*.
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));
        TraceSearchCriteria criteria = new TraceSearchCriteria(
            Map.of("http.method", "GET"),
            20,
            null,
            null,
            Map.of("gravitee.module", "apim")
        );

        repository.searchTraces(QUERY_CONTEXT, criteria).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        JsonNode filter = body.path("query").path("bool").path("filter");
        boolean hasResourceModuleFilter = false;
        boolean hasSpanHttpMethodFilter = false;
        for (JsonNode clause : filter) {
            JsonNode term = clause.path("term");
            if ("apim".equals(term.path("resource.attributes.gravitee.module").asText(null))) hasResourceModuleFilter = true;
            if ("GET".equals(term.path("attributes.http.method").asText(null))) hasSpanHttpMethodFilter = true;
        }
        assertThat(hasResourceModuleFilter).as("gravitee.module → resource.attributes.gravitee.module").isTrue();
        assertThat(hasSpanHttpMethodFilter).as("http.method → attributes.http.method").isTrue();
    }

    @Test
    void searchTraces_should_match_all_when_no_filters_provided() throws Exception {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository.searchTraces(QUERY_CONTEXT, new TraceSearchCriteria(Map.of(), 20, null, null, Map.of())).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        // No time range, no attribute filters, no resource filters → match_all rather than an empty bool.filter
        // (ES rejects empty filter arrays).
        assertThat(body.path("query").has("match_all")).isTrue();
    }

    @Test
    void getTrace_should_use_top_level_term_when_no_resource_filters() throws Exception {
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        hits.setHits(List.of());
        response.setSearchHits(hits);
        stubBackends(response);

        repository.getTrace(QUERY_CONTEXT, "a1b2c3d4", Map.of()).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        // Falls back to the simpler top-level term query — the bool wrapping is only added when resource filters
        // are present.
        assertThat(body.path("query").path("term").path("trace_id").asText()).isEqualTo("a1b2c3d4");
    }

    @Test
    void searchTraces_should_emit_empty_list_when_aggregation_missing() {
        SearchResponse response = new SearchResponse();
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<Trace> traces = repository
            .searchTraces(QUERY_CONTEXT, new TraceSearchCriteria(Map.of(), 20, null, null, Map.of()))
            .blockingGet();

        assertThat(traces).isEmpty();
    }

    private static Aggregation buildTracesAggregation() {
        Aggregation traces = new Aggregation();
        Map<String, Object> bucket = new HashMap<>();
        bucket.put("key", "a1b2c3d4");
        // doc_count on a terms-agg bucket = number of docs (= spans) for this trace_id. Exposed to the caller via
        // Trace.spanCount so the listing row can render "N spans".
        bucket.put("doc_count", 3);
        Map<String, Object> traceStart = new HashMap<>();
        traceStart.put("value_as_string", "2026-04-30T10:00:00Z");
        bucket.put("trace_start", traceStart);

        Map<String, Object> rootSpan = new HashMap<>();
        Map<String, Object> rootSpanHits = new HashMap<>();
        Map<String, Object> rootSpanHitsHits = new HashMap<>();
        Map<String, Object> rootSource = new HashMap<>();
        rootSource.put("name", "GET /ok");
        rootSource.put("duration", 5_000_000L);
        Map<String, Object> resource = new HashMap<>();
        Map<String, Object> resourceAttributes = new HashMap<>();
        Map<String, Object> resourceService = new HashMap<>();
        resourceService.put("name", "test-service");
        resourceAttributes.put("service", resourceService);
        resource.put("attributes", resourceAttributes);
        rootSource.put("resource", resource);
        // Root span status drives the trace-level rollup when no span errored — the impl runs the same
        // normalizeStatusCode logic on the rootSpan top_hits as it does on per-span sources, so seed it the same way.
        Map<String, Object> rootStatus = new HashMap<>();
        rootStatus.put("code", "STATUS_CODE_OK");
        rootSource.put("status", rootStatus);
        rootSpanHitsHits.put("_source", rootSource);
        rootSpanHits.put("hits", List.of(rootSpanHitsHits));
        rootSpan.put("hits", rootSpanHits);
        bucket.put("root_span", rootSpan);

        Map<String, Object> errorCount = new HashMap<>();
        errorCount.put("doc_count", 0);
        bucket.put("error_count", errorCount);

        traces.setBuckets(List.of(MAPPER.valueToTree(bucket)));
        return traces;
    }

    private static JsonNode buildSpanSource() {
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", "a1b2c3d4");
        source.put("span_id", "span-1");
        source.put("name", "GET /ok");
        source.put("@timestamp", "2026-04-30T10:00:00Z");
        source.put("duration", 5_000_000L);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("http.status_code", "200");
        source.put("attributes", attributes);

        Map<String, Object> resource = new HashMap<>();
        Map<String, Object> resourceAttributes = new HashMap<>();
        Map<String, Object> resourceService = new HashMap<>();
        resourceService.put("name", "test-service");
        resourceAttributes.put("service", resourceService);
        resource.put("attributes", resourceAttributes);
        source.put("resource", resource);

        Map<String, Object> event = new HashMap<>();
        event.put("name", "exception");
        event.put("timestamp", "2026-04-30T10:00:01Z");
        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("exception.type", "RuntimeException");
        event.put("attributes", eventAttributes);
        source.put("events", List.of(event));

        Map<String, Object> status = new HashMap<>();
        status.put("code", "STATUS_CODE_OK");
        source.put("status", status);

        return MAPPER.valueToTree(source);
    }
}
