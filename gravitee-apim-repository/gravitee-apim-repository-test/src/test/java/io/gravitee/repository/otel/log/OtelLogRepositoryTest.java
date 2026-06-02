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
package io.gravitee.repository.otel.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared contract for {@link OtelLogRepository} implementations. Log records aren't writable through the
 * SPI, so this test doesn't drive a JSON-based create flow — implementations must seed the
 * {@link Fixtures fixture} records into their backend from {@code TestRepositoryInitializer.setUp()}
 * before each test.
 * <p>
 * The single query method is exercised against the seeded fixtures: one trace with two span-event docs
 * ({@code event.name} populated, no body) and one payload-log doc (body populated, no {@code event.name}).
 * Implementations whose backend doesn't store span events (Loki) should override the seed to skip them —
 * the timestamp-ordering assertion would then short-circuit on the payload log only.
 *
 * @author GraviteeSource Team
 */
public abstract class OtelLogRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected OtelLogRepository otelLogRepository;

    /** Fixed tenant context every assertion uses. */
    protected static final QueryContext QUERY_CONTEXT = new QueryContext(Fixtures.ORG_ID, Fixtures.ENV_ID);

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
    public void should_return_all_logs_for_known_trace_in_timestamp_ascending_order() {
        List<OtelLogRecord> records = otelLogRepository
            .findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria(Fixtures.TRACE_OK_ID, Map.of(), Map.of(), null, null, null))
            .blockingGet();

        // Fixtures: 2 span events (with event.name attribute, no body) then 1 payload log (body populated,
        // no event.name). Single query returns everything for the trace.
        assertThat(records).hasSize(3);
        assertThat(records).allMatch(r -> Fixtures.TRACE_OK_SPAN_ID.equals(r.spanId()));

        OtelLogRecord first = records.get(0);
        OtelLogRecord second = records.get(1);
        OtelLogRecord third = records.get(2);

        assertThat(first.attributes()).containsEntry("event.name", Fixtures.EVENT_PRE_NAME);
        assertThat(first.attributes()).containsEntry("gravitee.policy.id", Fixtures.EVENT_PRE_POLICY_ID);
        assertThat(first.body()).isNull();

        assertThat(second.attributes()).containsEntry("event.name", Fixtures.EVENT_POST_NAME);
        assertThat(second.body()).isNull();

        // Payload log: body populated, no event.name attribute — the discriminator consumers use.
        assertThat(third.body()).isEqualTo(Fixtures.PAYLOAD_BODY);
        assertThat(third.attributes()).doesNotContainKey("event.name");
    }

    @Test
    public void should_return_empty_list_when_trace_id_is_unknown() {
        List<OtelLogRecord> records = otelLogRepository
            .findLogs(QUERY_CONTEXT, new OtelLogSearchCriteria(Fixtures.UNKNOWN_TRACE_ID, Map.of(), Map.of(), null, null, null))
            .blockingGet();

        assertThat(records).isEmpty();
    }

    /**
     * Fixture records the implementation's {@code TestRepositoryInitializer.setUp()} must seed in the
     * backend before each test runs. Defined as a single source of truth so impl-side seeding and
     * assertion expectations stay aligned.
     */
    public static final class Fixtures {

        public static final String ORG_ID = "test-org";
        public static final String ENV_ID = "test-env";

        public static final String TRACE_OK_ID = "a1b2c3d4e5f60718293a4b5c6d7e8f01";
        public static final String TRACE_OK_SPAN_ID = "11223344aabbccdd";

        public static final String EVENT_PRE_NAME = "gravitee.policy.pre";
        public static final String EVENT_PRE_POLICY_ID = "policy-rate-limit";

        public static final String EVENT_POST_NAME = "gravitee.policy.post";

        public static final String PAYLOAD_BODY = "{\"hello\":\"world\"}";

        /** Valid-shape trace id (32-hex) that no fixture seeds — used by the not-found test. */
        public static final String UNKNOWN_TRACE_ID = "deadbeef0000000000000000beefdead";

        private Fixtures() {}
    }
}
