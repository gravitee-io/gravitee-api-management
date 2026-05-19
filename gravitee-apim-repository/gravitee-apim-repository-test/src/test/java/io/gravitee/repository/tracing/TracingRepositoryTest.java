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
package io.gravitee.repository.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared contract for {@link TracingRepository} implementations. Traces aren't writable through the SPI, so this test
 * doesn't drive a JSON-based create flow — implementations must seed the {@link Fixtures fixture} traces into their
 * backend from {@code TestRepositoryInitializer.setUp()} before each test.
 *
 * @author GraviteeSource Team
 */
public abstract class TracingRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected TracingRepository tracingRepository;

    /**
     * Fixed query context used by every assertion. Implementations whose backend is tenant-aware (Elasticsearch index
     * template substitution; future Tempo {@code X-Scope-OrgID}) must wire their fixture-seeding pipeline so the
     * fixtures land where this org/env resolves to.
     */
    protected static final QueryContext QUERY_CONTEXT = new QueryContext(Fixtures.ORG_ID, Fixtures.ENV_ID);

    /**
     * Tracing has no write-side SPI, so the standard JSON-driven {@code createModel} flow doesn't apply. Implementations
     * push the {@link Fixtures} traces into the backend from their {@code TestRepositoryInitializer.setUp()} instead.
     */
    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Override
    protected String getModelPackage() {
        return null;
    }

    @Override
    protected void createModel(Object object) {
        // no-op: see class-level Javadoc
    }

    @Test
    public void should_search_traces_matching_service_tag() {
        List<Trace> traces = tracingRepository
            .searchTraces(QUERY_CONTEXT, new TraceSearchCriteria(Map.of("service.name", Fixtures.SERVICE_NAME), 20, null, null, Map.of()))
            .blockingGet();

        assertThat(traces).extracting(Trace::traceId).containsExactlyInAnyOrder(Fixtures.TRACE_OK_ID, Fixtures.TRACE_ERROR_ID);
    }

    @Test
    public void should_return_empty_list_when_no_trace_matches() {
        List<Trace> traces = tracingRepository
            .searchTraces(
                QUERY_CONTEXT,
                new TraceSearchCriteria(Map.of("service.name", "service-that-does-not-exist"), 20, null, null, Map.of())
            )
            .blockingGet();

        assertThat(traces).isEmpty();
    }

    @Test
    public void should_get_trace_by_id_with_full_span_tree() {
        Trace trace = tracingRepository.getTrace(QUERY_CONTEXT, Fixtures.TRACE_OK_ID, Map.of()).blockingGet();

        assertThat(trace).isNotNull();
        assertThat(trace.traceId()).isEqualTo(Fixtures.TRACE_OK_ID);
        assertThat(trace.rootService()).isEqualTo(Fixtures.SERVICE_NAME);
        assertThat(trace.rootOperation()).isEqualTo(Fixtures.TRACE_OK_ROOT_OPERATION);
        assertThat(trace.spans()).hasSize(1);
        assertThat(trace.spans().get(0).attributes()).containsEntry("http.status_code", "200");
    }

    @Test
    public void should_complete_empty_when_trace_id_is_unknown() {
        Trace trace = tracingRepository.getTrace(QUERY_CONTEXT, Fixtures.UNKNOWN_TRACE_ID, Map.of()).blockingGet();

        assertThat(trace).isNull();
    }

    /**
     * Trace fixtures the implementation's {@code TestRepositoryInitializer.setUp()} must seed in the backend before each
     * test runs. Defined as a single source of truth so impl-side seeding and assertion expectations stay aligned.
     * <p>
     * Trace IDs are 32-character lowercase hex with a non-zero leading byte: some backends (Tempo) strip leading-zero
     * bytes when echoing IDs back through their search API, which would break exact-match assertions on round-trip.
     */
    public static final class Fixtures {

        /**
         * Tenancy values used by {@link #QUERY_CONTEXT}. Implementations whose backend resolves indices/tenants from
         * the org id ({@code traces-apim.otel-{orgId}} for Elasticsearch; {@code X-Scope-OrgID} for Tempo) must seed
         * fixtures under this org so the contract tests find them.
         */
        public static final String ORG_ID = "test-org";
        public static final String ENV_ID = "test-env";

        public static final String SERVICE_NAME = "test-service";

        public static final String TRACE_OK_ID = "a1b2c3d4e5f60718293a4b5c6d7e8f01";
        public static final String TRACE_OK_ROOT_OPERATION = "GET /ok";
        public static final Instant TRACE_OK_START_TIME = Instant.parse("2026-04-30T10:00:00Z");

        public static final String TRACE_ERROR_ID = "a1b2c3d4e5f60718293a4b5c6d7e8f02";
        public static final String TRACE_ERROR_ROOT_OPERATION = "GET /error";
        public static final Instant TRACE_ERROR_START_TIME = Instant.parse("2026-04-30T10:01:00Z");

        /** Valid-shape trace id (32-hex) that no fixture seeds — used by the not-found test. */
        public static final String UNKNOWN_TRACE_ID = "deadbeef0000000000000000beefdead";

        private Fixtures() {}
    }
}
