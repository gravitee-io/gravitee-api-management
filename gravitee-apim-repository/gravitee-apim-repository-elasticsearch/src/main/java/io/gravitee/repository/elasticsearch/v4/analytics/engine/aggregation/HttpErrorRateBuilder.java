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
public class HttpErrorRateBuilder {

    private static final String SCRIPT_SOURCE = """
        (params.success + params.error) > 0 ? params.error / (params.success + params.error) * 100 : 0
        """;

    private final SingleDateHistogramBucketBuilder singleBucketBuilder = new SingleDateHistogramBucketBuilder();

    public Map<String, JsonObject> build(String aggName, String field) {
        return Map.of("_" + aggName, json().put("date_histogram", singleBucketBuilder.build()).put("aggs", aggs(aggName)));
    }

    private JsonObject aggs(String aggName) {
        return json().put("_success_count", successCount()).put("_error_count", errorCount()).put(aggName, bucketScript());
    }

    private JsonObject bucketScript() {
        return json().put("bucket_script", json().put("buckets_path", bucketPath()).put("script", script()));
    }

    private JsonObject bucketPath() {
        return json().put("success", "_success_count>count").put("error", "_error_count>count");
    }

    private JsonObject script() {
        return json().put("source", SCRIPT_SOURCE);
    }

    private JsonObject successCount() {
        return json().put("filter", statusFilter(100, 400)).put("aggs", json().put("count", valueCount()));
    }

    private JsonObject errorCount() {
        return json().put("filter", statusFilter(400, 600)).put("aggs", json().put("count", valueCount()));
    }

    private JsonObject statusFilter(int gte, int lt) {
        return json().put("range", json().put("status", json().put("gte", gte).put("lt", lt)));
    }

    private JsonObject valueCount() {
        return json().put("value_count", json().put("field", "@timestamp"));
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
