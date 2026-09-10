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
 * Divides an aggregation by how many distinct values a keyword field holds — the counterpart of
 * {@link RateBuilder}, which divides by elapsed time rather than by a population.
 *
 * <p>It is what turns a per-row figure into a per-thing one. Cost per LLM <i>request</i> flatters an agent,
 * which calls the model several times to answer one question; cost per conversation says what answering the
 * question cost. Turns per conversation and model calls per turn are the same operation over other fields.
 *
 * <p>A {@code bucket_script} only runs inside a bucket, and a measures query has none, so the whole thing is
 * wrapped in a single all-encompassing date histogram — the same shape {@link RateBuilder} and
 * {@link HttpErrorRateBuilder} use, and which the response side already unwraps.
 *
 * @author GraviteeSource Team
 */
public class PerDistinctBuilder {

    /** Guarded: a window with rows but no distinct value would otherwise divide by zero. */
    private static final String SCRIPT_SOURCE = "params.divisor > 0 ? params.total / params.divisor : 0";

    private static final String TOTAL_AGG = "_total";
    private static final String DIVISOR_AGG = "_divisor";

    private final SingleDateHistogramBucketBuilder singleBucketBuilder = new SingleDateHistogramBucketBuilder();

    /** The keyword field whose distinct values are the denominator. */
    private final String divisorField;

    public PerDistinctBuilder(String divisorField) {
        this.divisorField = divisorField;
    }

    /**
     * @param aggName the name the caller reads the result back under.
     * @param total the aggregation to divide — a count of rows, a cardinality of another field, a sum.
     */
    public Map<String, JsonObject> build(String aggName, JsonObject total) {
        return Map.of("_" + aggName, json().put("date_histogram", singleBucketBuilder.build()).put("aggs", aggs(aggName, total)));
    }

    private JsonObject aggs(String aggName, JsonObject total) {
        return json().put(TOTAL_AGG, total).put(DIVISOR_AGG, divisor()).put(aggName, bucketScript());
    }

    private JsonObject divisor() {
        return json().put("cardinality", json().put("field", divisorField));
    }

    private JsonObject bucketScript() {
        return json().put("bucket_script", json().put("buckets_path", bucketPath()).put("script", script()));
    }

    private JsonObject bucketPath() {
        return json().put("total", TOTAL_AGG).put("divisor", DIVISOR_AGG);
    }

    private JsonObject script() {
        return json().put("source", SCRIPT_SOURCE);
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
