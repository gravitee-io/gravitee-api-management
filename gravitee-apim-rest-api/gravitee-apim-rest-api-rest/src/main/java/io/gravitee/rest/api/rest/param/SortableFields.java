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

import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import jakarta.ws.rs.BadRequestException;
import java.util.Set;

/**
 * The fields a listing can be sorted on. A sort parameter is a field, prefixed with {@code -} for a descending order.
 */
public record SortableFields(Set<String> fields) {
    public SortableFields(String... fields) {
        this(Set.of(fields));
    }

    /**
     * @return the sortable for this parameter, or {@code null} when the parameter is absent or empty
     * @throws BadRequestException when the field is not one of the supported fields
     */
    public Sortable toSortable(String param) {
        if (param == null || param.isEmpty()) {
            return null;
        }
        boolean isAsc = !param.startsWith("-");
        String field = isAsc ? param : param.substring(1);
        if (!fields.contains(field)) {
            throw new BadRequestException("Unsupported sort field");
        }
        return new SortableImpl(field, isAsc);
    }
}
