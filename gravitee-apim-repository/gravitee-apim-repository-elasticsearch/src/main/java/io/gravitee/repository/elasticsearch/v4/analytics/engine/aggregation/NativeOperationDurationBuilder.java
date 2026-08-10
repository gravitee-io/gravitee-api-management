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
 * Average duration of a native Kafka operation, in milliseconds.
 *
 * <p>The native reactor does not report individual latencies: every 5 seconds it flushes the
 * <em>sum</em> of the durations observed in the window, alongside the number of samples that made up
 * that sum. A plain {@code avg} on the duration field would therefore average the per-window sums —
 * a window with 100 operations would weigh the same as a window with one. The only sound average is
 * {@code sum(durations) / sum(samples)}, computed here with a {@code bucket_script}.
 *
 * <p>For the same reason percentiles are not offered: the stored values are window sums, so their
 * distribution says nothing about the distribution of operation latencies.
 *
 * <p>Like {@link HttpErrorRateBuilder}, the pipeline aggregation is wrapped in a single-bucket
 * {@code date_histogram} so the builder works unchanged at the root of a measures query and nested
 * inside the {@code date_histogram} of a time-series query.
 */
public class NativeOperationDurationBuilder {

    /** Nanoseconds to milliseconds; the reactor reports {@code Duration#toNanos}. */
    private static final String SCRIPT_SOURCE = """
        params.count > 0 ? params.duration / params.count / 1000000 : 0
        """;

    private static final String DURATION_SUM_AGG = "_duration_sum";
    private static final String SAMPLE_COUNT_SUM_AGG = "_sample_count_sum";

    private final SingleDateHistogramBucketBuilder singleBucketBuilder = new SingleDateHistogramBucketBuilder();

    public Map<String, JsonObject> build(String aggName, String durationField, String sampleCountField) {
        return Map.of(
            "_" + aggName,
            json().put("date_histogram", singleBucketBuilder.build()).put("aggs", aggs(aggName, durationField, sampleCountField))
        );
    }

    private JsonObject aggs(String aggName, String durationField, String sampleCountField) {
        return json()
            .put(DURATION_SUM_AGG, sum(durationField))
            .put(SAMPLE_COUNT_SUM_AGG, sum(sampleCountField))
            .put(aggName, bucketScript());
    }

    private JsonObject sum(String field) {
        return json().put("sum", json().put("field", field).put("missing", 0));
    }

    private JsonObject bucketScript() {
        return json().put("bucket_script", json().put("buckets_path", bucketPath()).put("script", script()));
    }

    private JsonObject bucketPath() {
        return json().put("duration", DURATION_SUM_AGG).put("count", SAMPLE_COUNT_SUM_AGG);
    }

    private JsonObject script() {
        return json().put("source", SCRIPT_SOURCE);
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
