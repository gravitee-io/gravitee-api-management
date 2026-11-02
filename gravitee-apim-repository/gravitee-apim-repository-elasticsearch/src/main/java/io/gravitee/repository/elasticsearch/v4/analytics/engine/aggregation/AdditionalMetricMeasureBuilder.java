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
 * The aggregations of a numeric additional metric field. Unlike the built-in builders, none counts a document without
 * the field as a zero: a plugin field present on a few requests must not average zeros in. So an average, a bound or
 * a percentile is Elasticsearch's own {@code null} when no document carries the field, a sum and a count are 0.
 */
public class AdditionalMetricMeasureBuilder {

    public Optional<Map<String, JsonObject>> build(String aggName, String field, Measure measure) {
        return switch (measure) {
            case AVG -> Optional.of(single(aggName, Aggs.AVG, field));
            case MIN -> Optional.of(single(aggName, Aggs.MIN, field));
            case MAX -> Optional.of(single(aggName, Aggs.MAX, field));
            case SUM -> Optional.of(single(aggName, Aggs.SUM, field));
            case COUNT -> Optional.of(single(aggName, Aggs.VALUE_COUNT, field));
            case P50, P90, P95, P99 -> SimplePercentileBuilder.percent(measure).map(percent -> percentile(aggName, field, percent));
            case PERCENTAGE -> Optional.empty();
        };
    }

    private static Map<String, JsonObject> single(String aggName, String type, String field) {
        return Map.of(aggName, JsonObject.of(type, JsonObject.of(Tokens.FIELD, field)));
    }

    private static Map<String, JsonObject> percentile(String aggName, String field, double percent) {
        return Map.of(aggName, JsonObject.of(Aggs.PERCENTILES, JsonObject.of(Tokens.FIELD, field, Aggs.PERCENTS, JsonArray.of(percent))));
    }
}
