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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.exception.ValidationDomainException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NumericRangeAccumulatorTest {

    @Nested
    class HasValue {

        @Test
        void should_return_false_when_nothing_accumulated() {
            var acc = new NumericRangeAccumulator<Integer>("TEST");

            assertThat(acc.hasValue()).isFalse();
        }

        @Test
        void should_return_true_after_setGte() {
            var acc = new NumericRangeAccumulator<Integer>("TEST");
            acc.setGte(100);

            assertThat(acc.hasValue()).isTrue();
        }

        @Test
        void should_return_true_after_setLte() {
            var acc = new NumericRangeAccumulator<Integer>("TEST");
            acc.setLte(200);

            assertThat(acc.hasValue()).isTrue();
        }
    }

    @Nested
    class Setters {

        @Test
        void should_set_gte_bound() {
            var acc = new NumericRangeAccumulator<Integer>("TEST");
            acc.setGte(400);

            var bounds = acc.build();
            assertThat(bounds.gte()).isEqualTo(400);
            assertThat(bounds.lte()).isNull();
        }

        @Test
        void should_set_lte_bound() {
            var acc = new NumericRangeAccumulator<Integer>("TEST");
            acc.setLte(500);

            var bounds = acc.build();
            assertThat(bounds.gte()).isNull();
            assertThat(bounds.lte()).isEqualTo(500);
        }

        @Test
        void should_set_both_bounds() {
            var acc = new NumericRangeAccumulator<Long>("TEST");
            acc.setBoth(1000L);

            var bounds = acc.build();
            assertThat(bounds.gte()).isEqualTo(1000L);
            assertThat(bounds.lte()).isEqualTo(1000L);
        }

        @Test
        void should_accumulate_gte_and_lte_separately() {
            var acc = new NumericRangeAccumulator<Integer>("TEST");
            acc.setGte(200);
            acc.setLte(399);

            var bounds = acc.build();
            assertThat(bounds.gte()).isEqualTo(200);
            assertThat(bounds.lte()).isEqualTo(399);
        }
    }

    @Nested
    class Build {

        @Test
        void should_throw_when_gte_greater_than_lte() {
            var acc = new NumericRangeAccumulator<Integer>("HTTP_STATUS");
            acc.setGte(500);
            acc.setLte(200);

            assertThatThrownBy(acc::build)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("gte")
                .hasMessageContaining("lte")
                .hasMessageContaining("HTTP_STATUS");
        }

        @Test
        void should_accept_equal_gte_and_lte() {
            var acc = new NumericRangeAccumulator<Long>("RESPONSE_TIME");
            acc.setGte(100L);
            acc.setLte(100L);

            var bounds = acc.build();
            assertThat(bounds.gte()).isEqualTo(100L);
            assertThat(bounds.lte()).isEqualTo(100L);
        }
    }
}
