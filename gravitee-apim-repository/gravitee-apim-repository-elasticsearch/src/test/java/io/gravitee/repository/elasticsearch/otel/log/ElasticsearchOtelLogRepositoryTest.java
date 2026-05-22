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
package io.gravitee.repository.elasticsearch.otel.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ElasticsearchOtelLogRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("test-org", "test-env");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Client client;
    private ElasticsearchOtelLogRepository repository;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(Client.class);
        repository = new ElasticsearchOtelLogRepository("logs-apim.otel-{orgId}", client);
    }

    @Test
    void findLogs_should_target_org_scoped_index_and_filter_by_trace_id_without_event_name_discriminator() throws Exception {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        ArgumentCaptor<String> indexCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(indexCaptor.capture(), eq(null), bodyCaptor.capture());

        assertThat(indexCaptor.getValue()).isEqualTo("logs-apim.otel-test-org");

        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        JsonNode filter = body.path("query").path("bool").path("filter");
        boolean hasTraceId = false;
        for (JsonNode clause : filter) {
            if ("a1b2c3d4".equals(clause.path("term").path("trace_id").asText(null))) hasTraceId = true;
            // No event_name existence filter — the SPI returns span events and payload logs together.
            assertThat(clause.path("exists").path("field").asText(null)).isNotEqualTo("event_name");
        }
        assertThat(hasTraceId).isTrue();
        assertThat(body.path("query").path("bool").path("must_not").isMissingNode()).isTrue();
        // Sort by @timestamp ascending so consumers see entries in chronological order.
        assertThat(body.path("sort").get(0).path("@timestamp").path("order").asText()).isEqualTo("asc");
        // Default limit from the criteria's compact constructor.
        assertThat(body.path("size").asInt()).isEqualTo(5000);
    }

    @Test
    void findLogs_should_omit_trace_id_filter_when_criteria_has_none() throws Exception {
        // Logs-explorer use case: filter by resource scope + time range only, no trace_id.
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository
            .findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria(null, Map.of(), Map.of("gravitee.api.id", "api-1"), null, null, null))
            .blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        for (JsonNode clause : body.path("query").path("bool").path("filter")) {
            assertThat(clause.path("term").path("trace_id").isMissingNode()).isTrue();
        }
    }

    @Test
    void findLogs_should_propagate_resource_attribute_filters_as_resource_terms() throws Exception {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository
            .findLogs(
                QUERY_CONTEXT,
                new OtelLogSearchCriteria(
                    "a1b2c3d4",
                    Map.of(),
                    Map.of("gravitee.env.id", "test-env", "gravitee.module", "apim"),
                    null,
                    null,
                    null
                )
            )
            .blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        boolean hasEnv = false;
        boolean hasModule = false;
        for (JsonNode clause : body.path("query").path("bool").path("filter")) {
            JsonNode term = clause.path("term");
            if ("test-env".equals(term.path("resource.attributes.gravitee.env.id").asText(null))) hasEnv = true;
            if ("apim".equals(term.path("resource.attributes.gravitee.module").asText(null))) hasModule = true;
        }
        assertThat(hasEnv).isTrue();
        assertThat(hasModule).isTrue();
    }

    @Test
    void findLogs_should_propagate_record_attribute_filters_under_attributes_prefix() throws Exception {
        // Record attributes live under attributes.<key> (vs resource.attributes.<key> for resource scope),
        // matching how the OTel ES exporter writes them.
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository
            .findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria("a1b2c3d4", Map.of("some.attribute", "value"), Map.of(), null, null, null))
            .blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        boolean hasAttribute = false;
        for (JsonNode clause : body.path("query").path("bool").path("filter")) {
            if ("value".equals(clause.path("term").path("attributes.some.attribute").asText(null))) hasAttribute = true;
        }
        assertThat(hasAttribute).isTrue();
    }

    @Test
    void findLogs_should_map_event_name_attribute_filter_to_top_level_event_name_field() throws Exception {
        // The OTel ES exporter promotes the standard event.name attribute to the top-level event_name
        // field — query-side filters on event.name must hit that field, not attributes.event.name (which
        // doesn't exist in the indexed document and would silently match nothing).
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository
            .findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria("a1b2c3d4", Map.of("event.name", "exception"), Map.of(), null, null, null))
            .blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        boolean hasEventName = false;
        boolean usesAttributesPrefix = false;
        for (JsonNode clause : body.path("query").path("bool").path("filter")) {
            JsonNode term = clause.path("term");
            if ("exception".equals(term.path("event_name").asText(null))) hasEventName = true;
            if (term.has("attributes.event.name")) usesAttributesPrefix = true;
        }
        assertThat(hasEventName).isTrue();
        assertThat(usesAttributesPrefix).isFalse();
    }

    @Test
    void findLogs_should_emit_half_open_timestamp_range_when_bounds_set() throws Exception {
        Instant start = Instant.parse("2026-04-30T10:00:00Z");
        Instant end = Instant.parse("2026-04-30T11:00:00Z");
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository.findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria("a1b2c3d4", Map.of(), Map.of(), start, end, null)).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        boolean hasRange = false;
        for (JsonNode clause : body.path("query").path("bool").path("filter")) {
            JsonNode range = clause.path("range").path("@timestamp");
            if (!range.isMissingNode()) {
                // gte = inclusive lower, lt = exclusive upper — half-open like every other time range in
                // the analytics plugin, so back-to-back ranges don't double-count boundary records.
                assertThat(range.path("gte").asText()).isEqualTo("2026-04-30T10:00:00Z");
                assertThat(range.path("lt").asText()).isEqualTo("2026-04-30T11:00:00Z");
                hasRange = true;
            }
        }
        assertThat(hasRange).isTrue();
    }

    @Test
    void findLogs_should_use_criteria_limit_as_search_size() throws Exception {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository.findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria("a1b2c3d4", Map.of(), Map.of(), null, null, 100)).blockingGet();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).search(any(), eq(null), bodyCaptor.capture());
        JsonNode body = MAPPER.readTree(bodyCaptor.getValue());
        assertThat(body.path("size").asInt()).isEqualTo(100);
    }

    @Test
    void findLogs_should_promote_top_level_event_name_into_attributes_under_otel_key() {
        // ES exporter writes event_name as a top-level field; the repository normalises it back to the
        // OTel-standard `event.name` attribute so consumers don't carry ES-specific knowledge.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        hits.setHits(
            List.of(
                eventHit("a1b2c3d4", "11223344aabbccdd", "gravitee.policy.pre", "2026-04-30T10:00:00.100Z", "policy-id-1"),
                eventHit("a1b2c3d4", "11223344aabbccdd", "gravitee.policy.post", "2026-04-30T10:00:00.200Z", "policy-id-1")
            )
        );
        response.setSearchHits(hits);
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records)
            .extracting(r -> r.attributes().get("event.name"))
            .containsExactly("gravitee.policy.pre", "gravitee.policy.post");
        assertThat(records.get(0).traceId()).isEqualTo("a1b2c3d4");
        assertThat(records.get(0).spanId()).isEqualTo("11223344aabbccdd");
        assertThat(records.get(0).attributes()).containsEntry("gravitee.policy.id", "policy-id-1");
        assertThat(records.get(0).body()).isNull();
    }

    @Test
    void findLogs_should_preserve_event_name_attribute_when_already_present_in_attributes_block() {
        // Defensive: if a future collector writes event.name into the OTel attributes block instead of
        // the top-level field, putIfAbsent must not clobber it with a null from `source.event_name`.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", "a1b2c3d4");
        source.put("span_id", "11223344aabbccdd");
        source.put("@timestamp", "2026-04-30T10:00:00.100Z");
        source.put("attributes", Map.of("event.name", "exception"));
        SearchHit hit = new SearchHit();
        hit.setSource(MAPPER.valueToTree(source));
        hits.setHits(List.of(hit));
        response.setSearchHits(hits);
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records.get(0).attributes()).containsEntry("event.name", "exception");
    }

    @Test
    void findLogs_should_map_body_as_top_level_string_for_payload_logs() {
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", "a1b2c3d4");
        source.put("span_id", "11223344aabbccdd");
        source.put("@timestamp", "2026-04-30T10:00:00.100Z");
        source.put("body", "{\"hello\":\"world\"}");
        source.put("severity_text", "INFO");
        SearchHit hit = new SearchHit();
        hit.setSource(MAPPER.valueToTree(source));
        hits.setHits(List.of(hit));
        response.setSearchHits(hits);
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records).hasSize(1);
        OtelLogRecord payload = records.get(0);
        assertThat(payload.body()).isEqualTo("{\"hello\":\"world\"}");
        assertThat(payload.severity()).isEqualTo("INFO");
        // Payload logs have no event.name — that's the discriminator consumers use to slice them out.
        assertThat(payload.attributes()).doesNotContainKey("event.name");
    }

    @Test
    void findLogs_should_map_body_text_when_body_is_nested() {
        // Some OTel SDK / collector versions write the body as a nested object with a typed `text`
        // field rather than a top-level string. The reader must accept both shapes.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", "a1b2c3d4");
        source.put("span_id", "11223344aabbccdd");
        source.put("@timestamp", "2026-04-30T10:00:00.100Z");
        source.put("body", Map.of("text", "nested-body"));
        SearchHit hit = new SearchHit();
        hit.setSource(MAPPER.valueToTree(source));
        hits.setHits(List.of(hit));
        response.setSearchHits(hits);
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records.get(0).body()).isEqualTo("nested-body");
    }

    @Test
    void should_drop_records_with_missing_span_id() {
        // A record without a span_id can't be stitched onto any span, so it's dropped at the boundary
        // rather than synthesising an orphan that would confuse the consumer.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        SearchHit orphan = new SearchHit();
        Map<String, Object> orphanSource = new HashMap<>();
        orphanSource.put("trace_id", "a1b2c3d4");
        orphanSource.put("event_name", "exception");
        orphanSource.put("@timestamp", "2026-04-30T10:00:00.100Z");
        orphan.setSource(MAPPER.valueToTree(orphanSource));
        hits.setHits(
            List.of(orphan, eventHit("a1b2c3d4", "11223344aabbccdd", "gravitee.policy.pre", "2026-04-30T10:00:00.200Z", "policy-id-1"))
        );
        response.setSearchHits(hits);
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records).hasSize(1);
        assertThat(records.get(0).attributes()).containsEntry("event.name", "gravitee.policy.pre");
    }

    @Test
    void should_return_empty_list_when_logs_data_stream_is_missing() {
        // Operators may run the gateway tracing pipeline without a separate logs pipeline configured —
        // the data stream doesn't exist and the search 404s. Repository degrades to empty list rather
        // than failing so consumers (gamma use case) can still return the trace's spans.
        when(client.search(any(), any(), any())).thenReturn(Single.error(new RuntimeException("logs index missing")));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records).isEmpty();
    }

    @Test
    void should_parse_otel_epoch_millis_decimal_timestamp() {
        // OTel ES exporter (otel mapping mode) writes @timestamp as "<epoch-ms>.<sub-ms-digits>" — a
        // decimal-shaped string of epoch ms with sub-millisecond precision. parseInstant() must
        // extract both parts to produce an accurate Instant.
        SearchResponse response = new SearchResponse();
        SearchHits hits = new SearchHits();
        hits.setHits(List.of(eventHit("a1b2c3d4", "11223344aabbccdd", "exception", "1778515640168.675208", "policy-id-1")));
        response.setSearchHits(hits);
        when(client.search(any(), any(), any())).thenReturn(Single.just(response));

        List<OtelLogRecord> records = repository.findLogs(QUERY_CONTEXT, criteriaForTrace("a1b2c3d4")).blockingGet();

        assertThat(records.get(0).timestamp()).isEqualTo(Instant.ofEpochSecond(1778515640L, 168_675_208L));
    }

    private static OtelLogSearchCriteria criteriaForTrace(String traceId) {
        return new OtelLogSearchCriteria(traceId, Map.of(), Map.of(), null, null, null);
    }

    private static SearchHit eventHit(String traceId, String spanId, String name, String timestamp, String policyId) {
        Map<String, Object> source = new HashMap<>();
        source.put("trace_id", traceId);
        source.put("span_id", spanId);
        source.put("event_name", name);
        source.put("@timestamp", timestamp);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("gravitee.policy.id", policyId);
        source.put("attributes", attributes);
        SearchHit hit = new SearchHit();
        hit.setSource(MAPPER.valueToTree(source));
        return hit;
    }
}
