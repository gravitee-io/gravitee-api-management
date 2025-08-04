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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimeRangeAdapterTest {

    @Test
    void shouldConvertTimeRangeToRangeNode() {
        Instant from = Instant.parse("2025-07-01T00:00:00Z");
        Instant to = Instant.parse("2025-07-02T00:00:00Z");
        TimeRange timeRange = new TimeRange(from, to, Optional.of(Duration.ofHours(1)));
        ObjectNode node = TimeRangeAdapter.toRangeNode(timeRange);
        assertNotNull(node);
        assertTrue(node.has("range"));
        ObjectNode range = (ObjectNode) node.get("range");
        assertTrue(range.has("@timestamp"));
        ObjectNode ts = (ObjectNode) range.get("@timestamp");
        assertEquals(from.toEpochMilli(), ts.get("from").asLong());
        assertEquals(to.toEpochMilli(), ts.get("to").asLong());
        assertTrue(ts.get("include_lower").asBoolean());
        assertTrue(ts.get("include_upper").asBoolean());
    }

    @Test
    void shouldCreateTimestampAggregationNode() {
        Instant from = Instant.parse("2025-07-01T00:00:00Z");
        Instant to = Instant.parse("2025-07-02T00:00:00Z");
        Duration interval = Duration.ofHours(1);
        var query = new io.gravitee.repository.log.v4.model.analytics.HistogramQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, "api-1"),
            new TimeRange(from, to, interval),
            java.util.List.of(),
            Optional.empty()
        );
        ObjectNode node = TimeRangeAdapter.createTimestampAggregationNode(query);
        assertNotNull(node);
        assertTrue(node.has("date_histogram"));
        ObjectNode hist = (ObjectNode) node.get("date_histogram");
        assertEquals("@timestamp", hist.get("field").asText());
        assertEquals(interval.toMillis() + "ms", hist.get("fixed_interval").asText());
        assertEquals(0, hist.get("min_doc_count").asInt());
        ObjectNode bounds = (ObjectNode) hist.get("extended_bounds");
        assertEquals(from.toEpochMilli(), bounds.get("min").asLong());
        assertEquals(to.toEpochMilli(), bounds.get("max").asLong());
    }

    @Test
    void shouldThrowExceptionForTimestampAggregationNodeWithNullInterval() {
        Instant from = Instant.parse("2025-07-01T00:00:00Z");
        Instant to = Instant.parse("2025-07-02T00:00:00Z");
        var query = new io.gravitee.repository.log.v4.model.analytics.HistogramQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, "api-1"),
            new TimeRange(from, to),
            java.util.List.of(),
            Optional.empty()
        );
        assertThrows(IllegalArgumentException.class, () -> TimeRangeAdapter.createTimestampAggregationNode(query));
    }
}
