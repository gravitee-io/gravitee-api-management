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
package io.gravitee.repository.log.v4.model.connection;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiMetricsQueryTest {

    @Test
    void validate_when_from_and_to_values_correct() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").from(1L).to(2L).page(1).size(20).build();

        assertThatCode(query::validate).doesNotThrowAnyException();
    }

    @Test
    void validate_throws_exception_when_apiId_is_null() {
        var query = NativeApiMetricsQuery.builder().build();

        assertThatNullPointerException().isThrownBy(query::validate).withMessage("apiId");
    }

    @Test
    void validate_throws_IllegalArgumentException_when_page_below_one() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").page(0).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("page must be >= 1");
    }

    @Test
    void validate_throws_IllegalArgumentException_when_size_below_one() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").size(0).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size must be >= 1");
    }

    @Test
    void validate_throws_IllegalArgumentException_when_from_greater_than_to() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").from(2L).to(1L).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("from must be <= to");
    }

    @Test
    void validate_accepts_query_with_only_from_or_to__value() {
        var fromOnly = NativeApiMetricsQuery.builder().apiId("api-1").from(1L).build();
        var toOnly = NativeApiMetricsQuery.builder().apiId("api-1").to(1L).build();

        assertThatCode(fromOnly::validate).doesNotThrowAnyException();
        assertThatCode(toOnly::validate).doesNotThrowAnyException();
    }
}
