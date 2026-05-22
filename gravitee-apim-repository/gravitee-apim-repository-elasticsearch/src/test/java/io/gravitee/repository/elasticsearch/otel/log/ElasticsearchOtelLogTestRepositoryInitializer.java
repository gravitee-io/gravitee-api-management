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

import static io.gravitee.repository.elasticsearch.otel.log.ElasticsearchOtelLogTestRepositoryConfiguration.OTEL_COLLECTOR_OTLP_HTTP_PORT;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.awaitility.Awaitility.await;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.config.TestRepositoryInitializer;
import io.gravitee.repository.otel.log.OtelLogRepositoryTest.Fixtures;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.GenericContainer;

/**
 * Pushes the {@link Fixtures} OTel log records into the collector via OTLP HTTP, then waits for them to
 * become queryable through the production {@link ElasticsearchOtelLogRepository}. Two distinct producer
 * paths are exercised so the contract test sees both shapes:
 * <ul>
 *   <li>OTLP traces with span events — the elasticsearch exporter (in {@code mapping.mode: otel})
 *       extracts the span events into the logs data stream with {@code event_name} set, which the
 *       production repository normalises to {@code attributes["event.name"]}.</li>
 *   <li>OTLP log records — direct logs emitted by the seeder, modelling what
 *       {@code gravitee-reporter-otel} ships at runtime when the gateway captures a request/response
 *       payload. These land in the same data stream without {@code event_name}.</li>
 * </ul>
 * Seeding runs once across the suite — ES persists ingested records, so re-pushing on every test would
 * inflate {@code findLogs} results.
 *
 * @author GraviteeSource Team
 */
public class ElasticsearchOtelLogTestRepositoryInitializer implements TestRepositoryInitializer {

    private static final Duration QUERYABLE_POLL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration QUERYABLE_POLL_INTERVAL = Duration.ofMillis(500);

    /** Anchor instant for the seeded fixtures — events / log are spaced ~100ms apart from here. */
    private static final Instant SEED_ANCHOR = Instant.parse("2026-04-30T10:00:00Z");

    private final GenericContainer<?> otelCollectorContainer;
    private final OtelLogRepository otelLogRepository;

    private boolean seeded = false;

    @Autowired
    public ElasticsearchOtelLogTestRepositoryInitializer(
        GenericContainer<?> otelLogTestOtelCollectorContainer,
        OtelLogRepository otelLogRepository
    ) {
        this.otelCollectorContainer = otelLogTestOtelCollectorContainer;
        this.otelLogRepository = otelLogRepository;
    }

    @Override
    public void setUp() {
        if (seeded) {
            return;
        }
        String collectorHost = otelCollectorContainer.getHost() + ":" + otelCollectorContainer.getMappedPort(OTEL_COLLECTOR_OTLP_HTTP_PORT);
        seedSpanEvents(collectorHost);
        seedPayloadLog(collectorHost);
        awaitLogsQueryable();
        seeded = true;
    }

    @Override
    public void tearDown() {
        // ES storage is bound to the container lifecycle (managed by the Spring context), so per-test
        // cleanup is unnecessary — the suite seeds once and tests share the read-only fixture set.
    }

    /**
     * Emits a single OTLP trace whose span carries two events (pre + post). The collector's elasticsearch
     * exporter promotes each event into a log document in the logs data stream with {@code event_name}
     * set — matching what the production repository normalises back to {@code attributes["event.name"]}.
     */
    private void seedSpanEvents(String collectorHost) {
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder().setEndpoint("http://" + collectorHost + "/v1/traces").build();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(Resource.create(Attributes.of(stringKey("service.name"), "otel-log-fixtures")))
            .setIdGenerator(new FixtureIdGenerator())
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();

        try (OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()) {
            Tracer tracer = sdk.getTracer("elasticsearch-otel-log-test-fixtures");

            long spanStartNanos = epochNanos(SEED_ANCHOR);
            Span span = tracer.spanBuilder("seed-span").setStartTimestamp(spanStartNanos, TimeUnit.NANOSECONDS).startSpan();

            // Pre event — carries the policy id attribute the contract test asserts on.
            span.addEvent(
                Fixtures.EVENT_PRE_NAME,
                Attributes.of(stringKey("gravitee.policy.id"), Fixtures.EVENT_PRE_POLICY_ID),
                spanStartNanos + Duration.ofMillis(100).toNanos(),
                TimeUnit.NANOSECONDS
            );
            // Post event — no extra attributes; ordering by @timestamp is what matters.
            span.addEvent(Fixtures.EVENT_POST_NAME, spanStartNanos + Duration.ofMillis(200).toNanos(), TimeUnit.NANOSECONDS);
            span.end(spanStartNanos + Duration.ofMillis(300).toNanos(), TimeUnit.NANOSECONDS);

            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        }
    }

