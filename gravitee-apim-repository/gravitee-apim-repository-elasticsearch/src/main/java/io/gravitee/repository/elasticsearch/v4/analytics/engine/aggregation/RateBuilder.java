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
import java.time.Duration;
import java.util.Map;

/**
 * Documents per second over {@code window}: the count of {@code field} divided by the window length. Like
 * {@link HttpErrorRateBuilder}, the pipeline aggregation is wrapped in a single-bucket date histogram so it can sit
 * at the top level of a query as well as under a facet or time-series bucket.
 */
public class RateBuilder {

    private static final String SCRIPT_SOURCE = "params.seconds > 0 ? params.count / params.seconds : 0";

    private final SingleDateHistogramBucketBuilder singleBucketBuilder = new SingleDateHistogramBucketBuilder();

    public Map<String, JsonObject> build(String aggName, String field, Duration window) {
        return Map.of("_" + aggName, json().put("date_histogram", singleBucketBuilder.build()).put("aggs", aggs(aggName, field, window)));
    }

    private JsonObject aggs(String aggName, String field, Duration window) {
        return json().put("_count", json().put("value_count", json().put("field", field))).put(aggName, bucketScript(window));
    }

    private JsonObject bucketScript(Duration window) {
        var script = json().put("source", SCRIPT_SOURCE).put("params", json().put("seconds", window.toMillis() / 1000.0));
        return json().put("bucket_script", json().put("buckets_path", json().put("count", "_count")).put("script", script));
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
