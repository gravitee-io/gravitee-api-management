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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation;

import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPRPSBuilder {

    private static final String SCRIPT_SOURCE = """
          params.min_time != null &&
          params.max_time != null &&
          (params.max_time - params.min_time) > 0 ?
              params.count / ((params.max_time - params.min_time) / 1000)
              : 0
        """;

    private final SingleDateHistogramBucketBuilder singleBucketBuilder = new SingleDateHistogramBucketBuilder();

    public Map<String, JsonObject> build(String aggName, String field) {
        return Map.of("_" + aggName, json().put("date_histogram", singleBucketBuilder.build()).put("aggs", aggs(aggName)));
    }

    private JsonObject aggs(String aggName) {
        return json().put("_rps_count", valueCount()).put("_time_range", timeRange()).put(aggName, bucketScript());
    }

    private JsonObject bucketScript() {
        return json().put("bucket_script", json().put("buckets_path", bucketPath()).put("script", script()));
    }

    private JsonObject bucketPath() {
        return json().put("count", "_rps_count").put("min_time", "_time_range.min").put("max_time", "_time_range.max");
    }

    private JsonObject script() {
        return json().put("source", SCRIPT_SOURCE);
    }

    private JsonObject valueCount() {
        return json().put("value_count", json().put("field", "@timestamp"));
    }

    private JsonObject timeRange() {
        return json().put("stats", json().put("field", "@timestamp"));
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
