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

import static io.gravitee.repository.elasticsearch.tracing.ElasticsearchTracingTestRepositoryConfiguration.OTEL_COLLECTOR_OTLP_HTTP_PORT;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.awaitility.Awaitility.await;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.config.TestRepositoryInitializer;
import io.gravitee.repository.tracing.TracingRepositoryTest.Fixtures;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.GenericContainer;

/**
 * Pushes the {@link Fixtures} traces into the OTel Collector via OTLP HTTP, then waits for them to become queryable
 * through the production {@link ElasticsearchTracingRepository} (which queries Elasticsearch directly). The collector
 * forwards the spans to ES through its {@code elasticsearch} exporter in {@code mapping.mode: otel}, so the
 * production repository's snake_case field parser sees the same document layout it does at runtime.
 * <p>
 * Seeding runs once across the suite — ES persists ingested spans, so re-pushing on every test would inflate
 * {@code searchTraces} results.
 *
 * @author GraviteeSource Team
 */
public class ElasticsearchTracingTestRepositoryInitializer implements TestRepositoryInitializer {

    private static final Duration QUERYABLE_POLL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration QUERYABLE_POLL_INTERVAL = Duration.ofMillis(500);

    private final GenericContainer<?> otelCollectorContainer;
    private final TracingRepository tracingRepository;

    private boolean seeded = false;

    @Autowired
    public ElasticsearchTracingTestRepositoryInitializer(
        GenericContainer<?> tracingTestOtelCollectorContainer,
        TracingRepository tracingRepository
    ) {
        this.otelCollectorContainer = tracingTestOtelCollectorContainer;
        this.tracingRepository = tracingRepository;
    }

    @Override
    public void setUp() {
        if (seeded) {
            return;
        }
        seedFixtureTraces();
        awaitTracesQueryable();
        seeded = true;
    }

    @Override
    public void tearDown() {
        // ES storage is bound to the container lifecycle (managed by the Spring context), so per-test cleanup is
        // unnecessary — the suite seeds once and tests share the read-only fixture set.
    }

    private void seedFixtureTraces() {
        String otlpEndpoint =
            "http://" +
            otelCollectorContainer.getHost() +
            ":" +
            otelCollectorContainer.getMappedPort(OTEL_COLLECTOR_OTLP_HTTP_PORT) +
            "/v1/traces";

        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder().setEndpoint(otlpEndpoint).build();
        // The contract test calls with QueryContext("test-org", "test-env"), so the impl auto-injects an env clause
        // (resource.attributes.gravitee.environment.id = "test-env"). The fixture spans must carry that resource
        // attribute or the env filter will reject them.
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(
                Resource.create(
                    Attributes.of(stringKey("service.name"), Fixtures.SERVICE_NAME, stringKey("gravitee.environment.id"), Fixtures.ENV_ID)
                )
            )
            .setIdGenerator(new FixtureIdGenerator())
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();

        try (OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()) {
            Tracer tracer = sdk.getTracer("elasticsearch-tracing-test-fixtures");

            long okStartNanos = nanos(Fixtures.TRACE_OK_START_TIME.getEpochSecond());
            Span okSpan = tracer
                .spanBuilder(Fixtures.TRACE_OK_ROOT_OPERATION)
                .setStartTimestamp(okStartNanos, TimeUnit.NANOSECONDS)
                .setAttribute("http.status_code", "200")
                .startSpan();
            okSpan.end(okStartNanos + Duration.ofMillis(5).toNanos(), TimeUnit.NANOSECONDS);

            long errorStartNanos = nanos(Fixtures.TRACE_ERROR_START_TIME.getEpochSecond());
            Span errorSpan = tracer
                .spanBuilder(Fixtures.TRACE_ERROR_ROOT_OPERATION)
                .setStartTimestamp(errorStartNanos, TimeUnit.NANOSECONDS)
                .setAttribute("http.status_code", "500")
                .startSpan();
            errorSpan.setStatus(StatusCode.ERROR);
            errorSpan.end(errorStartNanos + Duration.ofMillis(7).toNanos(), TimeUnit.NANOSECONDS);

            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        }
    }

    private void awaitTracesQueryable() {
        QueryContext queryContext = new QueryContext(Fixtures.ORG_ID, Fixtures.ENV_ID);
        TraceSearchCriteria criteria = new TraceSearchCriteria(Map.of("service.name", Fixtures.SERVICE_NAME), 20, null, null, Map.of());

        await()
            .atMost(QUERYABLE_POLL_TIMEOUT)
            .pollInterval(QUERYABLE_POLL_INTERVAL)
            .until(() -> {
                List<Trace> traces = tracingRepository.searchTraces(queryContext, criteria).blockingGet();
                return traces.size() >= 2;
            });
    }

    private static long nanos(long epochSeconds) {
        return epochSeconds * 1_000_000_000L;
    }

    /**
     * Hands out the contract's fixture trace IDs to the OTel SDK in the order it asks. Each trace's first span emits
     * {@link IdGenerator#generateTraceId()} once; subsequent calls fall back to random IDs. Order matches the
     * sequence in which {@link #seedFixtureTraces()} starts spans (OK first, then error).
     */
    private static final class FixtureIdGenerator implements IdGenerator {

        private final AtomicInteger traceCursor = new AtomicInteger();
        private final AtomicInteger spanCursor = new AtomicInteger();

        @Override
        public String generateTraceId() {
            int index = traceCursor.getAndIncrement();
            return switch (index) {
                case 0 -> Fixtures.TRACE_OK_ID;
                case 1 -> Fixtures.TRACE_ERROR_ID;
                default -> IdGenerator.random().generateTraceId();
            };
        }

        @Override
        public String generateSpanId() {
            int index = spanCursor.getAndIncrement();
            return switch (index) {
                case 0 -> "0000000000000a01";
                case 1 -> "0000000000000a02";
                default -> IdGenerator.random().generateSpanId();
            };
        }
    }
}
