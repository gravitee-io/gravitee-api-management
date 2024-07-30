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
package io.gravitee.repository.management.api.search.builder;

import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class SortableImpl implements Sortable {

    private String field;
    private Order order;

    public SortableImpl(String field, Order order) {
        this.field = field;
        this.order = order;
    }

    @Override
    public String field() {
        return field;
    }

    @Override
    public Order order() {
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortableImpl sortable = (SortableImpl) o;
        return Objects.equals(field, sortable.field) && order == sortable.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, order);
    }
}
