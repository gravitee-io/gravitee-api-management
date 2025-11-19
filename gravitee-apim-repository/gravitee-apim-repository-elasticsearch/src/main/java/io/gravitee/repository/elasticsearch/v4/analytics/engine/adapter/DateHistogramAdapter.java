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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.vertx.core.json.JsonObject;
import java.sql.Time;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DateHistogramAdapter {

    public static JsonObject adapt(Long interval, TimeRange timeRange) {
        return fixedDateHistogram(interval, timeRange);
    }

    private static JsonObject fixedDateHistogram(Long interval, TimeRange timeRange) {
        return json().put(
            "date_histogram",
            field().put("fixed_interval", interval + "ms").put("min_doc_count", 0).put("extended_bounds", extendedBounds(timeRange))
        );
    }

    private static JsonObject field() {
        return json().put("field", "@timestamp");
    }

    private static JsonObject extendedBounds(TimeRange timeRange) {
        var min = timeRange.from().toEpochMilli();
        var max = timeRange.to().toEpochMilli();
        return json().put("min", min).put("max", max);
    }

    private static JsonObject json() {
        return new JsonObject();
    }
}
