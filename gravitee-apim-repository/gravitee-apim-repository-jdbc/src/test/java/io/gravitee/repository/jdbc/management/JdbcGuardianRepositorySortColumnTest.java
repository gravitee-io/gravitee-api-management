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
package io.gravitee.repository.jdbc.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The sort field is the one value {@code search} puts into its SQL rather than binding as a
 * parameter, so this repository validates it itself instead of trusting its caller: the
 * {@code GuardianRepository} SPI is public and the allow-list of its only caller today is two
 * layers away.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JdbcGuardianRepositorySortColumnTest {

    @ParameterizedTest
    @ValueSource(strings = { "name", "phase", "updatedAt", "createdAt" })
    void should_accept_every_sortable_column(String field) {
        assertThat(JdbcGuardianRepository.sortColumn(field)).isNotBlank();
    }

    @Test
    void should_map_a_camel_case_field_onto_its_column() {
        assertThat(JdbcGuardianRepository.sortColumn("updatedAt")).isEqualTo("updated_at");
        assertThat(JdbcGuardianRepository.sortColumn("createdAt")).isEqualTo("created_at");
    }

    @ParameterizedTest
    // apiType is refused because the definition carries the API types as a set, which has no single
    // value to order rows by; deployedAt because every value is null until deployment lands, and the
    // backends disagree on where nulls sort
    @ValueSource(strings = { "id", "definition", "environmentId", "apiType", "deployedAt", "nawak" })
    void should_refuse_a_column_that_is_not_sortable(String field) {
        assertThatThrownBy(() -> JdbcGuardianRepository.sortColumn(field))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(field);
    }

    @ParameterizedTest
    @ValueSource(strings = { "name; drop table guardians", "name--", "(select 1)", "name, definition" })
    void should_refuse_anything_that_would_reach_the_order_by_clause(String field) {
        assertThatThrownBy(() -> JdbcGuardianRepository.sortColumn(field)).isInstanceOf(IllegalArgumentException.class);
    }
}
