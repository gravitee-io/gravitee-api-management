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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TIMESTAMP;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;

public class TimeRangeAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String GTE = "gte";
    public static final String LTE = "lte";
    public static final String RANGE = "range";

    /**
     * Use {@code gte} and {@code lte} for ranges; {@code from}, {@code to}, {@code include_lower}, and {@code include_upper} are deprecated.
     * See {@link #createRangeFilterNode} for more details.
     */
    public static ObjectNode toRangeNode(TimeRange timeRange) {
        ObjectNode rangeNode = MAPPER.createObjectNode();
        ObjectNode tsRange = MAPPER.createObjectNode();
        tsRange.put("from", timeRange.from().toEpochMilli());
        tsRange.put("to", timeRange.to().toEpochMilli());
        tsRange.put("include_lower", true);
        tsRange.put("include_upper", true);
        rangeNode.set(TIMESTAMP, tsRange);
        return MAPPER.createObjectNode().set(RANGE, rangeNode);
    }

    public static ObjectNode createRangeFilterNode(TimeRange range) {
        ObjectNode rangeNode = MAPPER.createObjectNode();
        ObjectNode tsRange = MAPPER.createObjectNode();
        tsRange.put(GTE, range.from().toEpochMilli());
        tsRange.put(LTE, range.to().toEpochMilli());
        rangeNode.set(TIMESTAMP, tsRange);

        return MAPPER.createObjectNode().set(RANGE, rangeNode);
    }

    public static ObjectNode createTimestampAggregationNode(io.gravitee.repository.log.v4.model.analytics.HistogramQuery query) {
        long intervalMillis = query
            .timeRange()
            .interval()
            .orElseThrow(() -> new IllegalArgumentException("Interval is required for Timestamp aggregation"))
            .toMillis();

        ObjectNode byDate = MAPPER.createObjectNode();
        ObjectNode dateHistogram = MAPPER.createObjectNode();
        dateHistogram.put("field", "@timestamp");
        dateHistogram.put("fixed_interval", intervalMillis + "ms");
        dateHistogram.put("min_doc_count", 0);
        ObjectNode extendedBounds = MAPPER.createObjectNode();
        extendedBounds.put("min", query.timeRange().from().toEpochMilli());
        extendedBounds.put("max", query.timeRange().to().toEpochMilli());
        dateHistogram.set("extended_bounds", extendedBounds);
        byDate.set("date_histogram", dateHistogram);

        return byDate;
    }
}
