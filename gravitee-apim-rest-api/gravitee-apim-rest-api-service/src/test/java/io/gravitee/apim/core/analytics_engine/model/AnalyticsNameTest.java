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
package io.gravitee.apim.core.analytics_engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import io.gravitee.apim.core.observability.model.AdditionalMetricField.ValueType;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsNameTest {

    private static final TimeRange TIME_RANGE = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
    private static final AdditionalMetricName TIER = new AdditionalMetricName(
        new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier")
    );

    @Test
    void should_read_a_built_in_name_as_its_constant() {
        assertThat(FilterName.parse("MCP_PROXY_TOOL")).contains(FilterSpec.Name.MCP_PROXY_TOOL);
        assertThat(FacetName.parse("MCP_PROXY_TOOL")).contains(FacetSpec.Name.MCP_PROXY_TOOL);
    }

    @Test
    void should_read_a_well_formed_dynamic_name_as_an_additional_metric_field() {
        assertThat(FilterName.parse("additional-metrics.keyword_fraud-check_tier")).contains(TIER);
        assertThat(FacetName.parse("additional-metrics.keyword_fraud-check_tier")).contains(TIER);
    }

    @Test
    void should_keep_the_dynamic_form_of_a_key_a_built_in_reads() {
        assertThat(FilterName.parse("additional-metrics.keyword_mcp-proxy_tools/call")).contains(
            new AdditionalMetricName(new AdditionalMetricField(ValueType.KEYWORD, "keyword_mcp-proxy_tools/call"))
        );
    }

    @Test
    void should_accept_the_name_reserved_for_typed_custom_metrics() {
        assertThat(FilterName.parse("additional-metrics.double_custom_basket-value")).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = { "custom.basket-value", "UNKNOWN_NAME", "additional-metrics.float_ratio", "api" })
    void should_read_nothing_from_a_name_that_is_neither(String name) {
        assertThat(FilterName.parse(name)).isEmpty();
        assertThat(FacetName.parse(name)).isEmpty();
    }

    @Test
    void should_write_a_dynamic_name_back_on_the_wire() {
        assertThat(TIER.name()).isEqualTo("additional-metrics.keyword_fraud-check_tier");
    }

    @Test
    void should_report_a_skipped_additional_metric_under_its_wire_name() throws JsonProcessingException {
        var metric = new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(), List.of(FilterSpec.Name.API, TIER));

        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(metric));

        assertThat(json.at("/ignoredFilters").toString()).isEqualTo("[\"API\",\"additional-metrics.keyword_fraud-check_tier\"]");
    }

    @Nested
    class DataKind {

        private static final AdditionalMetricName CLIENT_ID = new AdditionalMetricName(
            new AdditionalMetricField(ValueType.KEYWORD, "keyword_native-kafka_client-id")
        );
        private static final AdditionalMetricName MODEL = new AdditionalMetricName(
            new AdditionalMetricField(ValueType.KEYWORD, "keyword_llm-proxy_model")
        );

        /** The native client id and the LLM model keep their values under their keys, as the analytics store reports. */
        private static final Predicate<AdditionalMetricField> IDENTIFIES_CALLER = field ->
            switch (field.rawKey()) {
                case "keyword_native-kafka_client-id" -> FilterSpec.Name.NATIVE_CLIENT_ID.isIdentifying();
                case "keyword_llm-proxy_model" -> FilterSpec.Name.LLM_PROXY_MODEL.isIdentifying();
                default -> false;
            };

        @Test
        void should_read_the_requests_when_a_field_holds_what_an_identifying_built_in_reads() {
            assertThat(facetsRequest(CLIENT_ID).dataKind(IDENTIFIES_CALLER)).isEqualTo(ApiDataKind.PER_REQUEST);
            assertThat(filteredRequest(CLIENT_ID).dataKind(IDENTIFIES_CALLER)).isEqualTo(ApiDataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_aggregates_when_a_field_holds_what_a_non_identifying_built_in_reads() {
            assertThat(facetsRequest(MODEL).dataKind(IDENTIFIES_CALLER)).isEqualTo(ApiDataKind.AGGREGATES);
            assertThat(filteredRequest(MODEL).dataKind(IDENTIFIES_CALLER)).isEqualTo(ApiDataKind.AGGREGATES);
        }

        @Test
        void should_read_the_aggregates_when_no_built_in_reads_the_field() {
            assertThat(facetsRequest(TIER).dataKind(IDENTIFIES_CALLER)).isEqualTo(ApiDataKind.AGGREGATES);
            assertThat(filteredRequest(TIER).dataKind(IDENTIFIES_CALLER)).isEqualTo(ApiDataKind.AGGREGATES);
        }

        private static FacetsRequest facetsRequest(AdditionalMetricName facet) {
            return new FacetsRequest(TIME_RANGE, List.of(), List.of(), List.of(facet), null, List.of());
        }

        private static FacetsRequest filteredRequest(AdditionalMetricName condition) {
            return new FacetsRequest(
                TIME_RANGE,
                List.of(new Filter(condition, FilterOperator.EQ, "value")),
                List.of(),
                List.of(FacetSpec.Name.API),
                null,
                List.of()
            );
        }
    }
}
