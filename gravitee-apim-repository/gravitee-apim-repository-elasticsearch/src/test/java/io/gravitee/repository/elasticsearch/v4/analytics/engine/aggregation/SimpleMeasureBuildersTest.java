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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The aggregations every built-in measure of a field is read with, on every search path that builds them: none counts a
 * document without the field, as Elasticsearch does by default.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SimpleMeasureBuildersTest {

    private static final String FIELD = "gateway-latency-ms";

    @ParameterizedTest(name = "{0}")
    @MethodSource("builders")
    void should_aggregate_only_the_documents_carrying_the_field(
        String name,
        BiFunction<String, String, Map<String, JsonObject>> builder,
        JsonObject expected
    ) {
        assertThat(builder.apply("METRIC#MEASURE", FIELD)).isEqualTo(Map.of("METRIC#MEASURE", expected));
    }

    static Stream<Arguments> builders() {
        return Stream.of(
            Arguments.of("AVG", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleAVGBuilder()::build, single("avg")),
            Arguments.of("MIN", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleMinBuilder()::build, single("min")),
            Arguments.of("MAX", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleMaxBuilder()::build, single("max")),
            Arguments.of("SUM", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleSUMBuilder()::build, single("sum")),
            Arguments.of("P50", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleP50Builder()::build, percentile(50.0)),
            Arguments.of("P90", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleP90Builder()::build, percentile(90.0)),
            Arguments.of("P95", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleP95Builder()::build, percentile(95.0)),
            Arguments.of("P99", (BiFunction<String, String, Map<String, JsonObject>>) new SimpleP99Builder()::build, percentile(99.0))
        );
    }

    private static JsonObject single(String type) {
        return JsonObject.of(type, JsonObject.of("field", FIELD));
    }

    private static JsonObject percentile(double percent) {
        return JsonObject.of("percentiles", JsonObject.of("field", FIELD, "percents", JsonArray.of(percent)));
    }
}
