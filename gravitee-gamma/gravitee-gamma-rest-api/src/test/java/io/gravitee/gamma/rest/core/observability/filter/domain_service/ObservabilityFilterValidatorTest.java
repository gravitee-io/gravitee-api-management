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
package io.gravitee.gamma.rest.core.observability.filter.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterValidatorTest {

    @Mock
    private FilterRegistry filterRegistry;

    private ObservabilityFilterValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ObservabilityFilterValidator(filterRegistry);
        when(filterRegistry.getFilters(any(), any())).thenReturn(
            List.of(
                new FilterSpec(
                    "HTTP_STATUS",
                    "Status Code",
                    FilterType.NUMBER,
                    List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
                    null,
                    new FilterSpec.Range(100, 599),
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    Set.of(ApiType.HTTP_PROXY)
                ),
                new FilterSpec(
                    "GATEWAY",
                    "Gateway",
                    FilterType.KEYWORD,
                    List.of(FilterOperator.EQ, FilterOperator.IN),
                    null,
                    null,
                    Set.of(Signal.ANALYTICS),
                    ApiType.ALL
                ),
                new FilterSpec(
                    "PAYLOAD",
                    "Payload content",
                    FilterType.STRING,
                    List.of(FilterOperator.CONTAINS),
                    null,
                    null,
                    Set.of(Signal.LOGS),
                    Set.of(ApiType.HTTP_PROXY)
                ),
                new FilterSpec(
                    "FAILURE_ORIGIN",
                    "Failure Origin",
                    FilterType.ENUM,
                    List.of(FilterOperator.EQ, FilterOperator.IN),
                    List.of(
                        new FilterSpec.EnumValue("NONE", "No failure"),
                        new FilterSpec.EnumValue("GATEWAY_TO_BROKER", "Gateway broker")
                    ),
                    null,
                    Set.of(Signal.LOGS),
                    Set.of(ApiType.NATIVE)
                )
            )
        );
    }

    @Test
    void should_pass_a_valid_condition() {
        var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("400")));

        assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_an_unknown_filter_name() {
        var conditions = List.of(new FilterCondition("UNKNOWN", FilterOperator.EQ, List.of("x")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("UNKNOWN");
    }

    @Test
    void should_reject_a_filter_not_applicable_to_the_signal() {
        var conditions = List.of(new FilterCondition("GATEWAY", FilterOperator.EQ, List.of("gw-1")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("GATEWAY");
    }

    @Test
    void should_reject_an_operator_not_advertised_for_the_filter() {
        var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.CONTAINS, List.of("200")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("CONTAINS");
    }

    @Test
    void should_pass_advertised_enum_values() {
        var conditions = List.of(new FilterCondition("FAILURE_ORIGIN", FilterOperator.IN, List.of("NONE", "GATEWAY_TO_BROKER")));

        assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_an_enum_value_the_filter_does_not_advertise() {
        var conditions = List.of(new FilterCondition("FAILURE_ORIGIN", FilterOperator.IN, List.of("BOGUS")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("BOGUS");
    }

    @Test
    void should_reject_enum_values_with_wrong_case() {
        // Silent case-mismatches would reach translators that drop clauses they cannot express.
        var conditions = List.of(new FilterCondition("FAILURE_ORIGIN", FilterOperator.EQ, List.of("none")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("none");
    }

    @Test
    void should_reject_blank_string_filter_values() {
        var conditions = List.of(new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, List.of("   ")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("non-blank");
    }

    @Test
    void should_reject_an_enum_condition_carrying_no_value() {
        var conditions = List.of(new FilterCondition("FAILURE_ORIGIN", FilterOperator.IN, List.of()));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("FAILURE_ORIGIN");
    }

    @Test
    void should_reject_a_keyword_condition_carrying_no_value() {
        // Not enum-specific: an empty list narrows nothing, so the caller would get every accessible
        // api back while the filter chip stays on screen.
        var conditions = List.of(new FilterCondition("GATEWAY", FilterOperator.IN, List.of()));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("GATEWAY");
    }

    @Test
    void should_reject_a_condition_whose_values_are_null() {
        var conditions = List.of(new FilterCondition("GATEWAY", FilterOperator.EQ, null));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("GATEWAY");
    }

    @Nested
    class Arity {

        @ParameterizedTest
        @CsvSource({ "GATEWAY, EQ, ANALYTICS", "PAYLOAD, CONTAINS, LOGS", "HTTP_STATUS, GTE, LOGS", "HTTP_STATUS, LTE, ANALYTICS" })
        void should_reject_a_single_value_operator_carrying_several_values(String filter, FilterOperator operator, Signal signal) {
            var conditions = List.of(new FilterCondition(filter, operator, List.of("400", "500")));

            assertThatThrownBy(() -> validator.validate(conditions, signal))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining(filter)
                .hasMessageContaining(operator.name())
                .hasMessageContaining("2 values")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_arity");
        }

        @Test
        void should_accept_several_values_on_in() {
            var conditions = List.of(new FilterCondition("GATEWAY", FilterOperator.IN, List.of("gw-1", "gw-2")));

            assertThatCode(() -> validator.validate(conditions, Signal.ANALYTICS)).doesNotThrowAnyException();
        }
    }

    @Nested
    class NumberValues {

        @Test
        void should_reject_a_value_that_is_not_a_number() {
            var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("abc")));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("abc")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_number");
        }

        // Every NUMBER filter of the catalog counts or measures in whole units; the logs translator parses them as such.
        @ParameterizedTest
        @ValueSource(strings = { "404.5", "1e2", "404.0" })
        void should_reject_a_value_that_is_not_a_whole_number(String value) {
            var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of(value)));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining(value)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.invalid_number");
        }

        @ParameterizedTest
        @ValueSource(strings = { "99", "600", "-1" })
        void should_reject_a_value_outside_the_declared_range(String value) {
            var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of(value)));

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining(value)
                .hasMessageContaining("100")
                .hasMessageContaining("599")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.value_out_of_range");
        }

        @ParameterizedTest
        @ValueSource(strings = { "100", "404", "599" })
        void should_accept_a_value_within_the_declared_range(String value) {
            var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of(value)));

            assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
        }
    }

    @Nested
    class Repetition {

        @Test
        void should_accept_a_gte_and_an_lte_on_one_number_filter() {
            var conditions = List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("400")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("499"))
            );

            assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
        }

        @Test
        void should_accept_a_closed_range_of_a_single_value() {
            var conditions = List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("404")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("404"))
            );

            assertThatCode(() -> validator.validate(conditions, Signal.ANALYTICS)).doesNotThrowAnyException();
        }

        // Logs refused it later without a code and analytics answered an empty set: one answer for both signals.
        @Test
        void should_reject_a_range_whose_lower_bound_is_above_its_upper_bound() {
            var conditions = List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("400"))
            );

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("HTTP_STATUS")
                .hasMessageContaining("500")
                .hasMessageContaining("400")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.inverted_range");
        }

        @Test
        void should_reject_the_same_condition_twice() {
            var conditions = List.of(
                new FilterCondition("GATEWAY", FilterOperator.IN, List.of("gw-1")),
                new FilterCondition("GATEWAY", FilterOperator.IN, List.of("gw-2"))
            );

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("GATEWAY")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.repeated");
        }

        @Test
        void should_reject_two_operators_on_one_filter_outside_a_numeric_range() {
            var conditions = List.of(
                new FilterCondition("GATEWAY", FilterOperator.EQ, List.of("gw-1")),
                new FilterCondition("GATEWAY", FilterOperator.IN, List.of("gw-2"))
            );

            assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("EQ")
                .hasMessageContaining("IN")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.repeated");
        }

        @ParameterizedTest
        @CsvSource({ "GTE, GTE", "LTE, LTE", "EQ, GTE", "EQ, LTE" })
        void should_reject_a_number_filter_repeated_other_than_as_one_closed_range(FilterOperator first, FilterOperator second) {
            var conditions = List.of(
                new FilterCondition("HTTP_STATUS", first, List.of("400")),
                new FilterCondition("HTTP_STATUS", second, List.of("500"))
            );

            assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.repeated");
        }
    }

    @Nested
    class DashboardFilters {

        @Test
        void should_accept_an_empty_slot_on_an_analytics_filter() {
            var conditions = List.of(
                new FilterCondition("GATEWAY", FilterOperator.IN, List.of()),
                new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of())
            );

            assertThatCode(() -> validator.validateDashboardFilters(conditions)).doesNotThrowAnyException();
        }

        @Test
        void should_accept_valued_analytics_filters() {
            var conditions = List.of(
                new FilterCondition("GATEWAY", FilterOperator.IN, List.of("gw-1", "gw-2")),
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500"))
            );

            assertThatCode(() -> validator.validateDashboardFilters(conditions)).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_a_filter_the_catalog_does_not_know() {
            var conditions = List.of(new FilterCondition("REQEUST_ID", FilterOperator.IN, List.of()));

            assertThatThrownBy(() -> validator.validateDashboardFilters(conditions))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("REQEUST_ID")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unknown_name");
        }

        // An empty slot is refused too: once a reader fills it, the dashboard page drops the value it cannot query.
        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_refuse_a_filter_the_analytics_signal_does_not_cover(boolean valued) {
            var conditions = List.of(new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, valued ? List.of("error") : List.of()));

            assertThatThrownBy(() -> validator.validateDashboardFilters(conditions))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("PAYLOAD")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.signal_mismatch");
        }

        @Test
        void should_refuse_an_operator_the_filter_does_not_advertise_even_on_an_empty_slot() {
            var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.IN, List.of()));

            assertThatThrownBy(() -> validator.validateDashboardFilters(conditions))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.unsupported_operator");
        }

        @Test
        void should_check_a_value_as_search_does() {
            var conditions = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("700")));

            assertThatThrownBy(() -> validator.validateDashboardFilters(conditions))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.value_out_of_range");
        }

        // Unlike a search, a dashboard holds one filter per field: the library keys its chips by field.
        @Test
        void should_refuse_a_field_filtered_twice_even_as_a_closed_range() {
            var conditions = List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("400")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("499"))
            );

            assertThatThrownBy(() -> validator.validateDashboardFilters(conditions))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("HTTP_STATUS")
                .hasMessageContaining("one filter per field")
                .extracting("technicalCode")
                .isEqualTo("observability.filter.repeated_on_dashboard");
        }
    }
}
