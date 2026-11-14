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

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SimplePercentileBuilder {

    /** The percent a percentile measure reads, which also keys its value in the response; empty for any other measure. */
    public static Optional<Double> percent(Measure measure) {
        return switch (measure) {
            case P50 -> Optional.of(50.0);
            case P90 -> Optional.of(90.0);
            case P95 -> Optional.of(95.0);
            case P99 -> Optional.of(99.0);
            case COUNT, AVG, MIN, MAX, PERCENTAGE, SUM -> Optional.empty();
        };
    }

    public static Map<String, JsonObject> build(String aggName, String field, Measure percentile) {
        var percents = JsonArray.of(
            percent(percentile).orElseThrow(() -> new IllegalArgumentException(percentile + " is not a percentile"))
        );
        return Map.of(aggName, JsonObject.of(Aggs.PERCENTILES, JsonObject.of(Tokens.FIELD, field, Aggs.PERCENTS, percents)));
    }
}
