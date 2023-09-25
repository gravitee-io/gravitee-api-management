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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;

public class SortableAdapter {

    public static SortableAdapter INSTANCE = new SortableAdapter() {};

    private SortableAdapter() {}

    public io.gravitee.repository.management.api.search.Sortable toSortableForRepository(Sortable sortable) {
        var builder = new SortableBuilder();
        if (sortable != null) {
            builder.field(sortable.getField()).order(Order.valueOf(sortable.getOrder().name()));
        }
        return builder.build();
    }
}
