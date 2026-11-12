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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * An LLM total adds the two values of a request, sent and received. What it measures is pinned on real documents by
 * {@code AnalyticsElasticsearchRepositoryBuiltInMeasuresTest}; this pins the aggregation each measure is read with.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LLMTotalBuilderTest {

    private final LLMTotalBuilder builder = new LLMTotalBuilder("additional-metrics.long_sent", "additional-metrics.long_received");

    @ParameterizedTest
    @CsvSource({ "COUNT, value_count", "SUM, sum", "AVG, avg" })
    void should_aggregate_the_total_of_each_request_with_a_script(Measure measure, String type) {
        var agg = builder.build("TOTAL#" + measure, measure).orElseThrow().get("TOTAL#" + measure);

        var source = agg.getJsonObject(type).getJsonObject("script").getString("source");
        assertThat(source).contains("doc['additional-metrics.long_sent']").contains("doc['additional-metrics.long_received']");
    }

    @ParameterizedTest
    @EnumSource(value = Measure.class, names = { "MIN", "MAX", "P50", "P90", "P95", "P99", "PERCENTAGE" })
    void should_not_aggregate_any_other_measure(Measure measure) {
        assertThat(builder.build("TOTAL#" + measure, measure)).isEmpty();
    }

    @Test
    void should_select_the_requests_carrying_either_value() {
        assertThat(builder.carrying()).isEqualTo(
            JsonObject.of(
                "bool",
                JsonObject.of(
                    "should",
                    JsonArray.of(
                        JsonObject.of("exists", JsonObject.of("field", "additional-metrics.long_sent")),
                        JsonObject.of("exists", JsonObject.of("field", "additional-metrics.long_received"))
                    ),
                    "minimum_should_match",
                    1
                )
            )
        );
    }
}
