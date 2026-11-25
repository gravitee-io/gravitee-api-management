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
package io.gravitee.apim.core.observability.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.observability.model.AdditionalMetricField.Root;
import io.gravitee.apim.core.observability.model.AdditionalMetricField.ValueType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricFieldTest {

    @Nested
    class Parse {

        @ParameterizedTest
        @CsvSource(
            {
                "additional-metrics.long_failover_count, LONG, long_failover_count",
                "additional-metrics.int_retry_attempts, INT, int_retry_attempts",
                "additional-metrics.double_llm-proxy_sent-cost, DOUBLE, double_llm-proxy_sent-cost",
                "additional-metrics.bool_cache_hit, BOOL, bool_cache_hit",
                "additional-metrics.keyword_mcp-proxy_tools/call, KEYWORD, keyword_mcp-proxy_tools/call",
                "additional-metrics.string_body-excerpt, STRING, string_body-excerpt",
                "additional-metrics.json_fraud-signals, JSON, json_fraud-signals",
                "additional-metrics.long_a.b, LONG, long_a.b",
                "additional-metrics.keyword_LLM_modelName, KEYWORD, keyword_LLM_modelName",
                "additional-metrics.double_custom_basket-value, DOUBLE, double_custom_basket-value",
            }
        )
        void should_read_the_value_type_and_the_raw_key_of_a_well_formed_name(String name, ValueType valueType, String rawKey) {
            assertThat(AdditionalMetricField.parse(name)).contains(new AdditionalMetricField(valueType, rawKey));
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "API",
                "additional-metrics.",
                "additional-metrics.long_",
                "additional-metrics.long",
                "additional-metrics.float_ratio",
                "additional-metrics.LONG_count",
                "Additional-Metrics.long_count",
                "additional-metrics.long_a..b",
                "additional-metrics.long_a.",
                "additional-metrics.long_.a",
                "additional-metrics.keyword_a b",
                "additional-metrics.keyword_a\"b",
                "additional-metrics.keyword_a*",
                "additional-metrics.keyword_a\nb",
                "additional-metrics.keyword_é",
                "long_failover_count",
            }
        )
        void should_refuse_a_malformed_name(String name) {
            assertThat(AdditionalMetricField.parse(name)).isEmpty();
        }

        @Test
        void should_read_a_key_made_of_many_segments() {
            var name = "additional-metrics.keyword_" + "a.".repeat(110) + "a";

            assertThat(AdditionalMetricField.parse(name)).isPresent();
            assertThat(AdditionalMetricField.parse(name + ".")).isEmpty();
        }

        @Test
        void should_refuse_a_null_name() {
            assertThat(AdditionalMetricField.parse(null)).isEmpty();
        }

        @Test
        void should_accept_a_name_of_256_characters_and_refuse_one_longer() {
            var prefix = "additional-metrics.keyword_";
            var longest = prefix + "a".repeat(AdditionalMetricField.MAX_NAME_LENGTH - prefix.length());

            assertThat(AdditionalMetricField.parse(longest)).isPresent();
            assertThat(AdditionalMetricField.parse(longest + "a")).isEmpty();
        }
    }

    @Nested
    class ParseCustom {

        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = {
                "custom.fraud-tier|fraud-tier",
                "custom.merchant-id|merchant-id",
                "custom.Merchant ID|Merchant ID",
                "'custom. tier '|' tier '",
                "custom.a/b|a/b",
                "custom.a.b|a.b",
                "custom.é|é",
                "custom.keyword_native-kafka_client-id|keyword_native-kafka_client-id",
                "custom.custom.x|custom.x",
            }
        )
        void should_read_a_keyword_field_under_the_custom_root(String name, String key) {
            var field = AdditionalMetricField.parse(name);

            assertThat(field).contains(new AdditionalMetricField(Root.CUSTOM, ValueType.KEYWORD, key));
            assertThat(field.orElseThrow().name()).isEqualTo(name);
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "custom.",
                "custom. ",
                "custom.   ",
                "custom.\t",
                "custom",
                "Custom.tier",
                "custom..tier",
                "custom.tier.",
                "custom.a..b",
                "custom.a\"b",
                "custom.a\\b",
                "custom.tier*",
                "custom.*",
            }
        )
        void should_refuse_a_custom_name_outside_the_grammar(String name) {
            assertThat(AdditionalMetricField.parse(name)).isEmpty();
        }

        @Test
        void should_accept_a_custom_key_of_256_characters_and_refuse_one_longer() {
            var longest = "custom." + "a".repeat(AdditionalMetricField.MAX_CUSTOM_KEY_LENGTH);

            assertThat(AdditionalMetricField.MAX_CUSTOM_KEY_LENGTH).isEqualTo(256);
            assertThat(AdditionalMetricField.parse(longest)).isPresent();
            assertThat(AdditionalMetricField.parse(longest + "a")).isEmpty();
        }

        @Test
        void should_keep_a_custom_key_apart_from_the_additional_metric_of_the_same_raw_key() {
            var custom = AdditionalMetricField.parse("custom.keyword_tier").orElseThrow();
            var additional = AdditionalMetricField.parse("additional-metrics.keyword_tier").orElseThrow();

            assertThat(custom.rawKey()).isEqualTo(additional.rawKey());
            assertThat(custom).isNotEqualTo(additional);
            assertThat(custom.name()).isNotEqualTo(additional.name());
        }
    }

    /** E2E finding C2: a refused name says which rule it breaks, the same text on every surface. */
    @Nested
    class Refusal {

        @Test
        void should_state_the_rules_from_the_grammar_constants() {
            assertThat(AdditionalMetricField.CUSTOM_NAME_RULE).isEqualTo(
                "a custom metric is named custom.<name>, <name> being 1 to 256 characters, not blank, with no leading or " +
                    "trailing dot, no '..', and none of '\"', '\\' and '*'"
            );
            assertThat(AdditionalMetricField.ADDITIONAL_METRIC_NAME_RULE).isEqualTo(
                "an additional metric is named additional-metrics.<type>_<key>, <type> being one of keyword, long, int, " +
                    "double, bool, string and json, <key> segments of letters, digits, '-', '_' and '/' joined by single dots, " +
                    "the whole name 256 characters at most"
            );
            assertThat(AdditionalMetricField.CUSTOM_NAME_RULE).contains(
                "1 to " + AdditionalMetricField.MAX_CUSTOM_KEY_LENGTH + " characters"
            );
            assertThat(AdditionalMetricField.ADDITIONAL_METRIC_NAME_RULE).contains(
                AdditionalMetricField.MAX_NAME_LENGTH + " characters at most"
            );
            for (ValueType type : ValueType.values()) {
                assertThat(AdditionalMetricField.ADDITIONAL_METRIC_NAME_RULE).contains(type.prefix().replace("_", ""));
            }
        }

        @ParameterizedTest
        @ValueSource(strings = { "custom.", "custom. ", "custom.a..b", "custom.trail.", "custom.a*", "custom.a\"b" })
        void should_name_the_custom_rule_a_refused_custom_name_breaks(String name) {
            assertThat(AdditionalMetricField.refusal(name)).hasValue(AdditionalMetricField.CUSTOM_NAME_RULE);
        }

        @Test
        void should_name_the_custom_rule_for_a_custom_key_too_long() {
            assertThat(AdditionalMetricField.refusal("custom." + "a".repeat(AdditionalMetricField.MAX_CUSTOM_KEY_LENGTH + 1))).hasValue(
                AdditionalMetricField.CUSTOM_NAME_RULE
            );
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "additional-metrics.", "additional-metrics.float_ratio", "additional-metrics.long_a..b", "additional-metrics.keyword_a b",
            }
        )
        void should_name_the_additional_metric_rule_a_refused_additional_metric_name_breaks(String name) {
            assertThat(AdditionalMetricField.refusal(name)).hasValue(AdditionalMetricField.ADDITIONAL_METRIC_NAME_RULE);
        }

        @ParameterizedTest
        @ValueSource(strings = { "custom.fraud-tier", "additional-metrics.long_failover_count", "API", "UNKNOWN", "CUSTOM.x", "custom" })
        void should_refuse_nothing_for_a_well_formed_name_or_a_name_under_no_dynamic_root(String name) {
            assertThat(AdditionalMetricField.refusal(name)).isEmpty();
        }

        @Test
        void should_refuse_nothing_for_no_name() {
            assertThat(AdditionalMetricField.refusal(null)).isEmpty();
        }
    }

    @Nested
    class Construction {

        @Test
        void should_refuse_a_key_outside_the_grammar() {
            assertThatThrownBy(() -> new AdditionalMetricField(ValueType.KEYWORD, "keyword_a*")).isInstanceOf(
                IllegalArgumentException.class
            );
        }

        @Test
        void should_refuse_a_value_type_other_than_the_one_of_its_prefix() {
            assertThatThrownBy(() -> new AdditionalMetricField(ValueType.LONG, "keyword_tier")).isInstanceOf(
                IllegalArgumentException.class
            );
        }

        @ParameterizedTest
        @ValueSource(strings = { "LONG", "INT", "DOUBLE", "BOOL", "STRING", "JSON" })
        void should_refuse_a_custom_field_other_than_a_keyword(ValueType valueType) {
            assertThatThrownBy(() -> new AdditionalMetricField(Root.CUSTOM, valueType, "tier")).isInstanceOf(
                IllegalArgumentException.class
            );
        }

        @Test
        void should_refuse_a_custom_key_outside_the_grammar() {
            assertThatThrownBy(() -> new AdditionalMetricField(Root.CUSTOM, ValueType.KEYWORD, "a..b")).isInstanceOf(
                IllegalArgumentException.class
            );
        }

        @Test
        void should_root_a_field_built_from_a_raw_key_under_the_additional_metrics() {
            assertThat(new AdditionalMetricField(ValueType.KEYWORD, "keyword_tier").root()).isEqualTo(Root.ADDITIONAL_METRICS);
        }

        @Test
        void should_name_the_prefix_of_each_value_type() {
            assertThat(ValueType.DOUBLE.prefix()).isEqualTo("double_");
            assertThat(ValueType.KEYWORD.prefix()).isEqualTo("keyword_");
        }
    }

    @Nested
    class Name {

        @Test
        void should_read_the_wire_name_back_from_the_raw_key() {
            var field = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");

            assertThat(field.name()).isEqualTo("additional-metrics.keyword_fraud-check_tier");
            assertThat(AdditionalMetricField.parse(field.name())).contains(field);
        }
    }

    @Nested
    class Capabilities {

        @Test
        void should_offer_equality_and_lists_on_a_keyword() {
            assertThat(ValueType.KEYWORD.operators()).containsExactlyInAnyOrder(FilterOperator.EQ, FilterOperator.IN);
            assertThat(ValueType.KEYWORD.groupsByTerms()).isTrue();
            assertThat(ValueType.KEYWORD.groupsByRanges()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "LONG", "INT", "DOUBLE" })
        void should_offer_equality_and_bounds_on_a_number_grouped_by_ranges_only(ValueType number) {
            assertThat(number.operators()).containsExactlyInAnyOrder(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE);
            assertThat(number.groupsByTerms()).isFalse();
            assertThat(number.groupsByRanges()).isTrue();
        }

        @Test
        void should_offer_equality_on_a_boolean_grouped_in_two_buckets() {
            assertThat(ValueType.BOOL.operators()).containsExactly(FilterOperator.EQ);
            assertThat(ValueType.BOOL.groupsByTerms()).isTrue();
            assertThat(ValueType.BOOL.groupsByRanges()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "STRING", "JSON" })
        void should_offer_nothing_on_a_detail_only_type(ValueType detailOnly) {
            assertThat(detailOnly.operators()).isEmpty();
            assertThat(detailOnly.groupsByTerms()).isFalse();
            assertThat(detailOnly.groupsByRanges()).isFalse();
        }

        @Test
        void should_take_decimals_on_a_double_only() {
            assertThat(ValueType.DOUBLE.isDecimal()).isTrue();
            assertThat(ValueType.LONG.isDecimal()).isFalse();
            assertThat(ValueType.INT.isDecimal()).isFalse();
        }
    }
}
