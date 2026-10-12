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
package io.gravitee.apim.rest.api.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The tool contracts spell the engines' vocabularies out for the model; they must move when the engines do.
 */
class ToolContractEnumsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    static Stream<Arguments> engine_vocabularies() {
        return Stream.of(
            Arguments.of("describe_metrics", "/inputSchema/properties/apiType", ApiSpec.Name.class),
            Arguments.of("describe_metrics", "/inputSchema/properties/metric", MetricSpec.Name.class),
            Arguments.of("describe_metrics", "/outputSchema/$defs/metric/properties/unit", MetricSpec.Unit.class),
            Arguments.of("list_filter_values", "/inputSchema/properties/filter", FilterSpec.Name.class),
            Arguments.of("query_analytics", "/inputSchema/properties/metrics/items/properties/name", MetricSpec.Name.class),
            Arguments.of("query_analytics", "/inputSchema/properties/metrics/items/properties/measures/items", MetricSpec.Measure.class),
            Arguments.of(
                "query_analytics",
                "/inputSchema/properties/metrics/items/properties/filters/items/properties/name",
                FilterSpec.Name.class
            ),
            Arguments.of(
                "query_analytics",
                "/inputSchema/properties/metrics/items/properties/filters/items/properties/operator",
                FilterOperator.class
            ),
            Arguments.of(
                "query_analytics",
                "/inputSchema/properties/metrics/items/properties/sorts/items/properties/measure",
                MetricSpec.Measure.class
            ),
            Arguments.of(
                "query_analytics",
                "/inputSchema/properties/metrics/items/properties/sorts/items/properties/order",
                FacetMetricMeasuresRequest.Sort.Order.class
            ),
            Arguments.of("query_analytics", "/inputSchema/properties/filters/items/properties/name", FilterSpec.Name.class),
            Arguments.of("query_analytics", "/inputSchema/properties/filters/items/properties/operator", FilterOperator.class),
            Arguments.of("query_analytics", "/inputSchema/properties/by/items", FacetSpec.Name.class),
            Arguments.of("search_logs", "/inputSchema/properties/filters/items/properties/name", FilterName.class),
            Arguments.of("search_logs", "/inputSchema/properties/filters/items/properties/operator", Operator.class)
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("engine_vocabularies")
    void should_list_exactly_the_values_the_engine_knows(String tool, String pointer, Class<? extends Enum<?>> vocabulary)
        throws IOException {
        var listed = StreamSupport.stream(contract(tool).at(pointer).path("enum").spliterator(), false).map(JsonNode::asText).toList();

        var known = Arrays.stream(vocabulary.getEnumConstants()).map(Enum::name).toList();

        assertThat(listed).as("%s %s", tool, pointer).containsExactlyInAnyOrderElementsOf(known);
    }

    private static JsonNode contract(String tool) throws IOException {
        try (var stream = ToolContractEnumsTest.class.getResourceAsStream("/mcp/tools/" + tool + ".json")) {
            return JSON.readTree(stream);
        }
    }
}
