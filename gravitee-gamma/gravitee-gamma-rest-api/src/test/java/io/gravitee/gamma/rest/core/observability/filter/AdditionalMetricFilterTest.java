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
package io.gravitee.gamma.rest.core.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * An additional metric field ({@code additional-metrics.<type>_<key>}, OBS-70) is off the catalog: the registry types
 * it by its prefix, so the validator checks a condition on it as it checks a built-in, discovery never consulted.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricFilterTest {

    private static final String TIER = "additional-metrics.keyword_fraud-check_tier";
    private static final String FAILOVERS = "additional-metrics.long_failover_count";
    private static final String SCORE = "additional-metrics.double_fraud-check_score";
    private static final String CACHE_HIT = "additional-metrics.bool_cache_hit";
    private static final String SIGNALS = "additional-metrics.json_fraud-signals";

    private final FilterRegistry registry = new SpiFilterRegistry(List.of());
    private final ObservabilityFilterValidator validator = new ObservabilityFilterValidator(registry);

    @Nested
    class Registry {

        @Test
        void should_answer_the_catalog_entry_of_a_built_in_name() {
            assertThat(registry.specFor("HTTP_STATUS"))
                .get()
                .extracting(FilterSpec::name, FilterSpec::valueType)
                .containsExactly("HTTP_STATUS", null);
        }

        @Test
        void should_type_a_keyword_field_on_both_signals() {
            var spec = registry.specFor(TIER).orElseThrow();

            assertThat(spec.type()).isEqualTo(FilterType.KEYWORD);
            assertThat(spec.operators()).containsExactlyInAnyOrder(FilterOperator.EQ, FilterOperator.IN);
            assertThat(spec.signals()).containsExactlyInAnyOrder(Signal.ANALYTICS, Signal.LOGS);
            assertThat(spec.apiTypes()).isEmpty();
            assertThat(spec.valueType()).isEqualTo(AdditionalMetricValueType.KEYWORD);
            assertThat(spec.label()).as("a validation spec, not a catalog entry").isNull();
        }

        @Test
        void should_type_a_number_field_with_equality_and_bounds() {
            var spec = registry.specFor(FAILOVERS).orElseThrow();

            assertThat(spec.type()).isEqualTo(FilterType.NUMBER);
            assertThat(spec.operators()).containsExactlyInAnyOrder(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE);
            assertThat(spec.takesDecimals()).isFalse();
            assertThat(registry.specFor(SCORE).orElseThrow().takesDecimals()).isTrue();
        }

        @Test
        void should_type_a_boolean_field_with_equality_only() {
            var spec = registry.specFor(CACHE_HIT).orElseThrow();

            assertThat(spec.type()).isEqualTo(FilterType.BOOLEAN);
            assertThat(spec.operators()).containsExactly(FilterOperator.EQ);
        }

        @Test
        void should_offer_no_operator_on_a_detail_only_field() {
            assertThat(registry.specFor(SIGNALS).orElseThrow().operators()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "custom.basket-value", "UNKNOWN", "additional-metrics.float_ratio", "additional-metrics.long_a..b" })
        void should_answer_nothing_for_a_name_neither_in_the_catalog_nor_well_formed(String name) {
            assertThat(registry.specFor(name)).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(AdditionalMetricField.ValueType.class)
        void should_name_every_value_type_as_the_engine_does(AdditionalMetricField.ValueType valueType) {
            assertThat(Arrays.stream(AdditionalMetricValueType.values()).map(Enum::name)).contains(valueType.name());
        }

        /** The definition lists a key as a metric when the engine measures its type: the two predicates must agree. */
        @ParameterizedTest
        @EnumSource(AdditionalMetricField.ValueType.class)
        void should_count_a_value_type_as_a_number_when_the_engine_does(AdditionalMetricField.ValueType valueType) {
            assertThat(AdditionalMetricValueType.valueOf(valueType.name()).isNumber()).isEqualTo(valueType.isNumber());
        }

        @Test
        void should_keep_the_catalog_free_of_additional_metric_fields() {
            assertThat(registry.getFilters(Set.of(), Set.of())).extracting(FilterSpec::valueType).containsOnlyNulls();
        }
    }

    @Nested
    class Validation {

        @Test
        void should_accept_a_well_formed_field_no_request_carries_yet() {
            var conditions = List.of(
                new FilterCondition(TIER, FilterOperator.EQ, List.of("high")),
                new FilterCondition(FAILOVERS, FilterOperator.GTE, List.of("2")),
                new FilterCondition(SCORE, FilterOperator.LTE, List.of("0.75")),
                new FilterCondition(CACHE_HIT, FilterOperator.EQ, List.of("false"))
            );

            assertThatCode(() -> validator.validate(conditions, Signal.ANALYTICS)).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_a_malformed_name_naming_it() {
            var conditions = List.of(new FilterCondition("additional-metrics.LONG_count", FilterOperator.EQ, List.of("1")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("additional-metrics.LONG_count")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unknown_name");
        }

        @Test
        void should_refuse_the_assign_metrics_form_as_unknown() {
            var conditions = List.of(new FilterCondition("custom.basket-value", FilterOperator.EQ, List.of("1")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unknown_name");
        }

        @Test
        void should_refuse_any_operator_on_a_detail_only_field() {
            var conditions = List.of(new FilterCondition(SIGNALS, FilterOperator.EQ, List.of("x")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .hasMessageContaining(SIGNALS)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unsupported_operator");
        }

        @Test
        void should_refuse_an_operator_the_value_type_does_not_take() {
            var conditions = List.of(new FilterCondition(TIER, FilterOperator.GTE, List.of("high")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unsupported_operator");
        }

        @Test
        void should_refuse_decimals_on_a_whole_number_field() {
            var conditions = List.of(new FilterCondition(FAILOVERS, FilterOperator.GTE, List.of("1.5")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_number");
        }

        @Test
        void should_refuse_a_value_that_is_no_number_on_a_decimal_field() {
            var conditions = List.of(new FilterCondition(SCORE, FilterOperator.GTE, List.of("high")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .hasMessageContaining("is not a number")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_number");
        }

        @Test
        void should_refuse_a_boolean_value_other_than_true_or_false() {
            var conditions = List.of(new FilterCondition(CACHE_HIT, FilterOperator.EQ, List.of("yes")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unknown_enum_value");
        }

        @Test
        void should_accept_a_field_among_a_dashboard_filters() {
            var conditions = List.of(new FilterCondition(TIER, FilterOperator.EQ, List.of()));

            assertThatCode(() -> validator.validateDashboardFilters(conditions)).doesNotThrowAnyException();
        }
    }

    @Nested
    class LogsSearch {

        @Test
        void should_accept_every_operator_its_value_type_takes() {
            var conditions = List.of(
                new FilterCondition(TIER, FilterOperator.IN, List.of("high", "medium")),
                new FilterCondition(FAILOVERS, FilterOperator.GTE, List.of("1")),
                new FilterCondition(FAILOVERS, FilterOperator.LTE, List.of("2")),
                new FilterCondition(SCORE, FilterOperator.LTE, List.of("0.75")),
                new FilterCondition(CACHE_HIT, FilterOperator.EQ, List.of("true"))
            );

            assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_a_malformed_name_naming_it() {
            var malformed = "additional-metrics.keyword_bad\"name";
            var conditions = List.of(new FilterCondition(malformed, FilterOperator.EQ, List.of("x")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .hasMessage("Filter '" + malformed + "' is not supported by this backend")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unknown_name");
        }

        @ParameterizedTest
        @ValueSource(strings = { SIGNALS, "additional-metrics.string_fraud-check_reason" })
        void should_refuse_any_operator_on_a_field_indexed_as_text_naming_it(String name) {
            var conditions = List.of(new FilterCondition(name, FilterOperator.EQ, List.of("x")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .hasMessage("Operator 'EQ' is not supported yet for filter '" + name + "'")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unsupported_operator");
        }

        @Test
        void should_refuse_an_operator_the_value_type_does_not_take_naming_it() {
            var conditions = List.of(new FilterCondition(CACHE_HIT, FilterOperator.IN, List.of("true", "false")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .hasMessageContaining("'" + CACHE_HIT + "'")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unsupported_operator");
        }

        @Test
        void should_refuse_decimals_on_a_whole_number_field_naming_it() {
            var conditions = List.of(new FilterCondition(FAILOVERS, FilterOperator.GTE, List.of("1.5")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .hasMessage("Value '1.5' is not a whole number, as filter '" + FAILOVERS + "' requires")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_number");
        }

        @ParameterizedTest
        @EnumSource(value = Signal.class, names = { "ANALYTICS", "LOGS" })
        void should_refuse_a_value_an_int_field_cannot_hold_naming_it(Signal signal) {
            var retries = "additional-metrics.int_retry_count";
            var conditions = List.of(new FilterCondition(retries, FilterOperator.GTE, List.of("2147483648")));

            assertThatThrownBy(() -> validator.validate(conditions, signal))
                .hasMessage("Value '2147483648' is outside the range [-2147483648, 2147483647] of filter '" + retries + "'")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.value_out_of_range");
        }

        @ParameterizedTest
        @CsvSource({ "ANALYTICS, 1e400", "LOGS, 1e400", "ANALYTICS, -1e400", "LOGS, -1e400" })
        void should_refuse_a_value_a_double_field_cannot_hold_naming_it(Signal signal, String value) {
            var conditions = List.of(new FilterCondition(SCORE, FilterOperator.GTE, List.of(value)));

            assertThatThrownBy(() -> validator.validate(conditions, signal))
                .hasMessage(
                    "Value '" + value + "' is outside the range [-1.7976931348623157E308, 1.7976931348623157E308] of filter '" + SCORE + "'"
                )
                .extracting("technicalCode")
                .isEqualTo("observability.filter.value_out_of_range");
        }

        @ParameterizedTest
        @EnumSource(value = Signal.class, names = { "ANALYTICS", "LOGS" })
        void should_refuse_non_ascii_digits_on_a_double_field_naming_it(Signal signal) {
            var conditions = List.of(new FilterCondition(SCORE, FilterOperator.GTE, List.of("\u0663.\u0665")));

            assertThatThrownBy(() -> validator.validate(conditions, signal))
                .hasMessage("Value '\u0663.\u0665' is not a number, as filter '" + SCORE + "' requires")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_number");
        }

        @ParameterizedTest
        @ValueSource(strings = { "1.7976931348623157E308", "-1.7976931348623157E308" })
        void should_accept_the_largest_finite_values_of_a_double_field(String bound) {
            var conditions = List.of(new FilterCondition(SCORE, FilterOperator.GTE, List.of(bound)));

            assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_bounds_nothing_lies_between() {
            var conditions = List.of(
                new FilterCondition(FAILOVERS, FilterOperator.GTE, List.of("5")),
                new FilterCondition(FAILOVERS, FilterOperator.LTE, List.of("1"))
            );

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .hasMessageContaining("'" + FAILOVERS + "'")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.inverted_range");
        }

        @Test
        void should_refuse_a_field_named_twice_but_for_a_range() {
            var conditions = List.of(
                new FilterCondition(TIER, FilterOperator.EQ, List.of("high")),
                new FilterCondition(TIER, FilterOperator.EQ, List.of("low"))
            );

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .hasMessageContaining("'" + TIER + "'")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.repeated");
        }
    }

    @Nested
    class CallerIdentification {

        private static final String CLIENT_ID = "additional-metrics.keyword_native-kafka_client-id";

        /** The analytics store keeps the native client id, which identifies a caller, under its additional metric key. */
        private final ObservabilityFilterValidator identifyingValidator = new ObservabilityFilterValidator(
            new SpiFilterRegistry(field -> field.rawKey().equals("keyword_native-kafka_client-id"))
        );

        @Test
        void should_identify_a_caller_through_the_key_of_an_identifying_built_in() {
            assertThat(identifyingValidator.identifiesCaller(CLIENT_ID)).isTrue();
        }

        @Test
        void should_not_identify_a_caller_through_a_key_no_identifying_built_in_reads() {
            assertThat(identifyingValidator.identifiesCaller(TIER)).isFalse();
        }

        @Test
        void should_keep_identifying_the_catalog_fields_holding_an_identifier() {
            assertThat(identifyingValidator.identifiesCaller("CONSUMER_IP")).isTrue();
            assertThat(identifyingValidator.identifiesCaller("HTTP_METHOD")).isFalse();
        }
    }
}
