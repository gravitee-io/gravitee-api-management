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
package io.gravitee.repository.elasticsearch.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.repository.log.model.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link LogBuilder}.
 *
 * <p>Covers the resilience of {@code createLog} to malformed {@code @timestamp}
 * values observed in production (notably from log ingestion pipelines that
 * inject pseudo-scientific-notation strings into {@code @timestamp}).
 *
 * @author GraviteeSource Team
 */
class LogBuilderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String VALID_TIMESTAMP = "2026-06-02T07:42:51.312Z";

    /**
     * Minimal valid log document with all the fields {@link LogBuilder#createLog} accesses.
     */
    private static final String VALID_LOG_JSON =
        "{" +
        "\"@timestamp\":\"" +
        VALID_TIMESTAMP +
        "\"," +
        "\"transaction\":\"tx-1\"," +
        "\"gateway\":\"gw-1\"," +
        "\"uri\":\"/foo\"," +
        "\"method\":1," +
        "\"status\":200," +
        "\"response-time\":42," +
        "\"local-address\":\"127.0.0.1\"," +
        "\"remote-address\":\"127.0.0.1\"" +
        "}";

    @Test
    void createLog_parses_valid_iso8601_timestamp() throws Exception {
        Log result = LogBuilder.createLog(hitFromJson("id-ok", VALID_LOG_JSON));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("id-ok");
        assertThat(result.getTimestamp()).isEqualTo(1780386171312L);
        assertThat(result.getTransactionId()).isEqualTo("tx-1");
        assertThat(result.getGateway()).isEqualTo("gw-1");
    }

    @Test
    void createLog_returns_null_when_timestamp_node_is_missing() throws Exception {
        String json = "{" + "\"transaction\":\"tx-1\"," + "\"gateway\":\"gw-1\"" + "}";

        Log result = LogBuilder.createLog(hitFromJson("id-missing-ts", json));

        assertThat(result).isNull();
    }

    @Test
    void createLog_returns_null_when_timestamp_is_json_null() throws Exception {
        String json = "{" + "\"@timestamp\":null," + "\"transaction\":\"tx-1\"," + "\"gateway\":\"gw-1\"" + "}";

        Log result = LogBuilder.createLog(hitFromJson("id-null-ts", json));

        assertThat(result).isNull();
    }

    @ParameterizedTest(name = "createLog returns null for blank @timestamp = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n" })
    void createLog_returns_null_when_timestamp_is_blank(String blankValue) throws Exception {
        String value = (blankValue == null) ? "null" : "\"" + blankValue.replace("\t", "\\t").replace("\n", "\\n") + "\"";
        String json = "{" + "\"@timestamp\":" + value + "," + "\"transaction\":\"tx-1\"," + "\"gateway\":\"gw-1\"" + "}";

        Log result = LogBuilder.createLog(hitFromJson("id-blank-ts", json));

        assertThat(result).isNull();
    }

    /**
     * Real-world malformed values observed at a SaaS customer (OldMutual). All
     * trigger {@link java.lang.NumberFormatException} inside
     * {@code SimpleDateFormat.parse} via {@code FloatingDecimal} / {@code DigitList},
     * which the buggy revision of {@link LogBuilder} did not catch.
     */
    @ParameterizedTest(name = "createLog returns null for malformed @timestamp = [{0}]")
    @ValueSource(strings = { ".22EE1", ".122EE2", ".2E2026", ".2202026EE4", ".202E202E1", "4482026.E44482026E4", "not-a-date" })
    void createLog_returns_null_when_timestamp_is_malformed(String malformedTimestamp) throws Exception {
        String json = "{" + "\"@timestamp\":\"" + malformedTimestamp + "\"," + "\"transaction\":\"tx-1\"," + "\"gateway\":\"gw-1\"" + "}";

        Log result = LogBuilder.createLog(hitFromJson("id-bad-ts", json));

        assertThat(result).isNull();
    }

    @Test
    void createLog_does_not_throw_NumberFormatException_for_scientific_like_value() {
        // Guard against regression: prior code propagated NFE because the catch
        // only handled ParseException, taking down the whole /apis/{id}/logs response.
        String json = "{" + "\"@timestamp\":\".122EE2\"," + "\"transaction\":\"tx-1\"," + "\"gateway\":\"gw-1\"" + "}";

        // Must not throw — must return null instead.
        Log result = LogBuilder.createLog(hitFromJson("id-nfe", json));

        assertThat(result).isNull();
    }

    private static SearchHit hitFromJson(String id, String json) {
        try {
            SearchHit hit = new SearchHit();
            hit.setId(id);
            JsonNode source = OBJECT_MAPPER.readTree(json);
            hit.setSource(source);
            return hit;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build SearchHit for test", e);
        }
    }
}
