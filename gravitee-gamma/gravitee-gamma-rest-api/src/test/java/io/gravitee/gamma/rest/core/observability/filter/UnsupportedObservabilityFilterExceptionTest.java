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

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnsupportedObservabilityFilterExceptionTest {

    @Test
    void unknown_name_should_carry_the_correct_technical_code() {
        var exception = UnsupportedObservabilityFilterException.unknownName("BOGUS");

        assertThat(exception.getTechnicalCode()).isEqualTo("observability.filter.unknown_name");
        assertThat(exception.getMessage()).contains("BOGUS");
    }

    @Test
    void unsupported_operator_should_carry_the_correct_technical_code() {
        var exception = UnsupportedObservabilityFilterException.unsupportedOperator("API", "REGEX");

        assertThat(exception.getTechnicalCode()).isEqualTo("observability.filter.unsupported_operator");
        assertThat(exception.getMessage()).contains("API").contains("REGEX");
    }
}