    /**
     * Emits a single OTLP log record stamped with the fixture trace_id / span_id, modelling what
     * {@code gravitee-reporter-otel} writes when the gateway captures a request/response payload. The
     * record lands in the logs data stream without {@code event_name} — the contract test uses that
     * absence to discriminate payload logs from span events.
     */
    private void seedPayloadLog(String collectorHost) {
        OtlpHttpLogRecordExporter exporter = OtlpHttpLogRecordExporter.builder()
            .setEndpoint("http://" + collectorHost + "/v1/logs")
            .build();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
            .setResource(Resource.create(Attributes.of(stringKey("service.name"), "otel-log-fixtures")))
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
            .build();

        try {
            Logger logger = loggerProvider.get("elasticsearch-otel-log-test-fixtures");
            // The OTel logs SDK ties trace/span ids to the active Context, not to LogRecordBuilder.setTraceId.
            // Wrap a no-op span carrying the fixture ids and emit the log under that context so the exporter
            // serialises trace_id / span_id correctly into the OTLP payload.
            Context context = Context.current().with(
                Span.wrap(FixtureIdGenerator.fixtureSpanContext(Fixtures.TRACE_OK_ID, Fixtures.TRACE_OK_SPAN_ID))
            );
            logger
                .logRecordBuilder()
                .setContext(context)
                .setTimestamp(SEED_ANCHOR.plusMillis(400).getEpochSecond() * 1_000_000_000L + 400_000_000L, TimeUnit.NANOSECONDS)
                .setSeverity(Severity.INFO)
                .setSeverityText("INFO")
                .setBody(Fixtures.PAYLOAD_BODY)
                .emit();

            loggerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        } finally {
            loggerProvider.close();
        }
    }

    private void awaitLogsQueryable() {
        QueryContext queryContext = new QueryContext(Fixtures.ORG_ID, Fixtures.ENV_ID);
        OtelLogSearchCriteria criteria = new OtelLogSearchCriteria(Fixtures.TRACE_OK_ID, Map.of(), Map.of(), null, null, null);

        // Two span events + one payload log = 3 docs once both producer paths have flushed end-to-end.
        await()
            .atMost(QUERYABLE_POLL_TIMEOUT)
            .pollInterval(QUERYABLE_POLL_INTERVAL)
            .until(() -> {
                List<OtelLogRecord> records = otelLogRepository.findLogs(queryContext, criteria).blockingGet();
                return records.size() >= 3;
            });
    }

    private static long epochNanos(Instant instant) {
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }

    /**
     * Hands the contract's fixture trace + span ids to the OTel SDK. Generates them once for the first
     * span the tracer creates (which is the only span the IT seeds); subsequent ids fall back to random
     * so a future test that opens another span here doesn't collide with the fixture.
     */
    private static final class FixtureIdGenerator implements IdGenerator {

        private final java.util.concurrent.atomic.AtomicBoolean traceServed = new java.util.concurrent.atomic.AtomicBoolean();
        private final java.util.concurrent.atomic.AtomicBoolean spanServed = new java.util.concurrent.atomic.AtomicBoolean();

        @Override
        public String generateTraceId() {
            return traceServed.compareAndSet(false, true) ? Fixtures.TRACE_OK_ID : IdGenerator.random().generateTraceId();
        }

        @Override
        public String generateSpanId() {
            return spanServed.compareAndSet(false, true) ? Fixtures.TRACE_OK_SPAN_ID : IdGenerator.random().generateSpanId();
        }

        /**
         * Builds a SpanContext carrying the fixture ids so the OTLP log exporter serialises trace_id /
         * span_id correctly on the payload log record (OTel logs SDK reads them from the active context
         * rather than from the LogRecordBuilder).
         */
        static io.opentelemetry.api.trace.SpanContext fixtureSpanContext(String traceId, String spanId) {
            return io.opentelemetry.api.trace.SpanContext.create(
                traceId,
                spanId,
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
            );
        }
    }
}
