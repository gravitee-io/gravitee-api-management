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
package io.gravitee.rest.api.rest.param;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.rest.api.model.common.Sortable;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

class SortableFieldsTest {

    private final SortableFields sortableFields = new SortableFields("name", "created_at");

    @Test
    void should_sort_ascending_on_a_supported_field() {
        Sortable sortable = sortableFields.toSortable("name");

        assertThat(sortable.getField()).isEqualTo("name");
        assertThat(sortable.isAscOrder()).isTrue();
    }

    @Test
    void should_sort_descending_when_the_field_is_prefixed_with_a_minus() {
        Sortable sortable = sortableFields.toSortable("-created_at");

        assertThat(sortable.getField()).isEqualTo("created_at");
        assertThat(sortable.isAscOrder()).isFalse();
    }

    @Test
    void should_not_sort_without_parameter() {
        assertThat(sortableFields.toSortable(null)).isNull();
        assertThat(sortableFields.toSortable("")).isNull();
    }

    @Test
    void should_reject_an_unsupported_field_without_echoing_it() {
        Throwable thrown = catchThrowable(() -> sortableFields.toSortable("name) , (select 1 from users"));

        assertThat(thrown).isInstanceOf(BadRequestException.class).hasMessage("Unsupported sort field");
    }
}
