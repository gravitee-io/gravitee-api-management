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
package io.gravitee.gamma.rest.resources.observability.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterValuesTest {

    @Test
    void should_read_a_scalar_as_a_single_value() {
        assertThat(FilterValues.normalize("HTTP_STATUS", 404)).containsExactly("404");
        assertThat(FilterValues.normalize("API", "api-1")).containsExactly("api-1");
    }

    @Test
    void should_read_an_array_of_scalars_as_its_values() {
        assertThat(FilterValues.normalize("API", List.of("api-1", "api-2"))).containsExactly("api-1", "api-2");
    }

    @Test
    void should_read_a_missing_value_as_no_value() {
        assertThat(FilterValues.normalize("API", null)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("malformedValues")
    void should_reject_a_value_that_is_neither_a_scalar_nor_an_array_of_scalars(Object value) {
        assertThatThrownBy(() -> FilterValues.normalize("API", value))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("API")
            .extracting("technicalCode")
            .isEqualTo("observability.filter.invalid_value_shape");
    }

    static Stream<Object> malformedValues() {
        return Stream.of(Map.of("id", "api-1"), Arrays.asList("api-1", null), List.of(List.of("api-1")), List.of(Map.of("id", "api-1")));
    }
}
