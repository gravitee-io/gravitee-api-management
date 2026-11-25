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
package io.gravitee.repository.log.v4.model.guardian;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDecisionLogQueryTest {

    @Test
    void accepts_a_query_on_every_api_of_the_environment() {
        var query = GuardianDecisionLogQuery.builder().build();

        assertThatCode(query::validate).doesNotThrowAnyException();
    }

    @Test
    void accepts_a_page_at_the_maximum_size() {
        var query = GuardianDecisionLogQuery.builder().size(GuardianDecisionLogQuery.MAX_SIZE).build();

        assertThatCode(query::validate).doesNotThrowAnyException();
    }

    @Test
    void rejects_a_page_below_one() {
        var query = GuardianDecisionLogQuery.builder().page(0).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("page must be >= 1");
    }

    @Test
    void rejects_a_size_below_one() {
        var query = GuardianDecisionLogQuery.builder().size(0).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size must be >= 1");
    }

    @Test
    void rejects_a_size_above_the_maximum() {
        var query = GuardianDecisionLogQuery.builder().size(GuardianDecisionLogQuery.MAX_SIZE + 1).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size must be <=");
    }

    @Test
    void rejects_an_inverted_time_range() {
        var query = GuardianDecisionLogQuery.builder().from(2L).to(1L).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("from must be <= to");
    }
}
