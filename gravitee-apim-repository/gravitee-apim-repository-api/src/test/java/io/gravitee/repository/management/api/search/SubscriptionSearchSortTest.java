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
package io.gravitee.repository.management.api.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import org.junit.jupiter.api.Test;

class SubscriptionSearchSortTest {

    @Test
    void should_default_to_updatedAt_for_null_sortable() {
        assertThat(SubscriptionSearchSort.fromSortable(null)).isEqualTo(SubscriptionSearchSort.UPDATED_AT);
    }

    @Test
    void should_default_to_updatedAt_for_null_field() {
        assertThat(SubscriptionSearchSort.fromSortable(new SortableBuilder().order(Order.ASC).build())).isEqualTo(
            SubscriptionSearchSort.UPDATED_AT
        );
    }

    @Test
    void should_resolve_updatedAt_field() {
        assertThat(SubscriptionSearchSort.fromSortable(new SortableBuilder().field("updatedAt").order(Order.ASC).build())).isEqualTo(
            SubscriptionSearchSort.UPDATED_AT
        );
    }

    @Test
    void should_resolve_plan_field_to_plan_id_keyset() {
        assertThat(SubscriptionSearchSort.fromSortable(new SortableBuilder().field("plan").order(Order.ASC).build())).isEqualTo(
            SubscriptionSearchSort.PLAN_ID
        );
    }

    @Test
    void should_resolve_id_field_to_id_only_keyset() {
        assertThat(SubscriptionSearchSort.fromSortable(new SortableBuilder().field("id").order(Order.ASC).build())).isEqualTo(
            SubscriptionSearchSort.ID
        );
    }

    @Test
    void should_reject_unsupported_field() {
        assertThatThrownBy(() -> SubscriptionSearchSort.fromSortable(new SortableBuilder().field("createdAt").order(Order.ASC).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("createdAt");
    }
}
