/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SortableBuilder {

    private String field;
    private Order order;

    public SortableBuilder field(String field) {
        this.field = field;
        return this;
    }

    public SortableBuilder order(Order order) {
        this.order = order;
        return this;
    }

    public SortableBuilder setAsc(boolean isAsc) {
        this.order = isAsc ? Order.ASC : Order.DESC;
        return this;
    }

    public Sortable build() {
        return new SortableImpl(field, order);
    }
}
