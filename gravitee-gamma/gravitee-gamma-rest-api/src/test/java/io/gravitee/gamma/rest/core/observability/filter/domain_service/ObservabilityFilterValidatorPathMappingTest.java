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

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Runs on the real registry: the path-mapping filter is accepted where the analytics engine reads {@code mapped-path}. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterValidatorPathMappingTest {

    private final ObservabilityFilterValidator validator = new ObservabilityFilterValidator(new SpiFilterRegistry());

    @Test
    void should_accept_one_path_mapping_on_analytics() {
        var conditions = List.of(new FilterCondition("HTTP_PATH_MAPPING", FilterOperator.EQ, List.of("/charges/:chargeId")));

        assertThatCode(() -> validator.validate(conditions, Signal.ANALYTICS)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_several_path_mappings_on_analytics() {
        var conditions = List.of(new FilterCondition("HTTP_PATH_MAPPING", FilterOperator.IN, List.of("/charges", "/charges/:chargeId")));

        assertThatCode(() -> validator.validate(conditions, Signal.ANALYTICS)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_a_path_mapping_on_a_dashboard() {
        var conditions = List.of(new FilterCondition("HTTP_PATH_MAPPING", FilterOperator.IN, List.of("/charges/:chargeId")));

        assertThatCode(() -> validator.validateDashboardFilters(conditions)).doesNotThrowAnyException();
    }

    @Test
    void should_refuse_a_path_mapping_on_the_logs() {
        var conditions = List.of(new FilterCondition("HTTP_PATH_MAPPING", FilterOperator.EQ, List.of("/charges/:chargeId")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.LOGS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.filter.signal_mismatch");
    }

    @Test
    void should_refuse_an_operator_the_path_mapping_does_not_offer() {
        var conditions = List.of(new FilterCondition("HTTP_PATH_MAPPING", FilterOperator.STARTS_WITH, List.of("/charges")));

        assertThatThrownBy(() -> validator.validate(conditions, Signal.ANALYTICS))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.filter.unsupported_operator");
    }
}
