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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
