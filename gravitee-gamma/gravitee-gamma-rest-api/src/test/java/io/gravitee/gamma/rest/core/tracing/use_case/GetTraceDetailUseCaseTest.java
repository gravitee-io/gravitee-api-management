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
package io.gravitee.gamma.rest.core.tracing.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.tracing.TracingResourceFilters;
import io.gravitee.gamma.rest.core.tracing.inmemory.InMemoryOtelLogPort;
import io.gravitee.gamma.rest.core.tracing.inmemory.InMemoryTracingPort;
import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import io.gravitee.gamma.rest.core.tracing.model.SpanKind;
import io.gravitee.gamma.rest.core.tracing.model.SpanStatus;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import io.gravitee.gamma.rest.core.tracing.model.TraceDetail;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetTraceDetailUseCaseTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final String API_ID = "api-1";
    private static final String TRACE_ID = "trace-abc";

    private final InMemoryTracingPort tracingPort = new InMemoryTracingPort();
    private final InMemoryOtelLogPort otelLogPort = new InMemoryOtelLogPort();
    private final GetTraceDetailUseCase useCase = new GetTraceDetailUseCase(tracingPort, otelLogPort);

    @BeforeEach
    void reset() {
        tracingPort.reset();
        otelLogPort.reset();
    }

    @Test
    void should_return_empty_when_trace_does_not_exist() {
        GetTraceDetailUseCase.Output output = useCase.execute(new GetTraceDetailUseCase.Input(ORG, ENV, API_ID, "unknown"));

        assertThat(output.trace()).isEmpty();
    }

    @Test
    void should_stitch_events_and_payload_logs_onto_their_parent_spans_by_spanId() {
        Span root = span("root", null, "GET /pets", Instant.parse("2026-05-26T15:00:00Z"));
        Span child = span("child", "root", "policy-pre", Instant.parse("2026-05-26T15:00:01Z"));
        givenTraceWithSpans(List.of(root, child));

        otelLogPort.givenEvents(
            TRACE_ID,
            List.of(
                new SpanEvent("root", "gravitee.policy.pre", Instant.parse("2026-05-26T15:00:00.500Z"), Map.of()),
                new SpanEvent("child", "gravitee.policy.post", Instant.parse("2026-05-26T15:00:01.500Z"), Map.of())
            )
        );
        otelLogPort.givenPayloadLogs(
            TRACE_ID,
            List.of(new PayloadLog("root", Instant.parse("2026-05-26T15:00:00.700Z"), "INFO", "{}", Map.of()))
        );

        TraceDetail detail = useCase.execute(input()).trace().orElseThrow();

        Span stitchedRoot = detail
            .spans()
            .stream()
            .filter(s -> "root".equals(s.spanId()))
            .findFirst()
            .orElseThrow();
        Span stitchedChild = detail
            .spans()
            .stream()
            .filter(s -> "child".equals(s.spanId()))
            .findFirst()
            .orElseThrow();
        assertThat(stitchedRoot.events()).extracting(SpanEvent::name).containsExactly("gravitee.policy.pre");
        assertThat(stitchedRoot.payloadLogs()).hasSize(1);
        assertThat(stitchedChild.events()).extracting(SpanEvent::name).containsExactly("gravitee.policy.post");
        assertThat(stitchedChild.payloadLogs()).isEmpty();
    }

    @Test
    void should_sort_events_and_payload_logs_by_timestamp_ascending() {
        Span root = span("root", null, "GET /pets", Instant.parse("2026-05-26T15:00:00Z"));
        givenTraceWithSpans(List.of(root));

        otelLogPort.givenEvents(
            TRACE_ID,
            List.of(
                new SpanEvent("root", "second", Instant.parse("2026-05-26T15:00:00.900Z"), Map.of()),
                new SpanEvent("root", "first", Instant.parse("2026-05-26T15:00:00.100Z"), Map.of())
            )
        );

        TraceDetail detail = useCase.execute(input()).trace().orElseThrow();

        assertThat(detail.spans().get(0).events()).extracting(SpanEvent::name).containsExactly("first", "second");
    }

    @Test
    void should_pick_root_span_for_summary_fields() {
        Span root = span("root", null, "GET /pets", Instant.parse("2026-05-26T15:00:00Z"));
        Span child = span("child", "root", "policy", Instant.parse("2026-05-26T15:00:01Z"));
        givenTraceWithSpans(List.of(child, root));

        TraceDetail detail = useCase.execute(input()).trace().orElseThrow();

        assertThat(detail.rootOperationName()).isEqualTo("GET /pets");
        assertThat(detail.startTime()).isEqualTo(Instant.parse("2026-05-26T15:00:00Z"));
    }

    @Test
    void should_fall_back_to_earliest_span_when_no_explicit_root_is_present() {
        // Both spans declare a parent — synthesises the "trace's true root wasn't ingested" case so the
        // summary still resolves to the chronologically-earliest span instead of nulling everything.
        Span later = span("a", "missing-parent", "later-op", Instant.parse("2026-05-26T15:00:05Z"));
        Span earlier = span("b", "missing-parent", "earlier-op", Instant.parse("2026-05-26T15:00:00Z"));
        givenTraceWithSpans(List.of(later, earlier));

        TraceDetail detail = useCase.execute(input()).trace().orElseThrow();

        assertThat(detail.rootOperationName()).isEqualTo("earlier-op");
    }

    @Test
    void should_flag_hasError_when_any_span_has_ERROR_status() {
        Span ok = span("ok", null, "GET /pets", Instant.parse("2026-05-26T15:00:00Z"));
        Span failing = new Span(
            "failing",
            "ok",
            "downstream-call",
            "gateway",
            Instant.parse("2026-05-26T15:00:01Z"),
            500L,
            SpanStatus.ERROR,
            SpanKind.INTERNAL,
            Map.of(),
            List.of(),
            List.of()
        );
        givenTraceWithSpans(List.of(ok, failing));

        TraceDetail detail = useCase.execute(input()).trace().orElseThrow();

        assertThat(detail.hasError()).isTrue();
    }

    @Test
    void should_not_flag_hasError_when_no_span_has_ERROR_status() {
        givenTraceWithSpans(List.of(span("root", null, "GET /pets", Instant.parse("2026-05-26T15:00:00Z"))));

        TraceDetail detail = useCase.execute(input()).trace().orElseThrow();

        assertThat(detail.hasError()).isFalse();
    }

    private Span span(String spanId, String parentSpanId, String op, Instant start) {
        return new Span(
            spanId,
            parentSpanId,
            op,
            "gateway",
            start,
            1_000L,
            SpanStatus.UNSET,
            SpanKind.INTERNAL,
            Map.of(),
            List.of(),
            List.of()
        );
    }

    private void givenTraceWithSpans(List<Span> spans) {
        tracingPort.givenTrace(
            ORG,
            ENV,
            Map.of(TracingResourceFilters.ENV_ID_KEY, ENV, TracingResourceFilters.API_ID_KEY, API_ID),
            new Trace(TRACE_ID, spans.get(0).startTime(), 1_000L, "gateway", spans.get(0).operationName(), false),
            spans
        );
    }

    private GetTraceDetailUseCase.Input input() {
        return new GetTraceDetailUseCase.Input(ORG, ENV, API_ID, TRACE_ID);
    }
}
